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

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.openjdk.jmc.agent.Parameter;
import org.openjdk.jmc.agent.jfr.JFRTransformDescriptor;
import org.openjdk.jmc.agent.util.TypeUtils;

/**
 * Code emitter for JFR next, i.e. the version of JFR distributed with JDK 9 and later.
 */
public class JFRNextMethodAdvisor extends AdviceAdapter {
	private final JFRTransformDescriptor transformDescriptor;
	private final Type[] argumentTypesRef;
	private final Type returnTypeRef;
	private final Type eventType;
	private int eventLocal = -1;

	protected JFRNextMethodAdvisor(JFRTransformDescriptor transformDescriptor, int api, MethodVisitor mv, int access,
			String name, String desc) {
		super(api, mv, access, name, desc);
		this.transformDescriptor = transformDescriptor;
		// These are not accessible from the super type (made private), so must save an extra reference. :/
		this.argumentTypesRef = Type.getArgumentTypes(desc);
		this.returnTypeRef = Type.getReturnType(desc);
		this.eventType = Type.getObjectType(transformDescriptor.getEventClassName());
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
					writeParameter(param, argumentType);
				}
			}
		}

		mv.visitMethodInsn(INVOKEVIRTUAL, transformDescriptor.getEventClassName(), "begin", "()V", false); //$NON-NLS-1$ //$NON-NLS-2$
		eventLocal = newLocal(eventType);
		mv.visitVarInsn(ASTORE, eventLocal);
	}

	private void writeParameter(Parameter param, Type type) {
		if (TypeUtils.shouldStringify(param, type)) {
			TypeUtils.stringify(mv, param, type);
			type = TypeUtils.STRING_TYPE;
		}
		putField(Type.getObjectType(transformDescriptor.getEventClassName()), param.getFieldName(), type);
	}

	@Override
	protected void onMethodExit(int opcode) {
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
		writeParameter(returnParam, returnTypeRef);
	}

	private void commitEvent() {
		mv.visitVarInsn(ALOAD, eventLocal);
		mv.visitMethodInsn(INVOKEVIRTUAL, transformDescriptor.getEventClassName(), "commit", "()V", false); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
