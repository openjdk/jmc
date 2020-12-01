/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 Red Hat Inc. All rights reserved.
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

package org.openjdk.jmc.agent.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import org.openjdk.jmc.agent.TransformRegistry;
import org.openjdk.jmc.agent.Transformer;
import org.openjdk.jmc.agent.impl.DefaultTransformRegistry;
import org.openjdk.jmc.agent.test.util.TestToolkit;

public class TestEmitOnlyOnException {

	private static final String EVENT_ID = "demo.jfr.test";
	private static final String EVENT_LABEL = "JFR Emit on Exception Event %TEST_NAME%";
	private static final String EVENT_DESCRIPTION = "JFR Emit on Exception Event %TEST_NAME%";
	private static final String EVENT_PATH = "demo/emitonexceptionevent";
	private static final String EVENT_CLASS_NAME = "org.openjdk.jmc.agent.test.TestDummy";
	private static final String METHOD_NAME = "testWithException";
	private static final String METHOD_NAME_2 = "testWithoutException";
	private static final String METHOD_DESCRIPTOR = "()V";

	private static final String XML_DESCRIPTION = "<jfragent>" + "<config>" + "<emitonexception>true</emitonexception>"
			+ "</config>" + "<events>" + "<event id=\"" + EVENT_ID + "\">" + "<label>" + EVENT_LABEL + "</label>"
			+ "<description>" + EVENT_DESCRIPTION + "</description>" + "<path>" + EVENT_PATH + "</path>"
			+ "<stacktrace>true</stacktrace>" + "<class>" + EVENT_CLASS_NAME + "</class>" + "<method>" + "<name>"
			+ METHOD_NAME + "</name>" + "<descriptor>" + METHOD_DESCRIPTOR + "</descriptor>" + "</method>"
			+ "<location>WRAP</location>" + "</event>" + "<event id=\"" + EVENT_ID + "2" + "\">" + "<label>"
			+ EVENT_LABEL + "2" + "</label>" + "<description>" + EVENT_DESCRIPTION + "2" + "</description>" + "<path>"
			+ EVENT_PATH + "</path>" + "<stacktrace>true</stacktrace>" + "<class>" + EVENT_CLASS_NAME + "</class>"
			+ "<method>" + "<name>" + METHOD_NAME_2 + "</name>" + "<descriptor>" + METHOD_DESCRIPTOR + "</descriptor>"
			+ "</method>" + "<location>WRAP</location>" + "</event>" + "</events>" + "</jfragent>";

	@Test
	public void testEmitOnException() throws Exception {
		TestDummy t = new TestDummy();
		TransformRegistry registry = DefaultTransformRegistry
				.from(new ByteArrayInputStream(XML_DESCRIPTION.getBytes())); //$NON-NLS-1$
		assertTrue(registry.hasPendingTransforms(Type.getInternalName(TestDummy.class)));

		Transformer jfrTransformer = new Transformer(registry);
		byte[] transformedClass = jfrTransformer.transform(TestDummy.class.getClassLoader(),
				Type.getInternalName(TestDummy.class), TestDummy.class, null, TestToolkit.getByteCode(TestDummy.class));

		assertNotNull(transformedClass);
		try {
			t.testWithoutException();
			t.testWithException();
		} catch (Exception e) {
		}
	}

	public void dumpByteCode(byte[] transformedClass) throws IOException {
		// If we've asked for verbose information, we write the generated class
		// and also dump the registry contents to stdout.
		TraceClassVisitor visitor = new TraceClassVisitor(new PrintWriter(System.out));
		CheckClassAdapter checkAdapter = new CheckClassAdapter(visitor);
		ClassReader reader = new ClassReader(transformedClass);
		reader.accept(checkAdapter, 0);
	}
}
