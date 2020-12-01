/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Red Hat Inc. All rights reserved.
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

import org.junit.Test;
import org.openjdk.jmc.agent.impl.DefaultTransformRegistry;

import javax.xml.stream.XMLStreamException;
import java.text.MessageFormat;
import java.util.Arrays;

public class TestProbeDefinitionValidation {
	private final String GLOBAL_PREFIX = "<jfragent><events>";
	private final String GLOBAL_POSTFIX = "</events></jfragent>";

	@Test
	public void testValidatingProbeDefinition() throws XMLStreamException {
		// a partially defined event with all optional elements unset
		String probe = "<event id=\"demo.event2\">\n" // 
				+ "    <label>Event 2</label>\n" //
				+ "    <class>org.company.project.MyDemoClass</class>\n" // 
				+ "    <method>\n" // 
				+ "        <name>targetFunction</name>\n" //
				+ "        <descriptor>(Ljava/lang/String;)V</descriptor>\n" // 
				+ "    </method>\n" //
				+ "</event>";

		DefaultTransformRegistry.validateProbeDefinition(GLOBAL_PREFIX + probe + GLOBAL_POSTFIX);
	}

	@Test
	public void testValidatingFullyDefinedProbe() throws XMLStreamException {
		// a fully defined event with all optional elements set
		String probe = "<event id=\"demo.event1\">\n" + "            <label>Event 1</label>\n"
				+ "            <class>com.company.project.MyDemoClass</class>\n"
				+ "            <description>demo event #1</description>\n" + "            <path>demo</path>\n"
				+ "            <stacktrace>true</stacktrace>\n" + "            <method>\n"
				+ "                <name>targetFunction</name>\n"
				+ "                <descriptor>(Ljava/lang/String;)I</descriptor>\n" + "                <parameters>\n"
				+ "                    <parameter index=\"0\">\n" + "                        <name>param 0</name>\n"
				+ "                        <description>the first parameter</description>\n"
				+ "                        <contenttype>None</contenttype>\n"
				+ "                        <relationkey>http://project.company.com/relation_id/parameter#0</relationkey>\n"
				+ "                        <converter>com.company.project.MyConverter</converter>\n"
				+ "                    </parameter>\n" + "                </parameters>\n"
				+ "                <returnvalue>\n" + "                    <name>returnValue</name>\n"
				+ "                    <description>the return value</description>\n"
				+ "                    <contenttype>None</contenttype>\n"
				+ "                    <relationkey>http://project.company.com/relation_id/parameter#0</relationkey>\n"
				+ "                    <converter>com.company.project.MyConverter</converter>\n"
				+ "                </returnvalue>\n" + "            </method>\n"
				+ "            <location>WRAP</location>\n" + "            <fields>\n" + "                <field>\n"
				+ "                    <name>count</name>\n"
				+ "                    <description>current value of 'count' member variable</description>\n"
				+ "                    <expression>com.company.product.MyClass.this</expression>\n"
				+ "                    <contenttype>None</contenttype>\n"
				+ "                    <relationkey>http://project.company.com/relation_id/field#0</relationkey>\n"
				+ "                    <converter>com.company.project.MyConverter</converter>\n"
				+ "                </field>\n" + "            </fields>\n" + "        </event>";

		DefaultTransformRegistry.validateProbeDefinition(GLOBAL_PREFIX + probe + GLOBAL_POSTFIX);
	}

	@Test(expected = XMLStreamException.class)
	public void testValidatingEmptyString() throws XMLStreamException {
		DefaultTransformRegistry.validateProbeDefinition("");
	}

	@Test(expected = XMLStreamException.class)
	public void testValidatingNonXmlInput() throws XMLStreamException {
		DefaultTransformRegistry.validateProbeDefinition("This is not an XML string");
	}

	@Test
	public void testValidatingCorrectClassNames() throws XMLStreamException {
		String probe = "<event id=\"demo.event2\">\n" // 
				+ "    <label>Event 2</label>\n" //
				+ "    <class>{0}</class>\n" // 
				+ "    <method>\n" // 
				+ "        <name>targetFunction</name>\n" //
				+ "        <descriptor>(Ljava/lang/String;)V</descriptor>\n" // 
				+ "    </method>\n" //
				+ "</event>";

		for (String clazz : Arrays.asList("MyClass", "pkg_name.MyClass", "com.company.project.MyClass",
				"MyClass$MyInnerClass")) {
			DefaultTransformRegistry
					.validateProbeDefinition(GLOBAL_PREFIX + MessageFormat.format(probe, clazz) + GLOBAL_POSTFIX);
		}
	}

