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
package org.openjdk.jmc.rjmx.triggers.internal;

import java.util.logging.Logger;

import org.w3c.dom.Element;

import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.rjmx.triggers.IValueEvaluator;

/**
 * Standard evaluator. Evaluates to true if the value matches a wildcard string.
 */
public final class ValueEvaluatorStringMatch implements IValueEvaluator {
	// The logger.
	private final static Logger LOGGER = Logger.getLogger("org.openjdk.jmc.rjmx.triggers"); //$NON-NLS-1$

	private static final String XML_ELEMENT_MATCHSTRING = "maxvalue"; //$NON-NLS-1$

	private String m_matchString;

	/**
	 * Constructor. Used when constructing from XML.
	 */
	public ValueEvaluatorStringMatch() {
	}

	/**
	 * Constructor.
	 *
	 * @param matchString
	 *            see class comment.
	 */
	public ValueEvaluatorStringMatch(String matchString) {
		m_matchString = matchString;
	}

	/**
	 * @see IValueEvaluator#triggerOn(Object)
	 */
	@Override
	public boolean triggerOn(Object val) {
		if (val != null && m_matchString != null) {
			boolean result = matcher(val.toString(), m_matchString);
			LOGGER.info("ValueEvaluatorStringMatch:" + val.toString() + " matches " + m_matchString + " = " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ result);
			return result;
		} else {
			return false;
		}
	}

	/**
	 * Naive implementation of a wildcard matcher.
	 * <p>
	 * Assumes s != null and matchString != null
	 */
	private boolean matcher(String s, String matchString) {
		if (s.length() == 0) {
			return matchString.length() == 0 || matchString.equals("*"); //$NON-NLS-1$
		}
		if (matchString.length() == 0) {
			return false;
		}
		if (matchString.charAt(0) == '*') // recursive check
		{
			for (int n = 0; n <= s.length(); n++) {
				if (matcher(s.substring(n), matchString.substring(1))) {
					return true;
				}
			}
		} else if (matchString.charAt(0) == '?') {
			return matcher(s.substring(1), matchString.substring(1));
		} else if (matchString.charAt(0) == s.charAt(0)) {
			return matcher(s.substring(1), matchString.substring(1));
		}

		return false;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "value matches " + m_matchString; //$NON-NLS-1$
	}

	/**
	 * @return long the max to evaluate against.
	 */
	public String getMatchString() {
		return m_matchString;
	}

	/**
	 * Sets the match string to evaluate against.
	 *
	 * @param matchString
	 *            the new match string.
	 */
	public void setMatchString(String matchString) {
		m_matchString = matchString;
	}

	@Override
	public void initializeEvaluatorFromXml(Element node) {
		setMatchString(XmlToolkit.getSetting(node, XML_ELEMENT_MATCHSTRING, "*")); //$NON-NLS-1$
	}

	@Override
	public void exportEvaluatorToXml(Element node) {
		XmlToolkit.setSetting(node, XML_ELEMENT_MATCHSTRING, getMatchString());
	}

	@Override
	public String getOperatorString() {
		return " matches"; //$NON-NLS-1$
	}

	@Override
	public String getEvaluationConditionString() {
		return "matches " + m_matchString; //$NON-NLS-1$
	}
}
