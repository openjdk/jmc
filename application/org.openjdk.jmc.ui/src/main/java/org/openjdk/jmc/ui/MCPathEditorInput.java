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
package org.openjdk.jmc.ui;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;

import org.openjdk.jmc.ui.common.util.AdapterUtil;

/**
 * Class for inputs that consists of a path
 */
public class MCPathEditorInput implements IPathEditorInput, IPersistableElement {
	private final File m_file;
	private final boolean m_persistable;

	/**
	 * @deprecated All users of this constructor should switch to the version with a boolean
	 *    parameter for whether or not it should be persistable.
	 */
	@Deprecated
	public MCPathEditorInput(File file) {
		this(file, false);
	}

	public MCPathEditorInput(File file, boolean persistable) {
		m_file = file;
		m_persistable = persistable;
	}

	@Override
	public boolean exists() {
		return m_file.exists();
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return m_file.getName();
	}

	@Override
	public IPersistableElement getPersistable() {
		if (m_persistable) {
			return this;
		} else {
			return null;
		}
	}

	@Override
	public String getToolTipText() {
		return m_file.getPath();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public IPath getPath() {
		return Path.fromOSString(m_file.getAbsolutePath());
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof IEditorInput) {
			return m_file.equals(getFile((IEditorInput) object));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return m_file.hashCode();
	}

	@Override
	public String getFactoryId() {
		return "mc.path.editorinput"; //$NON-NLS-1$
	}

	@Override
	public void saveState(IMemento memento) {
		memento.putString(MCPathFactory.PROPERTY_PORTABLE_STRING, m_file.getAbsolutePath());
		memento.putInteger(MCPathFactory.PROPERTY_PERSISTABLE, m_persistable ? 1 : 0);
	}

	/**
	 * @return The file in the editor input
	 * @throws IOException
	 *             If the path of the file can't be found.
	 */
	public static File getFile(IEditorInput ei) {
		IPathEditorInput ped = AdapterUtil.getAdapter(ei, IPathEditorInput.class);
		if (ped != null) {
			return ped.getPath().toFile();
		}
		return null;
	}

}
