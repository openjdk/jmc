/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.ui.pages;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters.MethodFilter;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFormatToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Branch;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Fork;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.AggregatableFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IDataPageFactory;
import org.openjdk.jmc.flightrecorder.ui.IDisplayablePage;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FilterComponent;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector.FlavorSelectorState;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.ItemHistogramBuilder;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.column.TableSettings.ColumnSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.handlers.MethodFormatter;
import org.openjdk.jmc.ui.misc.AbstractStructuredContentProvider;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.PersistableSashForm;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;

public class MethodProfilingPage extends AbstractDataPage {
	private static final Color ALTERNATE_COLOR = SWTColorToolkit.getColor(new RGB(255, 255, 240));
	private static final String COUNT_IMG_KEY = "countColor"; //$NON-NLS-1$
	private static final String SIBLINGS_IMG_KEY = "siblingsColor"; //$NON-NLS-1$
	private static final String PERCENTAGE_COL_ID = "HotMethods.Percentage"; //$NON-NLS-1$
	private static final Color SIBLINGS_COUNT_COLOR = SWTColorToolkit.getColor(new RGB(170, 250, 170));
	private static final Color COUNT_COLOR = SWTColorToolkit.getColor(new RGB(100, 200, 100));

	private static final Listener PERCENTAGE_BACKGROUND_DRAWER = new Listener() {
		@Override
		public void handleEvent(Event event) {
			StacktraceFrame frame = (StacktraceFrame) event.item.getData();
			Fork rootFork = getRootFork(frame.getBranch().getParentFork());
			double total;
			if (event.index == 2 && (total = rootFork.getItemsInFork()) > 0) { // index == 2 => percentage column
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

	private static final Listener SUCCESSOR_PERCENTAGE_BACKGROUND_DRAWER = new Listener() {
		@Override
		public void handleEvent(Event event) {
			SuccessorNode node = (SuccessorNode) event.item.getData();
			double total;
			if (event.index == 2 && (total = node.model.root.count) > 0) { // index == 2 => percentage column
				// Draw siblings
				int siblingsStart = 0;
				int siblingsWidth = (int) Math.round(event.width * node.count / total);
				event.gc.setBackground(SIBLINGS_COUNT_COLOR);
				event.gc.fillRectangle(event.x + siblingsStart, event.y, siblingsWidth, event.height);
				// Draw group
				double fraction = node.count / total;
				event.gc.setBackground(COUNT_COLOR);
				int startPixel = 0;
				int widthPixel = (int) Math.round(event.width * fraction);
				event.gc.fillRectangle(event.x + startPixel, event.y, Math.max(widthPixel, 1), event.height);
				event.detail &= ~SWT.BACKGROUND;
			}
		}
	};

	public static class MethodProfilingPageFactory implements IDataPageFactory {
		@Override
		public String getName(IState state) {
			return Messages.MethodProfilingPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_METHOD);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.METHOD_PROFILING};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new MethodProfilingPage(dpd, items, editor);
		}

	}

	private static final IItemFilter TABLE_ITEMS = ItemFilters.type(JdkTypeIDs.EXECUTION_SAMPLE);
	private static final ItemHistogramBuilder HOT_METHODS_HISTOGRAM = new ItemHistogramBuilder();

	static {
		HOT_METHODS_HISTOGRAM.addCountColumn();
		HOT_METHODS_HISTOGRAM.addPercentageColumn(PERCENTAGE_COL_ID, Aggregators.count(), "Percentage",
				"Sample percentage over total");
	}

	private class MethodProfilingUi implements IPageUI {
		private static final String METHOD_TABLE = "methodTable"; //$NON-NLS-1$
		private static final String SASH_ELEMENT = "sash"; //$NON-NLS-1$
		private static final String TABLE_ELEMENT = "table"; //$NON-NLS-1$
		private static final String METHOD_FORMAT_KEY = "metodFormat"; //$NON-NLS-1$

		private final ItemHistogram table;
		private final TreeViewer successorTree;
		private final TreeViewer predecessorTree;
		private final SashForm sash;
		private FilterComponent tableFilter;
		private FlavorSelector flavorSelector;
		private MethodFormatter methodFormatter;
		private int[] columnWidths = {650, 80, 120};

		MethodProfilingUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());
			sash = new SashForm(form.getBody(), SWT.VERTICAL);
			toolkit.adapt(sash);

			table = HOT_METHODS_HISTOGRAM.buildWithoutBorder(sash, JdkAttributes.STACK_TRACE_TOP_METHOD,
					getTableSettings(state.getChild(TABLE_ELEMENT)));
			MCContextMenuManager mm = MCContextMenuManager.create(table.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(table.getManager(), mm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), table,
					Messages.FileIOPage_HISTOGRAM_SELECTION, mm);
			table.getManager().getViewer().addSelectionChangedListener(e -> updateDetails(e));
			table.getManager().getViewer()
					.addSelectionChangedListener(e -> pageContainer.showSelection(table.getSelection().getItems()));
			tableFilter = FilterComponent.createFilterComponent(table, MethodProfilingPage.this.tableFilter,
					getDataSource().getItems().apply(TABLE_ITEMS), pageContainer.getSelectionStore()::getSelections,
					this::onTableFilterChange);
			mm.add(tableFilter.getShowFilterAction());
			mm.add(tableFilter.getShowSearchAction());

			tableFilter.loadState(state.getChild(METHOD_TABLE));
			methodFormatter = new MethodFormatter(state.getChild(METHOD_FORMAT_KEY), this::refreshTrees);

			CTabFolder tabFolder = new CTabFolder(sash, SWT.NONE);
			toolkit.adapt(tabFolder);
			CTabItem t1 = new CTabItem(tabFolder, SWT.NONE);
			t1.setToolTipText(Messages.MethodProfilingPage_PREDECESSORS_DESCRIPTION);
			predecessorTree = buildTree(tabFolder, new StacktraceReducedTreeContentProvider());
			t1.setText(Messages.PAGES_PREDECESSORS);
			t1.setControl(predecessorTree.getControl());
			predecessorTree.getControl().addListener(SWT.EraseItem, PERCENTAGE_BACKGROUND_DRAWER);
			buildColumn(predecessorTree, Messages.STACKTRACE_VIEW_STACK_TRACE, SWT.NONE, columnWidths[0])
					.setLabelProvider(new StackTraceLabelProvider(methodFormatter));
			buildColumn(predecessorTree, Messages.STACKTRACE_VIEW_COUNT_COLUMN_NAME, SWT.RIGHT, columnWidths[1])
					.setLabelProvider(new CountLabelProvider());
			buildColumn(predecessorTree, Messages.STACKTRACE_VIEW_PERCENTAGE_COLUMN_NAME, SWT.RIGHT, columnWidths[2])
					.setLabelProvider(new PercentageLabelProvider());
			MCContextMenuManager predTreeMenu = MCContextMenuManager.create(predecessorTree.getControl());
			predTreeMenu.appendToGroup(MCContextMenuManager.GROUP_VIEWER_SETUP, methodFormatter.createMenu());

			CTabItem t2 = new CTabItem(tabFolder, SWT.NONE);
			t2.setToolTipText(Messages.MethodProfilingPage_SUCCESSORS_DESCRIPTION);
			successorTree = buildTree(tabFolder, new StacktraceTreeContentProvider());
			t2.setText(Messages.PAGES_SUCCESSORS);
			t2.setControl(successorTree.getControl());
			successorTree.getControl().addListener(SWT.EraseItem, SUCCESSOR_PERCENTAGE_BACKGROUND_DRAWER);
			successorTree.getControl().addDisposeListener(e -> columnWidths = getColumnWidths(successorTree));
			buildColumn(successorTree, Messages.STACKTRACE_VIEW_STACK_TRACE, SWT.NONE, columnWidths[0])
					.setLabelProvider(new StackTraceTreeLabelProvider(methodFormatter));
			buildColumn(successorTree, Messages.STACKTRACE_VIEW_COUNT_COLUMN_NAME, SWT.RIGHT, columnWidths[1])
					.setLabelProvider(new CountTreeLabelProvider());
			buildColumn(successorTree, Messages.STACKTRACE_VIEW_PERCENTAGE_COLUMN_NAME, SWT.RIGHT, columnWidths[2])
					.setLabelProvider(new PercentageTreeLabelProvider());
			MCContextMenuManager succTreeMenu = MCContextMenuManager.create(successorTree.getControl());
			succTreeMenu.appendToGroup(MCContextMenuManager.GROUP_VIEWER_SETUP, methodFormatter.createMenu());

			tabFolder.setSelection(t1);

			PersistableSashForm.loadState(sash, state.getChild(SASH_ELEMENT));

			flavorSelector = FlavorSelector.itemsWithTimerange(form, TABLE_ITEMS, getDataSource().getItems(),
					pageContainer, this::onInputSelected, flavorSelectorState);

			table.getManager().setSelectionState(tableSelection);

			addResultActions(form);

		}

