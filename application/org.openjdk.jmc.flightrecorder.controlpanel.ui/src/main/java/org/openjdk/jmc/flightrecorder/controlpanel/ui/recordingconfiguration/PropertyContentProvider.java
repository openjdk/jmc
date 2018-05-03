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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfigurationModel;

/**
 * Class responsible providing {@link Property} and {@link PropertyContainer} object to a JFAce
 * viewer.
 */
final class PropertyContentProvider implements ITreeContentProvider {
	private Object currentInput;
	private Object[] currentRootElements = new Object[0];

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof PropertyContainer) {
			return ((PropertyContainer) parentElement).getChildren().toArray();
		}
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof PathElement) {
			return ((PathElement) element).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return (element instanceof PropertyContainer) && ((PropertyContainer) element).hasChildren();
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement == currentInput) {
			return currentRootElements;
		}
		return new Object[0];
	}

	@Override
	public void dispose() {
		// ignore
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// Cache the built model to be able to do refresh() and filtering on the viewer without rebuilding.
		// (Another option would be to do update() on the viewer instead, but that doesn't work with filtering.)
		if (newInput instanceof EventConfigurationModel) {
			EventConfigurationModel model = (EventConfigurationModel) newInput;
			currentRootElements = PropertyContentBuilder.build(model).toArray();
		} else {
			currentRootElements = new Object[0];
		}
		currentInput = newInput;
	}
}
