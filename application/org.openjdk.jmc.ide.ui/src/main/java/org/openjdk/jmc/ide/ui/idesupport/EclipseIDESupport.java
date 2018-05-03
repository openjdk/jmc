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

import java.io.File;
import java.io.FileNotFoundException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.common.CorePlugin;
import org.openjdk.jmc.ui.common.idesupport.IIDESupport;
import org.openjdk.jmc.ui.common.resource.MCFile;

/**
 * Implementation of common IDE functionality in an Eclipse IDE environment
 */
public class EclipseIDESupport implements IIDESupport {
	private final static IStatus PROJECT_MISSING_STATUS = new Status(IStatus.WARNING, CorePlugin.PLUGIN_ID,
			Messages.PROJECT_IS_MISSING);
	private final static IStatus PROJECT_CLOSED_STATUS = new Status(IStatus.WARNING, CorePlugin.PLUGIN_ID,
			Messages.PROJECT_IS_CLOSED);
	private final static IPath MISSION_CONTROL_PROJECT_NAME = new Path(Messages.IDEToolkit_MISSION_CONTROL_PROJECT_NAME)
			.makeAbsolute();

	public EclipseIDESupport() {
		// ignore
	}

	@Override
	public MCFile browseForSaveAsFile(String title, String suggestedPathStr, String fileExtension, String description) {
		WizardNewFileCreationPage fcp = new WizardNewFileCreationPage(title, new StructuredSelection());
		IPath suggestedPath = new Path(suggestedPathStr);
		if (suggestedPath.segmentCount() > 1) {
			fcp.setContainerFullPath(suggestedPath.removeLastSegments(1));
			fcp.setFileName(suggestedPath.lastSegment());
		}
		if (fileExtension != null) {
			fcp.setFileExtension(fileExtension);
		}
		fcp.setTitle(title);
		fcp.setDescription(description);
		fcp.setAllowExistingResources(true);
		FileCreationWizard wizard = new FileCreationWizard(fcp);
		wizard.setDefaultPageImageDescriptor(UIPlugin.getDefault().getImageDescriptor(UIPlugin.ICON_LAYOUT));
		wizard.setDialogSettings(UIPlugin.getDefault().getDialogSettings());
		wizard.addPage(fcp);
		WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
		dialog.setTitle(title);
		if (dialog.open() == Window.OK) {
			IPath path = wizard.getContainerFullPath().makeAbsolute().append(wizard.getFileName());
			return createFileResource(path.toString());
		}
		return null;
	}

	@Override
	public EclipseFile createFileResource(String resourcePath) {
		return new EclipseFile(resourcePath);
	}

	@Override
	public MCFile createDefaultFileResource(String resourcePath) throws IllegalArgumentException {
		IPath path = new Path(resourcePath);
		if (path.segmentCount() == 1) {
			return createFileResource(MISSION_CONTROL_PROJECT_NAME.append(path).toString());
		} else {
			return createFileResource(resourcePath);
		}
	}

	@Override
	public String getIdentity() {
		return "org.openjdk.jmc.eclipse"; //$NON-NLS-1$
	}

	@Override
	public File resolveFileSystemPath(String resourcePath) throws FileNotFoundException {
		IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(resourcePath);
		if (resource != null && resource.getLocation() != null) {
			File fileSystemPath = new File(resource.getLocation().toOSString());
			if (fileSystemPath.exists()) {
				return fileSystemPath;
			}
		}
		throw new FileNotFoundException(NLS.bind(Messages.NOT_ON_LOCAL_FILE_SYSTEM, resourcePath));
	}

	@Override
	public IStatus validateFileResourcePath(String resourcePath) {
		IStatus validation = ResourcesPlugin.getWorkspace().validatePath(resourcePath, IResource.FILE);
		if (validation.getSeverity() != IStatus.ERROR) {
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(resourcePath));
			if (file.exists()) {
				return IIDESupport.FILE_EXISTS_STATUS;
			} else if (!file.getProject().exists()) {
				return PROJECT_MISSING_STATUS;
			} else if (!file.getProject().isOpen()) {
				return PROJECT_CLOSED_STATUS;
			} else if (ResourcesPlugin.getWorkspace().getRoot().findMember(resourcePath) == null) {
				return Status.OK_STATUS;
			} else {
				return IIDESupport.FILE_PATH_IS_A_FOLDER;
			}
		}
		return validation;
	}
}
