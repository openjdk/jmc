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

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openjdk.jmc.flightrecorder.rules.internal.IRuleProvider;

/**
 * Registry for rules. Uses Java Service Loader to discover implementations of the {@link IRule}
 * interface.
 * <p>
 * In order to add a new rule to the registry, create a new IRule implementation and add its fully
 * qualified class name in a file named META-INF/services/org.openjdk.jmc.flightrecorder.rules.IRule
 * (one line per class).
 */
public class RuleRegistry {

	private static final Collection<IRule> RULES;

	static {
		Map<String, IRule> rulesById = new HashMap<>();
		ServiceLoader<IRule> ruleLoader = ServiceLoader.load(IRule.class, IRule.class.getClassLoader());
		Iterator<IRule> ruleIter = ruleLoader.iterator();
		while (ruleIter.hasNext()) {
			try {
				IRule rule = ruleIter.next();
				add(rule, rulesById);
			} catch (ServiceConfigurationError e) {
				getLogger().log(Level.WARNING, "Could not create IRule instance specified in a JSL services file", e); //$NON-NLS-1$
			}
		}
		ServiceLoader<IRuleProvider> providerLoader = ServiceLoader.load(IRuleProvider.class,
				IRuleProvider.class.getClassLoader());
		Iterator<IRuleProvider> providerIter = providerLoader.iterator();
		while (providerIter.hasNext()) {
			try {
				IRuleProvider provider = providerIter.next();
				for (IRule rule : provider.getRules()) {
					add(rule, rulesById);
				}
			} catch (ServiceConfigurationError e) {
				getLogger().log(Level.WARNING,
						"Could not create IRuleProvider instance specified in a JSL services file", e); //$NON-NLS-1$
			}
		}

		RULES = Collections.unmodifiableCollection(rulesById.values());
	}

	// Do not instantiate
	private RuleRegistry() {
	}

	private static Logger getLogger() {
		return Logger.getLogger("org.openjdk.jmc.flightrecorder.rules"); //$NON-NLS-1$
	}

	private static void add(IRule rule, Map<String, IRule> rulesById) {
		if (rule != null) {
			if (!rulesById.containsKey(rule.getId())) {
				rulesById.put(rule.getId(), rule);
			} else {
				// FIXME: can we make it impossible to get rule id conflicts?
				IRule firstRule = rulesById.get(rule.getId());
				getLogger().log(Level.WARNING, MessageFormat.format(
						"Could not register rule \"{0}\" ({1}), because its id ({2}) conflicts with the rule \"{3}\" ({4}) which has already been registered", //$NON-NLS-1$
						rule.getName(), rule.getClass().getName(), rule.getId(), firstRule.getName(),
						firstRule.getClass().getName()));
			}
		}
	}

	/**
	 * @return a collection of all registered rules
	 */
	public static Collection<IRule> getRules() {
		return RULES;
	}

}
