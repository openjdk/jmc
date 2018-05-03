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
package org.openjdk.jmc.flightrecorder.ui.pages.itemhandler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.openjdk.jmc.common.item.IAccessorFactory;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.HistogramSelection;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.ItemHistogramBuilder;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStore;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;

class HistogramSequence {

	private static final ImageDescriptor IMAGE_DESCRIPTOR_GROUP_BY = FlightRecorderUI.getDefault()
			.getMCImageDescriptor(ImageConstants.ICON_GROUP_BY);

	private static final ImageDescriptor IMAGE_DESCRIPTOR_GROUP_BY_COMBINE = FlightRecorderUI.getDefault()
			.getMCImageDescriptor(ImageConstants.ICON_GROUP_BY_COMBINE);

	private class TablePart {
		private static final String GROUPED_ATTRIBUTE_DELIMITER = " / "; //$NON-NLS-1$
		private static final String GROUPED_ATTRIBUTE_DELIMITER_PERSISTENCE = "GROUPED_ATTRIBUTE_DELIMITER_PERSISTENCE"; //$NON-NLS-1$
		private final HistogramSettingsTree settings;
		private ItemHistogram histogram;
		private TablePart child;
		private IItemCollection items;
		private final List<IAttribute<?>> groupAttributes;
		AttributeComponentConfiguration acc;

		private TablePart(HistogramSettingsTree settings, AttributeComponentConfiguration acc) {
			this.settings = settings;
			this.acc = acc;
			groupAttributes = new ArrayList<>();
			if (settings.groupBy != null) {
				// FIXME: Should we handle the case of the grouped attribute not being a common one, if someone has manually edited the page config?
				acc.getCommonAttributes().values().stream()
						.filter(a -> Stream.of(settings.groupBy.split(GROUPED_ATTRIBUTE_DELIMITER_PERSISTENCE))
								.anyMatch(g -> g.equals(a.getIdentifier())))
						.forEach(a -> groupAttributes.add(0, a));
				if (groupAttributes.size() > 0) {
					buildHistogram();
				} else {
					// FIXME: Did not find the attributes, is that bad? Should we log?
				}
			}
		}

		private void buildHistogram() {
			assert child == null && histogram == null;
			if (groupAttributes.size() == 0) {
				return;
			} else if (groupAttributes.size() == 1) {
				IAttribute<?> attribute = groupAttributes.get(0);
				settings.groupBy = attribute.getIdentifier();
				// FIXME: Should we handle the case of the grouped attribute not being a common one, if someone has manually edited the page config?
				histogram = histogramBuilder.build(container, attribute, settings.tableSettings);
			} else {
				// FIXME: if we allow grouping by uncommon, should we be able to combine as well?
				settings.groupBy = groupAttributes.stream().map(a -> a.getIdentifier())
						.collect(Collectors.joining(GROUPED_ATTRIBUTE_DELIMITER_PERSISTENCE));
				String columnTitle = groupAttributes.stream().map(a -> a.getName())
						.collect(Collectors.joining(GROUPED_ATTRIBUTE_DELIMITER));
				histogram = histogramBuilder.build(container, columnTitle, UnitLookup.PLAIN_TEXT,
						accessorFactory(groupAttributes.toArray(new IAttribute<?>[groupAttributes.size()]),
								GROUPED_ATTRIBUTE_DELIMITER),
						settings.tableSettings);
			}
			ColumnViewer viewer = histogram.getManager().getViewer();
			viewer.getControl().moveAbove(pairControl);

			MCContextMenuManager mm = MCContextMenuManager.create(viewer.getControl());
			ColumnMenusFactory.addDefaultMenus(histogram.getManager(), mm);

			// FIXME: Maybe remove combine grouping, and use checkboxes instead?
			mm.add(AttributeMenuFactory.attributeMenu(true, this::setGroupByField,
					() -> acc.getCommonAttributes().values().stream(), Messages.TABLECOMPONENT_GROUP_BY,
					IMAGE_DESCRIPTOR_GROUP_BY));

			mm.add(AttributeMenuFactory.attributeMenu(false, this::combinedGrouping,
					() -> acc.getCommonAttributes().values().stream(), Messages.TABLECOMPONENT_COMBINE_GROUP_BY,
					IMAGE_DESCRIPTOR_GROUP_BY_COMBINE));
			SelectionStoreActionToolkit.addSelectionStoreActions(selectionStore, histogram,
					NLS.bind(Messages.TABLECOMPONENT_HISTOGRAM_SELECTION, pageName), mm);

			viewer.addSelectionChangedListener(this::selectionChanged);

			child = new TablePart(settings.getSelectedChild(), acc);
		}

