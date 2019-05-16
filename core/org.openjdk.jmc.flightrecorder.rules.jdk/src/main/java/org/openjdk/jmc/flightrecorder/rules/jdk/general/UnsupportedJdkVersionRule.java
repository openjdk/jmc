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
package org.openjdk.jmc.flightrecorder.rules.jdk.general;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

/**
 * This rule will warn if an unsupported JDK version is in use.
 * <p>
 * This rule will need to be maintained to offer the best suggestions, but even
 * if not maintained, it will still detect decidedly bad versions to use.
 * <p>
 * Note that this rule will not help ensure that the latest update releases for
 * an LTS is in use, as this would require a more rapid update cycle than can be
 * expected from the core rules project.
 */
public final class UnsupportedJdkVersionRule implements IRule {
	private static final String UNSUPPORTED_JDK_VERSION_ID = "UnsupportedJdkVersion";

	/**
	 * The time the rule was last updated. Make sure this is updated when you update
	 * the rule!
	 */
	private static final String LAST_UPDATE = "2019-05-15";

	/**
	 * This is the newest update of the oldest released still supported LTS we feel
	 * comfortable pointing to.
	 */
	private static final JavaVersion OLDEST_RECOMMENDED_LTS = new JavaVersion(8, 0, 211);

	/**
	 * This is the newest update of the newest released still supported LTS we feel
	 * comfortable pointing to.
	 */
	private static final JavaVersion LATEST_RECOMMENDED_LTS = new JavaVersion(11, 0, 3);

	/**
	 * This is the oldest LTS-(major) version we'd like for people to use.
	 */
	private static final JavaVersion OLDEST_LTS = new JavaVersion(8, 0, 0);

	/**
	 * This is the latest LTS-(major) version known by the rule.
	 */
	private static final JavaVersion LATEST_LTS = new JavaVersion(11, 0, 0);

	/**
	 * This is the latest non-LTS (major version) known by the rule.
	 */
	private static final JavaVersion LATEST_NON_LTS = new JavaVersion(12, 0, 0);

	/**
	 * These are the known "LTS" releases which still have support.
	 */
	private static final int[] LTS_VERSIONS = new int[] { 7, 8, 11 };

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.VM_INFO);
		if (eventAvailability != EventAvailability.ENABLED && eventAvailability != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.VM_INFO);
		}
		JavaVersion version = RulesToolkit.getJavaVersion(items);
		if (version == null) {
			return RulesToolkit.getNotApplicableResult(this,
					getStr(Messages.UnsupportedJdkVersionRule_TEXT_NO_VM_INFO));
		}

		if (version.isEarlyAccess()) {
			return new Result(this, 100, getStr(Messages.UnsupportedJdkVersionRule_TEXT_WARNING_EARLY_ACCESS),
					MessageFormat.format(getStr(Messages.UnsupportedJdkVersionRule_TEXT_WARNING_EARLY_ACCESS_LONG),
							version, LATEST_RECOMMENDED_LTS, OLDEST_RECOMMENDED_LTS, LATEST_NON_LTS));

		}

		if (version.getMajorVersion() < LTS_VERSIONS[0]) {
			return new Result(this, 90, getStr(Messages.UnsupportedJdkVersionRule_TEXT_WARNING_OLD_JDK),
					MessageFormat.format(getStr(Messages.UnsupportedJdkVersionRule_TEXT_WARNING_OLD_JDK_LONG), version,
							LATEST_RECOMMENDED_LTS, OLDEST_RECOMMENDED_LTS));
		}

		if (!isLTSVersion(version) && !version.isGreaterOrEqualThan(LATEST_NON_LTS)) {
			return new Result(this, 87, getStr(Messages.UnsupportedJdkVersionRule_TEXT_WARNING_OLD_NON_LTS),
					MessageFormat.format(getStr(Messages.UnsupportedJdkVersionRule_TEXT_WARNING_OLD_NON_LTS_LONG),
							version, LATEST_RECOMMENDED_LTS, OLDEST_RECOMMENDED_LTS));

		}
		// Still supported, but on the way out...
		if (!version.isGreaterOrEqualThan(OLDEST_LTS)) {
			return new Result(this, 40, getStr(Messages.UnsupportedJdkVersionRule_TEXT_INFO_OLD_JDK),
					MessageFormat.format(getStr(Messages.UnsupportedJdkVersionRule_TEXT_INFO_OLD_JDK_LONG), version,
							LATEST_RECOMMENDED_LTS, OLDEST_RECOMMENDED_LTS));
		}
		return new Result(this, 0, getStr(Messages.UnsupportedJdkVersionRule_TEXT_OK),
				MessageFormat.format(getStr(Messages.UnsupportedJdkVersionRule_TEXT_OK_LONG), version,
						LATEST_LTS.getMajorVersion(), LATEST_NON_LTS.getMajorVersion(), LAST_UPDATE));
	}

	@Override
	public RunnableFuture<Result> evaluate(final IItemCollection items, final IPreferenceValueProvider valueProvider) {
		FutureTask<Result> evaluationTask = new FutureTask<>(new Callable<Result>() {
			@Override
			public Result call() throws Exception {
				return getResult(items, valueProvider);
			}
		});
		return evaluationTask;
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return UNSUPPORTED_JDK_VERSION_ID;
	}

	@Override
	public String getName() {
		return getStr(Messages.UnsupportedJdkVersionRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.JVM_INFORMATION_TOPIC;
	}

	private static String getStr(String key) {
		return Messages.getString(key);
	}

	private static boolean isLTSVersion(JavaVersion version) {
		for (int i = 0; i < LTS_VERSIONS.length; i++) {
			if (version.getMajorVersion() == LTS_VERSIONS[i]) {
				return true;
			}
		}
		return false;
	}
}
