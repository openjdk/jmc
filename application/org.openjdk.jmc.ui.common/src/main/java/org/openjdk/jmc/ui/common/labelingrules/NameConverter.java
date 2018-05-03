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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.openjdk.jmc.ui.common.CorePlugin;
import org.openjdk.jmc.ui.common.idesupport.IDESupportFactory;
import org.openjdk.jmc.ui.common.jvm.JVMCommandLineToolkit;
import org.openjdk.jmc.ui.common.jvm.JVMDescriptor;
import org.openjdk.jmc.ui.common.resource.Resource;
import org.openjdk.jmc.ui.common.util.Environment;

/**
 * Converts names according to rules specified with the {@value #LABELING_RULES_EXTENSION_POINT}
 * extension point.
 */
public final class NameConverter {
	private static final String LABELING_RULES_EXTENSION_POINT = "org.openjdk.jmc.ui.common.labelingRules"; //$NON-NLS-1$
	private static final String ATTRIBUTE_ICON = "icon"; //$NON-NLS-1$
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

	private final List<NamingRule> rules = new ArrayList<>();

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
		initializeRulesFromExtensions();
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
		return MessageFormat.format(Messages.NameConverter_LOCAL_NAME_TEMPLATE, descriptor);
	}

	public Resource getImageResource(JVMDescriptor descriptor) {
		NamingRule rule = getMatchingRule(prepareValues(descriptor));
		return rule == null ? null : rule.getImageResource();
	}

	/**
	 * Adds a rule to the name converter.
	 * <p>
	 * Adding rules should normally not be done using this method, but rather through the
	 * {@value #LABELING_RULES_EXTENSION_POINT} extension point.
	 *
	 * @param rule
	 *            the rule to add.
	 */
	public void addNamingRule(NamingRule rule) {
		rules.add(rule);
		Collections.sort(rules, COMPARATOR);
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

	private void initializeRulesFromExtensions() {
		IExtensionRegistry er = Platform.getExtensionRegistry();
		IExtensionPoint ep = er.getExtensionPoint(LABELING_RULES_EXTENSION_POINT);
		IExtension[] extensions = ep.getExtensions();
		for (IExtension extension : extensions) {
			IConfigurationElement[] configs = extension.getConfigurationElements();
			for (IConfigurationElement config : configs) {
				if (config.getName().equals("rule")) { //$NON-NLS-1$
					try {
						rules.add(createRule(config));
					} catch (Exception e) {
						CorePlugin.getDefault().getLogger().log(Level.SEVERE, e.getMessage(), e);
					}
				}
			}
		}
		Collections.sort(rules, COMPARATOR);
	}

	private NamingRule createRule(IConfigurationElement config) throws Exception {
		String name = config.getAttribute("name"); //$NON-NLS-1$
		// Try/Catch here to at least have a chance of providing the user with a hint
		// should something go wrong.
		try {
			int priority = Integer.parseInt(config.getAttribute("priority")); //$NON-NLS-1$
			String matchingPart = config.getAttribute("match"); //$NON-NLS-1$
			String formattingPart = config.getAttribute("format"); //$NON-NLS-1$
			return new NamingRule(name, matchingPart, formattingPart, priority, getIcon(config));
		} catch (Exception e) {
			throw new Exception("Problem instantiating naming rule named " + name); //$NON-NLS-1$
		}
	}

	private Resource getIcon(IConfigurationElement configElement) {
		String iconName = configElement.getAttribute(ATTRIBUTE_ICON);
		if (iconName != null) {
			String extendingPluginId = configElement.getDeclaringExtension().getContributor().getName();
			return new Resource(extendingPluginId, iconName);
		}
		return null;
	}

	private Object[] prepareValues(JVMDescriptor descriptor) {
		return new Object[] {descriptor.getJavaVersion(), descriptor.getJvmType(), descriptor.getJvmArch(),
				getValidName(descriptor), descriptor.getJavaCommand(),
				descriptor.getPid() != null ? String.valueOf(descriptor.getPid()) : "", descriptor.isDebug(), //$NON-NLS-1$
				descriptor.getJVMArguments()};
	}

	private String getValidName(JVMDescriptor descriptor) {
		Integer pid = descriptor.getPid();
		if (pid != null && pid.intValue() == Environment.getThisPID() && descriptor.isAttachable()) {
			return IDESupportFactory.getIDESupport().getIdentity() + ".this"; //$NON-NLS-1$
		}
		String name = JVMCommandLineToolkit.getMainClassOrJar(descriptor.getJavaCommand());
		if (name != null && name.length() > 0) {
			return name;
		}
		return Messages.NameConverter_UNKNOWN_LOCAL_JVM;
	}
}
