/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.ui.common;

import static org.openjdk.jmc.ui.charts.QuantitySpanRenderer.MISSING_END;
import static org.openjdk.jmc.ui.charts.QuantitySpanRenderer.MISSING_START;

import java.awt.Color;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.ui.EventTypeFolderNode;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.LaneEditor.LaneDefinition;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.charts.IQuantitySeries;
import org.openjdk.jmc.ui.charts.ISpanSeries;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.QuantitySeries;
import org.openjdk.jmc.ui.charts.QuantitySpanRenderer;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.charts.SpanRenderer;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;

public class ThreadGraphLanes {

	private static final String EDIT_LANES = "editLanes"; //$NON-NLS-1$
	private static final Color THREAD_BG_COLOR = new Color(
			Color.HSBtoRGB(Color.RGBtoHSB(200, 255, 200, null)[0], 0.6f, 0.5f));
	private static final String THREAD_LANE = "threadLane"; //$NON-NLS-1$

	private List<LaneDefinition> laneDefs;
	private List<LaneDefinition> naLanes;
	private Supplier<StreamModel> dataSourceSupplier;
	private Runnable buildChart;
	private List<IAction> actions;
	private String tooltipTitle;
	private EventTypeFolderNode typeTree;

	public ThreadGraphLanes(Supplier<StreamModel> dataSourceSupplier, Runnable buildChart) {
		this.dataSourceSupplier = dataSourceSupplier;
		this.buildChart = buildChart;
		this.actions = new ArrayList<>();
		this.typeTree = dataSourceSupplier.get()
				.getTypeTree(dataSourceSupplier.get().getItems().stream().filter(this::typeWithThreadAndDuration));
	}

	protected EventTypeFolderNode getTypeTree() {
		return typeTree;
	}

	public void openEditLanesDialog(MCContextMenuManager mm, boolean isLegendMenu) {
		// FIXME: Might there be other interesting events that don't really have duration?
		typeTree = dataSourceSupplier.get()
				.getTypeTree(dataSourceSupplier.get().getItems().stream().filter(this::typeWithThreadAndDuration));
		laneDefs = LaneEditor.openDialog(typeTree, laneDefs.stream().collect(Collectors.toList()),
				Messages.JavaApplicationPage_EDIT_THREAD_LANES_DIALOG_TITLE,
				Messages.JavaApplicationPage_EDIT_THREAD_LANES_DIALOG_MESSAGE);
		updateContextMenu(mm, isLegendMenu);
		buildChart.run();
	}

	public void openEditLanesDialog(MCContextMenuManager[] mms, boolean isLegendMenu) {
		// FIXME: Might there be other interesting events that don't really have duration?
		typeTree = dataSourceSupplier.get()
				.getTypeTree(dataSourceSupplier.get().getItems().stream().filter(this::typeWithThreadAndDuration));
		laneDefs = LaneEditor.openDialog(typeTree, laneDefs.stream().collect(Collectors.toList()),
				Messages.JavaApplicationPage_EDIT_THREAD_LANES_DIALOG_TITLE,
				Messages.JavaApplicationPage_EDIT_THREAD_LANES_DIALOG_MESSAGE);
		updateContextMenus(mms, isLegendMenu);
		buildChart.run();
	}

	public List<LaneDefinition> getLaneDefinitions() {
		return laneDefs;
	}

	private Boolean typeWithThreadAndDuration(IItemIterable itemStream) {
		return DataPageToolkit.isTypeWithThreadAndDuration(itemStream.getType());
	}

	public IItemFilter getEnabledLanesFilter() {
		List<IItemFilter> laneFilters = laneDefs.stream()
				.filter((Predicate<? super LaneDefinition>) LaneDefinition::isEnabled).map(ld -> ld.getFilter())
				.collect(Collectors.toList());
		return ItemFilters.or(laneFilters.toArray(new IItemFilter[laneFilters.size()]));
	}

	private void setTooltipTitle(String description) {
		this.tooltipTitle = description;
	}

	private String getTooltipTitle() {
		return this.tooltipTitle;
	}

	private void resetTooltipTitle() {
		this.tooltipTitle = null;
	}

	public void buildChart() {
		buildChart.run();
	}

	private void setLaneDefinitionEnablement(LaneDefinition oldLd, int laneIndex, boolean isEnabled) {
		LaneDefinition newLd = new LaneDefinition(oldLd.getName(), isEnabled, oldLd.getFilter(), oldLd.isRestLane());
		laneDefs.set(laneIndex, newLd);
	}

