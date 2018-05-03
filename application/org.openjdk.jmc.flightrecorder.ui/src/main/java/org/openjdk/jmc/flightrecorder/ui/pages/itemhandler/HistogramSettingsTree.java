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
package org.openjdk.jmc.flightrecorder.ui.pages.itemhandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.ui.column.TableSettings;

class HistogramSettingsTree {

	private static final String ATTRIBUTE_PARENT_GROUPBY = "parentGroupBy"; //$NON-NLS-1$
	private static final String ATTRIBUTE_GROUPBY = "groupBy"; //$NON-NLS-1$
	private static final String ELEMENT_TABLE = "table"; //$NON-NLS-1$
	private static final String ELEMENT_CHILD = "child"; //$NON-NLS-1$

	public HistogramSettingsTree(IState state) {
		groupBy = state.getAttribute(ATTRIBUTE_GROUPBY);
		IState table = state.getChild(ELEMENT_TABLE);
		tableSettings = TableSettings.forState(table);
		for (IState child : state.getChildren(ELEMENT_CHILD)) {
			children.put(child.getAttribute(ATTRIBUTE_PARENT_GROUPBY), new HistogramSettingsTree(child));
		}
	}

	public HistogramSettingsTree() {

	}

	public HistogramSettingsTree(String groupBy, TableSettings tableConfig) {
		this.groupBy = groupBy;
		tableSettings = tableConfig;
	}

	public String groupBy;
	public TableSettings tableSettings;
	private final Map<String, HistogramSettingsTree> children = new HashMap<>();

	public HistogramSettingsTree getSelectedChild() {
		return children.computeIfAbsent(groupBy, key -> new HistogramSettingsTree());
	}

	public void saveState(IWritableState state) {
		state.putString(ATTRIBUTE_GROUPBY, groupBy);
		if (tableSettings != null) {
			tableSettings.saveState(state.createChild(ELEMENT_TABLE));
		}
		for (Entry<String, HistogramSettingsTree> e : children.entrySet()) {
			IWritableState childState = state.createChild(ELEMENT_CHILD);
			childState.putString(ATTRIBUTE_PARENT_GROUPBY, e.getKey());
			e.getValue().saveState(childState);
		}

	}

}
