/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.common.labelingrules;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openjdk.jmc.common.jvm.JVMCommandLineToolkit;
import org.openjdk.jmc.common.jvm.JVMDescriptor;
import org.openjdk.jmc.common.messages.internal.Messages;
import org.openjdk.jmc.common.resource.Resource;
import org.openjdk.jmc.common.util.Environment;

/**
 * Converts names according to rules specified with the <code>labelingRules</code> extension point.
 */
public class NameConverter {
	private static final Comparator<NamingRule> COMPARATOR = new Comparator<NamingRule>() {
		@Override
		public int compare(NamingRule o1, NamingRule o2) {
			return o2.getPriority() - o1.getPriority();
		}
	};

	private static final NameConverter INSTANCE = new NameConverter();

	static enum ValueArrayInfo {
		JAVAVERSION(0, "JDK", "{0}"), //$NON-NLS-1$ //$NON-NLS-2$
		JVMTYPE(1, "JVMType", "{1}"), //$NON-NLS-1$ //$NON-NLS-2$
		JVMARCH(2, "JVMArch", "{2}"), //$NON-NLS-1$ //$NON-NLS-2$
		NAME(3, "Name", "{3}"), //$NON-NLS-1$ //$NON-NLS-2$
		JAVACOMMAND(4, "JavaCmd", "{4}"), //$NON-NLS-1$ //$NON-NLS-2$
		PID(5, "PID", "{5}"), //$NON-NLS-1$ //$NON-NLS-2$
		DEBUG(6, "IsDebug", "{6}"), //$NON-NLS-1$ //$NON-NLS-2$
		JVMARGS(7, "JVMArgs", "{7}"); //$NON-NLS-1$ //$NON-NLS-2$

		private int index;
		private String valueName;
		private String matchExpression;

		ValueArrayInfo(int index, String valueName, String matchExpression) {
			this.index = index;
			this.valueName = valueName;
			this.matchExpression = matchExpression;
		}

		public int getIndex() {
			return index;
		}

		public String getValueName() {
			return valueName;
		}

		public String getMatchExpression() {
			return matchExpression;
		}
	}

	protected List<NamingRule> rules = new ArrayList<>();
	private String identity;

	/**
	 * @return a singleton instance
	 */
	public static NameConverter getInstance() {
		return INSTANCE;
	}

	/**
	 * Create a new name converter instance. This should only be used if you want a new, clean
	 * instance. Normally the {@link NameConverter#getInstance()} method should be used instead to
	 * get a singleton instance.
	 */
	public NameConverter() {
		this(new ArrayList<>());
	}

	public NameConverter(List<NamingRule> rules) {
		this.rules = rules;
	}

	/**
	 * @param descriptor
	 * @return the properly formatted values. If no matching formatter could be found, the default
	 *         format String as defined in NameConverter_LOCAL_NAME_TEMPLATE will be used.
	 */
	public String format(JVMDescriptor descriptor) {
		// FIXME: Somehow rewrite this to avoid things like [Unknown][Unknown] and empty () when the pid is unknown.
		// JDP being the typical use case.
		Object[] values = prepareValues(descriptor);
		NamingRule rule = getMatchingRule(values);
		if (rule != null) {
			return rule.format(values);
		}
		// Should always be a catch all rule, but if someone messes up, we will use the LOCAL_NAME_TEMPLATE.
		return MessageFormat.format(Messages.getString(Messages.NameConverter_LOCAL_NAME_TEMPLATE), descriptor);
	}

	public Resource getImageResource(JVMDescriptor descriptor) {
		NamingRule rule = getMatchingRule(prepareValues(descriptor));
		return rule == null ? null : rule.getImageResource();
	}

	/**
	 * Adds a rule to the name converter.
	 * <p>
	 * Adding rules should normally not be done using this method, but rather through the
	 * <code>labelingRules</code> extension point.
	 *
	 * @param rule
	 *            the rule to add.
	 */
	public void addNamingRule(NamingRule rule) {
		rules.add(rule);
		rules.sort(COMPARATOR);
	}

	/**
	 * @return an immutable list of the available rules.
	 */
	public List<NamingRule> getRules() {
		return Collections.unmodifiableList(rules);
	}

	private NamingRule getMatchingRule(Object[] values) {
		for (NamingRule rule : rules) {
			try {
				if (rule.matches(values)) {
					return rule;
				}
			} catch (RuntimeException e) {
				// Silently ignore broken rules for now.
			}
		}
		return null;
	}

	public void setRules(List<NamingRule> rules) {
		this.rules = rules;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}

	private Object[] prepareValues(JVMDescriptor descriptor) {
		return new Object[] {descriptor.getJavaVersion(), descriptor.getJvmType(), descriptor.getJvmArch(),
				getValidName(descriptor), descriptor.getJavaCommand(),
				descriptor.getPid() != null ? String.valueOf(descriptor.getPid()) : "", descriptor.isDebug(), //$NON-NLS-1$
				descriptor.getJVMArguments()};
	}

	private String getValidName(JVMDescriptor descriptor) {
		Integer pid = descriptor.getPid();
		if (identity != null && pid != null && pid.intValue() == Environment.getThisPID()
				&& descriptor.isAttachable()) {
			return identity + ".this"; //$NON-NLS-1$
		}
		String name = JVMCommandLineToolkit.getMainClassOrJar(descriptor.getJavaCommand());
		if (name != null && name.length() > 0) {
			return name;
		}
		return Messages.getString(Messages.NameConverter_UNKNOWN_LOCAL_JVM);
	}
}