	public IXDataRenderer buildThreadRenderer(Object thread, IItemCollection items) {
		this.resetTooltipTitle();
		String threadName = thread == null ? "" : ((IMCThread) thread).getThreadName(); //$NON-NLS-1$
		// FIXME: Workaround since this method can be called from super class constructor. Refactor to avoid this.
		List<LaneDefinition> laneFilters = this.laneDefs == null ? Collections.emptyList() : this.laneDefs;
		List<IXDataRenderer> lanes = new ArrayList<>(laneFilters.size());
		laneFilters.stream().filter(ld -> ld.isEnabled()).forEach(lane -> {
			IItemCollection laneItems = items.apply(lane.getFilter());
			if (laneItems.iterator().hasNext()) {
				ISpanSeries<IItem> laneSeries = QuantitySeries.max(laneItems, JfrAttributes.START_TIME,
						JfrAttributes.END_TIME);
				this.setTooltipTitle(
						MessageFormat.format(Messages.ThreadsPage_LANE_TOOLTIP_TITLE, threadName, lane.getName()));
				lanes.add(new ItemRow(
						SpanRenderer.withBoundaries(laneSeries, DataPageToolkit.ITEM_COLOR, this.getTooltipTitle()),
						laneItems));
			}
		});
		IXDataRenderer renderer = !lanes.isEmpty() ? RendererToolkit.uniformRows(lanes)
				: new ItemRow(RendererToolkit.empty(), ItemCollectionToolkit.EMPTY);
		IItemCollection itemsAndThreadLifespan = addThreadLifeSpanEvents(thread, items);
		// If the lane doesn't match a filter, display the Thread name as the tooltip title
		if (this.getTooltipTitle() == null) {
			this.setTooltipTitle(threadName);
		} else {
			this.resetTooltipTitle();
		}
		return new QuantitySpanRenderer(threadRanges(threadName, itemsAndThreadLifespan), renderer, THREAD_BG_COLOR, 10,
				threadName, this.getTooltipTitle(), thread);
	}

	private IItemCollection addThreadLifeSpanEvents(Object thread, final IItemCollection items) {
		IItemCollection threadLifeSpan = dataSourceSupplier.get().getItems()
				.apply(ItemFilters.and(ItemFilters.equals(JfrAttributes.EVENT_THREAD, (IMCThread) thread),
						ItemFilters.type(JdkTypeIDs.JAVA_THREAD_START, JdkTypeIDs.JAVA_THREAD_END)));
		IItemCollection itemsAndThreadLifespan = ItemCollectionToolkit.merge(() -> Stream.of(items, threadLifeSpan));
		return itemsAndThreadLifespan;
	}

	private IQuantitySeries<?> threadRanges(String threadName, IItemCollection items) {
		IItemCollection startEvents = items.apply(ItemFilters.type(JdkTypeIDs.JAVA_THREAD_START));
		IItemCollection endEvents = items.apply(ItemFilters.type(JdkTypeIDs.JAVA_THREAD_END));
		Iterator<IQuantity> start = startEvents.values(JfrAttributes.START_TIME).get().sorted().iterator();
		Iterator<IQuantity> end = endEvents.values(JfrAttributes.END_TIME).get().sorted().iterator();

		ArrayList<IQuantity> startList = new ArrayList<>();
		ArrayList<IQuantity> endList = new ArrayList<>();
		IQuantity sq = start.hasNext() ? start.next() : MISSING_START;
		IQuantity eq = end.hasNext() ? end.next() : MISSING_END;
		if (sq.compareTo(eq) >= 0) {
			startList.add(MISSING_START);
			endList.add(eq);
			eq = end.hasNext() ? end.next() : MISSING_END;
		}

		while (start.hasNext()) {
			startList.add(sq);
			endList.add(eq);
			sq = start.hasNext() ? start.next() : MISSING_START;
			eq = end.hasNext() ? end.next() : MISSING_END;
		}
		startList.add(sq);
		endList.add(eq);

		String text = NLS.bind(Messages.JavaApplicationPage_THREAD_LIFESPAN, threadName);
		return QuantitySeries.all(startList, endList, new IDisplayable() {

			@Override
			public String displayUsing(String formatHint) {
				return text;
			}

		});
	}

	public void saveTo(IWritableState writableState) {
		laneDefs.stream().forEach(f -> f.saveTo(writableState.createChild(THREAD_LANE)));
		// FIXME: This will change the order from the original lane order, putting all the non applicable lanes last, can we live with that?
		naLanes.stream().forEach(f -> f.saveTo(writableState.createChild(THREAD_LANE)));
	}

