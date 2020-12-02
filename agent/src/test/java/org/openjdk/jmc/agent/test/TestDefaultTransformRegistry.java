/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;
import org.objectweb.asm.Type;
import org.openjdk.jmc.agent.TransformDescriptor;
import org.openjdk.jmc.agent.TransformRegistry;
import org.openjdk.jmc.agent.impl.DefaultTransformRegistry;
import org.openjdk.jmc.agent.test.util.TestToolkit;

public class TestDefaultTransformRegistry {

	private static final String XML_EVENT_DESCRIPTION = "<event id=\"demo.jfr.test1\">"
			+ "<label>JFR Hello World Event 1 %TEST_NAME% </label>"
			+ "<description>Defined in the xml file and added by the agent.</description>"
			+ "<path>demo/jfrhelloworldevent1</path>" + "<stacktrace>true</stacktrace>"
			+ "<class>org.openjdk.jmc.agent.test.InstrumentMe</class>" + "<method>" + "<name>printHelloWorldJFR1</name>"
			+ "<descriptor>()V</descriptor>" + "</method>" + "<location>WRAP</location>" + "</event>";

	public static String getTemplate() throws IOException {
		return TestToolkit.readTemplate(TestDefaultTransformRegistry.class, TestToolkit.DEFAULT_TEMPLATE_NAME);
	}

	@Test
	public void testHasPendingTransforms() throws XMLStreamException, IOException {
		TransformRegistry registry = DefaultTransformRegistry
				.from(TestToolkit.getProbesXMLFromTemplate(getTemplate(), "HasPendingTransforms")); //$NON-NLS-1$
		assertNotNull(registry);
		assertTrue(registry.hasPendingTransforms(Type.getInternalName(InstrumentMe.class)));
	}

	@Test
	public void testEmpty() {
		TransformRegistry registry = DefaultTransformRegistry.empty();
		assertNotNull(registry);
	}

	@Test
	public void testFrom() throws XMLStreamException, IOException {
		TransformRegistry registry = DefaultTransformRegistry
				.from(TestToolkit.getProbesXMLFromTemplate(getTemplate(), "From")); //$NON-NLS-1$
		assertNotNull(registry);
	}

	@Test
	public void testGetTransformData() throws XMLStreamException, IOException {
		TransformRegistry registry = DefaultTransformRegistry
				.from(TestToolkit.getProbesXMLFromTemplate(getTemplate(), "GetTransformData")); //$NON-NLS-1$
		assertNotNull(registry);
		List<TransformDescriptor> transformData = registry.getTransformData(Type.getInternalName(InstrumentMe.class));
		assertNotNull(transformData);
		assertTrue(transformData.size() > 0);
	}

	@Test
	public void testModify() throws XMLStreamException, IOException {
		TransformRegistry registry = DefaultTransformRegistry
				.from(TestToolkit.getProbesXMLFromTemplate(getTemplate(), "Modify")); //$NON-NLS-1$
		assertNotNull(registry);
		Set<String> modifiedClassNames = registry.modify(getXMLDescription(XML_EVENT_DESCRIPTION));
		assertNotNull(modifiedClassNames);
		assertTrue(modifiedClassNames.size() == 1);
		assertEquals(modifiedClassNames.iterator().next(), Type.getInternalName(InstrumentMe.class));
		assertTrue(registry.hasPendingTransforms(Type.getInternalName(InstrumentMe.class)));
	}

	@Test
	public void testModifyNameCollision() throws XMLStreamException, IOException {
		TransformRegistry registry = DefaultTransformRegistry
				.from(TestToolkit.getProbesXMLFromTemplate(getTemplate(), "Modify")); //$NON-NLS-1$
		assertNotNull(registry);
		final String collisionDescription = getXMLDescription(XML_EVENT_DESCRIPTION.concat(XML_EVENT_DESCRIPTION));
		Set<String> modifiedClassNames = registry.modify(collisionDescription);
		assertNotNull(modifiedClassNames);
		assertTrue(modifiedClassNames.size() == 1);
	}

	@Test
	public void testModifyInvalidXml() throws XMLStreamException, IOException {
		TransformRegistry registry = DefaultTransformRegistry
				.from(TestToolkit.getProbesXMLFromTemplate(getTemplate(), "Modify")); //$NON-NLS-1$
		assertNotNull(registry);
		final String initialConfiguration = registry.getCurrentConfiguration();
		final String invalidSnippet = XML_EVENT_DESCRIPTION;
		Set<String> modifiedClassNames = registry.modify(invalidSnippet);
		assertNull(modifiedClassNames);
		assertEquals(registry.getCurrentConfiguration(), initialConfiguration);
	}

	@Test
	public void testClearAllTransformData() throws XMLStreamException, IOException {
		TransformRegistry registry = DefaultTransformRegistry.from(TestToolkit
				.getProbesXMLFromTemplate(getXMLDescription(XML_EVENT_DESCRIPTION), "clearAllTransformData")); //$NON-NLS-1$
		assertNotNull(registry);
		Set<String> classesCleared = registry.clearAllTransformData();
		assertEquals(1, classesCleared.size());
		assertEquals(classesCleared.iterator().next(), Type.getInternalName(InstrumentMe.class));
		assertEquals(0, registry.getTransformData(Type.getInternalName(InstrumentMe.class)).size());
	}

	private String getXMLDescription(String eventsDescription) {
		return "<jfragent><events>".concat(eventsDescription).concat("</events></jfragent>");
	}

}
