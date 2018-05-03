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
package org.openjdk.jmc.console.ui.notification.tab;

import java.util.Collection;
import java.util.HashMap;

import org.eclipse.jface.viewers.ITreeContentProvider;

import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;
import org.openjdk.jmc.ui.misc.AbstractStructuredContentProvider;

/**
 * TriggerContentProvider
 */
public class TriggerContentProvider extends AbstractStructuredContentProvider implements ITreeContentProvider {

	private final NotificationRegistry model;

	public TriggerContentProvider(NotificationRegistry model) {
		this.model = model;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof RuleGroup) {
			return ((RuleGroup) parentElement).getRules().toArray();
		}
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	public Collection<RuleGroup> makeRulesGroup() {
		HashMap<String, RuleGroup> ruleGroups = new HashMap<>();
		for (TriggerRule rule : model.getAvailableRules()) {
			RuleGroup ruleGroup = ruleGroups.get(rule.getRulePath());
			if (ruleGroup == null) {
				ruleGroup = new RuleGroup(rule.getRulePath());
				ruleGroups.put(rule.getRulePath(), ruleGroup);
			}
			ruleGroup.addRule(rule);
		}
		return ruleGroups.values();
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return makeRulesGroup().toArray();
	}
}
