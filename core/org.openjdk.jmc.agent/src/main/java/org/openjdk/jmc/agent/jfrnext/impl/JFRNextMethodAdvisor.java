/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmc.agent.jfrnext.impl;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.openjdk.jmc.agent.IAttribute;
import org.openjdk.jmc.agent.Parameter;
import org.openjdk.jmc.agent.Watch;
import org.openjdk.jmc.agent.jfr.JFRTransformDescriptor;
import org.openjdk.jmc.agent.util.TypeUtils;
import org.openjdk.jmc.agent.util.expression.FieldReference;
import org.openjdk.jmc.agent.util.expression.IllegalSyntaxException;
import org.openjdk.jmc.agent.util.expression.ReferenceChain;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Code emitter for JFR next, i.e. the version of JFR distributed with JDK 9 and later.
 */
public class JFRNextMethodAdvisor extends AdviceAdapter {
	private static final String THROWABLE_BINARY_NAME = "java/lang/Throwable"; //$NON-NLS-1$

	private final JFRTransformDescriptor transformDescriptor;
	private final Class<?> classBeingRedefined;
	private final Type[] argumentTypesRef;
	private final Type returnTypeRef;
	private final Type eventType;
	private int eventLocal = -1;

	private Label tryBegin = new Label();
	private Label tryEnd = new Label();

	private boolean shouldInstrumentThrow;

	protected JFRNextMethodAdvisor(JFRTransformDescriptor transformDescriptor, Class<?> classBeingRedefined, int api, MethodVisitor mv, int access,
								   String name, String desc) {
		super(api, mv, access, name, desc);
		this.transformDescriptor = transformDescriptor;
		this.classBeingRedefined = classBeingRedefined;
		// These are not accessible from the super type (made private), so must save an extra reference. :/
		this.argumentTypesRef = Type.getArgumentTypes(desc);
		this.returnTypeRef = Type.getReturnType(desc);
		this.eventType = Type.getObjectType(transformDescriptor.getEventClassName());

		this.shouldInstrumentThrow = !transformDescriptor.isUseRethrow(); // don't instrument inner throws if rethrow is enabled
	}

	@Override
	public void visitCode() {
		super.visitCode();

		if (transformDescriptor.isUseRethrow()) {
			visitLabel(tryBegin);
		}
	}

	@Override
	public void visitEnd() {
		if (transformDescriptor.isUseRethrow()) {
			visitLabel(tryEnd);
			visitTryCatchBlock(tryBegin, tryEnd, tryEnd, THROWABLE_BINARY_NAME);

			visitFrame(Opcodes.F_NEW, 0, null, 1, new Object[] {THROWABLE_BINARY_NAME});

			// Simply rethrow. Event commits are instrumented by onMethodExit()
			shouldInstrumentThrow = true;
			visitInsn(ATHROW);
		}

		super.visitEnd();
	}

	@Override
	protected void onMethodEnter() {
		createEvent();
	}

	private void createEvent() {
		mv.visitTypeInsn(NEW, transformDescriptor.getEventClassName());
		mv.visitInsn(DUP);
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, transformDescriptor.getEventClassName(), "<init>", "()V", false); //$NON-NLS-1$ //$NON-NLS-2$
		for (Parameter param : transformDescriptor.getParameters()) {
			if (!param.isReturn()) {
				Type argumentType = argumentTypesRef[param.getIndex()];
				if (transformDescriptor.isAllowedFieldType(argumentType)) {
					mv.visitInsn(DUP);
					loadArg(param.getIndex());
					writeAttribute(param, argumentType);
				}
			}
		}

		for (Watch watch : transformDescriptor.getWatches()) {
			ReferenceChain refChain;
			try {
				refChain = watch.resolveReferenceChain(classBeingRedefined, Modifier.isStatic(getAccess())).normalize();
			} catch (IllegalSyntaxException e) {
                throw new RuntimeException(e); // TODO: figure out what to do with this error
            }
            if (transformDescriptor.isAllowedFieldType(refChain.getType())) {
				mv.visitInsn(DUP);
				loadWatch(watch, refChain);
				writeAttribute(watch, refChain.getType());
			}
		}

