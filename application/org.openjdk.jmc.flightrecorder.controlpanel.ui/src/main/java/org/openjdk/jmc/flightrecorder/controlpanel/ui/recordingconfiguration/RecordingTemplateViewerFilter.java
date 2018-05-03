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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration;

import java.util.HashSet;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.openjdk.jmc.ui.common.util.FilterMatcher;
import org.openjdk.jmc.ui.common.util.FilterMatcher.Where;

/**
 * Class responsible for filtering which elements should be shown in a recording template part.
 */
class RecordingTemplateViewerFilter extends ViewerFilter {
	private String m_filterString;
	private final FilterMatcher m_filterMatcher = FilterMatcher.getInstance();;
	private HashSet<PathElement> m_filterCache;

	public void update(String filterString) {
		m_filterString = filterString;

		if (filterIsEmpty()) {
			return;
		}
		m_filterString = FilterMatcher.autoAddKleene(m_filterString, Where.BEFORE_AND_AFTER);
		m_filterCache = new HashSet<>();
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		return matchAgainstFilter((PathElement) element);
	}

	private boolean matchAgainstFilter(PathElement element) {
		if (noFilterOrInFilterCache(element)) {
			return true;
		}
		return matchesFilter(element);
	}

	private boolean noFilterOrInFilterCache(PathElement element) {
		return filterIsEmpty() || m_filterCache.contains(element);
	}

	private boolean matchesFilter(PathElement element) {
		if (matchFilterString(element)) {
			m_filterCache.add(element);
			return true;
		}
		return parentMatchesFilter(element) || childrenMatchesFilter(element);
	}

	private boolean matchFilterString(PathElement element) {
		return m_filterMatcher.match(element.getName(), m_filterString, true);
	}

	private boolean parentMatchesFilter(PathElement element) {
		for (PathElement parent = element.getParent(); parent.getParent() != null; parent = parent.getParent()) {
			if (filterIsEmpty() || matchFilterString(parent)) {
				return true;
			}
		}
		return false;
	}

	private boolean childrenMatchesFilter(PathElement element) {
		if (element instanceof PropertyContainer) {
			for (PathElement child : ((PropertyContainer) element).getChildren()) {
				if (matchAgainstFilter(child)) {
					m_filterCache.add(element);
					return true;
				}
			}
		}
		return false;
	}

	private boolean filterIsEmpty() {
		return m_filterString.length() == 0;
	}
}
