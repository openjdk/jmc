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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.text.MessageFormat;
import java.util.List;

import org.junit.Test;
import org.objectweb.asm.Type;
import org.openjdk.jmc.agent.TransformDescriptor;
import org.openjdk.jmc.agent.TransformRegistry;
import org.openjdk.jmc.agent.Transformer;
import org.openjdk.jmc.agent.impl.DefaultTransformRegistry;
import org.openjdk.jmc.agent.jfr.JFRTransformDescriptor;
import org.openjdk.jmc.agent.test.util.TestToolkit;

public class TestIncorrectMethodDescriptor {

	private static final String EVENT_ID = "demo.jfr.test6";
	private static final String CORRECT_EVENT_LABEL = "JFR Hello World Event 6 %TEST_NAME%";
	private static final String INCORRECT_EVENT_LABEL = "JFR Hello World Event 6 Incorrect %TEST_NAME%";
	private static final String EVENT_DESCRIPTION = "JFR Hello World Event 6 %TEST_NAME%";
	private static final String EVENT_PATH = "demo/jfrhelloworldevent6";
	private static final String EVENT_CLASS_NAME = "org.openjdk.jmc.agent.test.InstrumentMe";
	private static final String METHOD_NAME = "printHelloWorldJFR6";
	private static final String CORRECT_METHOD_DESCRIPTOR = "()D";
	private static final String INCORRECT_METHOD_DESCRIPTOR = "()Z";

	private static final String XML_DESCRIPTION = "<jfragent>" + "<events>" + "<event id=\"" + EVENT_ID + "\">"
			+ "<label>{0}</label>" + "<description>" + EVENT_DESCRIPTION + "</description>" + "<path>" + EVENT_PATH
			+ "</path>" + "<stacktrace>true</stacktrace>" + "<class>" + EVENT_CLASS_NAME + "</class>" + "<method>"
			+ "<name>" + METHOD_NAME + "</name>" + "<descriptor>{1}</descriptor>" + "</method>"
			+ "<location>WRAP</location>" + "</event>" + "</events>" + "</jfragent>";

	@Test
	public void testCorrectMethodDescriptor() throws Exception {
		String xmlDescription = MessageFormat.format(XML_DESCRIPTION, CORRECT_EVENT_LABEL, CORRECT_METHOD_DESCRIPTOR);

		TransformRegistry registry = DefaultTransformRegistry.from(new ByteArrayInputStream(xmlDescription.getBytes()));
		assertTrue(registry.hasPendingTransforms(Type.getInternalName(InstrumentMe.class)));

		Transformer jfrTransformer = new Transformer(registry);
		jfrTransformer.transform(InstrumentMe.class.getClassLoader(), Type.getInternalName(InstrumentMe.class),
				InstrumentMe.class, null, TestToolkit.getByteCode(InstrumentMe.class));

		List<TransformDescriptor> descriptors = registry.getTransformData(Type.getInternalName(InstrumentMe.class));
		assertEquals(descriptors.size(), 1);

		JFRTransformDescriptor descriptor = (JFRTransformDescriptor) descriptors.get(0);
		assertTrue(descriptor.isMatchFound());
	}

	@Test
	public void testIncorrectMethodDescriptor() throws Exception {
		String xmlDescription = MessageFormat.format(XML_DESCRIPTION, INCORRECT_EVENT_LABEL,
				INCORRECT_METHOD_DESCRIPTOR);

		TransformRegistry registry = DefaultTransformRegistry.from(new ByteArrayInputStream(xmlDescription.getBytes()));
		assertTrue(registry.hasPendingTransforms(Type.getInternalName(InstrumentMe.class)));

		Transformer jfrTransformer = new Transformer(registry);
		jfrTransformer.transform(InstrumentMe.class.getClassLoader(), Type.getInternalName(InstrumentMe.class),
				InstrumentMe.class, null, TestToolkit.getByteCode(InstrumentMe.class));

		List<TransformDescriptor> descriptors = registry.getTransformData(Type.getInternalName(InstrumentMe.class));
		assertEquals(descriptors.size(), 1);
		JFRTransformDescriptor descriptor = (JFRTransformDescriptor) descriptors.get(0);

		assertFalse(descriptor.isMatchFound());
	}

}
