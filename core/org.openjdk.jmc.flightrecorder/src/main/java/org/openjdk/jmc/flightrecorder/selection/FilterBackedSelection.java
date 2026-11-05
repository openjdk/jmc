/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.flightrecorder.selection;

import java.util.Objects;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;

/**
 * A selection defined as the events of a source collection that matches a filter. Its items are a
 * view over the source, so it retains no copy of the selected events. Set operations in
 * {@link SelectionToolkit} on two selections with the same source compose their filters instead of
 * materializing events.
 */
public class FilterBackedSelection extends ItemBackedSelection {

	private final IItemCollection source;
	private final IItemFilter filter;

	public FilterBackedSelection(IItemCollection source, IItemFilter filter, String name) {
		super(Objects.requireNonNull(source, "source").apply(Objects.requireNonNull(filter, "filter")), name); //$NON-NLS-1$ //$NON-NLS-2$
		this.source = source;
		this.filter = filter;
	}

	/**
	 * @return the collection this selection filters; selections can only be composed by filter when
	 *         they share the same source instance
	 */
	public IItemCollection getSource() {
		return source;
	}

	public IItemFilter getFilter() {
		return filter;
	}

	/**
	 * Narrows this selection, keeping the same source so that the result remains composable.
	 */
	public FilterBackedSelection apply(IItemFilter narrowing, String name) {
		return new FilterBackedSelection(source, ItemFilters.and(filter, narrowing), name);
	}

}