		mv.visitMethodInsn(INVOKEVIRTUAL, transformDescriptor.getEventClassName(), "begin", "()V", false); //$NON-NLS-1$ //$NON-NLS-2$
		eventLocal = newLocal(eventType);
		mv.visitVarInsn(ASTORE, eventLocal);
	}

	private void loadWatch(Watch watch, ReferenceChain refChain) {
		Type type = refChain.getType();
		boolean isStatic = Modifier.isStatic(getAccess());
		Label nullCase = new Label();
		Label continueCase = new Label();
		List<Object> localVarVerifications = new ArrayList<>();
		if (!isStatic) {
			localVarVerifications.add(Type.getInternalName(classBeingRedefined)); // "this"
		}
		for (Type argType : argumentTypesRef) {
			localVarVerifications.add(TypeUtils.getFrameVerificationType(argType));
		}

		// Assumes the reference chain is normalized already. See ReferenceChain.normalize()
		List<FieldReference> refs = refChain.getReferences();
		for (int i = 0; i < refs.size(); i++) {
			FieldReference ref = refs.get(i);
			if (ref instanceof FieldReference.ThisReference) {
				mv.visitVarInsn(ALOAD, 0); // load "this"
			} else {
				mv.visitFieldInsn(Modifier.isStatic(ref.getModifiers()) ? GETSTATIC : GETFIELD,
						ref.getMemberingType().getInternalName(),
						ref.getName(), ref.getType().getDescriptor());
			}

			// null check for field references
			if (!(ref instanceof FieldReference.ThisReference) && !(ref instanceof FieldReference.QualifiedThisReference) && i != refs.size() - 1) {
				mv.visitInsn(DUP);
				mv.visitJumpInsn(IFNULL, nullCase);
			}
		}
		// loaded value, jump to writing attribute
		mv.visitJumpInsn(GOTO, continueCase);

		// null reference on path, load zero value
		mv.visitLabel(nullCase);
		mv.visitFrame(F_NEW,
				localVarVerifications.size(),
				localVarVerifications.toArray(),
				4,
				new Object[]{eventType.getInternalName(), eventType.getInternalName(), eventType.getInternalName(), Type.getInternalName(Object.class)});
		mv.visitInsn(POP);
		mv.visitInsn(TypeUtils.getConstZeroOpcode(type));

		// must verify frame for jump targets
		mv.visitLabel(continueCase);
		mv.visitFrame(F_NEW,
				localVarVerifications.size(),
				localVarVerifications.toArray(),
				4, 
				new Object[]{eventType.getInternalName(), eventType.getInternalName(), eventType.getInternalName(), TypeUtils.getFrameVerificationType(type)});
	}

	private void writeAttribute(IAttribute param, Type type) {
		if (TypeUtils.shouldStringify(param, type)) {
			TypeUtils.stringify(mv, param, type);
			type = TypeUtils.STRING_TYPE;
		}
		putField(Type.getObjectType(transformDescriptor.getEventClassName()), param.getFieldName(), type);
	}

	@Override
	protected void onMethodExit(int opcode) {
		if (opcode == ATHROW && !shouldInstrumentThrow) {
			return;
		}

		if (returnTypeRef.getSort() != Type.VOID && opcode != ATHROW) {
			Parameter returnParam = TypeUtils.findReturnParam(transformDescriptor.getParameters());
			if (returnParam != null) {
				emitSettingReturnParam(opcode, returnParam);
			}
		}
		commitEvent();
	}

	private void emitSettingReturnParam(int opcode, Parameter returnParam) {
		if (returnTypeRef.getSize() == 1) {
			dup();
			mv.visitVarInsn(ALOAD, eventLocal);
			swap();
		} else {
			dup2();
			mv.visitVarInsn(ALOAD, eventLocal);
			dupX2();
			pop();
		}
		writeAttribute(returnParam, returnTypeRef);
	}

	private void commitEvent() {
		mv.visitVarInsn(ALOAD, eventLocal);
		mv.visitMethodInsn(INVOKEVIRTUAL, transformDescriptor.getEventClassName(), "commit", "()V", false); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
		// force to always use expanded frames
		super.visitFrame(Opcodes.F_NEW, numLocal, local, numStack, stack);
	}
}
