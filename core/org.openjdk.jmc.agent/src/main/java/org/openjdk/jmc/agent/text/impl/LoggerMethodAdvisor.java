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
package org.openjdk.jmc.agent.text.impl;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openjdk.jmc.agent.util.TypeUtils;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Text logging code emitter.
 */
// FIXME: Use j.u.l. instead and let people configure severity?
public class LoggerMethodAdvisor extends AdviceAdapter {
	private final TextTransformDescriptor transformDescriptor;

	private final Type[] argumentTypesRef;
	private final Type returnTypeRef;
	private final int maxRequestedMessageParamIndex;
	private final int maxEnterMessageParamIndex;
	private final int maxExitMessageParamIndex;

	private boolean isValid = true;
	private int localVariableIndexTime = -1;
	private int localVariableIndexObjectArray = -1;

	protected LoggerMethodAdvisor(TextTransformDescriptor transformDescriptor, int api, MethodVisitor mv, int access,
			String name, String desc) {
		super(api, mv, access, name, desc);
		this.transformDescriptor = transformDescriptor;
		// This is not accessible from the super type (private), so must save an extra reference for us. :/
		this.argumentTypesRef = Type.getArgumentTypes(desc);
		this.returnTypeRef = Type.getReturnType(desc);

		maxEnterMessageParamIndex = parseHighestParamIndex(transformDescriptor.getEnterMessage());
		maxExitMessageParamIndex = parseHighestParamIndex(transformDescriptor.getExitMessage());
		maxRequestedMessageParamIndex = Math.max(maxEnterMessageParamIndex, maxExitMessageParamIndex);
		validateParamIndex();
	}

	@Override
	protected void onMethodEnter() {
		super.onMethodEnter();
		if (!isValid) {
			return;
		}
		if (useMessageParams()) {
			emitParamArray();
		}
		String message = transformDescriptor.getEnterMessage();
		if (message != null) {
			if (maxEnterMessageParamIndex != -1) {
				emitMessage(message);
			} else {
				emitPlainMessage(message);
			}
		}

		if (useTiming()) {
			emitTimingPrologue();
		}
	}

	@Override
	protected void onMethodExit(int opcode) {
		super.onMethodExit(opcode);
		if (!isValid) {
			return;
		}
		if (useTiming()) {
			emitTimingEpilogue();
		}
		if (useReturnValueOrTiming()) {
			emitAddReturnValueAndTiming(opcode);
		}
		String message = transformDescriptor.getExitMessage();
		if (maxExitMessageParamIndex != -1) {
			emitMessage(message);
		} else {
			emitPlainMessage(message);
		}
	}

