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
package org.openjdk.jmc.agent.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.instrument.IllegalClassFormatException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import org.openjdk.jmc.agent.Agent;
import org.openjdk.jmc.agent.TransformRegistry;
import org.openjdk.jmc.agent.Transformer;
import org.openjdk.jmc.agent.impl.DefaultTransformRegistry;
import org.openjdk.jmc.agent.test.util.TestToolkit;

public class TestJFRTransformer {
	private static AtomicInteger runCount = new AtomicInteger(0);

	@Test
	public void testRunTransforms() throws XMLStreamException, IllegalClassFormatException, IOException {
		TransformRegistry registry = DefaultTransformRegistry
				.from(TestToolkit.getProbesXML("RunTransforms" + runCount.getAndIncrement())); //$NON-NLS-1$

		assertTrue(registry.hasPendingTransforms(Type.getInternalName(InstrumentMe.class)));

		Transformer jfrTransformer = new Transformer(registry);
		byte[] transformedClass = jfrTransformer.transform(InstrumentMe.class.getClassLoader(),
				Type.getInternalName(InstrumentMe.class), InstrumentMe.class, null,
				TestToolkit.getByteCode(InstrumentMe.class));

		assertNotNull(transformedClass);
		assertFalse(registry.hasPendingTransforms(Type.getInternalName(InstrumentMe.class)));

		if (Agent.getLogger().isLoggable(Level.FINE)) {
			// If we've asked for verbose information, we write the generated class
			// and also dump the registry contents to stdout.
			TraceClassVisitor visitor = new TraceClassVisitor(new PrintWriter(System.out));
			CheckClassAdapter checkAdapter = new CheckClassAdapter(visitor);
			ClassReader reader = new ClassReader(transformedClass);
			reader.accept(checkAdapter, 0);
			System.out.println(registry);
		}
	}
}