		private void setGroupByField(IAttribute<?> attribute, Boolean ignore) {
			if (attribute != null && attribute.getIdentifier().equals(settings.groupBy)) {
				return;
			}
			storeSettings();
			dispose();
			settings.groupBy = null;
			groupAttributes.clear();
			if (attribute != null) {
				groupAttributes.add(attribute);
				try {
					buildHistogram();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			refreshViewer();
			onGroup();
		}

		private void combinedGrouping(IAttribute<?> attribute, Boolean ignore) {
			if (attribute == null || groupAttributes.contains(attribute)) {
				return;
			}
			storeSettings();
			dispose();
			groupAttributes.add(attribute);
			try {
				buildHistogram();
			} catch (Exception e) {
				e.printStackTrace();
			}
			refreshViewer();
			onGroup();
		}

		private void setItems(IItemCollection items) {
			this.items = items;
			refreshViewer();
		}

		private void selectionChanged(Object ignored) {
			child.setItems(getHistogramSelection());
			itemListener.accept(getHistogramSelection());
		}

		private IItemCollection getHistogramSelection() {
			HistogramSelection selection = histogram.getSelection();
			return selection.getRowCount() == 0 ? items : selection.getItems();
		}

		private void refreshViewer() {
			if (histogram != null) {
				histogram.show(items);
				child.setItems(getHistogramSelection());
			} else {
				itemListener.accept(items);
			}
			container.layout();
		}

		private void dispose() {
			if (histogram != null) {
				child.dispose();
				histogram.getManager().getViewer().getControl().dispose();
				histogram = null;
				child = null;
			}
		}

		private void storeSettings() {
			if (histogram != null) {
				settings.tableSettings = histogram.getManager().getSettings();
				child.storeSettings();
			}
		}

		private void storeSelectionState(LinkedList<SelectionState> list) {
			if (histogram != null) {
				list.addLast(histogram.getManager().getSelectionState());
				child.storeSelectionState(list);
			}
		}

		private void setSelectionStates(LinkedList<SelectionState> list) {
			if (histogram != null && list != null) {
				histogram.getManager().setSelectionState(list.poll());
				child.setSelectionStates(list);
			}
		}

		private void addHistogram(IAttribute<?> attribute, TableSettings parentSettings) {
			if (child == null) {
				if (settings.tableSettings == null) {
					settings.tableSettings = parentSettings;
				}
				groupAttributes.add(attribute);
				buildHistogram();

				refreshViewer();
			} else {
				child.addHistogram(attribute, settings.tableSettings);
			}
			container.layout();
		}

	}

	private final Composite container;
	private final ItemHistogramBuilder histogramBuilder;
	private final TablePart topPart;
	private final Consumer<Boolean> groupingListener;
	private final Control pairControl;
	private final Consumer<IItemCollection> itemListener;
	private final SelectionStore selectionStore;
	private String pageName;

	public HistogramSequence(Control pairControl, List<IContributionManager> pairMenuManagers,
			List<TriConsumer<String, ImageDescriptor, IMenuListener>> pairMenuConsumers, String pageName,
			HistogramSettingsTree settings, ItemHistogramBuilder histogramBuilder,
			Consumer<IItemCollection> itemListener, Consumer<Boolean> groupingListener,
			AttributeComponentConfiguration acc, SelectionStore selectionStore) {
		this.pageName = pageName;
		this.itemListener = itemListener;
		this.pairControl = pairControl;
		this.selectionStore = selectionStore;
		container = pairControl.getParent();
		this.histogramBuilder = histogramBuilder;
		this.groupingListener = groupingListener;
		// FIXME: Would like to merge the menu managers and the menu consumers.
		pairMenuManagers.forEach(cm -> {
			// FIXME: We can't know from here that this is the id, since it's set elsewhere.
			cm.remove(Messages.TABLECOMPONENT_GROUP_BY);
			cm.add(AttributeMenuFactory.attributeMenu(false, this::addHistogram,
					() -> acc.getCommonAttributes().values().stream(), Messages.TABLECOMPONENT_GROUP_BY,
					IMAGE_DESCRIPTOR_GROUP_BY));
		});
		pairMenuConsumers.forEach(mc -> {
			mc.accept(Messages.TABLECOMPONENT_GROUP_BY, IMAGE_DESCRIPTOR_GROUP_BY,
					AttributeMenuFactory.attributeMenuListener(false, this::addHistogram,
							() -> acc.getCommonAttributes().values().stream()));
		});

		topPart = new TablePart(settings, acc);
		onGroup();
	}

	public HistogramSettingsTree getHistogramSettings() {
		topPart.storeSettings();
		return topPart.settings;
	}

	public LinkedList<SelectionState> getSelectionStates() {
		LinkedList<SelectionState> list = new LinkedList<>();
		topPart.storeSelectionState(list);
		return list;
	}

	public void setSelectionStates(LinkedList<SelectionState> list) {
		topPart.setSelectionStates(list);
	}

	public void setItems(IItemCollection items) {
		topPart.setItems(items);
	}

	public void addHistogram(IAttribute<?> groupByAttribute, Boolean ignore) {
		topPart.storeSettings();
		topPart.addHistogram(groupByAttribute, topPart.settings.tableSettings);
		onGroup();
	}

	private void onGroup() {
		groupingListener.accept(isGrouped());
	}

	private TablePart getLowestHistogram() {
		TablePart tp = topPart;
		while (tp.child != null && tp.child.histogram != null) {
			tp = tp.child;
		}
		return tp;
	}

	public HistogramSelection getSelection() {
		TablePart tp = getLowestHistogram();
		if (tp.histogram != null) {
			HistogramSelection selection = tp.histogram.getSelection();
			return selection != null && selection.getRowCount() > 0 ? selection : null;
		}
		return null;
	}

	public HistogramSelection getAllRows() {
		TablePart tp = getLowestHistogram();
		if (tp.histogram != null) {
			return tp.histogram.getAllRows();
		}
		return null;
	}

	public boolean isGrouped() {
		return topPart != null && topPart.histogram != null;
	}

	private static IAccessorFactory<String> accessorFactory(final IAttribute<?>[] attributes, final String delimiter) {
		// FIXME: Make this into a set.
		return new IAccessorFactory<String>() {

			@Override
			public <T> IMemberAccessor<String, T> getAccessor(IType<T> type) {
				final List<IMemberAccessor<?, T>> accessors = new ArrayList<>();
				for (IAttribute<?> attribute : attributes) {
					accessors.add(attribute.getAccessor(type));
				}
				return new IMemberAccessor<String, T>() {

					@Override
					public String getMember(T inObject) {
						List<String> members = new ArrayList<>();
						for (IMemberAccessor<?, T> accessor : accessors) {
							members.add(TypeHandling.getValueString(accessor.getMember(inObject)));
						}
						return StringToolkit.join(members, delimiter);
					}

				};
			}
		};

	}
}
