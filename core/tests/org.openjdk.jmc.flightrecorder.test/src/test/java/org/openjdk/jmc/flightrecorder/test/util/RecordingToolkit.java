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
package org.openjdk.jmc.flightrecorder.test.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.test.TestToolkit;
import org.openjdk.jmc.common.test.io.IOResourceSet;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;

@SuppressWarnings("nls")
public class RecordingToolkit {
	static final String RECORDING_TEXT_FILE_CHARSET = "UTF-8";
	private static final String RECORDINGS_DIRECTORY = "recordings";
	private static final String RECORDINGS_INDEXFILE = "index.txt";

	/**
	 * Return the directory where the recording files reside.
	 *
	 * @return the recording file directory
	 * @throws IOException
	 *             if the directory could not be found
	 */
	public static File getRecordingDirectory() throws IOException {
		return TestToolkit.getProjectDirectory(RecordingToolkit.class, RECORDINGS_DIRECTORY);
	}

	static IOResourceSet getRecordings() throws IOException {
		return TestToolkit.getResourcesInDirectory(RecordingToolkit.class, RECORDINGS_DIRECTORY, RECORDINGS_INDEXFILE);
	}

	public static IItemCollection getFlightRecording(IOResourceSet resourceSet)
			throws IOException, CouldNotLoadRecordingException {
		File tmpRecording = createResultFile("recordingTest", "tmp_recording", true);
		InputStream is = resourceSet.getResource(0).open();
		OutputStream os = new FileOutputStream(tmpRecording);
		int read = 0;
		byte[] tmp = new byte[4096];
		while ((read = is.read(tmp)) > 0) {
			os.write(tmp, 0, read);
		}
		IOToolkit.closeSilently(os);
		IOToolkit.closeSilently(is);
		return JfrLoaderToolkit.loadEvents(tmpRecording);
	}

	public static File createResultFile(String prefix, String suffix, boolean deleteTempOnExit) throws IOException {
		String resultDir = System.getProperty("results.dir");
		File resultFile;
		if (resultDir != null) {
			resultFile = new File(resultDir, prefix + '.' + System.currentTimeMillis() + '.' + suffix);
		} else {
			resultFile = File.createTempFile(prefix, suffix);
			if (deleteTempOnExit) {
				resultFile.deleteOnExit();
			}
		}
		return resultFile;
	}

}
