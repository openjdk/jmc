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

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;

import org.openjdk.jmc.rjmx.subscription.IMRIService;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;

/**
 * Class for providing checked state information for a {@link TriggerRule}s.
 */
public class RuleCheckedStateProvider implements ICheckStateProvider, ICheckStateListener {
	final private IMRIService mriService;
	final private String serverGuid;
	final private NotificationRegistry notificationModel;

	public RuleCheckedStateProvider(IMRIService mriService, String serverGuid, NotificationRegistry notificationModel) {
		this.mriService = mriService;
		this.serverGuid = serverGuid;
		this.notificationModel = notificationModel;
	}

	@Override
	public boolean isChecked(Object element) {
		if (element instanceof TriggerRule) {
			return notificationModel.getRegisteredRules(serverGuid).contains(element);
		}
		return false;
	}

	@Override
	public boolean isGrayed(Object element) {
		return false;
	}

	@Override
	public void checkStateChanged(CheckStateChangedEvent event) {
		Object element = event.getElement();
		if (element instanceof TriggerRule) {
			setRuleChecked((TriggerRule) element, event.getChecked());
		} else if (element instanceof RuleGroup) {
			RuleGroup group = ((RuleGroup) element);
			group.getRules().forEach(rule -> setRuleChecked(rule, event.getChecked()));
		}
	}

	private void setRuleChecked(TriggerRule rule, boolean checked) {
		if (!checked) {
			unregisterRule(rule);
		} else if (canRegister(rule)) {
			if (!registerRule(rule)) {
				unregisterRule(rule);
			}
		}
	}

	boolean canRegister(TriggerRule rule) {
		return rule.isReady() && mriService != null
				&& mriService.isMRIAvailable(rule.getTrigger().getAttributeDescriptor());
	}

	private void unregisterRule(TriggerRule rule) {
		notificationModel.unregisterRule(rule, serverGuid);
	}

	private boolean registerRule(TriggerRule rule) {
		return notificationModel.registerRule(rule, serverGuid);
	}
}
