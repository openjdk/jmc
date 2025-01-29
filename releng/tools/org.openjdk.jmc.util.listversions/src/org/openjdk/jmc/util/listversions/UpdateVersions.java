/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2024, 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.util.listversions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateVersions {

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out
					.println("Usage: java UpdatePlatformDefinition <Eclipse version> <Platform definition file path>");
			System.exit(2);
		}
		String eclipseVersion = args[0];
		String platformDefinitionPath = args[1];

		Map<String, String> newVersions = ListVersions.getNewVersions(eclipseVersion);
		String updatedContent = updatePlatformDefinition(platformDefinitionPath, newVersions, eclipseVersion);
		writePlatformDefinition(platformDefinitionPath, updatedContent);
	}

	private static void writePlatformDefinition(String platformDefinitionPath, String updatedContent)
			throws IOException {
		System.out.println("Updated content...");
		System.out.println(updatedContent);
		Path originalFile = Paths.get(platformDefinitionPath);
		Path directory = originalFile.getParent();
		String fileName = originalFile.getFileName().toString();
		String newFileName = "updated_" + fileName;
		Path newFile = directory.resolve(newFileName);

		Files.write(newFile, updatedContent.getBytes());
		System.out.println("Updated platform definition written to: " + newFile);
	}

	private static String updatePlatformDefinition(String filePath, Map<String, String> newVersions, String newEclipseVersion)
			throws IOException {
		String content = new String(Files.readAllBytes(Paths.get(filePath)));
		content = updateTargetName(content, newEclipseVersion);
		content = updateRepositoryLocation(content, newEclipseVersion);

		// Pattern to match <unit> elements with their id and version
		Pattern pattern = Pattern.compile("<unit\\s+id=\"([^\"]+)\"\\s+version=\"([^\"]+)\"\\s*/>");

		StringBuffer sb = new StringBuffer();
		Matcher matcher = pattern.matcher(content);

		while (matcher.find()) {
			String id = matcher.group(1);
			String currentVersion = matcher.group(2);

			if (newVersions.containsKey(id)) {
				String newVersion = newVersions.get(id);
				// Replace only the version, keeping everything else the same
				String replacement = matcher.group().replace("version=\"" + currentVersion + "\"",
						"version=\"" + newVersion + "\"");
				matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
				System.out.println("Updated " + id + " from version " + currentVersion + " to " + newVersion);
			} else {
				// If no new version, keep the original
				matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
			}
		}
		matcher.appendTail(sb);

		return sb.toString();
	}

	private static String updateTargetName(String content, String newEclipseVersion) {
		Pattern pattern = Pattern.compile("<target\\s+name=\"jmc-target-\\d{4}-\\d{2}\"");
		Matcher matcher = pattern.matcher(content);

		if (matcher.find()) {
			String oldTargetName = matcher.group();
			String newTargetName = "<target name=\"jmc-target-" + newEclipseVersion + "\"";
			content = content.replace(oldTargetName, newTargetName);
		}

		return content;
	}

	private static String updateRepositoryLocation(String content, String newEclipseVersion) {
		Pattern pattern = Pattern
				.compile("<repository\\s+location=\"https://download.eclipse.org/releases/\\d{4}-\\d{2}/\"\\s*/>");
		Matcher matcher = pattern.matcher(content);

		if (matcher.find()) {
			String oldRepository = matcher.group();
			String newRepository = "<repository location=\"https://download.eclipse.org/releases/" + newEclipseVersion
					+ "/\" />";
			content = content.replace(oldRepository, newRepository);
		}

		return content;
	}
}
