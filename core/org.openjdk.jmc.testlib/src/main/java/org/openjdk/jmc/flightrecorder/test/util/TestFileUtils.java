/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmc.test.io.IOResource;

/**
 * Utility class for file operations used in testing frame filtering.
 */
public class TestFileUtils {

	private static final String LICENSE_HEADER = "# Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.\n"
			+ "# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.\n" + "#\n"
			+ "# This code is free software; you can redistribute it and/or modify it\n"
			+ "# under the terms of the GNU General Public License version 2 only, as\n"
			+ "# published by the Free Software Foundation.\n" + "#\n"
			+ "# This code is distributed in the hope that it will be useful, but WITHOUT\n"
			+ "# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or\n"
			+ "# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License\n"
			+ "# version 2 for more details (a copy is included in the LICENSE file that\n"
			+ "# accompanied this code).\n" + "#\n"
			+ "# You should have received a copy of the GNU General Public License version\n"
			+ "# 2 along with this work; if not, write to the Free Software Foundation,\n"
			+ "# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.\n" + "#\n"
			+ "# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA\n"
			+ "# or visit www.oracle.com if you need additional information or have any\n" + "# questions.\n" + "#\n\n";

	/**
	 * Reads lines from a resource file, skipping the license header.
	 * 
	 * @param resource
	 *            the resource to read from
	 * @return list of lines from the file (excluding license header)
	 * @throws IOException
	 *             if the file cannot be read
	 */
	public static List<String> readLinesFromResource(IOResource resource) throws IOException {
		List<String> lines = new ArrayList<>();

		try (InputStream is = resource.open();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

			String line;
			boolean headerSkipped = false;

			while ((line = reader.readLine()) != null) {
				// Skip license header lines
				if (!headerSkipped) {
					if (line.startsWith("#") || line.trim().isEmpty()) {
						continue;
					}
					headerSkipped = true;
				}

				lines.add(line);
			}
		}

		return lines;
	}

	/**
	 * Writes lines to a file with the standard license header.
	 * 
	 * @param filePath
	 *            the path to write to
	 * @param lines
	 *            the lines to write
	 * @throws IOException
	 *             if the file cannot be written
	 */
	public static void writeLinesToFile(String filePath, List<String> lines) throws IOException {
		Path path = Paths.get(filePath);

		// Create parent directories if they don't exist
		if (path.getParent() != null) {
			Files.createDirectories(path.getParent());
		}

		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			// Write license header
			writer.write(LICENSE_HEADER);

			// Write the actual content
			for (String line : lines) {
				writer.write(line);
				writer.newLine();
			}
		}
	}

	/**
	 * Creates an index.txt file listing all the baseline files in a directory.
	 * 
	 * @param directory
	 *            the directory containing baseline files
	 * @throws IOException
	 *             if the index file cannot be created
	 */
	public static void createIndexFile(String directory) throws IOException {
		Path dirPath = Paths.get(directory);
		if (!Files.exists(dirPath)) {
			return;
		}

		List<String> files = new ArrayList<>();
		Files.list(dirPath)
				.filter(p -> p.toString().endsWith(".txt") && !p.getFileName().toString().equals("index.txt"))
				.forEach(p -> files.add(p.getFileName().toString()));

		files.sort(String::compareTo);

		Path indexPath = dirPath.resolve("index.txt");
		writeLinesToFile(indexPath.toString(), files);
	}
}
