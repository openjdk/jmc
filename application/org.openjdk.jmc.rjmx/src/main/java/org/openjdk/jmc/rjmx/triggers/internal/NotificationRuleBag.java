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
package org.openjdk.jmc.rjmx.triggers.internal;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.rjmx.triggers.IActivatableTriggerAction;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;

class NotificationRuleBag {

	private final Map<TriggerRule, IMRIValueListener> rules = new HashMap<>();
	private WeakReference<IConnectionHandle> handleRef = new WeakReference<>(null);
	private final String serverGuid;

	public NotificationRuleBag(String serverGuid) {
		this.serverGuid = serverGuid;
	}

	void activate(IConnectionHandle handle) {
		assert (handle.getServerDescriptor().getGUID().equals(serverGuid));
		handleRef = new WeakReference<>(handle);

		Iterator<Map.Entry<TriggerRule, IMRIValueListener>> rulesIter = rules.entrySet().iterator();
		while (rulesIter.hasNext()) {
			Map.Entry<TriggerRule, IMRIValueListener> rule = rulesIter.next();
			if (!activateRule(rule.getKey(), rule.getValue(), handle)) {
				rulesIter.remove();
			}
		}
	}

	void deactivate() {
		IConnectionHandle handle = handleRef.get();
		for (Entry<TriggerRule, IMRIValueListener> rule : rules.entrySet()) {
			deactivateRule(rule.getKey(), rule.getValue(), handle);
		}

	}

	Collection<TriggerRule> getAllRegisteredRules() {
		return rules.keySet();
	}

	boolean removeRule(TriggerRule r) {
		if (r != null && r.getTrigger() != null && r.getTrigger().getAttributeDescriptor() != null) {
			IMRIValueListener listener = rules.remove(r);
			if (listener != null) {
				deactivateRule(r, listener, handleRef.get());
				return true;
			}
		}
		return false;
	}

	boolean addRule(final TriggerRule r) {
		if (r == null || r.getTrigger() == null || r.getTrigger().getAttributeDescriptor() == null
				|| rules.containsKey(r)) {
			return false;
		}
		IMRIValueListener listener = new IMRIValueListener() {

			@Override
			public void valueChanged(MRIValueEvent event) {
				r.getTrigger().triggerOn(handleRef.get(), r, event);
			}
		};

		boolean activateOk = activateRule(r, listener, handleRef.get());
		rules.put(r, listener);
		return activateOk;
	}

	/**
	 * Activates rule, returns true if the action of the rule is support, false otherwise.
	 *
	 * @param r
	 * @param handle
	 * @return
	 */
	private boolean activateRule(TriggerRule r, IMRIValueListener listener, IConnectionHandle handle) {
		if (handle != null && handle.isConnected() && (!(r.getAction() instanceof IActivatableTriggerAction)
				|| ((IActivatableTriggerAction) r.getAction()).isActivatable(handle))) {
			handle.getServiceOrDummy(ISubscriptionService.class)
					.addMRIValueListener(r.getTrigger().getAttributeDescriptor(), listener);
			return true;
		}
		return false;
	}

	private void deactivateRule(TriggerRule r, IMRIValueListener listener, IConnectionHandle handle) {
		if (handle != null && handle.isConnected()) {
			handle.getServiceOrDummy(ISubscriptionService.class).removeMRIValueListener(listener);
		}
		if (r.getTrigger() instanceof NotificationTrigger) {
			// FIXME: This needs to go away...
			r.getTrigger().clearState(serverGuid);
		}
	}
}
