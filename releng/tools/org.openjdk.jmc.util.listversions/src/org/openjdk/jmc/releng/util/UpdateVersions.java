/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2024, 2026, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.releng.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateVersions {

	private static final Pattern UNIT_PATTERN = Pattern
			.compile("<unit\\s+id=\"([^\"]+)\"\\s+version=\"([^\"]+)\"\\s*/>");
	private static final Pattern TARGET_NAME_PATTERN = Pattern.compile("<target\\s+name=\"jmc-target-(\\d{4}-\\d{2})\"");
	private static final Pattern REPOSITORY_PATTERN = Pattern
			.compile("<repository\\s+location=\"https://download.eclipse.org/releases/(\\d{4}-\\d{2})/\"\\s*/>");
	private static final Pattern LOCATION_PATTERN = Pattern.compile("<location\\b[^>]*>(.*?)</location>", Pattern.DOTALL);
	private static final Pattern LOCATION_REPOSITORY_PATTERN = Pattern
			.compile("<repository\\s+location=\"([^\"]+)\"\\s*/?>");
	private static final Pattern ECLIPSE_RELEASE_REPO_PATTERN = Pattern
			.compile("https://download\\.eclipse\\.org/releases/\\d{4}-\\d{2}/?");

	private static final class Change {
		final String label;
		final String oldValue;
		final String newValue;

		Change(String label, String oldValue, String newValue) {
			this.label = label;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Usage: java UpdateVersions <Eclipse version> <Platform definition file path>");
			System.exit(2);
		}
		String eclipseVersion = args[0];
		String platformDefinitionPath = args[1];

		Path file = Paths.get(platformDefinitionPath);
		String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);

		System.out.println("Fetching unit versions for Eclipse " + eclipseVersion + "...");
		Map<String, String> newVersions = ListVersions.getNewVersions(eclipseVersion);
		if (newVersions.isEmpty()) {
			System.err.println("No versions retrieved for Eclipse " + eclipseVersion + ". Aborting.");
			System.exit(1);
		}

		List<Change> changes = new ArrayList<>();
		String updated = content;
		updated = collectTargetNameChange(updated, eclipseVersion, changes);
		updated = collectRepositoryChange(updated, eclipseVersion, changes);
		updated = collectUnitChanges(updated, newVersions, changes);

		if (changes.isEmpty()) {
			System.out.println("No updates needed. " + platformDefinitionPath + " is already up to date.");
			return;
		}

		printChanges(platformDefinitionPath, changes);
		if (Prompts.yesNo("Apply these " + changes.size() + " update(s) to " + platformDefinitionPath + "? [y/N]: ")) {
			Files.write(file, updated.getBytes(StandardCharsets.UTF_8));
			System.out.println("Wrote updated platform definition to: " + file);
		} else {
			System.out.println("Aborted. No changes written.");
		}
	}

	private static String collectTargetNameChange(String content, String newEclipseVersion, List<Change> changes) {
		Matcher matcher = TARGET_NAME_PATTERN.matcher(content);
		if (!matcher.find()) {
			return content;
		}
		String currentVersion = matcher.group(1);
		if (currentVersion.equals(newEclipseVersion)) {
			return content;
		}
		String oldTag = matcher.group();
		String newTag = "<target name=\"jmc-target-" + newEclipseVersion + "\"";
		changes.add(new Change("target name", "jmc-target-" + currentVersion, "jmc-target-" + newEclipseVersion));
		return content.replace(oldTag, newTag);
	}

	private static String collectRepositoryChange(String content, String newEclipseVersion, List<Change> changes) {
		Matcher matcher = REPOSITORY_PATTERN.matcher(content);
		if (!matcher.find()) {
			return content;
		}
		String currentVersion = matcher.group(1);
		if (currentVersion.equals(newEclipseVersion)) {
			return content;
		}
		String oldRepo = matcher.group();
		String newRepo = "<repository location=\"https://download.eclipse.org/releases/" + newEclipseVersion + "/\" />";
		changes.add(new Change("repository location", "releases/" + currentVersion + "/",
				"releases/" + newEclipseVersion + "/"));
		return content.replace(oldRepo, newRepo);
	}

	private static String collectUnitChanges(String content, Map<String, String> newVersions, List<Change> changes) {
		// Only update units that live inside a <location> block whose repository points at the Eclipse
		// releases update site. Units in other blocks (the local p2 site, the Babel archive) come from
		// repositories that don't host the same versions, so blindly applying the Eclipse fetch there
		// would propose bogus and often unresolvable downgrades.
		StringBuffer sb = new StringBuffer();
		Matcher locationMatcher = LOCATION_PATTERN.matcher(content);
		int lastEnd = 0;
		while (locationMatcher.find()) {
			sb.append(content, lastEnd, locationMatcher.start());
			String block = locationMatcher.group();
			boolean isEclipse = isEclipseReleaseLocation(block);
			sb.append(isEclipse ? rewriteUnitsInBlock(block, newVersions, changes) : block);
			lastEnd = locationMatcher.end();
		}
		sb.append(content, lastEnd, content.length());
		return sb.toString();
	}

	private static boolean isEclipseReleaseLocation(String block) {
		Matcher repo = LOCATION_REPOSITORY_PATTERN.matcher(block);
		return repo.find() && ECLIPSE_RELEASE_REPO_PATTERN.matcher(repo.group(1)).matches();
	}

	private static String rewriteUnitsInBlock(String block, Map<String, String> newVersions, List<Change> changes) {
		StringBuffer sb = new StringBuffer();
		Matcher matcher = UNIT_PATTERN.matcher(block);
		while (matcher.find()) {
			String id = matcher.group(1);
			String currentVersion = matcher.group(2);
			String newVersion = newVersions.get(id);
			if (newVersion != null && !newVersion.equals(currentVersion)) {
				String replacement = matcher.group().replace("version=\"" + currentVersion + "\"",
						"version=\"" + newVersion + "\"");
				matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
				changes.add(new Change("unit " + id, currentVersion, newVersion));
			} else {
				matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
			}
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private static void printChanges(String filePath, List<Change> changes) {
		System.out.println();
		System.out.println("Proposed updates to " + filePath + ":");
		int labelWidth = 0;
		for (Change c : changes) {
			labelWidth = Math.max(labelWidth, c.label.length());
		}
		for (Change c : changes) {
			System.out.printf("  %-" + labelWidth + "s : %s -> %s%n", c.label, c.oldValue, c.newValue);
		}
		System.out.println();
	}

}
