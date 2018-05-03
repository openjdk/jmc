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
package org.openjdk.jmc.ui.common.util;

import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * A string matcher for use in the Mission Control UI.
 */
public class FilterMatcher {
	/**
	 * Prefix to use for indicating that a match string is a regular expression. For use in
	 * {@link #match(String, String, boolean)}.
	 */
	public final static String REGEXP_PREFIX = "regexp:"; //$NON-NLS-1$

	private final static char KLEENE_STAR = '*';
	private final static String KLEENE_STAR_STRING = Character.toString(KLEENE_STAR);
	private final static String QUESTION_STRING = "?"; //$NON-NLS-1$
	private final static String SPACE_STRING = " "; //$NON-NLS-1$

	private final static FilterMatcher instance = new FilterMatcher();

	private static volatile Pattern lastPattern = null;

	public static enum Where {
		BEFORE, AFTER, BEFORE_AND_AFTER
	};

	/**
	 * @return a singleton instance
	 */
	public static FilterMatcher getInstance() {
		return instance;
	}

	/**
	 * Match a string to an expression.
	 *
	 * @param stringToFilter
	 *            the string to check
	 * @param filterString
	 *            The matching expression. If it starts with {@value #REGEXP_PREFIX}, then it is
	 *            treated as a regular expression. Otherwise, only Kleene star (any string) and
	 *            question mark (any character) matching will be performed.
	 * @param caseInsensitive
	 *            True if case should not matter. Ignored if the filter string is a regular
	 *            expression.
	 * @return {@code true} if the stringToFilter matches the filterString, {@code false} otherwise.
	 */
	public boolean match(String stringToFilter, String filterString, boolean caseInsensitive) {
		if (isUseRegexp(filterString)) {
			String trimmedFilterString = filterString.substring(REGEXP_PREFIX.length()).trim();
			return regexpMatch(stringToFilter, trimmedFilterString);
		}
		return stringToFilter != null && regexpMatch(stringToFilter, kleeneToRegexp(filterString, caseInsensitive));
	}

	/**
	 * Match a string using a regular expression.
	 *
	 * @param stringToFilter
	 *            the string to check
	 * @param filterString
	 *            the matching expression
	 * @return true if the stringToFilter matches the filterString, false otherwise.
	 */
	public boolean regexpMatch(String stringToFilter, String filterString) {
		// Keep a reference to the cached pattern for use during this call
		Pattern pattern = lastPattern;
		if (pattern == null || !pattern.pattern().equals(filterString)) {
			try {
				pattern = Pattern.compile(filterString);
			} catch (Exception e) {
				return false;
			}
			/*
			 * Might be destroyed by other threads, but unlikely since this is mostly used by the UI
			 * thread only, and we will still be okay even if overwritten.
			 */
			lastPattern = pattern;
		}
		return pattern.matcher(stringToFilter).matches();
	}

	/**
	 * Add Kleene stars to the expression. Will not add duplicate stars.
	 *
	 * @param filterText
	 *            the match expression to add Kleene stars to
	 * @param where
	 *            where to add the Kleene stars
	 * @return the modified match expression
	 */
	public static String autoAddKleene(String filterText, Where where) {
		/*
		 * People using regexp matching don't want unexpected things to happen to their match
		 * expressions.
		 */
		if (isUseRegexp(filterText)) {
			return filterText;
		}
		String newText = filterText;
		if (where == Where.BEFORE || where == Where.BEFORE_AND_AFTER) {
			if (!filterText.startsWith(FilterMatcher.KLEENE_STAR_STRING)) {
				newText = FilterMatcher.KLEENE_STAR + newText;
			}
		}
		if (where == Where.AFTER || where == Where.BEFORE_AND_AFTER) {
			if (!filterText.endsWith(FilterMatcher.KLEENE_STAR_STRING)) {
				newText = newText + FilterMatcher.KLEENE_STAR;
			}
		}
		return newText;
	}

	private static boolean isUseRegexp(String filterString) {
		if (filterString.startsWith(REGEXP_PREFIX)) {
			return true;
		}
		return false;
	}

	private static String kleeneToRegexp(String kleene, boolean caseInsensitive) {
		StringBuilder sb = new StringBuilder();
		if (caseInsensitive) {
			sb.append("(?iu)"); //$NON-NLS-1$
		}
		StringTokenizer st = new StringTokenizer(kleene, KLEENE_STAR_STRING + SPACE_STRING + QUESTION_STRING, true);
		while (st.hasMoreTokens()) {
			String s = st.nextToken();
			if (KLEENE_STAR_STRING.equals(s)) {
				s = ".*"; //$NON-NLS-1$
			} else if (QUESTION_STRING.equals(s)) {
				s = "."; //$NON-NLS-1$
			} else if (SPACE_STRING.equals(s)) {
				// Also match non-breaking spaces
				s = "[ \u00a0]"; //$NON-NLS-1$
			} else {
				s = Pattern.quote(s);
			}
			sb.append(s);
		}
		return sb.toString();
	}
}
