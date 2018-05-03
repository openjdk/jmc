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
package org.openjdk.jmc.flightrecorder.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.io.IOToolkit;

/**
 * Contain algorithmic conversion and overrides for the display name of event path segments. Also,
 * algorithmic generation and overrides for the color of event types. For these purposes, a case
 * insensitive matching of the path is made. (That is, it is converted to the canonical lower case
 * representation.)
 */
/*
 * FIXME: This class provides knowledge about JDK events. It would make sense to make this
 * extendable.
 * 
 * FIXME: This class (or to be precise, the flightrecorder.internal package) is exposed using a
 * friends-only export to flightrecorder.configuration. We could instead move it to for example the
 * flightrecorder.jdk package and perhaps rename it to something more related to path segments.
 */
public class EventAppearance {
	private static final Pattern PATH_SPLIT_REGEX = Pattern.compile("\\/"); //$NON-NLS-1$
	private static final Map<String, String> HUMAN_NAMES;
	static {
		{
			Properties props = loadProperties("segments.properties"); //$NON-NLS-1$
			HashMap<String, String> segments = new HashMap<>(props.size());
			for (Entry<Object, Object> entry : props.entrySet()) {
				String key = (String) entry.getKey();
				segments.put(key.toLowerCase(Locale.ENGLISH), (String) entry.getValue());
			}
			HUMAN_NAMES = Collections.unmodifiableMap(segments);
		}
	}

	private static Properties loadProperties(String fileName) {
		// Reading through Properties now, for simplicity.
		// Might change to ResourceBundle, or do as FieldToolkit (or NLS),
		// if localization is needed. (Which I doubt, since it would be confusing.)
		Properties properties = new Properties();
		InputStream in = EventAppearance.class.getResourceAsStream(fileName);
		if (in != null) {
			try {
				properties.load(in);
			} catch (IOException e) {
				System.err.println("Problem loading file '" + fileName + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				e.printStackTrace();
			} finally {
				IOToolkit.closeSilently(in);
			}
		} else {
			System.err.println("Couldn't find file '" + fileName + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return properties;
	}

	public static String[] getHumanSegmentArray(String path) {
		String[] pathArray = PATH_SPLIT_REGEX.split(path);
		for (int i = 0; i < pathArray.length; i++) {
			pathArray[i] = getHumanSegmentName(pathArray[i].trim());
		}
		return pathArray;
	}

	private static String getHumanSegmentName(String path) {
		path = path.toLowerCase(Locale.ENGLISH);
		String humanName = HUMAN_NAMES.get(path);
		if (humanName != null) {
			return humanName;
		}
		// NOTE: In order to be thread safe, do not save the human readable name.
		return humanifyName(path);
	}

	// Migrated from PathDescriptorRepository
	public static String humanifyName(String identifier) {
		if (identifier == null) {
			return null;
		} else if (identifier.length() == 0) {
			// This is so that malformed event paths, should they get this far, are noticed and not hidden.
			return "<Empty>"; //$NON-NLS-1$
		}

		StringBuilder humanReadable = new StringBuilder(identifier.length());
		boolean firstLetter = true;
		for (int n = 0; n < identifier.length(); n++) {
			char c = identifier.charAt(n);
			if (c == '_') {
				c = ' ';
			}
			if (firstLetter && Character.isLetter(c)) {
				humanReadable.append(Character.toUpperCase(c));
			} else {
				humanReadable.append(c);
			}
			firstLetter = (c == ' ');
		}
		return humanReadable.toString();
	}
}
