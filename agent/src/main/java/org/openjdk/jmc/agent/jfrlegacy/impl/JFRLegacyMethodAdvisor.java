/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.agent.jfrlegacy.impl;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.openjdk.jmc.agent.Attribute;
import org.openjdk.jmc.agent.Field;
import org.openjdk.jmc.agent.Parameter;
import org.openjdk.jmc.agent.ReturnValue;
import org.openjdk.jmc.agent.jfr.JFRTransformDescriptor;
import org.openjdk.jmc.agent.util.TypeUtils;
import org.openjdk.jmc.agent.util.expression.IllegalSyntaxException;
import org.openjdk.jmc.agent.util.expression.ReferenceChain;
import org.openjdk.jmc.agent.util.expression.ReferenceChainElement;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Code emitter for JFR distributed with pre-JDK 9 releases. Probably works with JRockit too. ;)
 */
public class JFRLegacyMethodAdvisor extends AdviceAdapter {
	private static final String THROWABLE_BINARY_NAME = "java/lang/Throwable"; //$NON-NLS-1$

	private final JFRTransformDescriptor transformDescriptor;
	private final Class<?> inspectionClass;
	private final Type[] argumentTypesRef;
	private final Type returnTypeRef;
	private final Type eventType;
	private int eventLocal = -1;

	private Label tryBegin = new Label();
	private Label tryEnd = new Label();

	private boolean shouldInstrumentThrow;