	private void emitPlainMessage(String message) {
		// FIXME: Could make it configurable to emit to stdout, stderr and j.u.l
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		mv.visitLdcInsn(message);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private void emitMessage(String message) {
		// FIXME: Could make it configurable to emit to stdout, stderr and j.u.l
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		mv.visitLdcInsn(message);
		mv.visitVarInsn(ALOAD, localVariableIndexObjectArray);
		mv.visitMethodInsn(INVOKESTATIC, "java/text/MessageFormat", "format", //$NON-NLS-1$ //$NON-NLS-2$
				"(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false); //$NON-NLS-1$
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Emits code for creating the parameter array to use as argument to the formatter. Will copy
	 * arguments as necessary. Operand stack will be unchanged after, and the reference located in
	 * local variable objectArrayLocalVariableIndex.
	 */
	private void emitParamArray() {
		int size = maxRequestedMessageParamIndex + (useTiming() ? 1 : 0);
		push(size);
		newArray(TypeUtils.OBJECT_TYPE);

		for (int i = 0; i < Math.min(argumentTypesRef.length, maxRequestedMessageParamIndex + 1); i++) {
			dup();
			push(i);
			loadArg(i);
			convert(argumentTypesRef[i]);
			arrayStore(TypeUtils.OBJECT_TYPE);
		}
		localVariableIndexObjectArray = newLocal(TypeUtils.OBJECT_ARRAY_TYPE);
		storeLocal(localVariableIndexObjectArray);
	}

	private void convert(Type type) {
		// Typically just box the value, but for arrays we want to do Arrays.toString
		// and for the Objects other than String we simply want to do toString.
		if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
			mv.visitMethodInsn(INVOKESTATIC, TypeUtils.INAME, "toString", "(Ljava/lang/Object;)Ljava/lang/String;", //$NON-NLS-1$ //$NON-NLS-2$
					false);
		} else {
			box(type);
		}
	}

	private void emitAddReturnValueAndTiming(int opcode) {
		if (hasReturnValue()) {
			emitAddReturnValue(opcode);
		}
		if (useTiming()) {
			emitAddTiming();
		}
	}

	private void emitAddTiming() {
		mv.visitVarInsn(ALOAD, localVariableIndexObjectArray);
		push(maxExitMessageParamIndex);
		mv.visitVarInsn(LLOAD, localVariableIndexTime);
		box(Type.LONG_TYPE);
		mv.visitInsn(AASTORE);
	}

	private void emitAddReturnValue(int opcode) {
		// Create a copy of the return value, or null if we're throwing an error.
		if (opcode == ATHROW) {
			mv.visitInsn(ACONST_NULL);
		} else if (returnTypeRef.getSize() == 2) {
			mv.visitInsn(DUP2);
		} else {
			mv.visitInsn(DUP);
		}
		if (opcode != ATHROW) {
			box(returnTypeRef);
		}
		mv.visitVarInsn(ALOAD, localVariableIndexObjectArray);
		// Swaps to bubble the return value reference to the top of the stack for the array store...
		swap();
		push(maxExitMessageParamIndex - 1);
		swap();
		mv.visitInsn(AASTORE);
	}

	private boolean useReturnValueOrTiming() {
		return (calculateHighestExitParamIndex() - maxExitMessageParamIndex) < 2;
	}

	private void emitTimingEpilogue() {
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		mv.visitVarInsn(LLOAD, localVariableIndexTime);
		mv.visitInsn(LSUB);
		mv.visitVarInsn(LSTORE, localVariableIndexTime);
	}

	private void emitTimingPrologue() {
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		localVariableIndexTime = newLocal(Type.LONG_TYPE);
		mv.visitVarInsn(LSTORE, localVariableIndexTime);
	}

	private void validateParamIndex() {
		if (maxEnterMessageParamIndex != -1 && maxEnterMessageParamIndex > calculateHighestEnterParamIndex()) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
					"Warning, the method entry instrumentation message for " + transformDescriptor //$NON-NLS-1$
							+ " specifies the use of more message variables than parameter available. Will not transform!"); //$NON-NLS-1$
			isValid = false;
		}
		if (maxExitMessageParamIndex != -1 && maxExitMessageParamIndex > calculateHighestExitParamIndex()) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
					"Warning, the method exit instrumentation message for " + transformDescriptor //$NON-NLS-1$
							+ " specifies the use of more message variables than available. Will not transform!"); //$NON-NLS-1$
			isValid = false;
		}
	}

	private int calculateHighestExitParamIndex() {
		int count = calculateHighestEnterParamIndex();
		if (hasReturnValue()) {
			count++;
		}
		return ++count; // method timing information;
	}

	private boolean hasReturnValue() {
		return this.returnTypeRef.getSort() != Type.VOID;
	}

	private int calculateHighestEnterParamIndex() {
		return this.argumentTypesRef.length - 1;
	}

	private int parseHighestParamIndex(String message) {
		// FIXME: Hard code faster lookup of these for performance?
		// FIXME: This does not take into account escaped curly braces with numbers in them, and allows for all kinds of odd expressions.
		int max = -1;
		if (message != null) {
			Pattern pattern = Pattern.compile("\\{(\\d+)[,.#a-zA-Z]*\\}"); //$NON-NLS-1$
			Matcher matcher = pattern.matcher(message);
			while (matcher.find()) {
				max = Math.max(max, Integer.parseInt(matcher.group(1)));
			}
		}
		return max;
	}

	private boolean useTiming() {
		return maxRequestedMessageParamIndex == calculateHighestExitParamIndex();
	}

	private boolean useMessageParams() {
		return maxRequestedMessageParamIndex >= 0;
	}
}
