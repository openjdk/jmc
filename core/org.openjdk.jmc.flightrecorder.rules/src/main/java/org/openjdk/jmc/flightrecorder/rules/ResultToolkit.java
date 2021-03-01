/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.rules;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFormatToolkit;
import org.owasp.encoder.Encode;

public class ResultToolkit {

	private static final Pattern TEMPLATE_PATTERN = Pattern.compile("(\\{.*?\\})"); //$NON-NLS-1$
	private static final FrameSeparator DEFAULT_SEPARATOR = new FrameSeparator(FrameCategorization.LINE, false);

	/**
	 * Populates the result message with result data.
	 * 
	 * @param result
	 *            the result to get the data from
	 * @param string
	 *            the message to populate
	 * @return a message populated with the formatted result values
	 */
	public static String populateMessage(IResult result, String string, boolean withHtml) {
		if (string == null) {
			return string;
		}
		String s = string;
		Matcher matcher = TEMPLATE_PATTERN.matcher(s);
		while (matcher.find()) {
			for (int i = 0; i < matcher.groupCount(); i++) {
				String group = matcher.group(i);
				String subGroup = group.substring(1, group.length() - 1);
				TypedResult<?> typedResult = getResultById(result.getRule(), subGroup);
				if (typedResult != null) {
					if (typedResult instanceof TypedCollectionResult<?>) {
						if (withHtml) {
							StringBuilder collection = new StringBuilder();
							collection.append("<ul>"); //$NON-NLS-1$
							Collection<?> results = result.getResult((TypedCollectionResult<?>) typedResult);
							for (Object o : results) {
								collection.append("<li>"); //$NON-NLS-1$
								if (o instanceof IDisplayable) {
									collection
											.append(Encode.forHtml(((IDisplayable) o).displayUsing(IDisplayable.AUTO)));
								} else if (o instanceof IMCFrame) {
									collection.append(Encode.forHtml(
											StacktraceFormatToolkit.formatFrame((IMCFrame) o, DEFAULT_SEPARATOR)));
								} else if (o instanceof IMCStackTrace) {
									collection
											.append(Encode.forHtml(FormatToolkit.getHumanReadable((IMCStackTrace) o)));
								} else {
									collection.append(Encode.forHtml(typedResult.format(o)));
								}
								collection.append("</li>"); //$NON-NLS-1$
							}
							collection.append("</ul>"); //$NON-NLS-1$
							s = s.replace(group, collection.toString());
						} else {
							Collection<?> results = result.getResult((TypedCollectionResult<?>) typedResult);
							String joined = StringToolkit.join(results, ","); //$NON-NLS-1$
							s = s.replace(group, encodeIfNeeded(joined, withHtml));
						}
					} else {
						Object typedResultInstance = result.getResult(typedResult);
						if (typedResultInstance != null) {
							if (typedResultInstance instanceof IDisplayable) {
								s = s.replace(group,
										encodeIfNeeded(
												((IDisplayable) typedResultInstance).displayUsing(IDisplayable.AUTO),
												withHtml));
							} else if (typedResultInstance instanceof IMCFrame) {
								s = s.replace(group, encodeIfNeeded(StacktraceFormatToolkit
										.formatFrame((IMCFrame) typedResultInstance, DEFAULT_SEPARATOR), withHtml));
							} else if (typedResultInstance instanceof IMCStackTrace) {
								s = s.replace(group, encodeIfNeeded(
										FormatToolkit.getHumanReadable((IMCStackTrace) typedResultInstance), withHtml));
							} else {
								s = s.replace(group, encodeIfNeeded(typedResult.format(typedResultInstance), withHtml));
							}
						}
					}
				} else {
					TypedPreference<?> typedPreference = getPreferenceById(result.getRule(), subGroup);
					if (typedPreference != null) {
						Object preference = result.getPreference(typedPreference);
						if (preference instanceof IDisplayable) {
							s = s.replace(group, encodeIfNeeded(
									((IDisplayable) preference).displayUsing(IDisplayable.AUTO), withHtml));
						} else {
							s = s.replace(group, encodeIfNeeded(preference.toString(), withHtml));
						}
					}
				}
			}
		}
		return s;
	}

	private static String encodeIfNeeded(String input, boolean shouldEncode) {
		if (shouldEncode) {
			return Encode.forHtml(input);
		}
		return input;
	}

	private static TypedResult<?> getResultById(IRule rule, String identifier) {
		Collection<TypedResult<?>> results = rule.getResults();
		if (results != null) {
			for (TypedResult<?> typedResult : results) {
				if (typedResult.getIdentifier().equals(identifier)) {
					return typedResult;
				}
			}
		}
		return null;
	}

	private static TypedPreference<?> getPreferenceById(IRule rule, String identifier) {
		Collection<TypedPreference<?>> preferences = rule.getConfigurationAttributes();
		if (preferences != null) {
			for (TypedPreference<?> typedPreference : preferences) {
				if (typedPreference.getIdentifier().equals(identifier)) {
					return typedPreference;
				}
			}
		}
		return null;
	}

}
