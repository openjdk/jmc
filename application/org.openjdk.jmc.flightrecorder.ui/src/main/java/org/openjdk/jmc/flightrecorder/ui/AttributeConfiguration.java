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
package org.openjdk.jmc.flightrecorder.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;

public class AttributeConfiguration {

	private String title;
	private String description;
	private List<AttributeGroup> pageAttributes;

	private AttributeConfiguration(String title, String description) {
		this.title = title;
		this.description = description;
	}

	public AttributeConfiguration(String title, String description, List<AttributeGroup> attributeGroups) {
		this(title, description);
		this.pageAttributes = attributeGroups;
	}

	public AttributeConfiguration(String title, String description, AttributeGroup ... attributeGroups) {
		this(title, description, new ArrayList<>(Arrays.asList(attributeGroups)));
	}

	public AttributeConfiguration(IRule rule) {
		this(MessageFormat.format(Messages.ATTRIBUTE_CONFIG_RULE, rule.getName()),
				MessageFormat.format(Messages.ATTRIBUTE_CONFIG_RULE_DESC, rule.getName()),
				new AttributeGroup(rule.getName(), rule.getConfigurationAttributes()));
	}

	public AttributeConfiguration(Collection<IRule> rules) {
		this(MessageFormat.format(Messages.ATTRIBUTE_CONFIG_RULES, rules.size()),
				MessageFormat.format(Messages.ATTRIBUTE_CONFIG_RULES_DESC, rules.size()), new ArrayList<>());
		addAttributesFromRules(rules);
	}

	/**
	 * Adds any not previously added configuration attributes from the given collection of rules.
	 * Attributes that are only used for a single rule will be placed on one group per rule.
	 * Attributes that configure several rules will be added last in a separate group.
	 *
	 * @param rules
	 *            The collection of rules to scan for previously not added configuration attributes
	 */
	public void addAttributesFromRules(Collection<IRule> rules) {
		Set<TypedPreference<?>> currentPreferences = pageAttributes.stream().map(a -> a.attributes)
				.flatMap(a -> a.stream()).map(a -> a.preference).collect(Collectors.toSet());

		List<TypedPreference<?>> unknownPreferences = rules.stream()
				.flatMap(r -> r.getConfigurationAttributes().stream()).filter(p -> !currentPreferences.contains(p))
				.collect(Collectors.toList());
		if (!unknownPreferences.isEmpty()) {
			Set<TypedPreference<?>> preferences = rules.parallelStream()
					.filter(r -> r.getConfigurationAttributes().size() > 0)
					.flatMap(r -> r.getConfigurationAttributes().stream()).distinct().collect(Collectors.toSet());
			Map<String, Collection<IRule>> rulesByPreferences = new HashMap<>();
			for (TypedPreference<?> preference : preferences) {
				List<IRule> rulesWithPreference = rules.stream()
						.filter(rule -> rule.getConfigurationAttributes().contains(preference))
						.collect(Collectors.toList());
				rulesByPreferences.put(preference.getIdentifier(), rulesWithPreference);
			}
			List<TypedPreference<?>> rest = new ArrayList<>();
			for (IRule rule : rules) {
				List<TypedPreference<?>> unique = rule.getConfigurationAttributes().stream()
						.filter(p -> rulesByPreferences.get(p.getIdentifier()).size() == 1)
						.collect(Collectors.toList());
				rest.addAll(rule.getConfigurationAttributes().stream()
						.filter(p -> rulesByPreferences.get(p.getIdentifier()).size() > 1)
						.collect(Collectors.toList()));
				if (unique.size() > 0) {
					pageAttributes.add(new AttributeGroup(rule.getName(), unique));
				}
			}
			Collections.sort(pageAttributes, (p1, p2) -> p1.groupTitle.compareTo(p2.groupTitle));
			if (rest.size() > 0) {
				pageAttributes.add(new AttributeGroup(Messages.ATTRIBUTE_CONFIG_SHARED_GROUP_NAME, rest));
			}
		}
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public List<AttributeGroup> getPageAttributes() {
		return pageAttributes;
	}

	public static class GroupEntry {
		private String simpleName;
		private TypedPreference<?> preference;

		public GroupEntry(String name, TypedPreference<?> preference) {
			this.simpleName = name;
			this.preference = preference;
		}

		public String getName() {
			return simpleName;
		}

		public TypedPreference<?> getPreference() {
			return preference;
		}
	}

	public static class AttributeGroup {

		private String groupTitle;
		private List<GroupEntry> attributes;

		public AttributeGroup(String title, GroupEntry ... attributes) {
			this.groupTitle = title;
			this.attributes = Arrays.asList(attributes);
		}

		public AttributeGroup(String title, List<GroupEntry> attributes) {
			this.groupTitle = title;
			this.attributes = attributes;
		}

		public AttributeGroup(String title, Collection<TypedPreference<?>> preferences) {
			this.groupTitle = title;
			this.attributes = new ArrayList<>();
			for (TypedPreference<?> preference : preferences) {
				attributes.add(new GroupEntry(preference.getName(), preference));
			}
		}

		public List<GroupEntry> getEntries() {
			return attributes;
		}

		public String getTitle() {
			return groupTitle;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(groupTitle);
			for (GroupEntry entry : attributes) {
				sb.append(", "); //$NON-NLS-1$
				sb.append("["); //$NON-NLS-1$
				sb.append(entry.getName());
				sb.append(", "); //$NON-NLS-1$
				sb.append(entry.getPreference());
				sb.append("]"); //$NON-NLS-1$
			}
			return sb.toString();
		}

	}

}
