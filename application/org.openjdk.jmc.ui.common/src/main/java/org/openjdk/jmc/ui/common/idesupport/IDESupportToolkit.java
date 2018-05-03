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
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;

import org.openjdk.jmc.ui.common.CorePlugin;
import org.openjdk.jmc.ui.common.resource.MCFile;
import org.openjdk.jmc.ui.common.util.Filename;

/**
 * Convenience methods for the IDESupport.
 */
public final class IDESupportToolkit {

	private IDESupportToolkit() {
		throw new AssertionError("Toolkit - do not instantiate!"); //$NON-NLS-1$
	}

	/**
	 * <p>
	 * Will write the contents of the supplied stream to the specified file using a separate job.
	 * The job will be scheduled directly after it has been created. Any error taking place during
	 * the write can be seen in the returning IStatus of the job.
	 * </p>
	 * <p>
	 * Note that this uses the same semantics as
	 * {@link MCFile#writeStream(InputStream, org.eclipse.core.runtime.IProgressMonitor)} - if there
	 * is already an existing file with the same name, it will be overwritten.
	 * </p>
	 *
	 * @param jobName
	 *            the name of the job that is created.
	 * @param file
	 *            the file to write to.
	 * @param stream
	 *            the stream from which to write to the file.
	 * @param append
	 *            {@code true} if the file should be appended, {@code false} if it should be
	 *            overwritten
	 */
	public static Job writeAsJob(String jobName, MCFile file, InputStream stream, boolean append) {
		Job writeJob = new JobFileWrite(jobName, file, stream, append);
		writeJob.schedule();
		return writeJob;
	}

	/**
	 * @param resourcePath
	 *            the path in the resource tree for the created file
	 * @return the file at resourcePath in the resource tree
	 * @throws IllegalArgumentException
	 *             if resourcePath is empty
	 */
	public static MCFile createFileResource(String resourcePath) throws IllegalArgumentException {
		return CorePlugin.getDefault().getIDESupport().createFileResource(resourcePath);
	}

	public static MCFile createDefaultFileResource(String resourcePath) throws IllegalArgumentException {
		return CorePlugin.getDefault().getIDESupport().createDefaultFileResource(resourcePath);
	}

	/**
	 * @param resourcePath
	 *            the resource path to resolve
	 * @return the local file system path of the resource
	 * @throws FileNotFoundException
	 *             if the resource is not a valid existing file
	 */
	public static File resolveFileSystemPath(String resourcePath) throws FileNotFoundException {
		return CorePlugin.getDefault().getIDESupport().resolveFileSystemPath(resourcePath);
	}

	public static IStatus validateFileResourcePath(String resourcePath) {
		return CorePlugin.getDefault().getIDESupport().validateFileResourcePath(resourcePath);
	}

	public static File writeToUniqueFile(MCFile file, InputStream stream, IProgressMonitor monitor) throws IOException {
		IPath path = new Path(file.getPath());
		Filename filename = Filename.splitFilename(path.lastSegment());
		path = path.removeLastSegments(1);
		while (!file.tryWriteStream(stream, monitor)) {
			IPath uniquePath = path.append(filename.asRandomFilename().toString());
			file = createFileResource(uniquePath.toOSString());
		}
		return resolveFileSystemPath(file.getPath());
	}

}
