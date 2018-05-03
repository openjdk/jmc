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
package org.openjdk.jmc.ui.misc;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.ui.Messages;
import org.openjdk.jmc.ui.UIPlugin;

public class MementoToolkit {

	private static class MementoState implements IState {

		final IMemento state;

		private MementoState(IMemento state) {
			this.state = state;
		}

		@Override
		public String getType() {
			return state.getType();
		}

		@Override
		public String[] getAttributeKeys() {
			return state.getAttributeKeys();
		}

		@Override
		public String getAttribute(String key) {
			return state.getString(key);
		}

		@Override
		public IState getChild(String type) {
			IMemento child = state.getChild(type);
			return child == null ? null : new MementoState(child);
		}

		@Override
		public IState[] getChildren() {
			return childArray(state.getChildren());
		}

		@Override
		public IState[] getChildren(String type) {
			return childArray(state.getChildren(type));
		}

		private static IState[] childArray(IMemento[] mementos) {
			IState[] children = new IState[mementos.length];
			for (int i = 0; i < children.length; i++) {
				children[i] = new MementoState(mementos[i]);
			}
			return children;
		}

	}

	private static class WritableMementoState implements IWritableState {

		final IMemento state;

		private WritableMementoState(IMemento state) {
			this.state = state;
		}

		@Override
		public IWritableState createChild(String type) {
			return new WritableMementoState(state.createChild(type));
		}

		@Override
		public void putString(String key, String value) {
			state.putString(key, value);
		}

	}

	public static IMemento fromString(String state) {
		if (state != null) {
			try {
				return XMLMemento.createReadRoot(new StringReader(state));
			} catch (WorkbenchException e) {
				UIPlugin.getDefault().getLogger().log(Level.WARNING,
						Messages.MCAbstractUIPlugin_CANNOT_LOAD_CORRUPT_STATE, e);
				return null;
			}
		}
		return null;
	}

	public static String asString(XMLMemento state) {
		StringWriter sw = new StringWriter();
		try {
			state.save(sw);
		} catch (IOException e) {
			// Will not happen
			assert false : e.toString();
		}
		return sw.toString();
	}

	public static IWritableState asWritableState(IMemento state) {
		return state == null ? null : new WritableMementoState(state);
	}

	public static IState asState(IMemento state) {
		return state == null ? null : new MementoState(state);
	}

	public static void copy(IConfigurationElement fromElement, IMemento toMemento) {
		for (String attr : fromElement.getAttributeNames()) {
			toMemento.putString(attr, fromElement.getAttribute(attr));
		}
		if (fromElement.getValue() != null) {
			toMemento.putTextData(fromElement.getValue());
		}
		for (IConfigurationElement child : fromElement.getChildren()) {
			copy(child, toMemento.createChild(child.getName()));
		}
	}

	public static void copy(IMemento srcElement, IMemento dstElement) {
		for (String attrKey : srcElement.getAttributeKeys()) {
			dstElement.putString(attrKey, srcElement.getString(attrKey));
		}
		if (srcElement.getTextData() != null && !srcElement.getTextData().trim().isEmpty()) {
			dstElement.putTextData(srcElement.getTextData().trim());
		}
		for (IMemento srcChild : srcElement.getChildren()) {
			copy(srcChild, dstElement.createChild(srcChild.getType()));
		}
	}
}
