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
package org.openjdk.jmc.flightrecorder.configuration.spi;

import java.io.IOException;
import java.io.InputStream;

/**
 * Storage delegate for storing text based configuration files. It can be thought of as a fancy URL
 * wrapper.
 */
public interface IConfigurationStorageDelegate {
	/**
	 * The only currently supported character encoding.
	 */
	final static String CHARSET_UTF8 = "UTF-8"; //$NON-NLS-1$

	/**
	 * @return an input stream or null if the underlying storage does not yet have any contents.
	 */
	InputStream getContents();

	/**
	 * If the underlying storage can be saved to. This method can be used to enable or disable save
	 * options. Also optionally to avoid having to generate the content for {@link #save(String)},
	 * when it won't be used.
	 *
	 * @return
	 */
	boolean isSaveable();

	/**
	 * Save the given content string to the underlying storage, replacing previous content. The
	 * character encoding is currently forced to be {@value #CHARSET_UTF8}, as specified by
	 * {@link #CHARSET_UTF8}, which is important for XML stating the encoding.
	 * <p>
	 * NOTE: Passing the contents as a String are fine for small files that are seldom written to.
	 * Should this method be used more often or with larger files, a callback with methods for
	 * writing to a output stream and for providing an input stream (for IResource destinations)
	 * could be passed instead.
	 *
	 * @param fileContent
	 * @return true iff the save operation succeded.
	 * @throws IOException
	 *             if some problem occurred while writing to the underlying storage. The contents
	 *             might have been partially written.
	 */
	boolean save(String fileContent) throws IOException;

	/**
	 * If the underlying storage can likely be deleted permanently. Note that returning true here
	 * does not guarantee that a {@link #delete()} will succeed, only that it can be attempted.
	 *
	 * @return
	 */
	boolean isDeletable();

	/**
	 * Attempt to delete the underlying storage. If successful, the storage delegate should no
	 * longer be used, which might be enforced in the future.
	 *
	 * @return true iff the underlying storage was deleted.
	 */
	boolean delete();

	/**
	 * Get additional information on the underlying storage, if available.
	 *
	 * @return a descriptive string which the name of the template can be augmented with, or null.
	 */
	String getLocationInfo();

	/**
	 * Get information on the file system location for the underlying storage, if available.
	 *
	 * @return a string that represents the file system location of this template, or null.
	 */
	String getLocationPath();
}
