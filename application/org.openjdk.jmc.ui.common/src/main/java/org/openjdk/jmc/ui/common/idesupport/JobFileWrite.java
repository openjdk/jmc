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

import java.io.InputStream;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.ui.common.CorePlugin;
import org.openjdk.jmc.ui.common.resource.MCFile;

/**
 * An Eclipse job for writing the contents of an input stream to a file.
 */
final class JobFileWrite extends Job {
	private final MCFile file;
	private final InputStream stream;
	private final boolean append;

	/**
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
	JobFileWrite(String jobName, MCFile file, InputStream stream, boolean append) {
		super(jobName);
		this.file = file;
		this.stream = stream;
		this.append = append;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			file.writeStream(stream, monitor, append);
		} catch (Exception e) {
			// Want non-localized message in the log!
			CorePlugin.getDefault().getLogger().log(Level.SEVERE, "Could not write the specified file!", e); //$NON-NLS-1$
			return new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID,
					NLS.bind(Messages.JobFileWrite_ERROR_FILE_WRITE_FAILED, file.getPath()), e);
		} finally {
			IOToolkit.closeSilently(stream);
		}
		return new Status(IStatus.OK, CorePlugin.PLUGIN_ID,
				NLS.bind(Messages.JobFileWrite_MESSAGE_FILE_WRITE_SUCCESS, file.getPath()));
	}
}
