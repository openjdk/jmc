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
package org.openjdk.jmc.flightrecorder.ext.g1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.ext.g1.visualizer.HeapLayout;
import org.openjdk.jmc.flightrecorder.ext.g1.visualizer.HeapLayout.CurveType;
import org.openjdk.jmc.flightrecorder.ext.g1.visualizer.HeapRegionSelectionEvent;
import org.openjdk.jmc.flightrecorder.ext.g1.visualizer.HeapRegionView;
import org.openjdk.jmc.flightrecorder.ext.g1.visualizer.region.HeapRegion;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IDataPageFactory;
import org.openjdk.jmc.flightrecorder.ui.IDisplayablePage;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.ItemHistogramBuilder;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.column.TableSettings.ColumnSettings;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

// Remove this suppress when translation is required
@SuppressWarnings("nls")
public class G1Page extends AbstractDataPage {

	public static class G1PageFactory implements IDataPageFactory {

		@Override
		public String getName(IState state) {
			return "G1 Heap Layout";
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_GC);
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition definition, StreamModel items, IPageContainer editor) {
			return new G1Page(definition, items, editor);
		}

	}

	private static final ItemHistogramBuilder BY_ID = new ItemHistogramBuilder();

	static {
		BY_ID.addColumn(JfrAttributes.START_TIME);
		BY_ID.addColumn(JdkAttributes.GC_CAUSE);
		BY_ID.addColumn(JdkAttributes.GC_SUM_OF_PAUSES);
	}

	private class G1PageUI implements IPageUI {

		private static final String CONT_HUMONGOUS_NAME = "contHumongous";
		private static final String HUMONGOUS_NAME = "humongous";
		private static final String FREE_NAME = "free";
		private static final String SURVIVOR_NAME = "survivor";
		private static final String OLD_NAME = "old";
		private static final String EDEN_NAME = "eden";
		private static final String HEAP_SASH = "heapSash";
		private static final String RESET = "Reset";
		private static final String START = "Play";
		private static final String STOP = "Stop";

		private ListIterator<HeapRegion> heapRegionUpdateIterator;
		private volatile IQuantity time;
		private volatile Boolean paused;
		private HeapLayout heapVisualizer;
		private Text timeLabel;
		private IQuantity startTime;
		private boolean finished = false;
		private IPageContainer pageContainer;
		private ColorMap g1Colors;

		private Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					if (!paused) {
						int update = update();
						if (update != Integer.MIN_VALUE) {
							// NOTE: move this into the drawing of the frame to allow proper frame dropping
							Display.getCurrent().timerExec(Math.abs(16 - update), this);
						} else {
							start.setSelection(false);
							start.setText(START);
							paused = true;
							finished = true;
							Display.getCurrent().timerExec(-1, this);
						}
					} else {
						Display.getCurrent().timerExec(-1, this);
					}
				} catch (Exception e) {
					Display.getCurrent().timerExec(-1, this);
				}
			}
		};
		private Button start;
		private HeapRegionView regionVisualizer;
		private SashForm heapSash;
		private ItemHistogram gcTable;
		private List<List<HeapRegion>> heapDumps;
		private List<HeapRegion> allRegionDeltas;

		G1PageUI(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state) {
			this.pageContainer = editor;
			loadColors(state);
			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());
			Composite heapVisualizationComposite = toolkit.createComposite(form.getBody(), SWT.NONE);
			if (!getDataSource().getItems().apply(G1Constants.ALL_REGION_EVENTS).hasItems()) {
				Text error = new Text(form.getBody(), SWT.READ_ONLY);
				error.setText("No G1 region events found");
			} else {
				heapSash = new SashForm(heapVisualizationComposite, SWT.HORIZONTAL);
				GridLayout layout = new GridLayout(1, true);
				layout.horizontalSpacing = 0;
				layout.verticalSpacing = 0;
				layout.marginHeight = 0;
				layout.marginWidth = 0;
				heapVisualizationComposite.setLayout(layout);

				Composite controlsAndLegend = toolkit.createComposite(heapVisualizationComposite, SWT.NONE);
				controlsAndLegend.setLayout(new GridLayout(2, false));
				Composite controls = toolkit.createComposite(controlsAndLegend, SWT.NONE);
				controls.setLayout(new GridLayout(2, true));
				controls.setLayoutData(
						new GridData(GridData.HORIZONTAL_ALIGN_FILL, GridData.VERTICAL_ALIGN_FILL, true, false));
				Composite legend = toolkit.createComposite(controlsAndLegend);
				addColorsToLegend(legend);
				legend.setLayout(new GridLayout(6, false));

				heapSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				heapVisualizer = new HeapLayout(g1Colors, heapSash, SWT.DOUBLE_BUFFERED | SWT.BORDER);
				heapVisualizer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				heapVisualizer.setCurveType(StateToolkit.readEnum(state.getChild("heapLayout"), "curveType",
						CurveType.LEFT_RIGHT, CurveType.class));
				heapVisualizer.setPadding(StateToolkit.readBoolean(state.getChild("heapLayout"), "padding", false));
				setUpHeapDumps();
				setUpDeltas();
				heapVisualizer.addKeyListener(new KeyListener() {
					@Override
					public void keyReleased(KeyEvent e) {
						if (e.keyCode == SWT.SPACE) {
							step();
							heapVisualizer.redraw();
						}
					}

					@Override
					public void keyPressed(KeyEvent e) {
					}
				});

				regionVisualizer = new HeapRegionView(g1Colors, heapSash, editor, SWT.DOUBLE_BUFFERED | SWT.BORDER);

				IAggregator<IQuantity, ?> firstStartAggregator = Aggregators.min(JfrAttributes.START_TIME);
				regionVisualizer.setStart(getDataSource().getItems().getAggregate(firstStartAggregator));
				IAggregator<IQuantity, ?> lastStartAggregator = Aggregators.max(JfrAttributes.START_TIME);
				regionVisualizer.setEnd(getDataSource().getItems().getAggregate(lastStartAggregator));

				heapVisualizer.addListener(SWT.Selection, e -> {
					HeapRegionSelectionEvent event = (HeapRegionSelectionEvent) e;
					IItemCollection regionStates = event.regionIndexes.size() == 0
							? getDataSource().getItems().apply(G1Constants.ALL_REGION_EVENTS)
							: getDataSource().getItems()
									.apply(ItemFilters.memberOf(G1Constants.REGION_INDEX, event.regionIndexes));
					pageContainer.showSelection(regionStates);
					regionVisualizer.show(regionStates);
				});

				gcTable = BY_ID.build(heapSash, JdkAttributes.GC_ID, getTableSettings(state.getChild("gcTable")));
				IItemCollection gcPauses = getDataSource().getItems().apply(JdkFilters.GARBAGE_COLLECTION);
				gcTable.show(gcPauses);
				regionVisualizer
						.showGC(getDataSource().getItems().apply(ItemFilters.hasAttribute(JdkAttributes.GC_ID)));
				if (heapDumps != null) {
					gcTable.getManager().getViewer().addSelectionChangedListener(e -> {
						IQuantity newTime = gcTable.getSelection().getItems()
								.getAggregate(JdkAggregators.FIRST_ITEM_START);
						if (newTime == null) {
							regionVisualizer.showGC(
									getDataSource().getItems().apply(ItemFilters.hasAttribute(JdkAttributes.GC_ID)));
							time = getDataSource().getItems().apply(G1Constants.HEAP_REGION_DUMPS)
									.getAggregate(JdkAggregators.FIRST_ITEM_START);
						} else {
							IAggregator<Set<IQuantity>, ?> distinct = Aggregators.distinct(JdkAttributes.GC_ID);
							Set<IQuantity> gcIds = gcTable.getSelection().getItems().getAggregate(distinct);
							regionVisualizer.showGC(
									getDataSource().getItems().apply(ItemFilters.memberOf(JdkAttributes.GC_ID, gcIds)));
							time = newTime;
						}
						heapVisualizer.show(seekTo(time));
						timeLabel.setText(time.displayUsing(IDisplayable.VERBOSE));
						regionVisualizer.setCurrentTime(time);
						heapVisualizer.redraw();
					});
				}

				if (heapDumps != null) {
					heapVisualizer.show(heapDumps.get(0));
					regionVisualizer.show(getDataSource().getItems().apply(G1Constants.ALL_REGION_EVENTS));
				} else {
					heapVisualizer.show(null);
				}
				MCContextMenuManager mm = MCContextMenuManager.create(heapVisualizer);
				mm.add(ActionToolkit.radioAction(() -> {
					heapVisualizer.setCurveType(CurveType.HILBERT);
					heapVisualizer.redraw();
				}, "Hilbert", FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_GC)));
				mm.add(ActionToolkit.radioAction(() -> {
					heapVisualizer.setCurveType(CurveType.LEFT_RIGHT);
					heapVisualizer.redraw();
				}, "Left-to-right", FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_GC)));
				mm.add(ActionToolkit.radioAction(() -> {
					heapVisualizer.setCurveType(CurveType.ALTERNATING);
					heapVisualizer.redraw();
				}, "Alternating", FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_GC)));
				mm.add(ActionToolkit.checkAction(pad -> {
					heapVisualizer.setPadding(pad);
					heapVisualizer.fullRedraw();
				}, "Use Padding", FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_OVERVIEW)));

				if (startTime != null) {
					time = startTime;
					timeLabel = toolkit.createText(controls, startTime.displayUsing(IDisplayable.EXACT), SWT.READ_ONLY);
				}

				Composite controlButtons = toolkit.createComposite(controls);
				controlButtons.setLayout(new FillLayout());
				start = toolkit.createButton(controlButtons, START, SWT.TOGGLE);

				start.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (start.getSelection()) {
							if (finished) {
								reset();
								finished = false;
							}
							start.setText(STOP);
							Display.getCurrent().timerExec(10, runnable);
							paused = false;

						} else {
							start.setText(START);
							paused = true;
						}
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
						widgetSelected(e);
					}
				});
				Button reset = toolkit.createButton(controlButtons, RESET, SWT.PUSH);

				reset.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						synchronized (heapRegionUpdateIterator) {
							reset();
						}
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
						widgetSelected(e);
					}
				});
				PersistableSashForm.loadState(heapSash, state.getChild(HEAP_SASH));
			}
		}

		private void loadColors(IState state) {
			EDEN = readColor(state.getChild(EDEN_NAME), EDEN);
			OLD = readColor(state.getChild(OLD_NAME), OLD);
			SURVIVOR = readColor(state.getChild(SURVIVOR_NAME), SURVIVOR);
			FREE = readColor(state.getChild(FREE_NAME), FREE);
			HUMONGOUS = readColor(state.getChild(HUMONGOUS_NAME), HUMONGOUS);
			CONT_HUMONGOUS = readColor(state.getChild(CONT_HUMONGOUS_NAME), CONT_HUMONGOUS);

			Map<String, Color> colorMap = new HashMap<>();
			colorMap.put("Eden", EDEN);
			colorMap.put("Old", OLD);
			colorMap.put("Survivor", SURVIVOR);
			colorMap.put("Free", FREE);
			colorMap.put("Starts Humongous", HUMONGOUS);
			colorMap.put("Continues Humongous", CONT_HUMONGOUS);
			g1Colors = new ColorMap(colorMap);
		}

		private void addColorsToLegend(Composite legend) {
			addColorToLegend(legend, OLD, "Old", e -> {
				OLD = new Color(Display.getCurrent(), ((RGB) e.getNewValue()));
				g1Colors.updateColor("Old", OLD);
			});
			addColorToLegend(legend, EDEN, "Eden", e -> {
				EDEN = new Color(Display.getCurrent(), ((RGB) e.getNewValue()));
				g1Colors.updateColor("Eden", EDEN);
			});
			addColorToLegend(legend, SURVIVOR, "Survivor", e -> {
				SURVIVOR = new Color(Display.getCurrent(), ((RGB) e.getNewValue()));
				g1Colors.updateColor("Survivor", SURVIVOR);
			});
			addColorToLegend(legend, FREE, "Free", e -> {
				FREE = new Color(Display.getCurrent(), ((RGB) e.getNewValue()));
				g1Colors.updateColor("Free", FREE);
			});
			addColorToLegend(legend, HUMONGOUS, "Humongous", e -> {
				HUMONGOUS = new Color(Display.getCurrent(), ((RGB) e.getNewValue()));
				g1Colors.updateColor("Starts Humongous", HUMONGOUS);
			});
			addColorToLegend(legend, CONT_HUMONGOUS, "Cont. Humongous", e -> {
				CONT_HUMONGOUS = new Color(Display.getCurrent(), ((RGB) e.getNewValue()));
				g1Colors.updateColor("Continues Humongous", CONT_HUMONGOUS);
			});
		}

		private TableSettings getTableSettings(IState state) {
			if (state == null) {
				return new TableSettings(JdkAttributes.GC_ID.getIdentifier(),
						Arrays.asList(new ColumnSettings(ItemHistogram.KEY_COL_ID, false, 60, null),
								new ColumnSettings(JfrAttributes.START_TIME.getIdentifier(), false, 120, false),
								new ColumnSettings(JdkAttributes.GC_CAUSE.getIdentifier(), false, 120, false),
								new ColumnSettings(JdkAttributes.GC_SUM_OF_PAUSES.getIdentifier(), false, 120, false)));
			} else {
				return new TableSettings(state);
			}
		}

		private void addColorToLegend(Composite legend, Color color, String name, IPropertyChangeListener listener) {
			Composite part = new Composite(legend, SWT.NONE);
			part.setLayout(new GridLayout(2, false));
			Text label = new Text(part, SWT.NONE);
			label.setText(name);
			ColorSelector selector = new ColorSelector(part);
			selector.setColorValue(color.getRGB());
			selector.addListener(listener);
			selector.addListener(e -> {
				heapVisualizer.fullRedraw();
				regionVisualizer.redraw();
			});
		}

		private void reset() {
			setUpHeapDumps();
			setUpDeltas();
			heapVisualizer.show(heapDumps.get(0));
			heapRegionUpdateIterator = allRegionDeltas.listIterator(0);
			heapVisualizer.redraw();
			time = startTime;
			regionVisualizer.setCurrentTime(time);
			timeLabel.setText(time.displayUsing(IDisplayable.VERBOSE));
		}

		private List<HeapRegion> createRegionList(IItemCollection events) {
			IQuantity numEvents = events.getAggregate(Aggregators.count());
			if (numEvents == null) {
				return Collections.emptyList();
			}
			List<HeapRegion> allRegions = new ArrayList<>(numEvents.clampedIntFloorIn(UnitLookup.NUMBER_UNITY));
			for (IItemIterable itemIterable : events) {
				IType<IItem> type = itemIterable.getType();
				IMemberAccessor<IQuantity, IItem> startTimeAccessor = JfrAttributes.START_TIME.getAccessor(type);
				IMemberAccessor<IQuantity, IItem> indexAccessor = G1Constants.REGION_INDEX.getAccessor(type);
				IMemberAccessor<IQuantity, IItem> usedAccessor = G1Constants.REGION_USED.getAccessor(type);
				IMemberAccessor<String, IItem> typeAccessor = G1Constants.TYPE.getAccessor(type);

				for (IItem item : itemIterable) {
					allRegions.add(
							new HeapRegion(indexAccessor.getMember(item).clampedIntFloorIn(UnitLookup.NUMBER_UNITY),
									typeAccessor.getMember(item), startTimeAccessor.getMember(item),
									usedAccessor.getMember(item), item));
				}
			}
			return allRegions;
		}

		private void setUpHeapDumps() {
			IItemCollection heapDumpEvents = getDataSource().getItems().apply(G1Constants.HEAP_REGION_DUMPS);
			IAggregator<IQuantity, ?> maxIndexAggregator = Aggregators.max(G1Constants.REGION_INDEX);
			IQuantity maxIndex = getDataSource().getItems().getAggregate(maxIndexAggregator);
			List<HeapRegion> allRegions = createRegionList(heapDumpEvents);
			allRegions.sort((r1, r2) -> r1.getTimestamp().compareTo(r2.getTimestamp()));
			startTime = allRegions.get(0).getTimestamp();
			heapDumps = new ArrayList<>();
			int lastIndex = -1;
			List<HeapRegion> bucket = new ArrayList<>();
			for (HeapRegion region : allRegions) {
				if (region.getIndex() < lastIndex) {
					for (int i = bucket.size(); i <= maxIndex.clampedFloorIn(UnitLookup.NUMBER_UNITY); i++) {
						bucket.add(new HeapRegion(i, "Unallocated"));
					}
					heapDumps.add(bucket);
					bucket = new ArrayList<>();
				}
				bucket.add(region);
				lastIndex = region.getIndex();
			}
		}

		private void setUpDeltas() {
			IItemCollection deltas = getDataSource().getItems().apply(G1Constants.HEAP_REGION_TYPE_CHANGES);
			allRegionDeltas = createRegionList(deltas);
			allRegionDeltas.sort((r1, r2) -> r1.getTimestamp().compareTo(r2.getTimestamp()));
			heapRegionUpdateIterator = allRegionDeltas.listIterator();
		}

		private List<HeapRegion> seekTo(IQuantity seekTime) {
			setUpHeapDumps();
			HeapRegion[] startDump = new HeapRegion[1];
			if (heapDumps == null || seekTime.compareTo(startTime) < 0) {
				return null;
			}
			for (List<HeapRegion> dump : heapDumps) {
				IQuantity timestamp = dump.get(0).getTimestamp();
				if (timestamp.compareTo(seekTime) > 0) {
					break;
				}
				startDump = dump.toArray(startDump);
			}
			IQuantity firstDumpStart = startDump[0].getTimestamp();
			int finalIndex = 0;
			for (int i = 0; i < allRegionDeltas.size(); i++) {
				HeapRegion heapRegion = allRegionDeltas.get(i);
				if (heapRegion.getTimestamp().compareTo(seekTime) > 0) {
					finalIndex = i;
					break;
				}
				if (heapRegion.getTimestamp().compareTo(firstDumpStart) > 0) {
					startDump[heapRegion.getIndex()] = heapRegion;
				}
			}
			heapRegionUpdateIterator = allRegionDeltas.listIterator(finalIndex);
			return Arrays.asList(startDump);
		}

		public void step() {
			if (paused != null && !paused) {
				return;
			}
			if (heapRegionUpdateIterator.hasNext()) {
				HeapRegion next = heapRegionUpdateIterator.next();
				heapVisualizer.updateRegion(next.getIndex(), next.getType());
				time = next.getTimestamp();
				timeLabel.setText(time.displayUsing(IDisplayable.VERBOSE));
				regionVisualizer.setCurrentTime(time);
			}
		}

		public int update() {
			long start = System.currentTimeMillis();
			int waitTime = 16;
			IQuantity itemStart = null;
			synchronized (heapRegionUpdateIterator) {
				if (heapRegionUpdateIterator.hasNext()) {
					HeapRegion next = heapRegionUpdateIterator.next();
					itemStart = next.getTimestamp();
					heapRegionUpdateIterator.previous();
//					Set<Integer> previousRegionsThisTick = new HashSet<>();
					if (itemStart.subtract(time).compareTo(UnitLookup.MILLISECOND.quantity(waitTime)) <= 0) {
						while (heapRegionUpdateIterator.hasNext()) {
							next = heapRegionUpdateIterator.next();
							int index = next.getIndex();
//							if (previousRegionsThisTick.contains(index)) {
//								heapRegionUpdateIterator.previous();
//								previousRegionsThisTick.clear();
//								break;
//							}
//							previousRegionsThisTick.add(index);
							IQuantity subtract = next.getTimestamp().subtract(itemStart);
							if (subtract.compareTo(UnitLookup.MILLISECOND.quantity(waitTime)) <= 0) {
								heapVisualizer.updateRegion(index, next.getType());
							} else {
								heapRegionUpdateIterator.previous();
								break;
							}
						}
					} else {
						itemStart = time.add(UnitLookup.MILLISECOND.quantity(waitTime));
					}
//					previousRegionsThisTick.clear();
				}
			}
			heapVisualizer.redraw();
			if (itemStart != null) {
				time = itemStart;
				regionVisualizer.setCurrentTime(time);
				timeLabel.setText(time.displayUsing(IDisplayable.VERBOSE));
			} else {
				return Integer.MIN_VALUE;
			}
			return (int) (System.currentTimeMillis() - start);
		}

		@Override
		public void saveTo(IWritableState state) {
			if (heapSash != null) { // nothing was initialized
				persistColor(EDEN, state.createChild(EDEN_NAME));
				persistColor(OLD, state.createChild(OLD_NAME));
				persistColor(SURVIVOR, state.createChild(SURVIVOR_NAME));
				persistColor(FREE, state.createChild(FREE_NAME));
				persistColor(HUMONGOUS, state.createChild(HUMONGOUS_NAME));
				persistColor(CONT_HUMONGOUS, state.createChild(CONT_HUMONGOUS_NAME));
				PersistableSashForm.saveState(heapSash, state.createChild(HEAP_SASH));
				gcTable.getManager().getSettings().saveState(state.createChild("gcTable"));
				IWritableState createChild = state.createChild("heapLayout");
				StateToolkit.writeEnum(createChild, "curveType", heapVisualizer.getCurveType());
				StateToolkit.writeBoolean(createChild, "padding", heapVisualizer.isPadding());
			}
		}

		private Color readColor(IState state, Color defaultValue) {
			int r = StateToolkit.readInt(state, "red", defaultValue.getRed());
			int g = StateToolkit.readInt(state, "green", defaultValue.getGreen());
			int b = StateToolkit.readInt(state, "blue", defaultValue.getBlue());
			return new Color(Display.getCurrent(), r, g, b);
		}

		private void persistColor(Color color, IWritableState state) {
			StateToolkit.writeInt(state, "red", color.getRed());
			StateToolkit.writeInt(state, "green", color.getGreen());
			StateToolkit.writeInt(state, "blue", color.getBlue());
		}

	}

	private static Color EDEN = new Color(Display.getCurrent(), 30, 240, 30);
	private static Color SURVIVOR = new Color(Display.getCurrent(), 30, 30, 240);
	private static Color FREE = new Color(Display.getCurrent(), 170, 170, 170);
	private static Color OLD = new Color(Display.getCurrent(), 240, 240, 100);
	private static Color HUMONGOUS = new Color(Display.getCurrent(), 240, 30, 240);
	private static Color CONT_HUMONGOUS = new Color(Display.getCurrent(), 200, 10, 200);

	public G1Page(IPageDefinition definition, StreamModel model, IPageContainer editor) {
		super(definition, model, editor);
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state) {
		return new G1PageUI(parent, toolkit, editor, state);
	}

}