		private void refreshTrees() {
			predecessorTree.refresh();
			successorTree.refresh();
		}

		private TreeViewer buildTree(Composite parent, IContentProvider contentProvider) {
			TreeViewer treeViewer = new TreeViewer(parent,
					SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
			treeViewer.setContentProvider(contentProvider);
			treeViewer.getTree().setHeaderVisible(true);
			treeViewer.getTree().setLinesVisible(true);
			return treeViewer;
		}

		private int[] getColumnWidths(TreeViewer viewer) {
			if (!viewer.getControl().isDisposed()) {
				return Stream.of(viewer.getTree().getColumns()).mapToInt(TreeColumn::getWidth).toArray();
			}
			return columnWidths;
		}

		private ViewerColumn buildColumn(TreeViewer viewer, String text, int style, int width) {
			TreeViewerColumn vc = new TreeViewerColumn(viewer, style);
			vc.getColumn().setWidth(width);
			vc.getColumn().setText(text);
			return vc;
		}

		private void onTableFilterChange(IItemFilter filter) {
			tableFilter.filterChangeHelper(filter, table, getDataSource().getItems().apply(TABLE_ITEMS));
			MethodProfilingPage.this.tableFilter = filter;
		}

		@Override
		public void saveTo(IWritableState writableState) {
			PersistableSashForm.saveState(sash, writableState.createChild(SASH_ELEMENT));
			methodFormatter.saveState(writableState.createChild(METHOD_FORMAT_KEY));
			saveToLocal();
		}

		private void saveToLocal() {
			tableSelection = table.getManager().getSelectionState();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}

		private void updateDetails(SelectionChangedEvent event) {
			IItemCollection items = table.getSelection().getItems();
			// Release old model before building the new
			predecessorTree.setInput(null);
			successorTree.setInput(null);
			buildPredecessorTree(items);
			buildSuccessorTree(items);
		}

		private void buildPredecessorTree(IItemCollection items) {
			FrameSeparator frameSeparator = new FrameSeparator(FrameCategorization.METHOD, false);
			StacktraceModel stacktraceModel = new StacktraceModel(false, frameSeparator, items);
			CompletableFuture<StacktraceModel> modelPreparer = getModelPreparer(stacktraceModel, false);
			modelPreparer.thenAcceptAsync(this::setModelPredecessor, DisplayToolkit.inDisplayThread())
					.exceptionally(MethodProfilingPage::handleModelBuilException);
		}

		private void setModelPredecessor(StacktraceModel model) {
			if (!predecessorTree.getControl().isDisposed()) {
				predecessorTree.setInput(model.getRootFork());
			}
		}

		private void buildSuccessorTree(IItemCollection items) {
			CompletableFuture<SuccessorTreeModel> modelPreparer = getSuccessorModelPreparer(items);
			modelPreparer.thenAcceptAsync(this::setModelSuccessor, DisplayToolkit.inDisplayThread())
					.exceptionally(MethodProfilingPage::handleModelBuilException);

		}

		private void mergeNode(SuccessorTreeModel model, SuccessorNode destNode, Node srcNode) {
			destNode.count += (int) srcNode.getCumulativeWeight();
			for (Node child : srcNode.getChildren()) {
				String key = SuccessorTreeModel.makeKey(child.getFrame());
				SuccessorNode existing = destNode.children.putIfAbsent(key, new SuccessorNode(model, destNode, child));
				if (existing != null) {
					mergeNode(model, existing, child);
				}
			}
		}

		private void traverse(Node current, String typeName, String methodName, SuccessorTreeModel model) {
			if (methodName.equals(current.getFrame().getMethod().getMethodName())
					&& typeName.equals(current.getFrame().getMethod().getType().getFullName())) {
				if (model.root == null) {
					model.root = new SuccessorNode(model, null, current);
				}
				mergeNode(model, model.root, current);
			}
			for (Node child : current.getChildren()) {
				traverse(child, typeName, methodName, model);
			}
		}

		private void setModelSuccessor(SuccessorTreeModel model) {
			if (model == null) {
				return;
			}
			if (!successorTree.getControl().isDisposed()) {
				successorTree.setInput(model);
			}
		}

		private CompletableFuture<StacktraceModel> getModelPreparer(
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

		private CompletableFuture<SuccessorTreeModel> getSuccessorModelPreparer(IItemCollection items) {
			return CompletableFuture.supplyAsync(() -> {
				IItem execSample = null;
				if (items.hasItems()) {
					IItemIterable itemIterable = items.iterator().next();
					if (itemIterable.hasItems()) {
						execSample = itemIterable.iterator().next();
					}
				}
				if (execSample == null) {
					return null;
				}
				@SuppressWarnings("deprecation")
				IMemberAccessor<IMCStackTrace, IItem> accessor = ItemToolkit.accessor(JfrAttributes.EVENT_STACKTRACE);
				IMCStackTrace stackTrace = accessor.getMember(execSample);
				String methodName = null;
				String typeName = null;
				if (stackTrace != null) {
					if (!stackTrace.getFrames().isEmpty()) {
						IMCFrame topFrame = stackTrace.getFrames().get(0);
						methodName = topFrame.getMethod().getMethodName();
						typeName = topFrame.getMethod().getType().getFullName();
					}
				}
				if (methodName == null || typeName == null) {
					return null;
				}
				MethodFilter methodFilter = new JdkFilters.MethodFilter(typeName, methodName);
				// Filters event containing the current method
				IItemCollection methodEvents = getDataSource().getItems()
						.apply(ItemFilters.and(TABLE_ITEMS, methodFilter));
				StacktraceTreeModel stacktraceTreeModel = new StacktraceTreeModel(methodEvents);
				SuccessorTreeModel model = new SuccessorTreeModel();
				traverse(stacktraceTreeModel.getRoot(), typeName, methodName, model);
				return model;
			});
		}

		// See JMC-6787
		@SuppressWarnings("deprecation")
		private Branch getLastSelectedBranch(Fork fromFork) {
			Branch lastSelectedBranch = null;
			Branch branch = fromFork.getSelectedBranch();
			while (branch != null) {
				lastSelectedBranch = branch;
				branch = branch.getEndFork().getSelectedBranch();
			}
			return lastSelectedBranch;
		}

		private void onInputSelected(IItemCollection items, IRange<IQuantity> timeRange) {
		}
	}

	private static TableSettings getTableSettings(IState state) {
		if (state == null) {
			return new TableSettings(ItemHistogram.COUNT_COL_ID,
					Arrays.asList(new ColumnSettings(ItemHistogram.KEY_COL_ID, false, 500, null),
							new ColumnSettings(ItemHistogram.COUNT_COL_ID, false, 120, false),
							new ColumnSettings(PERCENTAGE_COL_ID, false, 120, false)));
		} else {
			return new TableSettings(state);
		}
	}

	private static Void handleModelBuilException(Throwable ex) {
		FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE, "Failed to build stacktrace view model", ex); //$NON-NLS-1$
		return null;
	}

