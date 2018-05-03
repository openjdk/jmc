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

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import org.openjdk.jmc.console.ui.notification.NotificationPlugin;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.ui.UIPlugin;

/**
 * TriggerLabelProvider
 */
public class TriggerLabelProvider extends LabelProvider implements IColorProvider {
	final private RuleCheckedStateProvider ruleStateProvider;

	public TriggerLabelProvider(RuleCheckedStateProvider ruleStateProvider) {
		this.ruleStateProvider = ruleStateProvider;
	}

	public TriggerLabelProvider() {
		this(null);
	}

	@Override
	public Image getImage(Object obj) {
		if (obj instanceof TriggerRule) {
			TriggerRule rule = (TriggerRule) obj;
			return NotificationPlugin.getDefault().getImage(rule.getAction(), isAvailable(rule));
		}
		if (obj instanceof RuleGroup) {
			return UIPlugin.getDefault().getImage(UIPlugin.ICON_FOLDER_CLOSED);
		}
		return null;
	}

	@Override
	public String getText(Object obj) {
		if (obj instanceof TriggerRule) {
			return ((TriggerRule) obj).getName();
		}
		if (obj instanceof RuleGroup) {
			return ((RuleGroup) obj).getName();
		}

		return null;
	}

	@Override
	public Color getForeground(Object element) {
		if (element instanceof TriggerRule && !isAvailable((TriggerRule) element)) {
			return JFaceResources.getColorRegistry().get(JFacePreferences.QUALIFIER_COLOR);
		}
		return null;
	}

	private boolean isAvailable(TriggerRule rule) {
		return ruleStateProvider == null || ruleStateProvider.canRegister(rule);
	}

	@Override
	public Color getBackground(Object element) {
		return null;
	}
}
