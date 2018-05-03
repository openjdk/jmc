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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

@SuppressWarnings("nls")
public class XMLTagInstanceTest {

	@Test
	public void testGetTagsInstances() {
		XMLTagInstance root = new XMLTagInstance(null, new XMLTag("root"));
		XMLTagInstance first = new XMLTagInstance(root, new XMLTag("first"));
		XMLTag secondTag = new XMLTag("second");
		/* XMLTagInstance second = */new XMLTagInstance(first, secondTag);
		assertTrue(root.getTagsInstances(secondTag).size() == 0);
		assertTrue(first.getTagsInstances(secondTag).size() >= 0);
	}

	@Test
	public void testFindTagWithAttribute() {
		XMLTag rootTag = new XMLTag("root");
		XMLTag firstTag = new XMLTag("first");
		rootTag.add(firstTag);
		XMLTag secondTag = new XMLTag("second");
		firstTag.add(secondTag);
		XMLAttribute attribute = new XMLAttribute("testAttribute", false, XMLNodeType.TEXT);
		secondTag.add(attribute);

		XMLTagInstance root = new XMLTagInstance(null, rootTag);
		XMLTagInstance first = root.create(firstTag);
		Map<String, String> attributeMap = new HashMap<>();
		String value = "hejsan";
		attributeMap.put(attribute.getName(), value);
		/* XMLTagInstance second = */first.create(secondTag.getName(), attributeMap);
		assertNull(root.findTagWithAttribute(secondTag, attribute, value));
		assertNotNull(first.findTagWithAttribute(secondTag, attribute, value));
	}
}
