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
package org.openjdk.jmc.rjmx.triggers.fields.internal;

import java.util.ArrayList;
import java.util.Iterator;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.openjdk.jmc.common.util.XmlToolkit;

final public class FieldHolder {
	private static final String XML_SETTINGS_TAG = "settings"; //$NON-NLS-1$
	private final ArrayList<Tuple> m_properties = new ArrayList<>();

	static class Tuple {
		Tuple(String id, Field field) {
			key = id;
			this.field = field;
		}

		public final String key;
		public final Field field;
	}

	public FieldHolder() {

	}

	public void addField(Field v) {
		if (getField(v.getName()) == null) {
			m_properties.add(new Tuple(v.getId(), v));
		}
	}

	public Field[] getFields() {
		Field[] fields = new Field[m_properties.size()];
		int n = 0;
		Iterator<Tuple> i = m_properties.iterator();
		while (i.hasNext()) {
			Tuple tuple = i.next();
			fields[n] = tuple.field;
			n++;
		}
		return fields;
	}

	public void exportToXml(Element parentNode) {
		Field[] fields = getFields();
		Element element = XmlToolkit.createElement(parentNode, XML_SETTINGS_TAG);
		for (Field field : fields) {
			field.exportToXml(element);
		}
	}

	public void initializeFromXml(Element node) {
		Field[] fields = getFields();
		for (Field field : fields) {
			NodeList list = node.getElementsByTagName(XML_SETTINGS_TAG);
			if (list != null && list.getLength() > 0) {
				// for (int j = 0; j < list.getLength(); j++)
				field.initializeFromXml((Element) list.item(0));
			}
		}
	}

	public Field getField(String id) {
		Iterator<Tuple> i = m_properties.iterator();

		while (i.hasNext()) {
			Tuple tuple = i.next();
			if (tuple.key.equals(id)) {
				return tuple.field;
			}
		}
		return null;
	}

}
