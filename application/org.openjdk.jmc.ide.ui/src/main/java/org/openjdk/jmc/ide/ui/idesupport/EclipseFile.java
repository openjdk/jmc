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
package org.openjdk.jmc.ide.ui.idesupport;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.ui.common.CorePlugin;
import org.openjdk.jmc.ui.common.resource.MCFile;

/**
 * The implementation of MCFile backed by a file in the eclipse workspace
 */
class EclipseFile implements MCFile {
	private final String path;

	EclipseFile(String path) {
		this.path = path;
	}

	@Override
	public String toString() {
		return path;
	}

	@Override
	public void writeStream(InputStream stream, IProgressMonitor monitor, boolean append) throws IOException {
		try {
			IFile file = getIFile();
			if (!file.exists()) {
				file.create(stream, true, monitor);
			} else if (append) {
				file.appendContents(stream, true, false, monitor);
			} else {
				file.setContents(stream, true, false, monitor);
			}
		} catch (CoreException e) {
			throw new IOException(NLS.bind(Messages.COULD_NOT_WRITE_FILE, path), e);
		}
	}

	@Override
	public boolean tryWriteStream(InputStream stream, IProgressMonitor monitor) throws IOException {
		try {
			IFile file = getIFile();
			if (!file.exists()) {
				file.create(stream, false, monitor);
				return true;
			}
			return false;
		} catch (CoreException e) {
			throw new IOException(NLS.bind(Messages.COULD_NOT_WRITE_FILE, path), e);
		}
	}

	private IFile getIFile() throws CoreException {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
		ensureExists(file.getParent());
		file.refreshLocal(IResource.DEPTH_ZERO, null);
		return file;
	}

	private static void ensureExists(IContainer container) throws CoreException {
		if (container instanceof IProject) {
			if (!container.exists()) {
				((IProject) container).create(null);
			}
			((IProject) container).open(null);
		} else if (container instanceof IFolder) {
			ensureExists(container.getParent());
			if (!container.exists()) {
				((IFolder) container).create(false, true, null);
			}
		} else {
			// Should never happen. All nested containers should be folders, and they should be contained in projects.
			throw new CoreException(
					new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID, container + " is not a IProject or IFolder")); //$NON-NLS-1$
		}
	}

	@Override
	public String getPath() {
		return path;
	}
}
