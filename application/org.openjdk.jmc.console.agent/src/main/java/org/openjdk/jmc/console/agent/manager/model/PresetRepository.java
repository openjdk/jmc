/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2025, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.console.agent.manager.model;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PresetRepository {
	private static final String DEFAULT_FILE_NAME = "new_preset.xml"; // $NON-NLS-1$
	private static final String FILE_NAME_EXTENSION = ".xml"; // $NON-NLS-1$

	private static final Pattern NAME_WITH_COUNT_PATTERN = Pattern.compile("^(.*)_(\\d+)$"); // $NON-NLS-1$
	private static final Pattern COUNT_SUFFIX_PATTERN = Pattern.compile("^_(\\d+)$"); // $NON-NLS-1$

	private List<IPreset> presets = new ArrayList<>();

	protected PresetRepository() {
	}

	public void removePreset(IPreset configuration) {
		if (presets.remove(configuration)) {
			configuration.delete();
		}
	}

	public void addPreset(IPreset configuration) throws IOException {
		presets.add(configuration);

		if (configuration.getStorageDelegate() == null) {
			configuration.setStorageDelegate(LocalStorageDelegate.getDelegate(configuration.getFileName()));
		}

		configuration.save();
	}

	public boolean containsPreset(IPreset configuration) {
		return presets.contains(configuration);
	}

	public IPreset[] listPresets() {
		return presets.toArray(new IPreset[0]);
	}

	public void updatePreset(IPreset original, IPreset workingCopy) throws IOException {
		if (containsPreset(original)) {
			removePreset(original);
			original.delete();

			addPreset(workingCopy);
		}
	}

	public IPreset createPreset() {
		String fileName = nextUniqueName(DEFAULT_FILE_NAME);
		Preset preset = new Preset(this);
		preset.setFileName(fileName);

		return preset;
	}

	public void importPreset(File file) throws IOException, SAXException {
		IPreset preset = createPreset();
		preset.setFileName(nextUniqueName(file.getName()));
		try (FileInputStream fis = new FileInputStream(file)) {
			preset.deserialize(fis);
		}
		addPreset(preset);
	}

	public void exportPreset(IPreset preset, File file) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(preset.serialize().getBytes(StandardCharsets.UTF_8));
		}
	}

	String nextUniqueName(String originalName) {
		originalName = originalName.trim();

		// First, extract a base name and a count of the original name.
		String baseName = originalName;
		if (baseName.endsWith(FILE_NAME_EXTENSION)) {
			baseName = baseName.substring(0, baseName.lastIndexOf(FILE_NAME_EXTENSION));
		}
		// Use count -1 to mean that no count should be appended, the baseName suffices.
		long proposedCount = -1;
		Matcher matcher = NAME_WITH_COUNT_PATTERN.matcher(originalName);
		if (matcher.matches()) {
			try {
				long count = Long.parseLong(matcher.group(2));
				// Valid match, use the shorter base and this count.
				baseName = matcher.group(1).trim();
				proposedCount = count;
			} catch (NumberFormatException e) {
				// Too large number. => Use the entire name as base.
				// (Yes, we could have used BigInteger, but which sane person would want such names?)
			}
		}

		// Second, find any existing templates matching the proposed baseName pattern,
		// with or without count, and make sure the proposed count is greater.
		int baseLen = baseName.length();
		for (IPreset preset : presets) {
			String tempName = preset.getFileName().trim();
			if (tempName.endsWith(FILE_NAME_EXTENSION)) {
				tempName = tempName.substring(0, tempName.lastIndexOf(FILE_NAME_EXTENSION));
			}
			if (tempName.startsWith(baseName)) {
				if (tempName.equals(baseName) && (proposedCount < 1)) {
					proposedCount = 1;
				} else {
					// Note that this pattern must ignore leading whitespace.
					Matcher tempMatch = COUNT_SUFFIX_PATTERN.matcher(tempName.substring(baseLen));
					if (tempMatch.matches()) {
						try {
							long count = Long.parseLong(tempMatch.group(1));
							if (count < Long.MAX_VALUE) {
								// Valid match, use a count greater than this, unless the proposed was greater.
								proposedCount = Math.max(proposedCount, count + 1);
							}
						} catch (NumberFormatException e) {
							// Too large number, pretend we didn't see this template.
						}
					}
				}
			}
		}
		if (proposedCount == -1) {
			return baseName + FILE_NAME_EXTENSION;
		} else {
			return baseName + '_' + proposedCount + FILE_NAME_EXTENSION;
		}
	}
}
