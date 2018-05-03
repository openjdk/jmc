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
package org.openjdk.jmc.ui.common.idesupport;

import java.io.File;
import java.io.FileNotFoundException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.openjdk.jmc.ui.common.CorePlugin;
import org.openjdk.jmc.ui.common.resource.MCFile;

/**
 * Interface for common IDE operations, eg. open file dialogs etc.
 * <p>
 * Typically this is used to show file dialogs etc. depending on if Mission Control is running in a
 * Eclipse IDE environment or just a RCP application environment. This interface is subject to
 * change at any time and it may be refactored away completely if we find a cleaner solution.
 */
public interface IIDESupport {

	IStatus FILE_EXISTS_STATUS = new Status(IStatus.WARNING, CorePlugin.PLUGIN_ID, Messages.IIDE_SUPPORT_FILE_EXISTS);

	IStatus FILE_PATH_IS_A_FOLDER = new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID,
			Messages.IIDE_SUPPORT_FILE_PATH_IS_A_FOLDER);

	/**
	 * Prompts the user with a save as dialog.
	 *
	 * @param title
	 *            the title when browsing
	 * @param suggestedPath
	 *            a suggested path
	 * @param fileExtension
	 *            the file extension the file must use, or null if it does not matter.
	 * @return the file where the user wants to save the file, or null if the user wants to cancel
	 *         the operation
	 */
	MCFile browseForSaveAsFile(String title, String suggestedPath, String fileExtension, String description);

	/**
	 * @param resourcePath
	 *            the path to a resource in the virtual resource tree of this application. The exact
	 *            syntax may vary between implementations of this interface
	 * @return the file at resourcePath in the resource tree
	 */
	MCFile createFileResource(String resourcePath);

	/**
	 * Creates a default file resource in the resource tree using resourcePath as template
	 *
	 * @param resourcePath
	 *            the path template
	 * @return the default file resource in the resource tree for resourcePath
	 */
	MCFile createDefaultFileResource(String resourcePath);

	/**
	 * @param resourcePath
	 *            the resource path to resolve
	 * @return the local file system path of the resource
	 * @throws FileNotFoundException
	 *             if the resource is not a valid existing file
	 */
	File resolveFileSystemPath(String resourcePath) throws FileNotFoundException;

	/**
	 * @param resourcePath
	 *            the path to a resource in the virtual resource tree of this application. The exact
	 *            syntax may vary between implementations of this interface
	 * @return severity will be IStatus.ERROR if resourcePath is not a valid file path. If the
	 *         severity is not IStatus.OK, the message will contain some information that may be
	 *         relevant to the user.
	 */
	IStatus validateFileResourcePath(String resourcePath);

	/**
	 * Returns a String identifying the application Mission Control is running in
	 *
	 * @return the identity of the IDE environment
	 */
	String getIdentity();
}
