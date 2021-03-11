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

import java.util.StringTokenizer;

import org.openjdk.jmc.common.resource.Resource;

/**
 */
public final class NamingRule {
	private final String normalizedExpression;
	private final Matcher matcher;
	private final Formatter formatter;
	private final int priority;
	private final String name;
	private final Resource icon;

	public NamingRule(String name, String matchingPart, String formattingPart, int priority, Resource icon) {
		this(name, createNormalizedExpression(matchingPart, formattingPart), priority, icon);
	}

	public NamingRule(String name, String normalizedExpression, int priority, Resource icon) {
		this.normalizedExpression = normalizedExpression;
		if (normalizedExpression.equals("")) { //$NON-NLS-1$
			throw new IllegalArgumentException("Invalid rule - you may not add empty rules!"); //$NON-NLS-1$
		}
		String expandedExpression = replaceConstants(normalizedExpression);
		String[] expressions = expandedExpression.split("=>"); //$NON-NLS-1$
		if (expressions.length != 2) {
			throw new IllegalArgumentException("Invalid rule - need both match part and transform part."); //$NON-NLS-1$
		}
		matcher = new Matcher(expressions[0]);
		formatter = new Formatter(expressions[1]);
		this.priority = priority;
		this.icon = icon;
		this.name = name;
	}

	private static String createNormalizedExpression(String matchPart, String formattingPart) {
		return matchPart + "=>" + formattingPart; //$NON-NLS-1$
	}

	/**
	 * @return the full naming rule in a single string expression on the format:
	 *         matchPart=&gt;formatPart.
	 */
	public String getNormalizedExpression() {
		return normalizedExpression;
	}

	public boolean matches(Object[] values) {
		return matcher.matches(values);
	}

	public String format(Object[] values) {
		return formatter.format(values);
	}

	@Override
	public String toString() {
		return "MatchingRule [name: " + name + ", expression: " + normalizedExpression + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	public int hashCode() {
		return normalizedExpression.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof NamingRule)) {
			return false;
		}
		return normalizedExpression.equals(((NamingRule) obj).normalizedExpression);
	}

	public int getPriority() {
		return priority;
	}

	public String getName() {
		return name;
	}

	private static String replaceConstants(String rule) {
		StringBuffer buffer = new StringBuffer();
		StringTokenizer stringTokenizer = new StringTokenizer(rule, "{}", true); //$NON-NLS-1$
		while (stringTokenizer.hasMoreTokens()) {
			String token = stringTokenizer.nextToken();
			if (token.equals("{")) { //$NON-NLS-1$
				if (!stringTokenizer.hasMoreTokens()) {
					throw new IllegalArgumentException("Found unmatched { in rule!"); //$NON-NLS-1$
				}
				String content = stringTokenizer.nextToken();
				if (Constants.getInstance().containsConstant(content)) {
					token = Constants.getInstance().getConstant(content).toString();
					stringTokenizer.nextToken();
				} else {
					token += content;
				}
			}
			buffer.append(token);
		}
		return buffer.toString();
	}

	public Resource getImageResource() {
		return icon;
	}
}