	@Test(expected = XMLStreamException.class)
	public void testValidatingEmptyClassName() throws XMLStreamException {
		String probe = "<event id=\"demo.event2\">\n" // 
				+ "    <label>Event 2</label>\n" //
				+ "    <method>\n" // 
				+ "        <name>targetFunction</name>\n" //
				+ "        <descriptor>(Ljava/lang/String;)V</descriptor>\n" // 
				+ "    </method>\n" //
				+ "</event>";

		DefaultTransformRegistry.validateProbeDefinition(GLOBAL_PREFIX + probe + GLOBAL_POSTFIX);
	}

	@Test(expected = XMLStreamException.class)
	public void testValidatingIncorrectClassPattern() throws XMLStreamException {
		String probe = "<event id=\"demo.event2\">\n" // 
				+ "    <label>Event 2</label>\n" //
				+ "    <class>not a validate full-qualified-class-name</class>\n" //
				+ "    <method>\n" // 
				+ "        <name>targetFunction</name>\n" //
				+ "        <descriptor>(Ljava/lang/String;)V</descriptor>\n" // 
				+ "    </method>\n" //
				+ "</event>";

		DefaultTransformRegistry.validateProbeDefinition(GLOBAL_PREFIX + probe + GLOBAL_POSTFIX);
	}

	@Test
	public void testValidatingMethodDescriptor() throws XMLStreamException {
		String probe = "<event id=\"demo.event2\">\n" // 
				+ "    <label>Event 2</label>\n" //
				+ "    <class>org.company.project.MyDemoClass</class>\n" // 
				+ "    <method>\n" // 
				+ "        <name>targetFunction</name>\n" //
				+ "        <descriptor>{0}</descriptor>\n" // 
				+ "    </method>\n" //
				+ "</event>";

		for (String descriptor : Arrays.asList("()D", "()V", // 
				"(Ljava/lang/String;)V", "(Ljava/lang/String;J)I", //
				"([Lcom/company/project/MyClass;)V", "([[Lcom/company/project/MyClass;)V", //
				"()[D", "()[[D")) {
			DefaultTransformRegistry
					.validateProbeDefinition(GLOBAL_PREFIX + MessageFormat.format(probe, descriptor) + GLOBAL_POSTFIX);
		}
	}

	@Test(expected = XMLStreamException.class)
	public void testValidatingEmptyDescriptor() throws XMLStreamException {
		String probe = "<event id=\"demo.event2\">\n" // 
				+ "    <label>Event 2</label>\n" //
				+ "    <class>org.company.project.MyDemoClass</class>" //
				+ "    <method>\n" // 
				+ "        <name>targetFunction</name>\n" //
				+ "    </method>\n" //
				+ "</event>";

		DefaultTransformRegistry.validateProbeDefinition(GLOBAL_PREFIX + probe + GLOBAL_POSTFIX);
	}

	@Test(expected = XMLStreamException.class)
	public void testValidatingIncorrectDescriptor() throws XMLStreamException {
		String probe = "<event id=\"demo.event2\">\n" // 
				+ "    <label>Event 2</label>\n" //
				+ "    <class>org.company.project.MyDemoClass</class>" //
				+ "    <method>\n" // 
				+ "        <name>targetFunction</name>\n" //
				+ "        <descriptor>not a valid descriptor</descriptor>\n" // 
				+ "    </method>\n" //
				+ "</event>";

		DefaultTransformRegistry.validateProbeDefinition(GLOBAL_PREFIX + probe + GLOBAL_POSTFIX);
	}

	@Test
	public void testValidatingExpressions() throws XMLStreamException {
		String probe = "<event id=\"demo.event2\">\n" // 
				+ "    <label>Event 2</label>\n" //
				+ "    <class>org.company.project.MyDemoClass</class>\n" // 
				+ "    <method>\n" // 
				+ "        <name>targetFunction</name>\n" //
				+ "        <descriptor>(Ljava/lang/String;)V</descriptor>\n" // 
				+ "    </method>\n" //
				+ "    <fields>" //
				+ "        <field>" //
				+ "            <name>a variable</name>" //
				+ "            <expression>${0}</expression>" //
				+ "        </field>" //
				+ "    </fields>" //
				+ "</event>";

		for (String expression : Arrays.asList("this", "this.field", "MyClass.this.field", "field", "super.field",
				"STATIC_FIELD")) {
			DefaultTransformRegistry
					.validateProbeDefinition(GLOBAL_PREFIX + MessageFormat.format(probe, expression) + GLOBAL_POSTFIX);
		}
	}
}
