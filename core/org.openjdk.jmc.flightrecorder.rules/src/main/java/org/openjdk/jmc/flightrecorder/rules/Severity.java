/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.rules;

import org.openjdk.jmc.flightrecorder.rules.messages.internal.Messages;

/**
 * The severity of a rule result.
 * <p>
 * The constants are declared in ascending order of severity, so that the natural ordering of this
 * enum agrees with the ordering of the severity scores returned by {@link #getLimit()}. Keep it
 * that way when adding constants: code that compares severities relies on it. Prefer
 * {@link #isAtLeast(Severity)} over {@link #compareTo(Object)} when filtering, since it states the
 * intent and compares the scores directly.
 */
public enum Severity {

	/**
	 * Results with this severity score should not be presented at all, because the rule does not
	 * apply in a way that is worth reporting.
	 */
	IGNORE(-3, Messages.getString(Messages.Severity_IGNORE)),
	/**
	 * Results with this severity score are not applicable to the recording, but it should be
	 * possible to view them so the user can see which results have not been possible to evaluate.
	 */
	NA(-1, Messages.getString(Messages.Severity_NOT_APPLICABLE)),
	/**
	 * Results with this severity score should be presented as OK.
	 */
	OK(0, Messages.getString(Messages.Severity_OK)),
	/**
	 * Results with this severity score should be presented as potential problems.
	 */
	INFO(25, Messages.getString(Messages.Severity_INFORMATION)),
	/**
	 * Results with this severity score should be presented as warnings.
	 */
	WARNING(75, Messages.getString(Messages.Severity_WARNING));

	private final double score;
	private final String localizedName;

	private Severity(double score, String localizedName) {
		this.score = score;
		this.localizedName = localizedName;
	}

	public String getLocalizedName() {
		return localizedName;
	}

	public double getLimit() {
		return score;
	}

	/**
	 * Checks whether this severity is at least as severe as the given one, comparing the severity
	 * scores. Use this rather than comparing severities directly when applying a minimum severity
	 * threshold.
	 *
	 * @param other
	 *            the severity to compare against
	 * @return true if this severity's score is greater than or equal to the other's
	 */
	public boolean isAtLeast(Severity other) {
		return score >= other.score;
	}

	/**
	 * The constants in descending order of severity score, as required by {@link #get(double)}.
	 */
	private static final Severity[] VALUES = {WARNING, INFO, OK, NA, IGNORE};

	public static Severity get(double score) {
		for (Severity s : VALUES) {
			if (score >= s.score) {
				return s;
			}
		}
		return Severity.NA;
	}

}
