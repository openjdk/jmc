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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IStateful;
import org.openjdk.jmc.common.IWritableState;

/**
 * Class used as a bridge between {@link IStateful} and {@link IState}. You can read individual
 * values from it and you can pass the entire class to an {@link IWritableState} to save all the
 * values.
 * <p>
 * Instances of this class should be immutable.
 */
public class StatefulState implements IState, IStateful {

	private final String type;
	private final Map<String, String> attributes = new HashMap<>();
	private final List<StatefulState> children = new ArrayList<>(4);

	private StatefulState(String type) {
		this.type = type;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public StatefulState getChild(String type) {
		for (StatefulState child : children) {
			if (child.type.equals(type)) {
				return child;
			}
		}
		return null;
	}

	@Override
	public String[] getAttributeKeys() {
		return attributes.keySet().toArray(new String[attributes.size()]);
	}

	@Override
	public String getAttribute(String key) {
		return attributes.get(key);
	}

	@Override
	public StatefulState[] getChildren() {
		return children.toArray(new StatefulState[children.size()]);
	}

	@Override
	public StatefulState[] getChildren(String type) {
		List<StatefulState> childrenOfType = new ArrayList<>(4);
		for (StatefulState child : children) {
			if (child.type.equals(type)) {
				childrenOfType.add(child);
			}
		}
		return childrenOfType.toArray(new StatefulState[childrenOfType.size()]);
	}

	@Override
	public void saveTo(IWritableState state) {
		for (Entry<String, String> e : attributes.entrySet()) {
			state.putString(e.getKey(), e.getValue());
		}
		for (StatefulState child : children) {
			child.saveTo(state.createChild(child.type));
		}
	}

	/**
	 * Create a new instance.
	 *
	 * @param state
	 *            object whose state will be loaded into the new instance
	 * @return a new state instance
	 */
	public static StatefulState create(IStateful state) {
		StatefulState stateRoot = new StatefulState(null);
		state.saveTo(new StatefulStateWriter(stateRoot));
		return stateRoot;
	}

	@Override
	public String toString() {
		return type + " [" + children.size() + "] " + attributes.toString(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * StatefulState instances should be kept immutable, so the writer must not be exposed.
	 */
	private static class StatefulStateWriter implements IWritableState {

		private final StatefulState destination;

		StatefulStateWriter(StatefulState destination) {
			this.destination = destination;
		}

		@Override
		public void putString(String key, String value) {
			destination.attributes.put(key, value);
		}

		@Override
		public IWritableState createChild(String type) {
			StatefulState child = new StatefulState(type);
			destination.children.add(child);
			return new StatefulStateWriter(child);
		}

	}
}
