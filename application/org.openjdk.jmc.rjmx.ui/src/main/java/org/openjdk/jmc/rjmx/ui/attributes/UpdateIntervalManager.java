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
package org.openjdk.jmc.rjmx.ui.attributes;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;

import org.openjdk.jmc.rjmx.services.IUpdateInterval;
import org.openjdk.jmc.rjmx.subscription.internal.UpdatePolicyToolkit;

/**
 * Context menu update interval menu holder.
 */
public class UpdateIntervalManager {

	private final MenuManager m_updateMenu = new MenuManager(
			Messages.UpdateIntervalManager_CHANGE_UPDATE_INTERVAL_MENU_ITEM);

	public UpdateIntervalManager(TreeViewer viewer) {
		setupUpdateIntervalMenu(viewer);
	}

	private MenuManager setupUpdateIntervalMenu(final TreeViewer viewer) {
		m_updateMenu.add(createUpdateAction(Messages.MRIAttributeInspector_UPDATE_INTERVAL_DEFAULT, viewer,
				IUpdateInterval.DEFAULT));
		m_updateMenu.add(
				createUpdateAction(Messages.MRIAttributeInspector_UPDATE_INTERVAL_ONCE, viewer, IUpdateInterval.ONCE));
		m_updateMenu.add(new Action(Messages.UpdateIntervalManager_CUSTOM_UPDATE_INTERVAL_MENU_ITEM) {
			@Override
			public void run() {
				UpdateIntervalDialog dialog = new UpdateIntervalDialog(viewer.getControl().getShell(),
						getCurrentUpdateInterval());
				if (dialog.open() == Window.OK) {
					applyUpdateInterval(viewer, dialog.getUpdateInterval());
				}
			}

			private int getCurrentUpdateInterval() {
				int updateInterval = IUpdateInterval.DEFAULT;
				boolean firstInterval = true;
				for (ReadOnlyMRIAttribute attribute : getSelectedAttributes(viewer)) {
					int interval = attribute.getUpdateInterval();
					if (firstInterval) {
						updateInterval = interval;
						firstInterval = false;
					} else if (updateInterval != interval) {
						updateInterval = IUpdateInterval.DEFAULT;
						break;
					}
				}
				if (updateInterval == IUpdateInterval.DEFAULT || updateInterval == IUpdateInterval.ONCE) {
					updateInterval = UpdatePolicyToolkit.getDefaultUpdateInterval();
				}
				return updateInterval;
			}
		});
		return m_updateMenu;
	}

	private Action createUpdateAction(String label, final TreeViewer viewer, final int updateTime) {
		return new Action(label) {
			@Override
			public void run() {
				applyUpdateInterval(viewer, updateTime);
			}
		};
	}

	private void applyUpdateInterval(TreeViewer viewer, int updateInterval) {
		for (ReadOnlyMRIAttribute attribute : getSelectedAttributes(viewer)) {
			attribute.setUpdateInterval(updateInterval);
			viewer.refresh(attribute);
		}
	}

	private List<ReadOnlyMRIAttribute> getSelectedAttributes(TreeViewer viewer) {
		final List<ReadOnlyMRIAttribute> attributes = new ArrayList<>();
		for (Object o : (List<?>) ((IStructuredSelection) viewer.getSelection()).toList()) {
			if (o instanceof ReadOnlyMRIAttribute) {
				attributes.add((ReadOnlyMRIAttribute) o);
			}
		}
		return attributes;
	}

	public MenuManager getUpdateIntervalMenu() {
		return m_updateMenu;
	}

}
