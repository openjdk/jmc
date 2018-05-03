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
package org.openjdk.jmc.ui.handlers;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.commands.ICommandImageService;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.services.IServiceLocator;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.ui.UIPlugin;

public class ActionToolkit {

	private static final String CHECKED_ACTION = "checkedAction"; //$NON-NLS-1$
	private static final String ID_ATTRIBUTE = "id"; //$NON-NLS-1$

	private static class SupplierSelectionProviderAction extends SelectionProviderAction {
		private final Function<IStructuredSelection, Runnable> actionProvider;
		private final boolean refreshViewer;
		private Runnable action;

		SupplierSelectionProviderAction(StructuredViewer viewer, String text, boolean refresh,
				Function<IStructuredSelection, Runnable> actionProvider) {
			super(viewer, text);
			this.refreshViewer = refresh;
			this.actionProvider = actionProvider;
			selectionChanged(getStructuredSelection());
		}

		@Override
		public void selectionChanged(IStructuredSelection selection) {
			action = actionProvider.apply(selection);
			setEnabled(action != null);
		}

		@Override
		public void run() {
			action.run();
			if (refreshViewer) {
				((StructuredViewer) getSelectionProvider()).refresh();
			}
		}
	}

	public static void loadCheckState(IState state, Stream<IAction> actions) {
		if (state != null) {
			Set<String> selected = Stream.of(state.getChildren(CHECKED_ACTION)).map(s -> s.getAttribute(ID_ATTRIBUTE))
					.collect(Collectors.toSet());
			actions.forEach(a -> a.setChecked(selected.contains(a.getId())));
		}
	}

	public static void saveCheckState(IWritableState state, Stream<IAction> actions) {
		actions.filter(a -> a.isChecked())
				.forEach(action -> state.createChild(CHECKED_ACTION).putString(ID_ATTRIBUTE, action.getId()));
	}

	public static void convertToCommandAction(IAction a, String commandId) {
		try {
			a.setActionDefinitionId(commandId);
			IServiceLocator sl = PlatformUI.getWorkbench();
			ICommandImageService is = sl.getService(ICommandImageService.class);
			ICommandService cs = sl.getService(ICommandService.class);
			Command command = cs.getCommand(commandId);
			a.setText(command.getName());
			a.setDescription(command.getDescription());
			a.setImageDescriptor(is.getImageDescriptor(commandId, ICommandImageService.TYPE_DEFAULT));
			a.setDisabledImageDescriptor(is.getImageDescriptor(commandId, ICommandImageService.TYPE_DISABLED));
			a.setHoverImageDescriptor(is.getImageDescriptor(commandId, ICommandImageService.TYPE_HOVER));
		} catch (RuntimeException e) {
			a.setText(commandId);
			UIPlugin.getDefault().getLogger().log(Level.SEVERE, "Could not create CommandAction for " + commandId, e); //$NON-NLS-1$
		} catch (NotDefinedException e) {
			a.setText(commandId);
			UIPlugin.getDefault().getLogger().log(Level.SEVERE, commandId + " not defined", e); //$NON-NLS-1$
		}
	}

	public static IAction commandAction(Runnable runnable, String commandId) {
		IAction a = action(runnable, null);
		convertToCommandAction(a, commandId);
		return a;
	}

	public static IAction action(Runnable runnable, String text) {
		return action(runnable, text, null);
	}

	public static IAction action(Runnable runnable, String text, ImageDescriptor icon) {
		Action a = new Action(text, icon) {
			@Override
			public void run() {
				runnable.run();
			}
		};
		return a;
	}

	public static IAction radioAction(Runnable runnable, String text, ImageDescriptor icon) {
		Action a = new Action(text, IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				if (isChecked()) {
					runnable.run();
				}
			}
		};
		a.setImageDescriptor(icon);
		return a;
	}

	public static IAction checkAction(Consumer<Boolean> onChange, String text, ImageDescriptor icon) {
		Action a = new Action(text, IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				onChange.accept(isChecked());
			}
		};
		a.setImageDescriptor(icon);
		return a;
	}

	public static IAction checkAction(
		Consumer<Boolean> onChange, String text, String description, ImageDescriptor icon, String id) {
		IAction a = checkAction(onChange, text, icon);
		a.setDescription(description);
		a.setId(id);
		return a;
	}

	public static IAction action(Runnable runnable, String text, ImageDescriptor icon, int style) {
		Action a = new Action(text, style) {
			@Override
			public void run() {
				runnable.run();
			}
		};
		a.setImageDescriptor(icon);
		return a;
	}

	public static IAction forTreeSelection(
		AbstractTreeViewer viewer, String name, boolean refresh, Function<TreeSelection, Runnable> actionProvider) {
		return new SupplierSelectionProviderAction(viewer, name, refresh,
				selection -> actionProvider.apply((TreeSelection) selection));
	}

	@SuppressWarnings("unchecked")
	public static <T> IAction forListSelection(
		StructuredViewer viewer, String name, boolean refresh, Function<List<T>, Runnable> actionProvider) {
		return new SupplierSelectionProviderAction(viewer, name, refresh,
				selection -> actionProvider.apply(selection.toList()));
	}

	public static <T> IAction forListSelection(
		StructuredViewer viewer, String name, boolean refresh, int minElements, Consumer<List<T>> action) {
		return forListSelection(viewer, name, refresh,
				((List<T> list) -> list.size() >= minElements ? (() -> action.accept(list)) : null));
	}
}