	public List<IAction> initializeChartConfiguration(Stream<IState> laneStates) {
		laneDefs = new ArrayList<>();
		laneStates.map(LaneDefinition::readFrom).forEach(laneDefs::add);
		if (laneDefs.isEmpty()) {
			laneDefs.add(new LaneDefinition(Messages.JavaApplicationPage_THREAD_LANE_JAVA_LATENCIES, true,
					JdkFilters.THREAD_LATENCIES, false));
		}
		// FIXME: Might be nice to make some sort of model for the whole lane set
		LaneEditor.ensureRestLane(laneDefs);
		Map<Boolean, List<LaneDefinition>> lanesByApplicability = laneDefs.stream()
				.collect(Collectors.partitioningBy(ld -> ld.isRestLane()
						|| dataSourceSupplier.get().getItems().apply(ld.getFilter()).iterator().hasNext()));
		laneDefs = lanesByApplicability.get(true);
		naLanes = lanesByApplicability.get(false);
		return Collections.emptyList();
	}

	//create two action identifiers to handle the chart context menu and the legend context menu
	private List<String> chartActionIdentifiers = new ArrayList<>();
	private List<String> legendActionIdentifiers = new ArrayList<>();

	public void updateContextMenus(MCContextMenuManager[] mms, boolean isLegendMenu) {
		if (isLegendMenu) {
			for (String id : legendActionIdentifiers) {
				for (MCContextMenuManager mm : mms) {
					mm.remove(id);
				}
			}
			legendActionIdentifiers.clear();
		} else {
			for (String id : chartActionIdentifiers) {
				for (MCContextMenuManager mm : mms) {
					mm.remove(id);
				}
			}
			chartActionIdentifiers.clear();
		}
		if (mms[0].indexOf(EDIT_LANES) == -1) {
			IAction action = ActionToolkit.action(() -> this.openEditLanesDialog(mms, isLegendMenu),
					Messages.JavaApplicationPage_EDIT_THREAD_LANES_ACTION,
					FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_LANES_EDIT));
			action.setId(EDIT_LANES);
			for (MCContextMenuManager mm : mms) {
				mm.add(action);
				mm.add(new Separator());
			}
			actions.add(action);
		}
		laneDefs.stream().forEach(ld -> {
			Action checkAction = new Action(ld.getName(), IAction.AS_CHECK_BOX) {
				int laneIndex = laneDefs.indexOf(ld);

				@Override
				public void run() {
					setLaneDefinitionEnablement(ld, laneIndex, isChecked());
					buildChart.run();
				}
			};
			String identifier = ld.getName() + checkAction.hashCode();
			checkAction.setId(identifier);
			if (isLegendMenu) {
				legendActionIdentifiers.add(identifier);
			} else {
				chartActionIdentifiers.add(identifier);
			}
			checkAction.setChecked(ld.isEnabled());
			// FIXME: Add a tooltip here
			for (MCContextMenuManager mm : mms) {
				mm.add(checkAction);
			}
			actions.add(checkAction);
		});
	}

	public void updateContextMenu(MCContextMenuManager mm, boolean isLegendMenu) {
		if (isLegendMenu) {
			for (String id : legendActionIdentifiers) {
				mm.remove(id);
			}
			legendActionIdentifiers.clear();
		} else {
			for (String id : chartActionIdentifiers) {
				mm.remove(id);
			}
			chartActionIdentifiers.clear();
		}
		if (mm.indexOf(EDIT_LANES) == -1) {
			IAction action = ActionToolkit.action(() -> this.openEditLanesDialog(mm, isLegendMenu),
					Messages.JavaApplicationPage_EDIT_THREAD_LANES_ACTION,
					FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_LANES_EDIT));
			action.setId(EDIT_LANES);
			mm.add(action);
			actions.add(action);
			mm.add(new Separator());
		}
		laneDefs.stream().forEach(ld -> {
			Action checkAction = new Action(ld.getName(), IAction.AS_CHECK_BOX) {
				int laneIndex = laneDefs.indexOf(ld);

				@Override
				public void run() {
					setLaneDefinitionEnablement(ld, laneIndex, isChecked());
					buildChart.run();
				}
			};
			String identifier = ld.getName() + checkAction.hashCode();
			checkAction.setId(identifier);
			if (isLegendMenu) {
				legendActionIdentifiers.add(identifier);
			} else {
				chartActionIdentifiers.add(identifier);
			}
			checkAction.setChecked(ld.isEnabled());
			// FIXME: Add a tooltip here
			mm.add(checkAction);
			actions.add(checkAction);
		});
	}

	public List<IAction> getContextMenuActions() {
		return actions;
	}

}
