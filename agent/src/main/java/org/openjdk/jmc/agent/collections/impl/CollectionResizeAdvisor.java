/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.agent.collections.impl;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Weaves the resize event call into a single resize method (e.g. {@code HashMap.resize()} or
 * {@code ArrayList.grow(int)}).
 * <p>
 * Entry stashes the old backing-array reference in a fresh local; the normal exit calls the
 * bootstrap {@code CollectionEventBridge} with the collection, its size and the old and new arrays.
 * The emitter computes the capacities (null-safe), so the woven code stays branch-free and
 * stack-neutral - it never disturbs the return value.
 */
public class CollectionResizeAdvisor extends AdviceAdapter {

	private static final String BRIDGE_CLASS = "org/openjdk/jmc/agent/collections/bootstrap/CollectionEventBridge"; //$NON-NLS-1$
	private static final String BRIDGE_METHOD = "onResize"; //$NON-NLS-1$
	private static final String BRIDGE_METHOD_DESCRIPTOR = "(Ljava/lang/Object;JLjava/lang/Object;Ljava/lang/Object;)V"; //$NON-NLS-1$
	private static final String INT_DESCRIPTOR = "I"; //$NON-NLS-1$
	private static final String INT_METHOD_DESCRIPTOR = "()I"; //$NON-NLS-1$

	private final String ownerClass;
	private final String arrayFieldName;
	private final String arrayFieldDescriptor;
	private final String sizeAccessorName;
	private final boolean sizeIsMethod;

	private int oldArrayLocal = -1;

	protected CollectionResizeAdvisor(MethodVisitor mv, int access, String name, String desc, String ownerClass,
			String arrayFieldName, String arrayFieldDescriptor, String sizeAccessorName, boolean sizeIsMethod) {
		super(Opcodes.ASM9, mv, access, name, desc);
		this.ownerClass = ownerClass;
		this.arrayFieldName = arrayFieldName;
		this.arrayFieldDescriptor = arrayFieldDescriptor;
		this.sizeAccessorName = sizeAccessorName;
		this.sizeIsMethod = sizeIsMethod;
	}

	@Override
	protected void onMethodEnter() {
		// Object oldArray = this.<arrayField>;
		oldArrayLocal = newLocal(Type.getType(arrayFieldDescriptor));
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, ownerClass, arrayFieldName, arrayFieldDescriptor);
		mv.visitVarInsn(ASTORE, oldArrayLocal);
	}

	@Override
	protected void onMethodExit(int opcode) {
		if (opcode == ATHROW) {
			// Skip abnormal (exception) exits.
			return;
		}
		// CollectionEventBridge.onResize(this, <size>, oldArray, this.<arrayField>);
		mv.visitVarInsn(ALOAD, 0);
		loadSize();
		mv.visitVarInsn(ALOAD, oldArrayLocal);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, ownerClass, arrayFieldName, arrayFieldDescriptor);
		mv.visitMethodInsn(INVOKESTATIC, BRIDGE_CLASS, BRIDGE_METHOD, BRIDGE_METHOD_DESCRIPTOR, false);
	}

	private void loadSize() {
		mv.visitVarInsn(ALOAD, 0);
		if (sizeIsMethod) {
			mv.visitMethodInsn(INVOKEVIRTUAL, ownerClass, sizeAccessorName, INT_METHOD_DESCRIPTOR, false);
		} else {
			mv.visitFieldInsn(GETFIELD, ownerClass, sizeAccessorName, INT_DESCRIPTOR);
		}
		// Widen the int size to the event's long.
		mv.visitInsn(I2L);
	}
}
