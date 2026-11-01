/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.joverflow.ui;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.part.Page;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.joverflow.dominator.DominatorTree;
import org.openjdk.jmc.joverflow.dominator.DominatorTreeBuilder;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;
import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;
import org.openjdk.jmc.joverflow.ui.viewers.DominatorTreeViewer;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

/**
 * Computes trees on request; the editor caches results across view reopenings.
 */
public class DominatorTreePage extends Page {
	private static final int PROGRESS_UPDATE_INTERVAL_MS = 250;
	private static final int MIN_READ_BUFFER_SIZE = 16 * 1024 * 1024;
	private static final int MAX_READ_BUFFER_SIZE = 256 * 1024 * 1024;
	private static final int LABEL_READ_BUFFER_SIZE = 1024 * 1024;

	private final JOverflowEditor editor;
	private final JOverflowEditor.UiLoadedListener uiLoadedListener = ui -> onSnapshotLoaded();

	private Composite container;
	private StackLayout containerLayout;

	private Composite startPanel;
	private Button calculateButton;
	private Label waitingLabel;

	private Composite progressPanel;
	private Label progressLabel;
	private ProgressBar progressBar;

	private Composite treePanel;
	private Label summaryLabel;
	private DominatorTreeViewer viewer;

	private DominatorTreeBuilder builder;
	private volatile IProgressMonitor jobMonitor;

	DominatorTreePage(JOverflowEditor editor) {
		this.editor = editor;
	}

	@Override
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		containerLayout = new StackLayout();
		container.setLayout(containerLayout);

		createStartPanel();
		createProgressPanel();
		createTreePanel();

