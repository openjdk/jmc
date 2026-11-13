/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Kantega AS.
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
package org.openjdk.jmc.ui.misc;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.StructuredViewer;

/**
 * This class should be used with care, so be sure to understand the semantics. It is a workaround
 * for an anti pattern of doing too much heavy work during GUI rendering of label providers.
 * 
 * <pre>
 * This is solved by: 
 * 1. returning quickly with a default/fallback value 
 * 
 * 2. Firing off a Job in the background to do the actual work and populate a cache 
 * 
 * 3. Refresh the viewer for this element 
 * 
 * 4. The next invocation will pick up the cached value and return quickly. 
 * 
 * LIMITATIONS: 
 * 
 * 1. This only works for "static" attributes / cell values as it uses a cache internally
 * 
 * 2. The cache is keyed on the element object directly, so may sure the model is stable and the
 * hashCode/equals are correct. If not the cache will not save anything
 * 
 * 2. It can potentially create A LOT of background jobs (as it is triggered for each hell), possibly
 * affecting other background tasks. Therefore it is not suitable for LARGE structures. 
 * 
 * 3. As it is a workaround, consider if the structure can be rewritten instead, so that heavy work is done in
 * the background and the label provider is only used for quick presentation logic.
 * </pre>
 * 
 * @param <K>
 *            the element type to key the cache
 * @param <V>
 *            the type you want to cache, for example String, Color or Image
 */
public class DeferredLabelProvider<K, V> {
	private final StructuredViewer viewer;
	private final Function<K, V> retriever;
	private final Map<K, V> cache = new WeakHashMap<>();

	public DeferredLabelProvider(StructuredViewer viewer, Function<K, V> retriever) {
		this.viewer = viewer;
		this.retriever = retriever;
	}

	public V getLabelAttribute(K element) {
		if (!cache.containsKey(element)) {
			Job.createSystem("Loading label attribute for " + element, monitor -> { //$NON-NLS-1$ This is technical and not displayed
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				V value = retriever.apply(element);
				cache.put(element, value);
				// trigger refresh of view for that element
				if (!this.viewer.getControl().isDisposed()) {
					this.viewer.getControl().getDisplay().asyncExec(() -> this.viewer.update(element, null));
				}
				return Status.OK_STATUS;
			}).schedule();
		}
		return cache.get(element);
	}

}
