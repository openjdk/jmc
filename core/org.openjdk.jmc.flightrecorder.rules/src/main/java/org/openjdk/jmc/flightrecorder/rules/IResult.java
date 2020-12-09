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

import org.openjdk.jmc.common.util.TypedPreference;

/**
 * Interface specifying result objects for use with {@link IRule} implementations. The default use
 * is to use {@link ResultBuilder}, but this interface exists to allow further customization by
 * downstream users.
 */
public interface IResult {

	/**
	 * Returns an enum of type {@link Severity} describing the importance of this result.
	 * 
	 * @return a {@linkplain Severity} describing the result
	 */
	Severity getSeverity();

	/**
	 * Returns the {@link IRule} that generated this result object.
	 * 
	 * @return the {@linkplain IRule} that generated this result
	 */
	IRule getRule();

	/**
	 * A short, one sentence, summary of the results of the rule evaluation. This is intended to
	 * give a very quick overview of what the rule detected. If the severity is {@code OK} or
	 * {@code NA} this should not be expected to contain any information, but it may. If the
	 * severity is {@code INFO} or {@code WARNING} this is expected to provide a very short summary
	 * of the found problem.
	 * 
	 * @return a short text describing the findings of the rule
	 */
	String getSummary();

	/**
	 * A more detailed explanation of what kind of problem was identified and why the identified
	 * issue is a problem. This field is always optional, but when it exists it may be very long and
	 * detailed.
	 * 
	 * @return a detailed explanation of why and how the result is relevant
	 */
	String getExplanation();

	/**
	 * An attempted solution for the identified problem. This field is always optional, but if
	 * included it can e.g. specify new JVM flags or suggest changes in the source code.
	 * 
	 * @return a text describing a possible solution for the found problem
	 */
	String getSolution();

	/**
	 * An optional field potentially used for rules that have a set of recording settings that may
	 * help the rule return more detailed information.
	 * 
	 * @return a set of recording settings
	 */
	Collection<IRecordingSetting> suggestRecordingSettings();

	/**
	 * Returns a collection of typed instances of a result contained in this result instance, i.e.
	 * is contained in {@code IRule.getResults()}.
	 * 
	 * @param <T>
	 *            a type parameter
	 * @param result
	 *            the typed result that is to be retrieved
	 * @return a typed result instance
	 */
	<T> Collection<T> getResult(TypedCollectionResult<T> result);

	/**
	 * Returns a preference value that was used when evaluating the rule for this particular result.
	 * 
	 * @param <T>
	 *            a type parameter
	 * @param preference
	 *            a preference used by this rule, i.e. contained in
	 *            {@code IRule.getConfigurationAttributes()}
	 * @return a preference value
	 */
	<T> T getPreference(TypedPreference<T> preference);

	/**
	 * Returns a typed instance of a result contained in this result instance, i.e. is contained in
	 * {@code IRule.getResults()}.
	 * 
	 * @param <T>
	 *            a type parameter
	 * @param result
	 *            the typed result that is to be retrieved
	 * @return a typed result instance
	 */
	<T> T getResult(TypedResult<T> result);
}
