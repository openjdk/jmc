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

import org.eclipse.ui.IMemento;
import org.eclipse.ui.INavigationLocation;

class JfrNavigationLocation implements INavigationLocation {

	private final JfrEditor m_editor;
	private final DataPageDescriptor m_page;

	private boolean m_disposed;

	public JfrNavigationLocation(JfrEditor editor, DataPageDescriptor page) {
		m_editor = editor;
		m_page = page;
	}

	@Override
	public void dispose() {
		m_disposed = true;
	}

	@Override
	public void releaseState() {
	}

	@Override
	public void saveState(IMemento memento) {
	}

	@Override
	public void restoreState(IMemento memento) {
		m_disposed = true;
	}

	@Override
	public void restoreLocation() {
		if (!m_disposed) {
			m_editor.navigateTo(m_page);
		}
	}

	@Override
	public boolean mergeInto(INavigationLocation currentLocation) {
		if (m_disposed) {
			return true;
		}
		if (currentLocation instanceof JfrNavigationLocation) {
			JfrNavigationLocation that = (JfrNavigationLocation) currentLocation;
			return that.m_editor == m_editor && that.m_page == m_page;

		}
		return false;
	}

	@Override
	public Object getInput() {
		return null;
	}

	@Override
	public String getText() {
		return m_page.getName() + " [" + m_editor.getPartName() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public String toString() {
		return super.toString() + '[' + getText() + ']';
	}

	@Override
	public void setInput(Object input) {
	}

	@Override
	public void update() {
	}

}
