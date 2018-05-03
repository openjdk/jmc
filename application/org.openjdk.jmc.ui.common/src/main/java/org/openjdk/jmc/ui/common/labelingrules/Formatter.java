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
package org.openjdk.jmc.ui.common.labelingrules;

import java.text.MessageFormat;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for doing the formatting.
 */
final class Formatter {
	private static final char REGEXP_DELIMITER = ':';
	private final String formatPart;

	public Formatter(String formatPart) {
		this.formatPart = formatPart;
	}

	/**
	 * @param vals
	 *            the array of values to format.
	 * @return a formatted string representing the values according to the formatting string
	 *         supplied on creation.
	 */
	public String format(Object[] vals) {
		return MessageFormat.format(Formatter.replaceVariables(formatPart, vals), vals).trim();
	}

	static String replaceVariables(String formatString, Object[] input) {
		StringBuffer buffer = new StringBuffer();
		StringTokenizer stringTokenizer = new StringTokenizer(formatString, "{}", true); //$NON-NLS-1$
		while (stringTokenizer.hasMoreTokens()) {
			String token = stringTokenizer.nextToken();
			if (token.equals("{")) { //$NON-NLS-1$
				if (!stringTokenizer.hasMoreTokens()) {
					throw new IllegalArgumentException("Found unmatched { in rule!"); //$NON-NLS-1$
				}
				String content = stringTokenizer.nextToken();
				String regexp = getRegexpPart(content);

				if (regexp != null) {
					content = getVariablePart(content);
				}

				if (Variables.getInstance().containsVariable(content)) {
					token = Variables.getInstance().getVariable(content).evaluate(input);
					if (regexp != null) {
						token = apply(regexp, token);
					}
					stringTokenizer.nextToken();
				} else {
					token = "{" + content; //$NON-NLS-1$
				}
			}
			buffer.append(token);
		}
		return buffer.toString();
	}

	/**
	 * Applies the regular expression on the content and returns the first capture group for each
	 * match appended together. The idea is to only have one. We could start supporting more if we
	 * need to.
	 *
	 * @param regexp
	 *            the regular expression (should currently contain at least one capture group, and
	 *            only the first will be used).
	 * @param content
	 *            the content to which apply the regular expression.
	 * @return the matching capture group for every match found, appended together.
	 */
	private static String apply(String regexp, String content) {
		StringBuilder builder = new StringBuilder();
		Pattern pattern = Pattern.compile(regexp);
		Matcher matcher = pattern.matcher(content);

		while (matcher.find()) {
			builder.append(matcher.group(1));
		}
		return builder.toString();
	}

	private static String getVariablePart(String content) {
		int index = content.indexOf(REGEXP_DELIMITER);
		return content.substring(0, index);
	}

	private static String getRegexpPart(String content) {
		int index = content.indexOf(REGEXP_DELIMITER);
		if (index < 0) {
			return null;
		}
		return content.substring(index + 1);
	}

}
