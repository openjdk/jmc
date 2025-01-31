/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2025, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.console.agent.raweditor.internal;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.editors.text.TextEditor;

public class XmlEditor extends TextEditor {
	private ColorManager colorManager;

	public XmlEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new XmlConfiguration(colorManager));
		setDocumentProvider(new XmlDocumentProvider());
	}

	@Override
	protected void doSetInput(IEditorInput input) throws CoreException {
		input = convertInput(input);

		super.doSetInput(input);
		setDocumentProvider(input);
	}

	@Override
	protected void createActions() {
		// Intentionally empty
	}

	@Override
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}

	public static IEditorInput convertInput(IEditorInput editorInput) {
		if (editorInput instanceof IFileEditorInput || editorInput instanceof IStorageEditorInput) {
			return editorInput;
		}

		if (editorInput instanceof IPathEditorInput) {
			IPath p = ((IPathEditorInput) editorInput).getPath();

			IStorage s = new LocalFileStorage(p.toFile()) {
				public boolean isReadOnly() {
					return false;
				};
			};

			return new IStorageEditorInput() {
				@SuppressWarnings("unchecked")
				public <T> T getAdapter(Class<T> adapter) {
					if (adapter.equals(ILocationProvider.class)) {
						return (T) (ILocationProvider) element -> p;
					}
					return editorInput.getAdapter(adapter);
				}

				public boolean exists() {
					return editorInput.exists();
				}

				public ImageDescriptor getImageDescriptor() {
					return editorInput.getImageDescriptor();
				}

				public String getName() {
					return editorInput.getName();
				}

				public IPersistableElement getPersistable() {
					return editorInput.getPersistable();
				}

				public String getToolTipText() {
					return editorInput.getToolTipText();
				}

				public IStorage getStorage() {
					return s;
				}
			};
		}

		throw new UnsupportedOperationException();

	}
}
