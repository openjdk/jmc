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
package org.openjdk.jmc.flightrecorder.rules;

import org.openjdk.jmc.common.item.IItemQuery;

/**
 * A result from evaluating a rule. It contains a score value that shows how severe the result
 * should be considered, text descriptions that can be used to tell the user about found problems
 * (and possible solutions), and a query that can be used to identify specific data items related to
 * the result.
 */
public final class Result {

	/**
	 * Magic numbers for specific cases where an IRule returns a Result without a score
	 */
	public static final double NOT_APPLICABLE = -1;
	public static final double FAILED = -2;
	public static final double IGNORE = -3;
	// JFR GUI specific magic number to indicate rule being evaluated
	private static final double IN_PROGRESS = -200;

	private final IRule rule;
	private final double score;
	private final String shortDescription;
	private final String longDescription;
	private final IItemQuery query;

	public Result(IRule rule, double score, String shortDescription) {
		this(rule, score, shortDescription, null, null);
	}

	public Result(IRule rule, double score, String shortDescription, String longDescription) {
		this(rule, score, shortDescription, longDescription, null);
	}

	public Result(IRule rule, double score, String shortDescription, String longDescription, IItemQuery query) {
		if (rule == null) {
			throw new IllegalArgumentException("Rule parameter cannot be null."); //$NON-NLS-1$
		}
		if (Double.isInfinite(score) || Double.isNaN(score)) {
			throw new IllegalArgumentException("Score cannot not be infinite or NaN."); //$NON-NLS-1$
		}
		if (!isValidScore(score)) {
			throw new IllegalArgumentException(
					"Score must be greater than or equal to 0 and less than or equal to 100."); //$NON-NLS-1$
		}
		if (shortDescription == null) {
			throw new IllegalArgumentException("Short description cannot be null."); //$NON-NLS-1$
		}
		this.rule = rule;
		this.score = score;
		this.shortDescription = shortDescription;
		this.longDescription = longDescription;
		this.query = query;
	}

	/**
	 * @param score
	 *            the score to validate
	 * @return {@code true} if score is in interval 0.0 <= score <= 100.0 or if it is one of the
	 *         defined magic numbers (including -200 for JMC UI purposes) {@code false} otherwise
	 */
	private boolean isValidScore(double score) {
		if (Double.compare(score, 0) < 0) {
			return Double.compare(score, NOT_APPLICABLE) == 0 || Double.compare(score, FAILED) == 0
					|| Double.compare(score, IGNORE) == 0 || Double.compare(score, IN_PROGRESS) == 0;
		}
		return Double.compare(score, 100) <= 0;
	}

	/**
	 * The rule which generated this result.
	 *
	 * @return the rule creating this result
	 */
	public IRule getRule() {
		return rule;
	}

	/**
	 * A score between 0 and 100 where 0 means "no problem" and 100 means "big problem". A score
	 * below zero means that the rule could not perform the evaluation for some reason. The score
	 * can be passed into {@link Severity#get(double)} to get a matching {@link Severity} value.
	 *
	 * @return the score for this result
	 */
	public double getScore() {
		return score;
	}

	/**
	 * @return a short text message describing the result
	 */
	public String getShortDescription() {
		return shortDescription;
	}

	/**
	 * @return A longer text message describing the result. Can be {@code null} if no long
	 *         description is provided.
	 */
	public String getLongDescription() {
		if (longDescription != null) {
			return longDescription;
		}
		return shortDescription;
	}

	/**
	 * @return the query of the result
	 */
	public IItemQuery getItemQuery() {
		return query;
	}

	@Override
	public String toString() {
		return String.format("[(%E): %s, %s]", getScore(), getShortDescription(), getRule().getTopic()); //$NON-NLS-1$
	}
}
