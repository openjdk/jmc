/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.ext.flameview.views;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.flightrecorder.ext.flameview.tree.TraceNode;
import org.openjdk.jmc.flightrecorder.ext.flameview.tree.TraceTreeUtils;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.CoreImages;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

public class FlameGraphView extends ViewPart implements ISelectionListener {
	private static ExecutorService MODEL_EXECUTOR = Executors.newFixedThreadPool(1);
	private FrameSeparator frameSeparator;

	private Browser browser;
	private SashForm container;
	private TraceNode currentRoot;
	private CompletableFuture<TraceNode> currentModelCalculator;
	private boolean threadRootAtTop;
	private IItemCollection currentItems;
	private GroupByAction[] groupByActions;

	private class GroupByAction extends Action {

		private final boolean fromThreadRootAction;

		GroupByAction(boolean fromRoot) {
			super(fromRoot ? Messages.STACKTRACE_VIEW_THREAD_ROOT : Messages.STACKTRACE_VIEW_LAST_FRAME,
					IAction.AS_RADIO_BUTTON);
			fromThreadRootAction = fromRoot;
			setToolTipText(fromRoot ? Messages.STACKTRACE_VIEW_GROUP_TRACES_FROM_ROOT
					: Messages.STACKTRACE_VIEW_GROUP_TRACES_FROM_LAST_FRAME);
			setImageDescriptor(fromRoot ? CoreImages.THREAD : CoreImages.METHOD_NON_OPTIMIZED);
			setChecked(fromRoot == threadRootAtTop);
		}

		@Override
		public void run() {
			boolean newValue = isChecked() == fromThreadRootAction;
			if (newValue != threadRootAtTop) {
				threadRootAtTop = newValue;
				rebuildModel(currentItems);
			}
		}
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		frameSeparator = new FrameSeparator(FrameCategorization.METHOD, false);
		groupByActions = new GroupByAction[] {new GroupByAction(false), new GroupByAction(true)};

		//methodFormatter = new MethodFormatter(null, () -> viewer.refresh());
		IMenuManager siteMenu = site.getActionBars().getMenuManager();
		siteMenu.add(new Separator(MCContextMenuManager.GROUP_TOP));
		siteMenu.add(new Separator(MCContextMenuManager.GROUP_VIEWER_SETUP));
		// addOptions(siteMenu);
		IToolBarManager toolBar = site.getActionBars().getToolBarManager();
		Stream.of(groupByActions).forEach(toolBar::add);
		getSite().getPage().addSelectionListener(this);
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		container = new SashForm(parent, SWT.HORIZONTAL);
		browser = new Browser(container, SWT.NONE);
		container.setMaximizedControl(browser);
	}

	@Override
	public void setFocus() {
		browser.setFocus();
	}

	@Override
	public void saveState(IMemento memento) {
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object first = ((IStructuredSelection) selection).getFirstElement();
			setItems(AdapterUtil.getAdapter(first, IItemCollection.class));
		}
	}

	private void setItems(IItemCollection items) {
		if (items != null) {
			currentItems = items;
			rebuildModel(items);
		}
	}

	private void rebuildModel(IItemCollection items) {
		// Release old model before building the new
		if (currentModelCalculator != null) {
			currentModelCalculator.cancel(true);
		}
		currentModelCalculator = getModelPreparer(items, frameSeparator, true);
		currentModelCalculator.thenAcceptAsync(this::setModel, DisplayToolkit.inDisplayThread())
				.exceptionally(FlameGraphView::handleModelBuildException);
	}

	private CompletableFuture<TraceNode> getModelPreparer(
		final IItemCollection items, final FrameSeparator separator, final boolean materializeSelectedBranches) {
		return CompletableFuture.supplyAsync(() -> {
			return TraceTreeUtils.createTree(items, separator, threadRootAtTop, "-- <Root> --");
		}, MODEL_EXECUTOR);
	}

	private static Void handleModelBuildException(Throwable ex) {
		if (!(ex.getCause() instanceof CancellationException)) {
			FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE, "Failed to build stacktrace view model", ex); //$NON-NLS-1$
		}
		return null;
	}

	private void setModel(TraceNode root) {
		if (!browser.isDisposed() && !root.equals(currentRoot)) {
			currentRoot = root;
			setViewerInput(root);
		}
	}

	private void setViewerInput(TraceNode root) {
		try {
			browser.setText(StringToolkit.readString(FlameGraphView.class.getResourceAsStream("page.html")));
			browser.addProgressListener(new ProgressAdapter() {
				@Override
				public void completed(ProgressEvent event) {
					browser.removeProgressListener(this);
					browser.execute(String.format("processGraph(%s);", toJSon(root)));
				}
			});
		} catch (IOException e) {
			browser.setText(e.getMessage());
			e.printStackTrace();
		}
	}

	private static String toJSon(TraceNode root) {
		if (root == null) {
			return "\"\"";
		}
		return render(root);
	}

	private static String render(TraceNode root) {
		StringBuilder builder = new StringBuilder();
		render(builder, root);
		return builder.toString();
	}

	private static void render(StringBuilder builder, TraceNode node) {
		String start = String.format("{%s,%s, \"children\": [ ", toJSonKeyValue("name", node.getName()),
				toJSonKeyValue("value", String.valueOf(node.getValue())));
		builder.append(start);
		for (int i = 0; i < node.getChildren().size(); i++) {
			render(builder, node.getChildren().get(i));
			if (i < node.getChildren().size() - 1) {
				builder.append(",");
			}
		}
		builder.append("]}");
	}

	private static String toJSonKeyValue(String key, String value) {
		return "\"" + key + "\": " + "\"" + value + "\"";
	}
}
