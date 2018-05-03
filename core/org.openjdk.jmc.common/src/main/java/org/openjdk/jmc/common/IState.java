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
package org.openjdk.jmc.common;

/**
 * A state container which can be read from. It is an hierarchical structure that can contain named
 * child states. Actual data values are stored in named attributes for each state node.
 * <p>
 * This interface is a companion to {@link IWritableState} which handles writing of state
 * information.
 */
public interface IState {

	/**
	 * Get the node type of this state.
	 *
	 * @return node type
	 */
	public String getType();

	/**
	 * Return an array with the attribute keys for this state.
	 *
	 * @return attribute keys
	 */
	public String[] getAttributeKeys();

	/**
	 * Gets a named attribute value.
	 *
	 * @param key
	 *            attribute key
	 * @return attribute value
	 */
	public String getAttribute(String key);

	/**
	 * Gets a child of the named type. If there are several child nodes of the same type then any of
	 * them may be returned.
	 *
	 * @param type
	 *            node type
	 * @return child node or {@code null} if no such child exists
	 */
	public IState getChild(String type);

	/**
	 * Gets all child nodes.
	 *
	 * @return an array of child nodes
	 */
	public IState[] getChildren();

	/**
	 * Gets all child nodes of a named type.
	 *
	 * @param type
	 *            node type
	 * @return an array of child nodes
	 */
	public IState[] getChildren(String type);

}