		DominatorTree tree = editor.getDominatorTree();
		if (tree != null) {
			showTree(tree);
		} else {
			showPanel(startPanel);
		}
		// Fires immediately if the snapshot is already loaded
		editor.addUiLoadedListener(uiLoadedListener);
	}

	private void createStartPanel() {
		startPanel = new Composite(container, SWT.NONE);
		startPanel.setLayout(new GridLayout(1, false));

		Label description = new Label(startPanel, SWT.WRAP);
		description.setText(Messages.DominatorTreePage_DESCRIPTION);
		GridData descriptionData = new GridData(SWT.FILL, SWT.TOP, true, false);
		descriptionData.widthHint = 500;
		description.setLayoutData(descriptionData);

		calculateButton = new Button(startPanel, SWT.PUSH);
		calculateButton.setText(Messages.DominatorTreePage_CALCULATE);
		calculateButton.setEnabled(false);
		calculateButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> startCalculation()));

		waitingLabel = new Label(startPanel, SWT.NONE);
		waitingLabel.setText(Messages.DominatorTreePage_WAITING_FOR_MODEL);
	}

	private void createProgressPanel() {
		progressPanel = new Composite(container, SWT.NONE);
		progressPanel.setLayout(new GridLayout(1, false));

		progressLabel = new Label(progressPanel, SWT.NONE);
		progressLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		progressBar = new ProgressBar(progressPanel, SWT.HORIZONTAL | SWT.SMOOTH);
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		GridData progressBarData = new GridData(SWT.FILL, SWT.TOP, true, false);
		progressBarData.widthHint = 400;
		progressBar.setLayoutData(progressBarData);

		Button cancelButton = new Button(progressPanel, SWT.PUSH);
		cancelButton.setText(Messages.DominatorTreePage_CANCEL);
		cancelButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> cancelCalculation()));
	}

	private void createTreePanel() {
		treePanel = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		treePanel.setLayout(layout);

		summaryLabel = new Label(treePanel, SWT.NONE);
		GridData summaryData = new GridData(SWT.FILL, SWT.TOP, true, false);
		summaryData.horizontalIndent = 5;
		summaryLabel.setLayoutData(summaryData);

		ReadBuffer.Factory factory = new ReadBuffer.CachedReadBufferFactory(editor.getInputPath(),
				LABEL_READ_BUFFER_SIZE);
		viewer = new DominatorTreeViewer(treePanel, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, factory);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	@Override
	public Control getControl() {
		return container;
	}

	@Override
	public void setFocus() {
		if (containerLayout.topControl == treePanel) {
			viewer.getControl().setFocus();
		} else {
			containerLayout.topControl.setFocus();
		}
	}

	@Override
	public void dispose() {
		editor.removeUiLoadedListener(uiLoadedListener);
		cancelCalculation();
		if (viewer != null && !viewer.getControl().isDisposed()) {
			viewer.setDominatorTree(null);
		}
		super.dispose();
	}

	private void onSnapshotLoaded() {
		if (calculateButton == null || calculateButton.isDisposed()) {
			return;
		}
		calculateButton.setEnabled(editor.getSnapshot() != null && editor.getInputPath() != null);
		waitingLabel.setVisible(!calculateButton.isEnabled());
	}

	private void startCalculation() {
		Snapshot snapshot = editor.getSnapshot();
		String inputPath = editor.getInputPath();
		if (snapshot == null || inputPath == null || builder != null) {
			return;
		}
		ReadBuffer.Factory factory = new ReadBuffer.CachedReadBufferFactory(inputPath, calculateReadBufferSize());
		DominatorTreeBuilder newBuilder = new DominatorTreeBuilder(snapshot, factory);
		builder = newBuilder;

		Job job = new Job(NLS.bind(Messages.DominatorTreePage_JOB_NAME, editor.getPartName())) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				jobMonitor = monitor;
				try {
					DominatorTree tree = newBuilder.build();
					DisplayToolkit.safeAsyncExec(container, () -> onCalculationDone(newBuilder, tree));
					return Status.OK_STATUS;
				} catch (HprofParsingCancelledException e) {
					DisplayToolkit.safeAsyncExec(container, () -> onCalculationCancelled(newBuilder));
					return Status.CANCEL_STATUS;
				} catch (IOException | RuntimeException | OutOfMemoryError e) {
					DisplayToolkit.safeAsyncExec(container, () -> onCalculationFailed(newBuilder, e));
					// The page reports failures; avoid a second job error dialog.
					return Status.OK_STATUS;
				} finally {
					jobMonitor = null;
				}
			}
		};
		job.setUser(false);
		job.setSystem(false);

		progressBar.setSelection(0);
		updateProgressLabel(newBuilder);
		showPanel(progressPanel);
		job.schedule();
		container.getDisplay().timerExec(PROGRESS_UPDATE_INTERVAL_MS, () -> updateProgress(newBuilder));
	}

	private void cancelCalculation() {
		if (builder != null) {
			builder.cancel();
		}
	}

	private void updateProgress(DominatorTreeBuilder currentBuilder) {
		if (currentBuilder != builder || container.isDisposed()) {
			return;
		}
		IProgressMonitor monitor = jobMonitor;
		if (monitor != null && monitor.isCanceled()) {
			currentBuilder.cancel();
		}
		progressBar.setSelection(currentBuilder.getProgressPercentage());
		updateProgressLabel(currentBuilder);
		container.getDisplay().timerExec(PROGRESS_UPDATE_INTERVAL_MS, () -> updateProgress(currentBuilder));
	}

	private void updateProgressLabel(DominatorTreeBuilder currentBuilder) {
		String phase = getPhaseDescription(currentBuilder.getPhase());
		progressLabel.setText(
				NLS.bind(Messages.DominatorTreePage_PROGRESS, phase, currentBuilder.getPhaseProgressPercentage()));
		progressPanel.layout();
	}

	private static String getPhaseDescription(DominatorTreeBuilder.Phase phase) {
		switch (phase) {
		case SCANNING_HEAP:
			return Messages.DominatorTreePage_PHASE_SCANNING_HEAP;
		case BUILDING_GRAPH:
			return Messages.DominatorTreePage_PHASE_BUILDING_GRAPH;
		case COMPUTING_DOMINATORS:
			return Messages.DominatorTreePage_PHASE_COMPUTING_DOMINATORS;
		case COMPUTING_RETAINED_SIZES:
		case DONE:
		default:
			return Messages.DominatorTreePage_PHASE_COMPUTING_RETAINED_SIZES;
		}
	}

	private void onCalculationDone(DominatorTreeBuilder finishedBuilder, DominatorTree tree) {
		if (finishedBuilder != builder) {
			return;
		}
		builder = null;
		editor.setDominatorTree(tree);
		showTree(tree);
	}

	private void onCalculationCancelled(DominatorTreeBuilder finishedBuilder) {
		if (finishedBuilder != builder) {
			return;
		}
		builder = null;
		showPanel(startPanel);
	}

	private void onCalculationFailed(DominatorTreeBuilder finishedBuilder, Throwable failure) {
		if (finishedBuilder != builder) {
			return;
		}
		builder = null;
		showPanel(startPanel);
		if (failure instanceof OutOfMemoryError) {
			DialogToolkit.showWarning(getSite().getShell(), Messages.DominatorTreePage_FAILED_TITLE,
					Messages.DominatorTreePage_OUT_OF_MEMORY);
		} else {
			DialogToolkit.showException(getSite().getShell(), Messages.DominatorTreePage_FAILED_TITLE,
					failure.getLocalizedMessage(), failure);
		}
	}

	private void showTree(DominatorTree tree) {
		int unreachableNode = tree.getUnreachableObjectsNode();
		int unreachableCount = unreachableNode == DominatorTree.NO_NODE ? 0
				: countObjectsInSubtree(tree, unreachableNode);
		summaryLabel.setText(NLS.bind(Messages.DominatorTreePage_SUMMARY,
				new Object[] {formatSize(tree.getTotalSize()), String.format("%,d", tree.getObjectCount()), //$NON-NLS-1$
						formatSize(tree.getUnreachableSize()), String.format("%,d", unreachableCount)})); //$NON-NLS-1$
		viewer.setDominatorTree(tree);
		showPanel(treePanel);
	}

	/**
	 * The unreachable node is created after all reachable nodes; its subtree occupies all later
	 * IDs.
	 */
	private static int countObjectsInSubtree(DominatorTree tree, int node) {
		return tree.getNodeCount() - node - 1;
	}

	private static String formatSize(long bytes) {
		return UnitLookup.BYTE.quantity(bytes).displayUsing(IDisplayable.AUTO);
	}

	private void showPanel(Composite panel) {
		if (container.isDisposed()) {
			return;
		}
		containerLayout.topControl = panel;
		container.layout();
	}

	/**
	 * Reserves most available memory for tree computation.
	 */
	private static int calculateReadBufferSize() {
		Runtime runtime = Runtime.getRuntime();
		long available = runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory();
		return (int) Math.max(MIN_READ_BUFFER_SIZE, Math.min(MAX_READ_BUFFER_SIZE, available / 5));
	}
}
