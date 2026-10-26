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
package org.openjdk.jmc.agent.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openjdk.jmc.agent.util.TypeUtils;

/**
 * Verifies that a class can be defined directly into the <em>bootstrap</em> class loader via
 * {@code Unsafe.defineClass} (by passing a {@code null} defining loader). This is the mechanism
 * that lets bootstrap-loaded classes such as {@code java.util.HashMap} resolve and call our
 * injected event bridge, without needing a bootstrap jar /
 * {@code appendToBootstrapClassLoaderSearch} (which would break the dynamic-attach flow).
 */
public class TestBootstrapDefine {

	private static final String PROBE_CLASS_NAME = "org.openjdk.jmc.agent.test.bootstrap.BootstrapProbe"; //$NON-NLS-1$

	@Test
	public void testDefineIntoBootstrapLoader() throws Exception {
		byte[] bytes = generateProbeClass();

		// Passing null as the defining class loader must place the class in the bootstrap loader.
		Class<?> defined = TypeUtils.defineClass(PROBE_CLASS_NAME, bytes, 0, bytes.length, null, null);

		assertNotNull("defineClass returned null - Unsafe.defineClass(null loader) not supported here", defined); //$NON-NLS-1$
		assertNull("Class was not defined in the bootstrap loader (getClassLoader() != null)", //$NON-NLS-1$
				defined.getClassLoader());

		int result = (int) defined.getMethod("probe").invoke(null); //$NON-NLS-1$
		assertEquals(42, result);
	}

	/**
	 * Generates a trivial class with a single {@code public static int probe()} that returns 42.
	 */
	private static byte[] generateProbeClass() {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, PROBE_CLASS_NAME.replace('.', '/'), null,
				"java/lang/Object", null); //$NON-NLS-1$

		MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "probe", "()I", null, null); //$NON-NLS-1$ //$NON-NLS-2$
		mv.visitCode();
		mv.visitIntInsn(Opcodes.BIPUSH, 42);
		mv.visitInsn(Opcodes.IRETURN);
		mv.visitMaxs(1, 0);
		mv.visitEnd();

		cw.visitEnd();
		return cw.toByteArray();
	}
}
