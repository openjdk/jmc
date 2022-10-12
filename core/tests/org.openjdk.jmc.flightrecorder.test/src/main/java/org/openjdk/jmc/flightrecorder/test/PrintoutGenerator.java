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
package org.openjdk.jmc.flightrecorder.test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.test.util.PrintoutsToolkit;
import org.openjdk.jmc.flightrecorder.test.util.RecordingToolkit;

/**
 * Class that generates recording printouts.. First it removes all the recording in the printout
 * directory and then generates new printouts for all the recordings in the recordings directory.
 */
// FIXME: This won't work for resource directories. We might want to generate to a temporary directory instead and log its name.
@SuppressWarnings("nls")
public class PrintoutGenerator {
	public static void main(String[] args) throws URISyntaxException, IOException, CouldNotLoadRecordingException {
		File printoutDirectory = PrintoutsToolkit.getPrintoutDirectory();
		File recordingDirectory = RecordingToolkit.getRecordingDirectory();

		// Delete old printouts
		System.out.println("Deleting all files in directory " + printoutDirectory);
		for (File file : printoutDirectory.listFiles()) {
			if (!file.delete()) {
				System.out.println("Could not remove old printouts!\nExiting!");
				System.exit(1);
			}
		}

		// Generate new printouts
		for (File recordingFile : recordingDirectory.listFiles()) {
			File printoutFile = new File(printoutDirectory, recordingFile.getName() + ".txt");
			System.out.println("Generating " + printoutFile + " ...");
			PrintoutsToolkit.printRecording(recordingFile, printoutFile);
			System.out.println(" finished!");
		}
	}
}
