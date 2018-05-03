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
package org.openjdk.jmc.flightrecorder.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IStateful;
import org.openjdk.jmc.common.util.StatefulState;

/**
 * A page descriptor. Contains description, state and children for a page. It is intended to be used
 * for holding information about a page that is relevant on a process global level. When a page is
 * to be shown it is converted to a {@link IDisplayablePage} which is held on a recording level
 * (i.e., one instance per open recording).
 */
public class DataPageDescriptor implements IPageDefinition {

	private StatefulState pageState;
	private final String id;
	private final String factoryId;
	private final IDataPageFactory factory;

	private DataPageDescriptor parent = null;
	private final List<DataPageDescriptor> children = new ArrayList<>();

	DataPageDescriptor(DataPageDescriptor source) {
		this(source.id, source.factoryId, source.factory, source.pageState);
	}

	DataPageDescriptor(String id, String factoryId, IDataPageFactory factory, StatefulState pageState) {
		this.id = id;
		this.factoryId = factoryId;
		this.factory = factory;
		setPageState(pageState);
	}

	String getId() {
		return id;
	}

	String getFactoryId() {
		return factoryId;
	}

	String getHelpContextId() {
		// FIXME: Somehow determine that page id should not be used as help context id (e.g. for generated pages)
		int index = id.lastIndexOf('.');
		// The help system will interpret the help context id as pluginId.contextId
		if (index != -1 && Platform.getBundle(id.substring(0, index)) != null) {
			return id;
		}
		return factoryId;
	}

	void setPageState(StatefulState state) {
		if (state == null) {
			pageState = StatefulState.create(writableState -> factory.resetToDefault(pageState, writableState));
		} else {
			pageState = state;
		}
	}

	DataPageDescriptor getParent() {
		return parent;
	}

	// Should only be called from PageManager
	void setParent(DataPageDescriptor parent) {
		this.parent = parent;
	}

	// Should only be called from PageManager as the list is mutable
	List<DataPageDescriptor> getChildList() {
		return children;
	}

	public boolean hasChildren() {
		return children.size() > 0;
	}

	public DataPageDescriptor[] getChildren() {
		return children.toArray(new DataPageDescriptor[children.size()]);
	}

	boolean contains(DataPageDescriptor dpd) {
		return dpd == this || (dpd.parent != null && contains(dpd.parent));
	}

	@Override
	public IState getState() {
		return getPageState();
	}

	StatefulState getPageState() {
		return pageState;
	}

	void readPageStateFrom(IStateful page) {
		pageState = StatefulState.create(page);
	}

	IDisplayablePage createPage(StreamModel items, IPageContainer editor) {
		return factory.createPage(this, items, editor);
	}

	@Override
	public String getName() {
		return factory.getName(pageState);
	}

	@Override
	public String getDescription() {
		return factory.getDescription(pageState);
	}

	@Override
	public String[] getTopics() {
		return factory.getTopics(pageState);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return factory.getImageDescriptor(pageState);
	}

}