	protected JFRLegacyMethodAdvisor(JFRTransformDescriptor transformDescriptor, Class<?> inspectionClass, int api,
			MethodVisitor mv, int access, String name, String desc) {
		super(api, mv, access, name, desc);
		this.transformDescriptor = transformDescriptor;
		this.inspectionClass = inspectionClass;
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
		try {
			createEvent();
		} catch (IllegalSyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private void createEvent() throws IllegalSyntaxException {
		mv.visitTypeInsn(NEW, transformDescriptor.getEventClassName());
		mv.visitInsn(DUP);
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, transformDescriptor.getEventClassName(), "<init>", "()V", false); //$NON-NLS-1$ //$NON-NLS-2$
		for (Parameter param : transformDescriptor.getParameters()) {
			Type argumentType = argumentTypesRef[param.getIndex()];
			if (transformDescriptor.isAllowedEventFieldType(param, argumentType)) {
				mv.visitInsn(DUP);
				loadArg(param.getIndex());
				writeAttribute(param, argumentType, transformDescriptor.isAllowToString());
			}
		}

		for (Field field : transformDescriptor.getFields()) {
			ReferenceChain refChain = field.resolveReferenceChain(inspectionClass).normalize();

			if (!refChain.isStatic() && Modifier.isStatic(getAccess())) {
				throw new IllegalSyntaxException(
						"Illegal non-static reference from a static context: " + field.getExpression());
			}

			if (transformDescriptor.isAllowedEventFieldType(field, refChain.getType())) {
				mv.visitInsn(DUP);
				loadField(refChain);
				writeAttribute(field, refChain.getType(), transformDescriptor.isAllowToString());
			}
		}

		mv.visitMethodInsn(INVOKEVIRTUAL, transformDescriptor.getEventClassName(), "begin", "()V", false); //$NON-NLS-1$ //$NON-NLS-2$
		eventLocal = newLocal(eventType);
		mv.visitVarInsn(ASTORE, eventLocal);
	}

	private void loadField(ReferenceChain refChain) {
		Type type = refChain.getType();
		boolean isStatic = Modifier.isStatic(getAccess());
		Label nullCase = new Label();
		Label continueCase = new Label();
		List<Object> localVarVerifications = new ArrayList<>();
		if (!isStatic) {
			localVarVerifications.add(Type.getInternalName(inspectionClass)); // "this"
		}
		for (Type argType : argumentTypesRef) {
			localVarVerifications.add(TypeUtils.getFrameVerificationType(argType));
		}

		// Assumes the reference chain is normalized already. See ReferenceChain.normalize()
		List<ReferenceChainElement> refs = refChain.getReferences();
		for (int i = 0; i < refs.size(); i++) {
			ReferenceChainElement ref = refs.get(i);

			if (ref instanceof ReferenceChainElement.ThisReference) {
				mv.visitVarInsn(ALOAD, 0); // load "this"
				continue;
			}

			if (ref instanceof ReferenceChainElement.FieldReference) {
				mv.visitFieldInsn(ref.isStatic() ? GETSTATIC : GETFIELD, ref.getMemberingType().getInternalName(),
						((ReferenceChainElement.FieldReference) ref).getName(),
						ref.getReferencedType().getDescriptor());

				// null check for field references
				if (i < refs.size() - 1) { // Skip null check for final reference. Null is acceptable here
					mv.visitInsn(DUP);
					mv.visitJumpInsn(IFNULL, nullCase);
				}

				continue;
			}

			if (ref instanceof ReferenceChainElement.QualifiedThisReference) {
				int suffix = ((ReferenceChainElement.QualifiedThisReference) ref).getDepth();
				Class<?> c = ref.getMemberingClass();
				while (!ref.getReferencedClass().equals(c)) {
					mv.visitFieldInsn(GETFIELD, Type.getType(c).getInternalName(), "this$" + (suffix--),
							Type.getType(c.getEnclosingClass()).getDescriptor());
					c = c.getEnclosingClass();
				}

				continue;
			}

			throw new UnsupportedOperationException("Unsupported reference chain element type");
		}

		// loaded a value, jump to writing attribute
		mv.visitJumpInsn(GOTO, continueCase);

		// null reference on path, load zero value
		mv.visitLabel(nullCase);
		mv.visitFrame(F_NEW, localVarVerifications.size(), localVarVerifications.toArray(), 4,
				new Object[] {eventType.getInternalName(), eventType.getInternalName(), eventType.getInternalName(),
						Type.getInternalName(Object.class)});
		mv.visitInsn(POP);
		mv.visitInsn(TypeUtils.getConstZeroOpcode(type));

		// must verify frame for jump targets
		mv.visitLabel(continueCase);
		mv.visitFrame(F_NEW, localVarVerifications.size(), localVarVerifications.toArray(), 4,
				new Object[] {eventType.getInternalName(), eventType.getInternalName(), eventType.getInternalName(),
						TypeUtils.getFrameVerificationType(type)});
	}

	private void writeAttribute(Attribute param, Type type, boolean allowToString) {
		if (!TypeUtils.isSupportedType(type) && allowToString) {
			TypeUtils.stringify(mv);
			type = TypeUtils.TYPE_STRING;
		}
		putField(Type.getObjectType(transformDescriptor.getEventClassName()), param.getFieldName(), type);
	}

	@Override
	protected void onMethodExit(int opcode) {
		if (opcode == ATHROW && !shouldInstrumentThrow) {
			return;
		}

		if (returnTypeRef.getSort() != Type.VOID && opcode != ATHROW) {
			ReturnValue returnValue = transformDescriptor.getReturnValue();
			if (returnValue != null) {
				emitSettingReturnParam(opcode, returnValue);
			}
		}
		commitEvent();
	}

	private void emitSettingReturnParam(int opcode, ReturnValue returnValue) {
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
		writeAttribute(returnValue, returnTypeRef, transformDescriptor.isAllowToString());
	}

	private void commitEvent() {
		mv.visitVarInsn(ALOAD, eventLocal);
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKEVIRTUAL, transformDescriptor.getEventClassName(), "end", "()V", false); //$NON-NLS-1$ //$NON-NLS-2$
		mv.visitMethodInsn(INVOKEVIRTUAL, transformDescriptor.getEventClassName(), "commit", "()V", false); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
