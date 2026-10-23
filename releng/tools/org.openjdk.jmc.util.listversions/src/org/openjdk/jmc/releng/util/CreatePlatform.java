/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates a new platform-definition-&lt;version&gt;/ directory under a platform-definitions root,
 * generating pom.xml and the .target file from templates and registering the new module in the
 * parent pom.xml. Unit versions are resolved automatically: bundles served from the Eclipse update
 * site are pulled fresh via {@link ListVersions}, and bundles from other locations (the local p2
 * site and the Babel archive) are inherited from the most recent existing platform definition.
 */
public class CreatePlatform {

	private static final String POM_TEMPLATE_RESOURCE = "templates/pom.xml.template";
	private static final String TARGET_TEMPLATE_RESOURCE = "templates/platform-definition.target.template";
	private static final String PROJECT_TEMPLATE_RESOURCE = "templates/.project.template";

	private static final Pattern VERSION_PATTERN = Pattern.compile("\\d{4}-\\d{2}");
	private static final Pattern PLATFORM_DIR_PATTERN = Pattern.compile("platform-definition-(\\d{4}-\\d{2})");
	private static final Pattern LOCATION_PATTERN = Pattern.compile("<location\\b[^>]*>(.*?)</location>", Pattern.DOTALL);
	private static final Pattern UNIT_PATTERN = Pattern.compile("<unit\\s+id=\"([^\"]+)\"\\s+version=\"([^\"]+)\"\\s*/>");
	private static final Pattern REPOSITORY_PATTERN = Pattern.compile("<repository\\s+location=\"([^\"]+)\"\\s*/?>");
	private static final Pattern ECLIPSE_RELEASE_PATTERN = Pattern
			.compile("https://download\\.eclipse\\.org/releases/\\d{4}-\\d{2}/?");
	private static final Pattern UNIT_VERSION_PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)\\.version\\}");
	private static final Pattern ANY_PLACEHOLDER = Pattern.compile("\\$\\{[^}]+\\}");

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Usage: java CreatePlatform <Eclipse version> <platform-definitions root>");
			System.out.println("  e.g. java CreatePlatform 2025-12 releng/platform-definitions");
			System.exit(2);
		}
		String eclipseVersion = args[0];
		Path root = Paths.get(args[1]).toAbsolutePath().normalize();

		if (!VERSION_PATTERN.matcher(eclipseVersion).matches()) {
			System.err.println("Eclipse version must be in YYYY-MM form, e.g. 2025-12. Got: " + eclipseVersion);
			System.exit(2);
		}
		if (!Files.isDirectory(root)) {
			System.err.println("Platform-definitions root not found or not a directory: " + root);
			System.exit(2);
		}

		Path platformDir = root.resolve("platform-definition-" + eclipseVersion);
		Path pomFile = platformDir.resolve("pom.xml");
		Path targetFile = platformDir.resolve("platform-definition-" + eclipseVersion + ".target");
		Path projectFile = platformDir.resolve(".project");
		Path parentPom = root.resolve("pom.xml");

		if (Files.exists(platformDir) && Files.isDirectory(platformDir) && hasContent(platformDir)) {
			System.err.println("Platform definition already exists: " + platformDir);
			System.err.println("Use UpdateVersions to refresh an existing platform definition.");
			System.exit(1);
		}

		Path baseline = findLatestPlatformTarget(root, eclipseVersion);
		if (baseline == null) {
			System.err.println("No prior platform-definition-YYYY-MM/ found in " + root
					+ " to inherit non-Eclipse bundle versions from.");
			System.exit(1);
		}
		System.out.println("Inheriting non-Eclipse bundle versions from: " + baseline);

		Map<String, String> baselineVersions = new HashMap<>();
		Set<String> eclipseManagedIds = new HashSet<>();
		parseBaselineTarget(baseline, baselineVersions, eclipseManagedIds);

		System.out.println("Fetching unit versions for Eclipse " + eclipseVersion + "...");
		Map<String, String> eclipseFresh = ListVersions.getNewVersions(eclipseVersion);
		if (eclipseFresh.isEmpty()) {
			System.err.println("No versions retrieved for Eclipse " + eclipseVersion + ". Aborting.");
			System.exit(1);
		}

		Map<String, String> resolved = new HashMap<>(baselineVersions);
		for (String id : eclipseManagedIds) {
			String fresh = eclipseFresh.get(id);
			if (fresh != null) {
				resolved.put(id, fresh);
			}
		}

		String year = String.valueOf(LocalDate.now().getYear());
		String pomContent = applyTopLevelPlaceholders(loadTemplate(POM_TEMPLATE_RESOURCE), eclipseVersion, year);
		String targetContent = applyTopLevelPlaceholders(loadTemplate(TARGET_TEMPLATE_RESOURCE), eclipseVersion, year);
		String projectContent = applyTopLevelPlaceholders(loadTemplate(PROJECT_TEMPLATE_RESOURCE), eclipseVersion, year);
		targetContent = substituteUnitVersions(targetContent, resolved);

		List<String> unresolved = findUnresolvedPlaceholders(targetContent);
		if (!unresolved.isEmpty()) {
			System.err.println("Could not resolve all placeholders in target template:");
			for (String p : unresolved) {
				System.err.println("  " + p);
			}
			System.err.println("Add the missing bundle to a prior platform definition or update the template.");
			System.exit(1);
		}

		boolean parentNeedsModule = Files.exists(parentPom) && !parentPomHasModule(parentPom, eclipseVersion);

		List<String> plan = new ArrayList<>();
		plan.add("Create directory: " + platformDir);
		plan.add("Create file:      " + projectFile);
		plan.add("Create file:      " + pomFile);
		plan.add("Create file:      " + targetFile);
		if (parentNeedsModule) {
			plan.add("Add module to:    " + parentPom + "  (<module>platform-definition-" + eclipseVersion
					+ "</module>)");
		}

		System.out.println();
		System.out.println("Plan:");
		for (String line : plan) {
			System.out.println("  " + line);
		}
		System.out.println();

		if (!Prompts.yesNo("Proceed? [y/N]: ")) {
			System.out.println("Aborted. No files written.");
			return;
		}

		Files.createDirectories(platformDir);
		Files.write(projectFile, projectContent.getBytes(StandardCharsets.UTF_8));
		Files.write(pomFile, pomContent.getBytes(StandardCharsets.UTF_8));
		Files.write(targetFile, targetContent.getBytes(StandardCharsets.UTF_8));
		System.out.println("Wrote " + projectFile);
		System.out.println("Wrote " + pomFile);
		System.out.println("Wrote " + targetFile);

		if (parentNeedsModule) {
			addModuleToParentPom(parentPom, eclipseVersion);
			System.out.println("Updated " + parentPom);
		}
	}

	private static boolean hasContent(Path dir) throws IOException {
		try (var stream = Files.list(dir)) {
			return stream.findAny().isPresent();
		}
	}

	private static Path findLatestPlatformTarget(Path root, String excludeVersion) throws IOException {
		String latest = null;
		Path latestTarget = null;
		try (var stream = Files.list(root)) {
			for (Path p : (Iterable<Path>) stream::iterator) {
				if (!Files.isDirectory(p)) {
					continue;
				}
				Matcher m = PLATFORM_DIR_PATTERN.matcher(p.getFileName().toString());
				if (!m.matches()) {
					continue;
				}
				String ver = m.group(1);
				if (ver.equals(excludeVersion)) {
					continue;
				}
				Path tgt = p.resolve("platform-definition-" + ver + ".target");
				if (!Files.exists(tgt)) {
					continue;
				}
				if (latest == null || ver.compareTo(latest) > 0) {
					latest = ver;
					latestTarget = tgt;
				}
			}
		}
		return latestTarget;
	}

	private static void parseBaselineTarget(Path baseline, Map<String, String> versions, Set<String> eclipseManagedIds)
			throws IOException {
		String content = new String(Files.readAllBytes(baseline), StandardCharsets.UTF_8);
		Matcher locM = LOCATION_PATTERN.matcher(content);
		while (locM.find()) {
			String block = locM.group(1);
			boolean isEclipse = false;
			Matcher repoM = REPOSITORY_PATTERN.matcher(block);
			if (repoM.find()) {
				isEclipse = ECLIPSE_RELEASE_PATTERN.matcher(repoM.group(1)).matches();
			}
			Matcher unitM = UNIT_PATTERN.matcher(block);
			while (unitM.find()) {
				String id = unitM.group(1);
				String ver = unitM.group(2);
				versions.put(id, ver);
				if (isEclipse) {
					eclipseManagedIds.add(id);
				}
			}
		}
	}

	private static String loadTemplate(String resourceName) throws IOException {
		try (InputStream in = CreatePlatform.class.getResourceAsStream(resourceName)) {
			if (in == null) {
				throw new IOException("Template resource not found on classpath: " + resourceName);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static String applyTopLevelPlaceholders(String template, String eclipseVersion, String year) {
		return template.replace("${ECLIPSE_VERSION}", eclipseVersion).replace("${COPYRIGHT_YEAR}", year);
	}

	private static String substituteUnitVersions(String content, Map<String, String> versions) {
		StringBuffer sb = new StringBuffer();
		Matcher m = UNIT_VERSION_PLACEHOLDER.matcher(content);
		while (m.find()) {
			String id = m.group(1);
			String ver = versions.get(id);
			if (ver == null) {
				// Leave the placeholder in place; the unresolved-check below will report it.
				m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
			} else {
				m.appendReplacement(sb, Matcher.quoteReplacement(ver));
			}
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private static List<String> findUnresolvedPlaceholders(String content) {
		List<String> out = new ArrayList<>();
		Matcher m = ANY_PLACEHOLDER.matcher(content);
		while (m.find()) {
			out.add(m.group());
		}
		return out;
	}

	static boolean parentPomHasModule(Path parentPom, String eclipseVersion) throws IOException {
		String content = new String(Files.readAllBytes(parentPom), StandardCharsets.UTF_8);
		return content.contains("<module>platform-definition-" + eclipseVersion + "</module>");
	}

	static void addModuleToParentPom(Path parentPom, String eclipseVersion) throws IOException {
		String content = new String(Files.readAllBytes(parentPom), StandardCharsets.UTF_8);
		Pattern firstModule = Pattern.compile("(?m)^([ \\t]*)<module>platform-definition-\\d{4}-\\d{2}</module>\\s*\\R");
		Matcher m = firstModule.matcher(content);
		String moduleEntry = "<module>platform-definition-" + eclipseVersion + "</module>";
		String updated;
		if (m.find()) {
			String indent = m.group(1);
			updated = content.substring(0, m.start()) + indent + moduleEntry + System.lineSeparator()
					+ content.substring(m.start());
		} else {
			Pattern modulesOpen = Pattern.compile("(?m)^([ \\t]*)<modules>\\s*\\R");
			Matcher mo = modulesOpen.matcher(content);
			if (!mo.find()) {
				throw new IOException("Could not find <modules> in parent pom: " + parentPom);
			}
			String indent = mo.group(1) + "\t";
			updated = content.substring(0, mo.end()) + indent + moduleEntry + System.lineSeparator()
					+ content.substring(mo.end());
		}
		Files.write(parentPom, updated.getBytes(StandardCharsets.UTF_8));
	}
}
