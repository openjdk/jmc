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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openjdk.jmc.agent.Agent;
import org.openjdk.jmc.agent.collections.CollectionTransformDescriptor;

/**
 * Class visitor that finds the single resize method described by a
 * {@link CollectionTransformDescriptor} and wraps it in a {@link CollectionResizeAdvisor}.
 */
public class CollectionResizeClassVisitor extends ClassVisitor {

	private final CollectionTransformDescriptor descriptor;
	private boolean matchFound;

	public CollectionResizeClassVisitor(ClassVisitor classVisitor, CollectionTransformDescriptor descriptor) {
		super(Opcodes.ASM9, classVisitor);
		this.descriptor = descriptor;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (name.equals(descriptor.getMethod().getName()) && desc.equals(descriptor.getMethod().getSignature())) {
			matchFound = true;
			return new CollectionResizeAdvisor(mv, access, name, desc, descriptor.getClassName(),
					descriptor.getArrayFieldName(), descriptor.getArrayFieldDescriptor(),
					descriptor.getSizeAccessorName(), descriptor.isSizeMethod());
		}
		return mv;
	}

	@Override
	public void visitEnd() {
		if (!matchFound) {
			// The baked-in target is a JDK-private method; if a JDK update renames or re-signatures
			// it, surface that instead of silently emitting no events for this collection.
			Agent.getLogger().warning("Collection tracking: method " + descriptor.getMethod().getName() //$NON-NLS-1$
					+ descriptor.getMethod().getSignature() + " not found in " + descriptor.getClassName() //$NON-NLS-1$
					+ "; no resize events will be emitted for this collection type."); //$NON-NLS-1$
		}
		super.visitEnd();
	}
}