	private static boolean isFirstInBranchWithSiblings(StacktraceFrame frame) {
		return frame.getBranch().getFirstFrame() == frame && frame.getBranch().getParentFork().getBranchCount() > 1;
	}

	private static boolean isLastFrame(StacktraceFrame frame) {
		return frame.getBranch().getLastFrame() == frame && frame.getBranch().getEndFork().getBranchCount() == 0;
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		return new MethodProfilingUi(parent, toolkit, pageContainer, state);
	}

	private IItemFilter tableFilter = null;
	private SelectionState tableSelection;
	public FlavorSelectorState flavorSelectorState;

	public MethodProfilingPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return TABLE_ITEMS;
	}

	private static Fork getRootFork(Fork fork) {
		while (fork.getParentBranch() != null) {
			fork = fork.getParentBranch().getParentFork();
		}
		return fork;
	}

	private static class StackTraceLabelProvider extends ColumnLabelProvider {
		FrameSeparator frameSeparator;
		MethodFormatter methodFormatter;

		public StackTraceLabelProvider(MethodFormatter methodFormatter) {
			frameSeparator = new FrameSeparator(FrameCategorization.METHOD, false);
			this.methodFormatter = methodFormatter;
		}

		@Override
		public String getText(Object element) {
			IMCFrame frame = ((StacktraceFrame) element).getFrame();
			return getText(frame, frameSeparator);
		}

		protected String getText(IMCFrame frame, FrameSeparator frameSeparator) {
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
			if (isFirstInBranch) {
				return plugin.getImage(ImageConstants.ICON_ARROW_CURVED_UP);
			} else if (isLastFrame(frame)) {
				return plugin.getImage(ImageConstants.ICON_ARROW_UP_END);
			} else {
				return plugin.getImage(ImageConstants.ICON_ARROW_UP);
			}
		}

		@Override
		public Color getBackground(Object element) {
			int parentCount = 0;
			Branch e = ((StacktraceFrame) element).getBranch();
			while (e != null) {
				e = e.getParentFork().getParentBranch();
				parentCount++;
			}
			return parentCount % 2 == 0 ? null : ALTERNATE_COLOR;
		}
	}

	private static class StackTraceTreeLabelProvider extends StackTraceLabelProvider {

		public StackTraceTreeLabelProvider(MethodFormatter methodFormatter) {
			super(methodFormatter);
		}

		@Override
		public String getText(Object element) {
			IMCFrame frame = ((SuccessorNode) element).frame;
			return getText(frame, frameSeparator);
		}

		@Override
		public Image getImage(Object element) {
			FlightRecorderUI plugin = FlightRecorderUI.getDefault();
			SuccessorNode node = (SuccessorNode) element;
			if (isFirstInBranchWithSiblings(node)) {
				return plugin.getImage(ImageConstants.ICON_ARROW_CURVED_UP);
			} else if (isLastFrame(node)) {
				return plugin.getImage(ImageConstants.ICON_ARROW_UP_END);
			} else {
				return plugin.getImage(ImageConstants.ICON_ARROW_UP);
			}
		}

		private boolean isFirstInBranchWithSiblings(SuccessorNode node) {
			SuccessorNode parent = node.parent;
			if (parent == null) {
				return false;
			}
			if (node.children.isEmpty()) {
				return false;
			}
			return true;
		}

		private boolean isLastFrame(SuccessorNode node) {
			SuccessorNode parent = node.parent;
			if (parent == null) {
				return false;
			}
			SuccessorNode[] children = parent.children.values().toArray(new SuccessorNode[0]);
			if (children.length == 0) {
				return false;
			}
			return children[children.length - 1] == node;
		}

		@Override
		public Color getBackground(Object element) {
			int parentCount = 0;
			SuccessorNode current = ((SuccessorNode) element).parent;
			while (current != null) {
				current = current.parent;
				parentCount++;
			}
			return parentCount % 2 == 0 ? null : ALTERNATE_COLOR;
		}
	}

	private static class CountLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText(Object element) {
			return Integer.toString(((StacktraceFrame) element).getItemCount());
		}
	}

	private static class CountTreeLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText(Object element) {
			return Integer.toString((int) ((SuccessorNode) element).count);
		}
	}

	private static class PercentageLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText(Object element) {
			StacktraceFrame frame = (StacktraceFrame) element;
			int itemCount = frame.getItemCount();
			int totalCount = getRootFork(frame.getBranch().getParentFork()).getItemsInFork();
			return UnitLookup.PERCENT_UNITY.quantity(itemCount / (double) totalCount).displayUsing(IDisplayable.AUTO);
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
			sb.append("<li style='image' value='" + COUNT_IMG_KEY + "'><span nowrap='true'>"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(Messages.stackTraceMessage(itemCount, totalCount, frameFraction));
			sb.append("</span></li>"); //$NON-NLS-1$
			sb.append("<li style='image' value='" + SIBLINGS_IMG_KEY + "'><span nowrap='true'>"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(Messages.siblingMessage(itemsInSiblings, parentFork.getBranchCount() - 1));
			sb.append("</span></li>"); //$NON-NLS-1$
			sb.append("</form>"); //$NON-NLS-1$
			return sb.toString();
		}
	}

	private static class PercentageTreeLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText(Object element) {
			SuccessorNode node = (SuccessorNode) element;
			int itemCount = node.count;
			int totalCount = node.model.root.count;
			return UnitLookup.PERCENT_UNITY.quantity(itemCount / (double) totalCount).displayUsing(IDisplayable.AUTO);
		}

		@Override
		public String getToolTipText(Object element) {
			SuccessorNode node = (SuccessorNode) element;
			int itemCount = node.count;
			int totalCount = node.model.root.count;
			String frameFraction = UnitLookup.PERCENT_UNITY.quantity(itemCount / (double) totalCount)
					.displayUsing(IDisplayable.AUTO);
			StringBuilder sb = new StringBuilder("<form>"); //$NON-NLS-1$
			sb.append("<li style='image' value='" + COUNT_IMG_KEY + "'><span nowrap='true'>"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(Messages.stackTraceMessage(itemCount, totalCount, frameFraction));
			sb.append("</span></li>"); //$NON-NLS-1$
			sb.append("</form>"); //$NON-NLS-1$
			return sb.toString();
		}
	}

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

	private static class StacktraceTreeContentProvider extends AbstractStructuredContentProvider
			implements ITreeContentProvider {

		@Override
		public SuccessorNode[] getElements(Object inputElement) {
			if (inputElement instanceof SuccessorTreeModel) {
				return new SuccessorNode[] {((SuccessorTreeModel) inputElement).root};
			}
			return new SuccessorNode[] {(SuccessorNode) inputElement};
		}

		@Override
		public SuccessorNode[] getChildren(Object inputElement) {
			return ((SuccessorNode) inputElement).children.values().toArray(new SuccessorNode[0]);
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return !((SuccessorNode) element).children.isEmpty();
		}
	}

	private static class SuccessorTreeModel {
		SuccessorNode root;

		public static String makeKey(IMCFrame frame) {
			return frame.getMethod().getType().getFullName() + "::" + frame.getMethod().getMethodName();
		}
	}

	private static class SuccessorNode {
		final SuccessorTreeModel model;
		final SuccessorNode parent;
		final AggregatableFrame frame;
		final Map<String, SuccessorNode> children = new HashMap<>();
		int count;

		SuccessorNode(SuccessorTreeModel model, SuccessorNode parent, Node node) {
			this.model = model;
			this.parent = parent;
			this.frame = node.getFrame();
			this.count = (int) node.getCumulativeWeight();
		}
	}
}
