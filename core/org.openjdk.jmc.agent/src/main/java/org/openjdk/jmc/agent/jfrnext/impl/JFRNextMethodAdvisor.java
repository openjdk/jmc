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
import org.openjdk.jmc.agent.Parameter;
import org.openjdk.jmc.agent.ReturnValue;
import org.openjdk.jmc.agent.jfr.JFRTransformDescriptor;
import org.openjdk.jmc.agent.util.TypeUtils;

/**
 * Code emitter for JFR next, i.e. the version of JFR distributed with JDK 9 and later.
 */
public class JFRNextMethodAdvisor extends AdviceAdapter {
	private static final String THROWABLE_BINARY_NAME = "java/lang/Throwable"; //$NON-NLS-1$

	private final JFRTransformDescriptor transformDescriptor;
	private final Type[] argumentTypesRef;
	private final Type returnTypeRef;
	private final Type eventType;
	private int eventLocal = -1;

	private Label tryBegin = new Label();
	private Label tryEnd = new Label();

	private boolean shouldInstrumentThrow;

	protected JFRNextMethodAdvisor(JFRTransformDescriptor transformDescriptor, int api, MethodVisitor mv, int access,
			String name, String desc) {
		super(api, mv, access, name, desc);
		this.transformDescriptor = transformDescriptor;
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
			Type argumentType = argumentTypesRef[param.getIndex()];
			if (transformDescriptor.isAllowedFieldType(argumentType)) {
				mv.visitInsn(DUP);
				loadArg(param.getIndex());
				writeParameter(param, argumentType);
			}
		}

		mv.visitMethodInsn(INVOKEVIRTUAL, transformDescriptor.getEventClassName(), "begin", "()V", false); //$NON-NLS-1$ //$NON-NLS-2$
		eventLocal = newLocal(eventType);
		mv.visitVarInsn(ASTORE, eventLocal);
	}

	private void writeParameter(Parameter param, Type type) {
		if (TypeUtils.shouldStringify(type)) {
			TypeUtils.stringify(mv);
			type = TypeUtils.STRING_TYPE;
		}
		putField(Type.getObjectType(transformDescriptor.getEventClassName()), param.getFieldName(), type);
	}

	private void writeReturnValue(ReturnValue returnValue, Type type) {
		if (TypeUtils.shouldStringify(type)) {
			TypeUtils.stringify(mv);
			type = TypeUtils.STRING_TYPE;
		}
		putField(Type.getObjectType(transformDescriptor.getEventClassName()), returnValue.getFieldName(), type);
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
		writeReturnValue(returnValue, returnTypeRef);
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
