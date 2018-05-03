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
package org.openjdk.jmc.flightrecorder.ui.pages;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.memleak.ReferenceTreeModel;
import org.openjdk.jmc.flightrecorder.memleak.ReferenceTreeObject;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IDataPageFactory;
import org.openjdk.jmc.flightrecorder.ui.IDisplayablePage;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FilterComponent;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemRow;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.AbstractStructuredContentProvider;
import org.openjdk.jmc.ui.misc.BackgroundFractionDrawer;
import org.openjdk.jmc.ui.misc.ChartCanvas;
import org.openjdk.jmc.ui.misc.PersistableSashForm;
import org.openjdk.jmc.ui.misc.TypedLabelProvider;

public class MemoryLeakPage extends AbstractDataPage {

	public static class MemoryLeakPageFactory implements IDataPageFactory {

		@Override
		public String getName(IState state) {
			return Messages.MemoryLeakPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_HEAP);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.MEMORY_LEAK_TOPIC};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition definition, StreamModel items, IPageContainer editor) {
			return new MemoryLeakPage(definition, items, editor);
		}

	}

	private static class ReferenceTreeContentProvider extends AbstractStructuredContentProvider
			implements ITreeContentProvider {

		IRange<IQuantity> timeRange = null;

		private final Predicate<ReferenceTreeObject> withinTimeRangePredicate = rto -> {
			if (timeRange != null) {
				return rto.getTimestamp().compareTo(timeRange.getStart()) >= 0
						&& rto.getTimestamp().compareTo(timeRange.getEnd()) <= 0;
			}
			return true;
		};

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof ReferenceTreeObject) {
				ReferenceTreeObject object = (ReferenceTreeObject) element;
				List<ReferenceTreeObject> children = object.getChildren();
				if (timeRange != null) {
					return children.stream().anyMatch(withinTimeRangePredicate);
				}
				return !children.isEmpty();
			}
			return false;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof ReferenceTreeObject) {
				return ((ReferenceTreeObject) element).getParent();
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof Collection<?>) {
				Collection<ReferenceTreeObject> collection = (Collection<ReferenceTreeObject>) inputElement;
				if (timeRange != null) {
					return collection.stream().filter(withinTimeRangePredicate).toArray();
				}
				return collection.toArray();
			}
			return new Object[0];
		}

		@Override
		public Object[] getChildren(Object element) {
			if (element instanceof ReferenceTreeObject) {
				ReferenceTreeObject object = (ReferenceTreeObject) element;
				List<ReferenceTreeObject> children = object.getChildren();
				if (timeRange != null) {
					return children.stream().filter(withinTimeRangePredicate).toArray();
				}
				return children.toArray();
			}
			return new Object[0];
		}
	}

	private static final IItemFilter TABLE_ITEMS = ItemFilters.type(JdkTypeIDs.OLD_OBJECT_SAMPLE);

	private class MemoryLeakPageUI implements IPageUI {

		private static final String OBJECT_FORMATTING_OPTIONS = "objectFormattingOptions"; //$NON-NLS-1$
		private ReferenceTreeModel model;
		private int objectFormattingOptions = 0b0000;
		private final ToDoubleFunction<ReferenceTreeObject> getSelectedFraction = o -> {
			if (model == null) {
				return 1d;
			}
			return ((double) o.getItems().size()) / model.getLeakObjects().size();
		};
		private final IColumn OBJECT_COLUMN = new ColumnBuilder(Messages.MemoryLeakPage_OBJECT_SAMPLE_COLUMN_HEADER,
				"object", //$NON-NLS-1$
				new TypedLabelProvider<ReferenceTreeObject>(ReferenceTreeObject.class) {
					@Override
					protected String getTextTyped(ReferenceTreeObject object) {
						if (object.getReferrerSkip() > 0) {
							MessageFormat.format(Messages.MemoryLeakPage_STEPS_SKIPPED,
									object.toString(objectFormattingOptions), object.getReferrerSkip());
						}
						return object.toString(objectFormattingOptions);
					};

					@Override
					protected Color getForegroundTyped(ReferenceTreeObject object) {
						if (object.getLeakRelevance() > 0) {
							int red = Math.min((int) (object.getLeakRelevance() * 100), 255);
							return new Color(Display.getCurrent(), new RGB(red, 0, 0));
						}
						return new Color(Display.getCurrent(), new RGB(0, 0, 0));
					}
				}).build();
		private final IColumn ADDRESS_COLUMN = new ColumnBuilder(Messages.MemoryLeakPage_ADDRESS_COLUMN_HEADER,
				"address", //$NON-NLS-1$
				new TypedLabelProvider<ReferenceTreeObject>(ReferenceTreeObject.class) {
					@Override
					protected String getTextTyped(ReferenceTreeObject object) {
						return object.getAddress().displayUsing(IDisplayable.AUTO);
					}
				}).build();
		private final IColumn COUNT_COLUMN = new ColumnBuilder(Messages.MemoryLeakPage_COUNT_COLUMN_HEADER, "count", //$NON-NLS-1$
				new TypedLabelProvider<ReferenceTreeObject>(ReferenceTreeObject.class) {
					@Override
					protected String getTextTyped(ReferenceTreeObject object) {
						return object == null ? "" : Integer.toString(object.getItems().size()); //$NON-NLS-1$
					};
				}).style(SWT.RIGHT).comparator((o1, o2) -> {
					if (o1 instanceof ReferenceTreeObject && o2 instanceof ReferenceTreeObject) {
						return ((ReferenceTreeObject) o1).getObjectsKeptAliveCount()
								- ((ReferenceTreeObject) o2).getObjectsKeptAliveCount();
					}
					return -1;
				}).columnDrawer(BackgroundFractionDrawer.<ReferenceTreeObject> unchecked(getSelectedFraction)).build();
		private final IColumn RELEVANCE_COLUMN = new ColumnBuilder(Messages.MemoryLeakPage_RELEVANCE_COLUMN_HEADER,
				"relevance", //$NON-NLS-1$
				new TypedLabelProvider<ReferenceTreeObject>(ReferenceTreeObject.class) {
					@Override
					protected String getTextTyped(ReferenceTreeObject object) {
						return Double.toString(object.getLeakRelevance());
					}
				}).build();
		private final IColumn DESCRIPTION_COLUMN = new ColumnBuilder(Messages.MemoryLeakPage_DESCRIPTION_COLUMN_HEADER,
				"description", //$NON-NLS-1$
				new TypedLabelProvider<ReferenceTreeObject>(ReferenceTreeObject.class) {
					@Override
					protected String getTextTyped(ReferenceTreeObject object) {
						if (object == null) {
							return ""; //$NON-NLS-1$
						}
						if (object.getParent() == null) {
							return object.getRootDescription();
						}
						return object.getDescription();
					};
				}).build();

		private static final String MAIN_SASH = "mainSash"; //$NON-NLS-1$
		private static final String REFERENCE_TREE = "referenceTree"; //$NON-NLS-1$

		private Form form;
		private TreeViewer aggregatedReferenceTree;

		private Composite chartContainer;
		private ChartCanvas chartCanvas;
		private XYChart chart;
		private SashForm mainSash;
		private ColumnManager referenceTree;

		public MemoryLeakPageUI(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state) {
			objectFormattingOptions = StateToolkit.readInt(state, OBJECT_FORMATTING_OPTIONS, 0);
			form = DataPageToolkit.createForm(parent, toolkit, getName(), getImageDescriptor().createImage());
			addResultActions(form);
			mainSash = new SashForm(form.getBody(), SWT.VERTICAL);
			toolkit.adapt(mainSash);

			buildChart(toolkit, editor, mainSash);

			aggregatedReferenceTree = new TreeViewer(mainSash, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
			aggregatedReferenceTree.setContentProvider(new ReferenceTreeContentProvider());
			referenceTree = ColumnManager.build(aggregatedReferenceTree,
					Arrays.asList(OBJECT_COLUMN, COUNT_COLUMN, DESCRIPTION_COLUMN, ADDRESS_COLUMN, RELEVANCE_COLUMN),
					TableSettings.forState(state.getChild(REFERENCE_TREE)));
			configureColumnManager(editor, referenceTree, null, Messages.MemoryLeakPage_OBJECT_SAMPLES_SELECTION,
					state.getChild(REFERENCE_TREE), null);
			model = ReferenceTreeModel.buildReferenceTree(getDataSource().getItems().apply(TABLE_ITEMS));
			model.getLeakCandidates(0.5d); // this doesn't really matter, since we're not saving the return value
			aggregatedReferenceTree.setInput(model.getRootObjects());
			chartCanvas.replaceRenderer(createChart());

			PersistableSashForm.loadState(mainSash, state.getChild(MAIN_SASH));
		}

		private void configureColumnManager(
			IPageContainer editor, ColumnManager manager, Supplier<IItemCollection> selectionStoreSupplier,
			String selectionName, IState state, FilterComponent filter) {
			MCContextMenuManager menuManager = MCContextMenuManager.create(manager.getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(manager, menuManager);
			if (selectionStoreSupplier != null) {
				SelectionStoreActionToolkit.addSelectionStoreActions(manager.getViewer(), editor.getSelectionStore(),
						selectionStoreSupplier, selectionName, menuManager);
			}
			manager.getViewer().addSelectionChangedListener(e -> {
				TreeSelection selection = (TreeSelection) e.getSelection();
				ReferenceTreeObject element = (ReferenceTreeObject) selection.getFirstElement();
				if (element != null) {
					editor.showSelection(ItemCollectionToolkit.build(element.getItems().stream()));
				}
			});
			addObjectFormattingOptions(menuManager);
			if (filter != null) {
				filter.loadState(state);
				menuManager.add(filter.getShowFilterAction());
				menuManager.add(filter.getShowSearchAction());
			}
		}

		private void addObjectFormattingOptions(MCContextMenuManager menuManager) {
			MenuManager displayOptions = new MenuManager(Messages.MemoryLeakPage_OBJECT_FORMATTING_OPTIONS);
			displayOptions.add(new GroupMarker(OBJECT_FORMATTING_OPTIONS));
			menuManager.appendToGroup(MCContextMenuManager.GROUP_ADDITIONS, displayOptions);
			addOption(displayOptions, ReferenceTreeObject.FORMAT_PACKAGE,
					Messages.MemoryLeakPage_OBJECT_FORMAT_PACKAGE);
			addOption(displayOptions, ReferenceTreeObject.FORMAT_FIELD, Messages.MemoryLeakPage_OBJECT_FORMAT_FIELD);
			addOption(displayOptions, ReferenceTreeObject.FORMAT_STATIC_MODIFIER,
					Messages.MemoryLeakPage_OBJECT_FORMAT_STATIC_MOD);
			addOption(displayOptions, ReferenceTreeObject.FORMAT_OTHER_MODIFIERS,
					Messages.MemoryLeakPage_OBJECT_FORMAT_OTHER_MOD);
			addOption(displayOptions, ReferenceTreeObject.FORMAT_ARRAY_INFO,
					Messages.MemoryLeakPage_OBJECT_FORMAT_ARRAY);
		}

		private void addOption(MenuManager displayOptions, int option, String text) {
			IAction formatAction = ActionToolkit.checkAction(b -> setDisplayOption(option), text, null);
			formatAction.setChecked((objectFormattingOptions & option) != 0);
			displayOptions.appendToGroup(OBJECT_FORMATTING_OPTIONS, formatAction);
		}

		private void setDisplayOption(int option) {
			objectFormattingOptions = objectFormattingOptions ^ option;
			aggregatedReferenceTree.refresh();
		}

		private void buildChart(FormToolkit toolkit, IPageContainer editor, Composite parent) {
			chartContainer = toolkit.createComposite(parent);
			chartContainer.setLayout(new GridLayout(2, false));
			chartCanvas = new ChartCanvas(chartContainer);
			chartCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			DataPageToolkit.createChartTimestampTooltip(chartCanvas);
			chart = new XYChart(editor.getRecordingRange(), RendererToolkit.empty(), 180);
			DataPageToolkit.setChart(chartCanvas, chart, JdkAttributes.ALLOCATION_TIME, i -> {
				if (aggregatedReferenceTree != null && model != null) {
					IRange<IQuantity> selectionRange = chart.getSelectionRange();
					if (selectionRange != null) {
						((ReferenceTreeContentProvider) aggregatedReferenceTree
								.getContentProvider()).timeRange = selectionRange;
						aggregatedReferenceTree.setInput(model.getRootObjects(selectionRange));
					} else {
						((ReferenceTreeContentProvider) aggregatedReferenceTree.getContentProvider()).timeRange = null;
						aggregatedReferenceTree.setInput(model.getRootObjects());
					}
				}
			});
			chart.setVisibleRange(visibleRange.getStart(), visibleRange.getEnd());
		}

		private IXDataRenderer createChart() {
			List<IXDataRenderer> rows = new ArrayList<>();
			IItemCollection items = getDataSource().getItems().apply(TABLE_ITEMS);
			rows.add(DataPageToolkit.buildTimestampHistogram(Messages.HeapPage_ROW_ALLOCATION,
					JdkAggregators.ALLOCATION_TOTAL.getDescription(), items, Aggregators.count(TABLE_ITEMS),
					JdkAttributes.ALLOCATION_TIME, DataPageToolkit.ALLOCATION_COLOR));
			IXDataRenderer root = RendererToolkit.uniformRows(rows);
			return new ItemRow(root, items);
		}

		@Override
		public void saveTo(IWritableState state) {
			referenceTree.getSettings().saveState(state.createChild(REFERENCE_TREE));
			PersistableSashForm.saveState(mainSash, state.createChild(MAIN_SASH));
			StateToolkit.writeInt(state, OBJECT_FORMATTING_OPTIONS, objectFormattingOptions);
			saveToLocal();
		}

		private void saveToLocal() {
			visibleRange = chart.getVisibleRange();
		}

	}

	private IRange<IQuantity> visibleRange;

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state) {
		return new MemoryLeakPageUI(parent, toolkit, editor, state);
	}

	public MemoryLeakPage(IPageDefinition definition, StreamModel model, IPageContainer editor) {
		super(definition, model, editor);
		visibleRange = editor.getRecordingRange();
	}

}
