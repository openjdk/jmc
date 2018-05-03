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
package org.openjdk.jmc.rcp.application;

import java.io.File;
import java.io.FileNotFoundException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.openjdk.jmc.ui.common.idesupport.IIDESupport;
import org.openjdk.jmc.ui.common.resource.MCFile;
import org.openjdk.jmc.ui.idesupport.IDESupportUIToolkit;

public class RCPApplicationIDESupport implements IIDESupport {

	@Override
	public MCFile browseForSaveAsFile(String title, String suggestedFile, String fileExtension, String description) {
		File file = IDESupportUIToolkit.browseForSaveAsFile(title, new File(suggestedFile), fileExtension, description);
		return file == null ? null : new BasicFile(file.getPath());
	}

	@Override
	public String getIdentity() {
		return "org.openjdk.jmc.rcp"; //$NON-NLS-1$
	}

	@Override
	public MCFile createFileResource(String path) {
		return new BasicFile(path);
	}

	@Override
	public MCFile createDefaultFileResource(String resourcePath) throws IllegalArgumentException {
		return createFileResource(resourcePath);
	}

	@Override
	public File resolveFileSystemPath(String resourcePath) throws FileNotFoundException {
		File absoultePath = BasicFile.makeAbsolute(resourcePath); // may be a file or folder
		if (absoultePath.exists()) {
			return absoultePath;
		}
		throw new FileNotFoundException(resourcePath);
	}

	@Override
	public IStatus validateFileResourcePath(String resourcePath) throws IllegalArgumentException {
		File absolutePath = BasicFile.makeAbsolute(resourcePath);
		if (absolutePath.getParentFile() == null) {
			// Guard against paths that don't exist but cannot be used as filenames (e.g. empty string)
			return IIDESupport.FILE_PATH_IS_A_FOLDER;
		} else if (!absolutePath.exists()) {
			return Status.OK_STATUS;
		} else if (absolutePath.isFile()) {
			return IIDESupport.FILE_EXISTS_STATUS;
		} else {
			return IIDESupport.FILE_PATH_IS_A_FOLDER;
		}
	}

}
