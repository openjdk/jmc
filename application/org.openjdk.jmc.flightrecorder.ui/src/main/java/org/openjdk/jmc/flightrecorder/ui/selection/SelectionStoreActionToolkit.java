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
package org.openjdk.jmc.flightrecorder.ui.selection;

import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.actions.SelectionProviderAction;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.RangeMatchPolicy;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList;
import org.openjdk.jmc.flightrecorder.ui.common.ItemRow;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;

public final class SelectionStoreActionToolkit {

	private SelectionStoreActionToolkit() {
	}

	private static class SelectionStoreAction extends SelectionProviderAction {

		private Runnable action;

		protected SelectionStoreAction(ISelectionProvider provider, Runnable action, String name) {
			super(provider, name);
			this.action = action;
			setEnabled(false);
		}

		@Override
		public void selectionChanged(IStructuredSelection selection) {
			setEnabled(!selection.isEmpty());
		}

		@Override
		public void run() {
			action.run();
		}

	}

	public static void addSelectionStoreActions(
		StructuredViewer viewer, Supplier<SelectionStore> store, Supplier<IFlavoredSelection> selection,
		IContributionManager cm) {
		// FIXME: Could consider a new group instead of edit.
		if (cm.find(MCContextMenuManager.GROUP_EDIT) == null) {
			cm.add(new Separator(MCContextMenuManager.GROUP_EDIT));
		}
		Runnable storeSelection = () -> store.get().addSelection(selection.get());
		IAction storeSelectionAction = (viewer == null)
				? ActionToolkit.action(storeSelection, Messages.STORE_SELECTION_ACTION)
				: new SelectionStoreAction(viewer, storeSelection, Messages.STORE_SELECTION_ACTION);
		Runnable storeAndSetSelection = () -> store.get().addAndSetAsCurrentSelection(selection.get());
		IAction storeAndSetSelectionAction = (viewer == null)
				? ActionToolkit.action(storeAndSetSelection, Messages.STORE_AND_ACTIVATE_SELECTION_ACTION)
				: new SelectionStoreAction(viewer, storeAndSetSelection, Messages.STORE_AND_ACTIVATE_SELECTION_ACTION);
		cm.appendToGroup(MCContextMenuManager.GROUP_EDIT, storeSelectionAction);
		cm.appendToGroup(MCContextMenuManager.GROUP_EDIT, storeAndSetSelectionAction);
	}

	public static void addSelectionStoreActions(
		StructuredViewer viewer, Supplier<Set<IType<?>>> typeSupplier, SelectionStore store, String selectionName,
		IContributionManager cm) {
		addSelectionStoreActions(viewer, () -> store, () -> new TypeSelection(typeSupplier.get(), selectionName), cm);
	}

	public static void addSelectionStoreActions(
		StructuredViewer viewer, SelectionStore store, Supplier<IItemCollection> itemStreamSupplier,
		String selectionName, IContributionManager cm) {
		addSelectionStoreActions(viewer, () -> store,
				() -> new ItemBackedSelection(itemStreamSupplier.get(), selectionName), cm);
	}

	public static void addSelectionStoreActions(
		StructuredViewer viewer, Supplier<SelectionStore> store, Supplier<IItemCollection> itemStreamSupplier,
		String selectionName, IContributionManager cm) {
		addSelectionStoreActions(viewer, store, () -> new ItemBackedSelection(itemStreamSupplier.get(), selectionName),
				cm);
	}

	public static void addSelectionStoreActions(
		SelectionStore store, ItemList list, String selectionName, IContributionManager cm) {
		addSelectionStoreActions(list.getManager().getViewer(), store,
				() -> ItemCollectionToolkit.build(list.getSelection().get()), selectionName, cm);
	}

	public static void addSelectionStoreActions(
		SelectionStore store, ItemHistogram histogram, String selectionName, IContributionManager cm) {
		addSelectionStoreActions(histogram.getManager().getViewer(), store, () -> histogram.getSelection().getItems(),
				selectionName, cm);
	}

	public static void addSelectionStoreActions(
		SelectionStore store, XYChart chart, IAttribute<IQuantity> xAttribute, String selectionName,
		IContributionManager cm) {
		addSelectionStoreActions(store, () -> chart, xAttribute, selectionName, cm);
	}

	public static void addSelectionStoreActions(
		SelectionStore store, Supplier<XYChart> chartSupplier, IAttribute<IQuantity> xAttribute, String selectionName,
		IContributionManager cm) {
		addSelectionStoreActions(null, () -> store,
				() -> new ChartSelection(selectionName, ItemRow.getSelection(chartSupplier.get()),
						chartSupplier.get().getSelectionStart(), chartSupplier.get().getSelectionEnd(), xAttribute),
				cm);
	}

	public static void addSelectionStoreRangeActions(
		SelectionStore store, XYChart chart, IAttribute<IRange<IQuantity>> xRangeAttribute, String selectionName,
		IContributionManager cm) {
		addSelectionStoreActions(null, () -> store,
				() -> new RangedChartSelection(selectionName, ItemRow.getSelection(chart), chart.getSelectionRange(),
						RangeMatchPolicy.CONTAINED_IN_CLOSED, xRangeAttribute),
				cm);
	}
}
