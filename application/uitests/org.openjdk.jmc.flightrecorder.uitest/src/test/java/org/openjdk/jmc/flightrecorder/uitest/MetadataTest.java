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
package org.openjdk.jmc.flightrecorder.uitest;

import java.io.File;
import java.util.SortedMap;

/**
 * Class for testing JFR metadata related stuff. The baseline recording is stored in both jfr format
 * and text format and these two are also compared to make sure that any problems found aren't
 * related to parsing errors.
 */

public class MetadataTest extends MetadataTestBase {

	@Override
	protected void handleRecording(String filename) {
		File storedJfrTextFile = materialize(JFR_FOLDER, filename + BASELINE_TXT_FILE_SUFFIX, MetadataTest.class);
		File storedJfrFile = materialize(JFR_FOLDER, filename + BASELINE_JFR_FILE_SUFFIX, MetadataTest.class);

		SortedMap<String, SortedMap<String, String>> storedEventTypeMap = parseTextFile(storedJfrTextFile);
		SortedMap<String, SortedMap<String, String>> storedJfrEventTypeMap = JfrMetadataToolkit
				.parseRecordingFile(storedJfrFile);

		if (!storedEventTypeMap.equals(storedJfrEventTypeMap)) {
			errors.add(
					"Comparing the stored jfr file with the text file showed problems. Re-check the stored file with the reference parser (comparing with the text file) to make sure that it's not a parsing related error");
			doComparison(storedEventTypeMap, storedJfrEventTypeMap, true);
			doComparison(storedJfrEventTypeMap, storedEventTypeMap, false);
			fail(prepareFailure());
		}

		SortedMap<String, SortedMap<String, String>> currentEventTypeMap = JfrMetadataToolkit
				.parseRecordingFile(currentJfrFile);

		doComparison(currentEventTypeMap, storedEventTypeMap, false);
		doComparison(storedEventTypeMap, currentEventTypeMap, true);

		if (errors.size() > 0) {
			fail(prepareFailure());
		}
	}
}
