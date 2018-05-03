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
package org.openjdk.jmc.flightrecorder.ui.views.stacktrace;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.part.ViewPart;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.collection.SimpleArray;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFormatToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Branch;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Fork;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.IFlavoredSelection;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStore;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.flightrecorder.ui.selection.StacktraceFrameSelection;
import org.openjdk.jmc.ui.CoreImages;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.accessibility.FocusTracker;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.CopySelectionAction;
import org.openjdk.jmc.ui.handlers.InFocusHandlerActivator;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.handlers.MethodFormatter;
import org.openjdk.jmc.ui.misc.AbstractStructuredContentProvider;
import org.openjdk.jmc.ui.misc.CompositeToolkit;
import org.openjdk.jmc.ui.misc.CopySettings;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.FormatToolkit;
import org.openjdk.jmc.ui.misc.MementoToolkit;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;

public class StacktraceView extends ViewPart implements ISelectionListener {

	static {
		// Adapt using IAdapterFactory to support object contribution for IMCMethod (e.g jump to source)
		Platform.getAdapterManager().registerAdapters(new IAdapterFactory() {

			@Override
			public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
				if (adaptableObject instanceof StacktraceFrame && adapterType == IMCFrame.class) {
					return adapterType.cast(((StacktraceFrame) adaptableObject).getFrame());
				}
				return null;
			}

			@Override
			public Class<?>[] getAdapterList() {
				return new Class[] {IMCFrame.class};
			}
		}, StacktraceFrame.class);
	}

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
				rebuildModel();
			}
		}
	}

	private static final String HELP_CONTEXT_ID = FlightRecorderUI.PLUGIN_ID + ".StacktraceView"; //$NON-NLS-1$
	// FIXME: Define dynamic color (editable in preferences, to handle dark themes etc.)
	private static final Color ALTERNATE_COLOR = SWTColorToolkit.getColor(new RGB(255, 255, 240));
	private static final String COUNT_IMG_KEY = "countColor"; //$NON-NLS-1$
	private static final Color COUNT_COLOR = SWTColorToolkit.getColor(new RGB(100, 200, 100));
	private static final String SIBLINGS_IMG_KEY = "siblingsColor"; //$NON-NLS-1$
	private static final Color SIBLINGS_COUNT_COLOR = SWTColorToolkit.getColor(new RGB(170, 250, 170));
	private static final int[] DEFAULT_COLUMN_WIDTHS = {700, 150};
	private static final String THREAD_ROOT_KEY = "threadRootAtTop"; //$NON-NLS-1$
	private static final String FRAME_OPTIMIZATION_KEY = "distinguishFramesByOptimization"; //$NON-NLS-1$
	private static final String FRAME_CATEGORIZATION_KEY = "distinguishFramesCategorization"; //$NON-NLS-1$
	private static final String TREE_LAYOUT_KEY = "treeLayout"; //$NON-NLS-1$
	private static final String REDUCED_TREE_KEY = "reducedTreeLayout"; //$NON-NLS-1$
	private static final String METHOD_FORMAT_KEY = "metodFormat"; //$NON-NLS-1$
	private static final String COLUMNS_KEY = "columns"; //$NON-NLS-1$
	private static final String COLUMNS_SEPARATOR = " "; //$NON-NLS-1$
	private ColumnViewer viewer;
	private boolean treeLayout;
	private boolean reducedTree;
	private boolean threadRootAtTop;
	private IItemCollection itemsToShow;
	private MethodFormatter methodFormatter;
	private FrameSeparatorManager frameSeparatorManager;
	private GroupByAction[] groupByActions;
	private IAction[] layoutActions;
	private ViewerAction[] viewerActions;
	private int[] columnWidths;

	private static class StacktraceViewToolTipSupport extends ColumnViewerToolTipSupport {

		StacktraceViewToolTipSupport(ColumnViewer viewer) {
			super(viewer, ToolTip.NO_RECREATE, false);
		}

		@Override
		protected Composite createViewerToolTipContentArea(Event event, ViewerCell cell, Composite parent) {
			FormText formText = CompositeToolkit.createInfoFormText(parent);
			formText.setImage(COUNT_IMG_KEY, SWTColorToolkit.getColorThumbnail(COUNT_COLOR.getRGB()));
			formText.setImage(SIBLINGS_IMG_KEY, SWTColorToolkit.getColorThumbnail(SIBLINGS_COUNT_COLOR.getRGB()));
			formText.setText(getText(event), true, false);
			return formText;
		}

	}

	private static class ViewerAction extends Action implements ISelectionChangedListener {

		protected StructuredViewer provider = null;

		public ViewerAction(String text) {
			super(text);
			setViewer(null);
			setEnabled(false);
		}

		public void setViewer(StructuredViewer provider) {
			this.provider = provider;
			if (provider != null) {
				provider.addSelectionChangedListener(this);
				selectionChanged(getStructuredSelection());
			} else {
				setEnabled(false);
			}
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			ISelection selection = event.getSelection();
			if (selection instanceof IStructuredSelection) {
				selectionChanged((IStructuredSelection) selection);
			}
		}

		protected void selectionChanged(IStructuredSelection selection) {
		}

		protected IStructuredSelection getStructuredSelection() {
			if (provider != null) {
				ISelection selection = provider.getSelection();
				if (selection instanceof IStructuredSelection) {
					return (IStructuredSelection) selection;
				}
			}
			return new StructuredSelection();
		}

	}

	static class SelectFrameGroupAction extends ViewerAction {

		SelectFrameGroupAction() {
			super(Messages.STACKTRACE_VIEW_FRAME_GROUP_CHOOSE);
			setImageDescriptor(
					FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_ARROW_FORK3_STAR));
			setAccelerator(SWT.CR);
		}

		@Override
		public void setViewer(StructuredViewer provider) {
			super.setViewer(provider);
			if (provider != null) {
				provider.addDoubleClickListener(e -> {
					if (isEnabled()) {
						run();
					}
				});
			}
		}

		@Override
		public void run() {
			StacktraceFrame frame = (StacktraceFrame) getStructuredSelection().getFirstElement();
			// FIXME: Would like to move the table cursor after changing sibling state, not just the selection.
			if (isInOpenFork(frame)) {
				frame.getBranch().selectSibling(0);
			} else {
				frame.getBranch().selectSibling(null);
			}
			provider.getControl().setRedraw(false);
			try {
				provider.refresh();
			} finally {
				provider.getControl().setRedraw(true);
			}
			provider.setSelection(new StructuredSelection(frame));

		}

		@Override
		public void selectionChanged(IStructuredSelection selection) {
			setEnabled(selection.size() == 1
					&& isFirstInBranchWithSiblings((StacktraceFrame) selection.getFirstElement()));
		}

	}

	static class NavigateAction extends ViewerAction implements TraverseListener {

		private final int offset;

		NavigateAction(boolean forward) {
			super(forward ? Messages.STACKTRACE_VIEW_FRAME_GROUP_NEXT : Messages.STACKTRACE_VIEW_FRAME_GROUP_PREVIOUS);
			setImageDescriptor(
					forward ? FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_ARROW_FORK3_RIGHT)
							: FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_ARROW_FORK3_LEFT));
			offset = forward ? 1 : -1;
			setAccelerator(forward ? SWT.ARROW_RIGHT : SWT.ARROW_LEFT);
		}

		@Override
		public void setViewer(StructuredViewer provider) {
			super.setViewer(provider);
			if (provider != null) {
				provider.getControl().addTraverseListener(this);
			}
		}

		@Override
		public void run() {
			Branch branch = ((StacktraceFrame) getStructuredSelection().getFirstElement()).getBranch();
			Branch selectedSibling = branch.selectSibling(offset);
			provider.refresh();
			provider.setSelection(new StructuredSelection(selectedSibling.getFirstFrame()));
		}

		@Override
		protected void selectionChanged(IStructuredSelection selection) {
			setEnabled(selection.size() == 1 && isNavigationFrame((StacktraceFrame) selection.getFirstElement()));
		}

		@Override
		public void keyTraversed(TraverseEvent e) {
			if (isEnabled()) {
				if (e.keyCode == getAccelerator()) {
					run();
					e.detail = SWT.TRAVERSE_NONE;
					e.doit = true;
				}
			}
		}

	};

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		IState state = MementoToolkit.asState(memento);
		threadRootAtTop = StateToolkit.readBoolean(state, THREAD_ROOT_KEY, false);
		groupByActions = new GroupByAction[] {new GroupByAction(false), new GroupByAction(true)};
		treeLayout = StateToolkit.readBoolean(state, TREE_LAYOUT_KEY, false);
		reducedTree = StateToolkit.readBoolean(state, REDUCED_TREE_KEY, true);

		IAction reducedTreeAction = ActionToolkit.checkAction(this::setReducedTree,
				Messages.STACKTRACE_VIEW_REDUCE_TREE_DEPTH, null);
		reducedTreeAction.setChecked(reducedTree);
		IAction treeAction = ActionToolkit.checkAction(this::setTreeLayout, Messages.STACKTRACE_VIEW_SHOW_AS_TREE,
				CoreImages.TREE_MODE);
		treeAction.setChecked(treeLayout);
		layoutActions = new IAction[] {treeAction, reducedTreeAction};

		NavigateAction forwardAction = new NavigateAction(true);
		NavigateAction backwardAction = new NavigateAction(false);
		SelectFrameGroupAction selectGroupAction = new SelectFrameGroupAction();
		viewerActions = new ViewerAction[] {selectGroupAction, forwardAction, backwardAction};

		try {
			columnWidths = Optional.ofNullable(state)
					.map(s -> Stream.of(s.getAttribute(COLUMNS_KEY).split(COLUMNS_SEPARATOR))
							.mapToInt(Integer::parseInt).toArray())
					.filter(widths -> widths.length == DEFAULT_COLUMN_WIDTHS.length
							&& Arrays.stream(widths).allMatch(w -> w >= 0))
					.orElse(DEFAULT_COLUMN_WIDTHS);
		} catch (RuntimeException e) {
			columnWidths = DEFAULT_COLUMN_WIDTHS;
		}

		FrameCategorization categorization = StateToolkit.readEnum(state, FRAME_CATEGORIZATION_KEY,
				FrameCategorization.METHOD, FrameCategorization.class);
		boolean byOptimization = StateToolkit.readBoolean(state, FRAME_OPTIMIZATION_KEY, false);
		frameSeparatorManager = new FrameSeparatorManager(this::rebuildModel,
				new FrameSeparator(categorization, byOptimization));
		methodFormatter = new MethodFormatter(memento == null ? null : memento.getChild(METHOD_FORMAT_KEY),
				() -> viewer.refresh());
		IMenuManager siteMenu = site.getActionBars().getMenuManager();
		siteMenu.add(new Separator(MCContextMenuManager.GROUP_TOP));
		siteMenu.add(new Separator(MCContextMenuManager.GROUP_VIEWER_SETUP));
		addOptions(siteMenu);
		IToolBarManager toolBar = site.getActionBars().getToolBarManager();
		toolBar.add(selectGroupAction);
		toolBar.add(backwardAction);
		toolBar.add(forwardAction);
		toolBar.add(new Separator());
		toolBar.add(treeAction);
		toolBar.add(new Separator());
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
		buildViewer(parent);
	}

	private void setTreeLayout(boolean treeLayout) {
		this.treeLayout = treeLayout;
		rebuildViewer();
	}

	private void setReducedTree(boolean reducedTree) {
		this.reducedTree = reducedTree;
		if (viewer instanceof TreeViewer) {
			viewer.setContentProvider(createTreeContentProvider());
		}
	}

	private void rebuildViewer() {
		boolean hasFocus = viewer.getControl().isFocusControl();
		ISelection oldSelection = viewer.getSelection();
		Fork oldInput = (Fork) viewer.getInput();
		Composite parent = viewer.getControl().getParent();
		viewer.getControl().dispose();
		buildViewer(parent);
		if (hasFocus) {
			viewer.getControl().setFocus();
		}
		parent.layout();
		if (viewer instanceof TreeViewer) {
			// Async set input to avoid drawing issue with tree
			Display.getCurrent().asyncExec(() -> {
				if (!viewer.getControl().isDisposed()) {
					setViewerInput(oldInput);
					if (reducedTree && oldInput != null) {
						Branch selectedBranch = getLastSelectedBranch(oldInput);
						if (selectedBranch != null) {
							viewer.getControl().setRedraw(false);
							((TreeViewer) viewer).expandToLevel(selectedBranch.getLastFrame(),
									AbstractTreeViewer.ALL_LEVELS);
							viewer.getControl().setRedraw(true);
						}
					}
					viewer.setSelection(oldSelection, true);
				}
			});
		} else {
			Branch branch = null;
			for (Object o : ((IStructuredSelection) oldSelection).toList()) {
				if (branch == null) {
					branch = ((StacktraceFrame) o).getBranch();
				} else if (branch != ((StacktraceFrame) o).getBranch()) {
					branch = null;
					break;
				}
			}
			if (branch != null) {
				branch.selectSibling(0);
			}
			setViewerInput(oldInput);
			viewer.setSelection(oldSelection, true);
		}
	}

	private void buildViewer(Composite parent) {
		if (treeLayout) {
			viewer = buildTree(parent);
		} else {
			viewer = buildTable(parent);
		}
		new StacktraceViewToolTipSupport(viewer);
		MCContextMenuManager mm = MCContextMenuManager.create(viewer.getControl());
		CopySelectionAction copyAction = new CopySelectionAction(viewer,
				FormatToolkit.selectionFormatter(stackTraceLabelProvider, countLabelProvider));
		InFocusHandlerActivator.install(viewer.getControl(), copyAction);
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT, copyAction);
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT, CopySettings.getInstance().createContributionItem());
		addOptions(mm);
		getSite().registerContextMenu(mm, viewer);
		if (!treeLayout) {
			String navigateGroupName = "NAVIGATE"; //$NON-NLS-1$
			mm.insert(0, new Separator(navigateGroupName));
			Stream.of(viewerActions).forEach(a -> {
				a.setViewer(viewer);
				mm.appendToGroup(navigateGroupName, a);
			});
		} else {
			Stream.of(viewerActions).forEach(a -> a.setViewer(null));
		}

		viewer.getControl().addListener(SWT.EraseItem, COUNT_BACKGROUND_DRAWER);
		viewer.getControl().addDisposeListener(e -> columnWidths = getColumnWidths());

		buildColumn(viewer, Messages.STACKTRACE_VIEW_STACK_TRACE, SWT.NONE, columnWidths[0])
				.setLabelProvider(stackTraceLabelProvider);
		buildColumn(viewer, Messages.STACKTRACE_VIEW_COUNT_COLUMN_NAME, SWT.RIGHT, columnWidths[1])
				.setLabelProvider(countLabelProvider);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), HELP_CONTEXT_ID);

		if (UIPlugin.getDefault().getAccessibilityMode()) {
			if (treeLayout) {
				FocusTracker.enableFocusTracking(((TreeViewer) viewer).getTree());
			} else {
				FocusTracker.enableFocusTracking(((TableViewer) viewer).getTable());
			}
		}
	}

	private static TableViewer buildTable(Composite parent) {
		TableViewer tableViewer = new TableViewer(parent,
				SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		tableViewer.setContentProvider(new AbstractStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				SimpleArray<StacktraceFrame> trace = new SimpleArray<>(new StacktraceFrame[100]);
				addSelectedBranches((Fork) inputElement, trace, false);
				return trace.elements();
			}
		});
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);
		return tableViewer;
	}

	private TreeViewer buildTree(Composite parent) {
		TreeViewer treeViewer = new TreeViewer(parent,
				SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		treeViewer.setContentProvider(createTreeContentProvider());
		treeViewer.getTree().setHeaderVisible(true);
		treeViewer.getTree().setLinesVisible(true);
		return treeViewer;
	}

	private static ViewerColumn buildColumn(ColumnViewer viewer, String text, int style, int width) {
		if (viewer instanceof TableViewer) {
			TableViewerColumn vc = new TableViewerColumn((TableViewer) viewer, style);
			vc.getColumn().setWidth(width);
			vc.getColumn().setText(text);
			return vc;
		} else {
			TreeViewerColumn vc = new TreeViewerColumn((TreeViewer) viewer, style);
			vc.getColumn().setWidth(width);
			vc.getColumn().setText(text);
			return vc;
		}
	}

	private int[] getColumnWidths() {
		if (!viewer.getControl().isDisposed()) {
			if (viewer instanceof TableViewer) {
				return Stream.of(((TableViewer) viewer).getTable().getColumns()).mapToInt(TableColumn::getWidth)
						.toArray();
			} else {
				return Stream.of(((TreeViewer) viewer).getTree().getColumns()).mapToInt(TreeColumn::getWidth).toArray();
			}
		}
		return columnWidths;
	}

	private void addOptions(IMenuManager menu) {
		MenuManager groupMenu = new MenuManager(Messages.STACKTRACE_VIEW_GROUP_FROM);
		Stream.of(groupByActions).forEach(groupMenu::add);
		menu.appendToGroup(MCContextMenuManager.GROUP_TOP, groupMenu);
		menu.appendToGroup(MCContextMenuManager.GROUP_TOP, frameSeparatorManager.createMenu());
		MenuManager layoutMenu = new MenuManager(Messages.STACKTRACE_VIEW_LAYOUT_OPTIONS);
		Stream.of(layoutActions).forEach(layoutMenu::add);
		menu.appendToGroup(MCContextMenuManager.GROUP_VIEWER_SETUP, layoutMenu);
		menu.appendToGroup(MCContextMenuManager.GROUP_VIEWER_SETUP, methodFormatter.createMenu());
		SelectionStoreActionToolkit.addSelectionStoreActions(viewer, this::getSelectionStore,
				this::getFlavoredSelection, menu);
	}

	private IFlavoredSelection getFlavoredSelection() {
		ISelection selection = viewer.getSelection();
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			List<?> selected = ((StructuredSelection) selection).toList();
			StacktraceFrame frame = (StacktraceFrame) selected.get(0);
			return new StacktraceFrameSelection(frame.getFrame(),
					ItemCollectionToolkit.build(Stream.of(frame.getItems().elements())),
					Messages.STACKTRACE_VIEW_SELECTION);
		}
		return null;
	}

	private SelectionStore getSelectionStore() {
		IEditorPart editorPart = null;
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			editorPart = getSite().getPage().getActiveEditor();
		} catch (Exception e) {
			FlightRecorderUI.getDefault().getLogger().log(Level.INFO,
					"Got exception while trying to get the active editor", e); //$NON-NLS-1$
		}
		if (editorPart instanceof IPageContainer) {
			return ((IPageContainer) editorPart).getSelectionStore();
		}
		return null;
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void saveState(IMemento memento) {
		memento.putString(COLUMNS_KEY, IntStream.of(getColumnWidths()).mapToObj(Integer::toString)
				.collect(Collectors.joining(COLUMNS_SEPARATOR)));
		methodFormatter.saveState(memento.createChild(METHOD_FORMAT_KEY));
		memento.putBoolean(THREAD_ROOT_KEY, threadRootAtTop);
		memento.putBoolean(TREE_LAYOUT_KEY, treeLayout);
		memento.putBoolean(REDUCED_TREE_KEY, reducedTree);
		FrameSeparator frameSeparator = frameSeparatorManager.getFrameSeparator();
		memento.putBoolean(FRAME_OPTIMIZATION_KEY, frameSeparator.isDistinguishFramesByOptimization());
		memento.putString(FRAME_CATEGORIZATION_KEY, frameSeparator.getCategorization().name());
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object first = ((IStructuredSelection) selection).getFirstElement();
			IItemCollection items = AdapterUtil.getAdapter(first, IItemCollection.class);
			if (items != null && !items.equals(itemsToShow)) {
				setItems(items);
			}
		}
	}

	private void setItems(IItemCollection items) {
		itemsToShow = items;
		rebuildModel();
	}

	private StacktraceModel createStacktraceModel() {
		return new StacktraceModel(threadRootAtTop, frameSeparatorManager.getFrameSeparator(), itemsToShow);
	}

	private void rebuildModel() {
		// Release old model before building the new
		setViewerInput(null);
		CompletableFuture<StacktraceModel> modelPreparer = getModelPreparer(createStacktraceModel(), !treeLayout);
		modelPreparer.thenAcceptAsync(this::setModel, DisplayToolkit.inDisplayThread())
				.exceptionally(StacktraceView::handleModelBuildException);
	}

	private static CompletableFuture<StacktraceModel> getModelPreparer(
		StacktraceModel model, boolean materializeSelectedBranches) {
		return CompletableFuture.supplyAsync(() -> {
			Fork root = model.getRootFork();
			if (materializeSelectedBranches) {
				Branch selectedBranch = getLastSelectedBranch(root);
				if (selectedBranch != null) {
					selectedBranch.getEndFork();
				}
			}
			return model;
		});
	}

	private static Void handleModelBuildException(Throwable ex) {
		FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE, "Failed to build stacktrace view model", ex); //$NON-NLS-1$
		return null;
	}

	private void setModel(StacktraceModel model) {
		// Check that the model is up to date
		if (model.equals(createStacktraceModel()) && !viewer.getControl().isDisposed()) {
			setViewerInput(model.getRootFork());
		}
	}

	private void setViewerInput(Fork rootFork) {
		// NOTE: will be slow for TreeViewer if number of roots or children of a node are more than ~1000
		viewer.setInput(rootFork);
	}

	private ITreeContentProvider createTreeContentProvider() {
		return reducedTree ? new StacktraceReducedTreeContentProvider() : new StacktraceTreeContentProvider();
	}

	private static final Listener COUNT_BACKGROUND_DRAWER = new Listener() {
		@Override
		public void handleEvent(Event event) {
			StacktraceFrame frame = (StacktraceFrame) event.item.getData();
			Fork rootFork = getRootFork(frame.getBranch().getParentFork());
			double total;
			if (event.index == 1 && (total = rootFork.getItemsInFork()) > 0) { // index == 1 => count column
				// Draw siblings
				Fork parentFork = frame.getBranch().getParentFork();
				int forkOffset = parentFork.getItemOffset();
				int siblingsStart = (int) Math.floor(event.width * forkOffset / total);
				int siblingsWidth = (int) Math.round(event.width * parentFork.getItemsInFork() / total);
				event.gc.setBackground(SIBLINGS_COUNT_COLOR);
				event.gc.fillRectangle(event.x + siblingsStart, event.y, siblingsWidth, event.height);
				// Draw group
				double offset = (forkOffset + frame.getBranch().getItemOffsetInFork()) / total;
				double fraction = frame.getItemCount() / total;
				event.gc.setBackground(COUNT_COLOR);
				int startPixel = (int) Math.floor(event.width * offset);
				int widthPixel = (int) Math.round(event.width * fraction);
				event.gc.fillRectangle(event.x + startPixel, event.y, Math.max(widthPixel, 1), event.height);
				event.detail &= ~SWT.BACKGROUND;
			}
		}
	};

	private final ColumnLabelProvider countLabelProvider = new ColumnLabelProvider() {
		@Override
		public String getText(Object element) {
			return Integer.toString(((StacktraceFrame) element).getItemCount());
		}

		@Override
		public String getToolTipText(Object element) {
			StacktraceFrame frame = (StacktraceFrame) element;
			Fork rootFork = getRootFork(frame.getBranch().getParentFork());
			int itemCount = frame.getItemCount();
			int totalCount = rootFork.getItemsInFork();
			Fork parentFork = frame.getBranch().getParentFork();
			int itemsInSiblings = parentFork.getItemsInFork() - frame.getBranch().getFirstFrame().getItemCount();
			String frameFraction = UnitLookup.PERCENT_UNITY.quantity(itemCount / (double) totalCount)
					.displayUsing(IDisplayable.AUTO);
			StringBuilder sb = new StringBuilder("<form>"); //$NON-NLS-1$
			sb.append("<li style='image' value='" + COUNT_IMG_KEY + "'>"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(Messages.stackTraceMessage(itemCount, totalCount, frameFraction));
			sb.append("</li>"); //$NON-NLS-1$
			sb.append("<li style='image' value='" + SIBLINGS_IMG_KEY + "'>"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(Messages.siblingMessage(itemsInSiblings, parentFork.getBranchCount() - 1));
			sb.append("</li>"); //$NON-NLS-1$
			sb.append("</form>"); //$NON-NLS-1$
			return sb.toString();
		}
	};

	private final ColumnLabelProvider stackTraceLabelProvider = new ColumnLabelProvider() {

		@Override
		public String getText(Object element) {
			IMCFrame frame = ((StacktraceFrame) element).getFrame();
			FrameSeparator frameSeparator = frameSeparatorManager.getFrameSeparator();
			return getText(frame, frameSeparator);
		}

		private String getText(IMCFrame frame, FrameSeparator frameSeparator) {
			return StacktraceFormatToolkit.formatFrame(frame, frameSeparator, methodFormatter.showReturnValue(),
					methodFormatter.showReturnValuePackage(), methodFormatter.showClassName(),
					methodFormatter.showClassPackageName(), methodFormatter.showArguments(),
					methodFormatter.showArgumentsPackage());
		}

		@Override
		public Image getImage(Object element) {
			StacktraceFrame frame = (StacktraceFrame) element;
			FlightRecorderUI plugin = FlightRecorderUI.getDefault();
			boolean isFirstInBranch = isFirstInBranchWithSiblings(frame);
			boolean firstInOpenFork = isFirstInBranch && isInOpenFork(frame);
			if (firstInOpenFork || treeLayout && (!reducedTree || isFirstInBranch)) {
				return plugin.getImage(
						threadRootAtTop ? ImageConstants.ICON_ARROW_CURVED_DOWN : ImageConstants.ICON_ARROW_CURVED_UP);
			} else if (isFirstInBranchWithSiblings(frame)) {
				return plugin.getImage(
						threadRootAtTop ? ImageConstants.ICON_ARROW_FORK3_DOWN : ImageConstants.ICON_ARROW_FORK3_UP);
			} else if (isLastFrame(frame)) {
				return plugin.getImage(threadRootAtTop ? ImageConstants.ICON_ARROW_DOWN_END : ImageConstants.ICON_ARROW_UP_END);
			} else {
				return plugin.getImage(threadRootAtTop ? ImageConstants.ICON_ARROW_DOWN : ImageConstants.ICON_ARROW_UP);
			}
		}

		@Override
		public Color getBackground(Object element) {
			if (treeLayout) {
				return null;
			} else {
				int parentCount = 0;
				Branch e = ((StacktraceFrame) element).getBranch();
				while (e != null) {
					e = e.getParentFork().getParentBranch();
					parentCount++;
				}
				return parentCount % 2 == 0 ? null : ALTERNATE_COLOR;
			}
		}
	};

	private static boolean isNavigationFrame(StacktraceFrame frame) {
		return isFirstInBranchWithSiblings(frame) && !isInOpenFork(frame);
	}

	private static boolean isInOpenFork(StacktraceFrame frame) {
		return frame.getBranch().getParentFork().getSelectedBranch() == null;
	}

	private static boolean isFirstInBranchWithSiblings(StacktraceFrame frame) {
		return frame.getBranch().getFirstFrame() == frame && frame.getBranch().getParentFork().getBranchCount() > 1;
	}
	
	private static boolean isLastFrame(StacktraceFrame frame) {
		return frame.getBranch().getLastFrame() == frame && frame.getBranch().getEndFork().getBranchCount() == 0;
	}

	/*
	 * FIXME: 'backwards' argument was used for displaying trace groups built from thread roots with
	 * the thread roots at the bottom. If we don't want to support that scenario then we can remove
	 * this argument.
	 */
	private static void addSelectedBranches(Fork fork, SimpleArray<StacktraceFrame> input, boolean backwards) {
		Branch selectedBranch = fork.getSelectedBranch();
		if (selectedBranch == null) {
			Stream.of(fork.getFirstFrames()).forEach(input::add);
		} else if (backwards) {
			addSelectedBranches(selectedBranch.getEndFork(), input, backwards);
			StacktraceFrame[] tail = selectedBranch.getTailFrames();
			for (int i = tail.length; i > 0; i--) {
				input.add(tail[i - 1]);
			}
			input.add(selectedBranch.getFirstFrame());
		} else {
			input.add(selectedBranch.getFirstFrame());
			input.addAll(selectedBranch.getTailFrames());
			addSelectedBranches(selectedBranch.getEndFork(), input, backwards);
		}
	}

	private static Branch getLastSelectedBranch(Fork fromFork) {
		Branch lastSelectedBranch = null;
		Branch branch = fromFork.getSelectedBranch();
		while (branch != null) {
			lastSelectedBranch = branch;
			branch = branch.getEndFork().getSelectedBranch();
		}
		return lastSelectedBranch;
	}

	private static Fork getRootFork(Fork fork) {
		while (fork.getParentBranch() != null) {
			fork = fork.getParentBranch().getParentFork();
		}
		return fork;
	}

	private static class StacktraceTreeContentProvider extends AbstractStructuredContentProvider
			implements ITreeContentProvider {

		@Override
		public StacktraceFrame[] getElements(Object inputElement) {
			return ((Fork) inputElement).getFirstFrames();
		}

		@Override
		public boolean hasChildren(Object element) {
			StacktraceFrame frame = (StacktraceFrame) element;
			return !isLastFrame(frame);
		}

		@Override
		public StacktraceFrame[] getChildren(Object parentElement) {
			StacktraceFrame frame = (StacktraceFrame) parentElement;
			StacktraceFrame[] tailFrames = frame.getBranch().getTailFrames();
			if (frame.getIndexInBranch() == tailFrames.length) {
				return frame.getBranch().getEndFork().getFirstFrames();
			} else {
				return new StacktraceFrame[] {tailFrames[frame.getIndexInBranch()]};
			}
		}

		@Override
		public StacktraceFrame getParent(Object element) {
			StacktraceFrame frame = (StacktraceFrame) element;
			int parentIndexInBranch = frame.getIndexInBranch() - 1;
			if (parentIndexInBranch > 0) {
				return frame.getBranch().getTailFrames()[parentIndexInBranch - 1];
			} else if (parentIndexInBranch == 0) {
				return frame.getBranch().getFirstFrame();
			} else {
				Branch parentBranch = frame.getBranch().getParentFork().getParentBranch();
				return parentBranch == null ? null : parentBranch.getLastFrame();
			}
		}
	};

	private static class StacktraceReducedTreeContentProvider extends AbstractStructuredContentProvider
			implements ITreeContentProvider {

		@Override
		public StacktraceFrame[] getElements(Object inputElement) {
			Fork rootFork = (Fork) inputElement;
			if (rootFork.getBranchCount() == 1) {
				Branch branch = rootFork.getBranch(0);
				return Stream
						.concat(Stream.concat(Stream.of(branch.getFirstFrame()), Stream.of(branch.getTailFrames())),
								Stream.of(branch.getEndFork().getFirstFrames()))
						.toArray(StacktraceFrame[]::new);
			} else {
				return rootFork.getFirstFrames();
			}
		}

		@Override
		public boolean hasChildren(Object element) {
			StacktraceFrame frame = (StacktraceFrame) element;
			return isFirstInBranchWithSiblings(frame) && frame.getBranch().hasTail();
		}

		@Override
		public StacktraceFrame[] getChildren(Object parentElement) {
			Stream<StacktraceFrame> children = Stream.empty();
			StacktraceFrame frame = (StacktraceFrame) parentElement;
			if (isFirstInBranchWithSiblings(frame)) {
				children = Stream.concat(Stream.of(frame.getBranch().getTailFrames()),
						Stream.of(frame.getBranch().getEndFork().getFirstFrames()));
			}
			return children.toArray(StacktraceFrame[]::new);
		}

		@Override
		public StacktraceFrame getParent(Object element) {
			StacktraceFrame frame = (StacktraceFrame) element;
			if (isFirstInBranchWithSiblings(frame) || frame.getBranch().getParentFork().getBranchCount() == 1) {
				Branch parentBranch = frame.getBranch().getParentFork().getParentBranch();
				return parentBranch == null ? null : parentBranch.getFirstFrame();
			} else {
				return frame.getBranch().getFirstFrame();
			}
		}
	}

}
