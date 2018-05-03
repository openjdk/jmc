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
package org.openjdk.jmc.common.util;

import java.util.Arrays;

import org.openjdk.jmc.common.IState;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A wrapper class used to read from an XML document using the {@link IState} interface.
 */
class StateElement implements IState {

	private final static IState[] NO_CHILDREN = new StateElement[] {};
	private final Element element;

	/**
	 * Create a new instance.
	 * 
	 * @param element
	 *            XML element to wrap
	 */
	StateElement(Element element) {
		this.element = element;
	}

	@Override
	public String getType() {
		return element.getNodeName();
	}

	@Override
	public String[] getAttributeKeys() {
		NamedNodeMap nodeMap = element.getAttributes();
		String[] keys = new String[nodeMap.getLength()];
		for (int i = 0; i < nodeMap.getLength(); i++) {
			keys[i] = nodeMap.item(i).getNodeName();
		}
		return keys;
	}

	@Override
	public String getAttribute(String key) {
		Attr attribute = element.getAttributeNode(key);
		if (attribute == null) {
			return null;
		}
		return attribute.getValue();
	}

	@Override
	public IState getChild(String type) {
		NodeList nodes = element.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node instanceof Element && type.equals(node.getNodeName())) {
				return new StateElement((Element) node);
			}
		}
		return null;
	}

	@Override
	public IState[] getChildren() {
		return getChildren(null);
	}

	@Override
	public IState[] getChildren(String type) {
		NodeList nodes = element.getChildNodes();
		if (nodes.getLength() == 0) {
			return NO_CHILDREN;
		}
		IState[] children = new IState[nodes.getLength()];
		int index = 0;
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node instanceof Element && (type == null || type.equals(node.getNodeName()))) {
				children[index++] = new StateElement((Element) node);
			}
		}
		if (children.length != index) {
			children = Arrays.copyOf(children, index);
		}
		return children;
	}
}
