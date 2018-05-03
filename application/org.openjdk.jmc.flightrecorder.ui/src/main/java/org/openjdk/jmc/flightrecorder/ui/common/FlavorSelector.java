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
package org.openjdk.jmc.flightrecorder.ui.common;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.Form;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.FlavorToolkit;
import org.openjdk.jmc.flightrecorder.ui.selection.IFlavoredSelection;
import org.openjdk.jmc.flightrecorder.ui.selection.IItemStreamFlavor;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStore;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStore.SelectionStoreEntry;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStore.SelectionStoreListener;
import org.openjdk.jmc.ui.charts.SubdividedQuantityRange;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;

/**
 * Class for creating a flavor chooser.
 */
public class FlavorSelector implements SelectionStoreListener {

	public static class FlavorSelectorState {

		private boolean showConcurrent = false;
		private boolean concurrentContained = false;
		private boolean sameThreads = true;
		private Map<IFlavoredSelection, IItemStreamFlavor[]> calculatedFlavorsMap = new HashMap<>();
		private Map<IFlavoredSelection, IItemStreamFlavor> selectedFlavorMap = new HashMap<>();

		public void clearFlavorMaps() {
			calculatedFlavorsMap.clear();
			selectedFlavorMap.clear();
		}
	}

	private final IItemFilter filter;
	private final IItemCollection items;
	private final IPageContainer pageContainer;
	private final SelectionWithThreadAndRangeConsumer onSelectWithThreads;

	private final Composite container;
	// FIXME: We can remove all references to useSelectionButton if we decide that we are not going to use it
//	private final Button useSelectionButton;
	private final ComboViewer selectionCombo;
	private final ComboViewer flavorCombo;
	private final Canvas canvas;
	private final RangePainter painter;
	private Button showButton;
	private Button resetButton;
	private Button showConcurrentButton;
	private Button rangeStyleButton;
	private Button sameThreadsButton;

	private boolean callbackActive = false;
	private List<IAttribute<?>> attributes;
	private FlavorSelectorState flavorSelectorState;

	/**
	 * Creates a flavor selector.
	 *
	 * @param form
	 *            Form to create selector in
	 * @param filter
	 *            Filter to use when choosing flavors
	 * @param items
	 *            Items to use when choosing flavors
	 * @param pageContainer
	 *            Page container used for providing selection store and time range
	 * @param onSelect
	 *            Called when a flavor is chosen. Arguments are the items from evaluating the flavor
	 *            and the calculated time range for them.
	 * @return A flavor selector
	 */
	public static FlavorSelector itemsWithTimerange(
		Form form, IItemFilter filter, IItemCollection items, IPageContainer pageContainer,
		BiConsumer<IItemCollection, IRange<IQuantity>> onSelect, Consumer<Boolean> onShow,
		FlavorSelectorState flavorSelectorState) {
		return new FlavorSelector(form, filter, null, items, pageContainer,
				(itemCollection, threadNames, range) -> onSelect.accept(itemCollection, range),
				Optional.ofNullable(onShow), flavorSelectorState);
	}

	/**
	 * Creates a flavor selector.
	 *
	 * @param form
	 *            Form to create selector in
	 * @param filter
	 *            Filter to use when choosing flavors
	 * @param items
	 *            Items to use when choosing flavors
	 * @param attributes
	 *            Attributes to use when choosing flavors
	 * @param pageContainer
	 *            Page container used for providing selection store and time range
	 * @param onSelectWithThreads
	 *            Called when a flavor is chosen. Arguments are the items from evaluating the flavor
	 *            (or null), the calculated thread names and time range
	 * @return A flavor selector
	 */
	public static FlavorSelector itemsWithTimerange(
		Form form, IItemFilter filter, List<IAttribute<?>> attributes, IItemCollection items,
		IPageContainer pageContainer, SelectionWithThreadAndRangeConsumer onSelectWithThreads,
		FlavorSelectorState flavorSelectorState) {
		return new FlavorSelector(form, filter, attributes, items, pageContainer, onSelectWithThreads,
				Optional.ofNullable(null), flavorSelectorState);
	}

	/**
	 * Creates a flavor selector.
	 *
	 * @param form
	 *            Form to create selector in
	 * @param filter
	 *            Filter to use when choosing flavors
	 * @param attributes
	 *            Attributes to use when choosing flavors
	 * @param items
	 *            Items to use when choosing flavors
	 * @param pageContainer
	 *            Page container used for providing selection store and time range
	 * @param onSelect
	 *            Called when a flavor is chosen. Arguments are the items from evaluating the flavor
	 *            (or null) and the calculated time range for them.
	 * @return A flavor selector
	 */
	public static FlavorSelector itemsWithTimerange(
		Form form, IItemFilter filter, List<IAttribute<?>> attributes, IItemCollection items,
		IPageContainer pageContainer, BiConsumer<IItemCollection, IRange<IQuantity>> onSelect,
		FlavorSelectorState flavorSelectorState) {
		return new FlavorSelector(form, filter, attributes, items, pageContainer,
				(itemCollection, threadNames, range) -> onSelect.accept(itemCollection, range),
				Optional.ofNullable(null), flavorSelectorState);
	}

	/**
	 * Creates a flavor selector.
	 *
	 * @param form
	 *            Form to create selector in
	 * @param filter
	 *            Filter to use when choosing flavors
	 * @param items
	 *            Items to use when choosing flavors
	 * @param pageContainer
	 *            Page container used for providing selection store and time range
	 * @param onSelect
	 *            Called when a flavor is chosen. Arguments are the items from evaluating the flavor
	 *            (or null) and the calculated time range for them.
	 * @return A flavor selector
	 */
	public static FlavorSelector itemsWithTimerange(
		Form form, IItemFilter filter, IItemCollection items, IPageContainer pageContainer,
		BiConsumer<IItemCollection, IRange<IQuantity>> onSelect, FlavorSelectorState flavorSelectorState) {
		return new FlavorSelector(form, filter, null, items, pageContainer,
				(itemCollection, threadNames, range) -> onSelect.accept(itemCollection, range),
				Optional.ofNullable(null), flavorSelectorState);
	}

	private FlavorSelector(Form form, IItemFilter filter, List<IAttribute<?>> attributes, IItemCollection items,
			IPageContainer pageContainer, SelectionWithThreadAndRangeConsumer onSelectWithThreads,
			Optional<Consumer<Boolean>> onShow, FlavorSelectorState flavorSelectorState) {
		this.filter = filter;
		this.attributes = attributes;
		this.items = items;
		this.pageContainer = pageContainer;
		this.onSelectWithThreads = onSelectWithThreads;
		flavorSelectorState = flavorSelectorState != null ? flavorSelectorState : new FlavorSelectorState();
		this.flavorSelectorState = flavorSelectorState;

		container = new Composite(form.getHead(), SWT.NONE);
		container.setLayout(GridLayoutFactory.fillDefaults().margins(0, 0).spacing(2, 2).create());

		Composite selectorRow = new Composite(container, SWT.NONE);
		selectorRow
				.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).create());
		selectorRow.setLayout(GridLayoutFactory.swtDefaults().numColumns(9).create());

//		useSelectionButton = new Button(selectorRow, SWT.CHECK);
//		useSelectionButton.setLayoutData(GridDataFactory.swtDefaults().create());
//		useSelectionButton.setText("Filter on:");
//		useSelectionButton.setEnabled(pageContainer.getSelectionStore().getSelections().count() > 0);
//		useSelectionButton.setSelection(pageContainer.getSelectionStore().isCurrentActive());
//		useSelectionButton.addSelectionListener(new SelectionCheckboxSelectionListener());

		selectionCombo = new ComboViewer(selectorRow);
		selectionCombo.getCombo().setLayoutData(GridDataFactory.swtDefaults().hint(200, SWT.DEFAULT)
				.minSize(100, SWT.DEFAULT).grab(false, false).create());
		selectionCombo.setContentProvider(new SelectionComboContentProvider());
		selectionCombo.setLabelProvider(new SelectionComboLabelProvider());
		selectionCombo.addSelectionChangedListener(new SelectionComboSelectionListener());
//		selectionCombo.getControl().setEnabled(pageContainer.getSelectionStore().isCurrentActive());

		Label flavorLabel = new Label(selectorRow, SWT.NONE);
		flavorLabel.setLayoutData(GridDataFactory.swtDefaults().create());
		flavorLabel.setText(Messages.FlavorSelector_LABEL_ASPECT);

		flavorCombo = new ComboViewer(selectorRow);
		flavorCombo.getCombo().setLayoutData(GridDataFactory.swtDefaults().hint(300, SWT.DEFAULT)
				.minSize(100, SWT.DEFAULT).grab(true, false).create());
		flavorCombo.setContentProvider(new FlavorComboContentProvider());
		flavorCombo.setLabelProvider(new FlavorComboLabelProvider());
		flavorCombo.addSelectionChangedListener(new FlavorComboSelectionListener());
//		flavorCombo.getControl().setEnabled(pageContainer.getSelectionStore().isCurrentActive());

		showConcurrentButton = new Button(selectorRow, SWT.CHECK);
		showConcurrentButton.setLayoutData(GridDataFactory.swtDefaults().create());
		showConcurrentButton.setText(Messages.FlavorSelector_BUTTON_SHOW_CONCURRENT);
		showConcurrentButton.setToolTipText(Messages.FlavorSelector_BUTTON_SHOW_CONCURRENT_TOOLTIP);
		showConcurrentButton.setSelection(this.flavorSelectorState.showConcurrent);
		showConcurrentButton.addSelectionListener(new ShowConcurrentSelectionListener());

		// FIXME: Instead use radio buttons with images?
		rangeStyleButton = new Button(selectorRow, SWT.CHECK);
		rangeStyleButton.setLayoutData(GridDataFactory.swtDefaults().create());
		rangeStyleButton.setText(Messages.FlavorSelector_BUTTON_CONTAINED);
		rangeStyleButton.setToolTipText(Messages.FlavorSelector_BUTTON_CONTAINED_TOOLTIP);
		rangeStyleButton.setEnabled(showConcurrentButton.getSelection());
		rangeStyleButton.setSelection(flavorSelectorState.concurrentContained);
		rangeStyleButton.addSelectionListener(new RangeStyleSelectionListener());

		// FIXME: Instead use radio buttons with images?
		sameThreadsButton = new Button(selectorRow, SWT.CHECK);
		sameThreadsButton.setLayoutData(GridDataFactory.swtDefaults().create());
		sameThreadsButton.setText(Messages.FlavorSelector_BUTTON_SAME_THREADS);
		sameThreadsButton.setToolTipText(Messages.FlavorSelector_BUTTON_SAME_THREADS_TOOLTIP);
		sameThreadsButton.setEnabled(showConcurrentButton.getSelection());
		sameThreadsButton.setSelection(flavorSelectorState.sameThreads);
		sameThreadsButton.addSelectionListener(new SameThreadsSelectionListener());

		// FIXME: Persist state for above checkboxes?

		onShow.ifPresent(on -> {
			Label rangeLabel = new Label(selectorRow, SWT.NONE);
			rangeLabel.setLayoutData(GridDataFactory.swtDefaults().create());
			rangeLabel.setText(Messages.FlavorSelector_LABEL_TIMERANGE);
			showButton = new Button(selectorRow, SWT.PUSH);
			showButton.setText(Messages.FlavorSelector_BUTTON_TIMERANGE_SET);
			showButton.setToolTipText(Messages.FlavorSelector_BUTTON_TIMERANGE_SET_TOOLTIP);
			showButton.setLayoutData(GridDataFactory.swtDefaults().create());
			resetButton = new Button(selectorRow, SWT.PUSH);
			resetButton.setText(Messages.FlavorSelector_BUTTON_TIMERANGE_CLEAR);
			resetButton.setToolTipText(Messages.FlavorSelector_BUTTON_TIMERANGE_CLEAR_TOOLTIP);
			resetButton.addListener(SWT.Selection, e -> on.accept(false));
			resetButton.setLayoutData(GridDataFactory.swtDefaults().create());
			showButton.addListener(SWT.Selection, e -> on.accept(true));
		});

		canvas = new Canvas(container, SWT.NO_BACKGROUND);
		canvas.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).hint(SWT.DEFAULT, 7)
				.grab(true, false).create());
		painter = new RangePainter(canvas, pageContainer.getRecordingRange());

		selectionCombo.setInput(pageContainer.getSelectionStore());
		selectionCombo.setSelection(getCurrentSelection());
		callbackActive = true;

		enableSelection();

		IItemStreamFlavor currentFlavor = null;
		if (pageContainer.getSelectionStore().isCurrentActive()) {
			currentFlavor = getSelectedFlavor();
		}
		useFlavor(currentFlavor);

		pageContainer.getSelectionStore().setListener(this);

		form.setHeadClient(container);
	}

	private ISelection getCurrentSelection() {
		return pageContainer.getSelectionStore().getCurrentSelection() != null
				? new StructuredSelection(pageContainer.getSelectionStore().getCurrentSelection())
				: new StructuredSelection(selectionCombo.getElementAt(0));
	}

	@Override
	public void selectionActive(boolean active) {
//		useSelectionButton.setSelection(active);
		selectionCombo.setSelection(getCurrentSelection());
	}

	@Override
	public void selectionAdded(SelectionStoreEntry selection) {
//		useSelectionButton.setEnabled(true);
		if (!selectionCombo.getControl().isDisposed()) {
			selectionCombo.refresh();
		}
	}

	private static String formatRange(IRange<IQuantity> range) {
		return range.getStart().displayUsing(IDisplayable.AUTO) + " - ( " //$NON-NLS-1$
				+ range.getExtent().displayUsing(IDisplayable.AUTO) + " ) - " //$NON-NLS-1$
				+ range.getEnd().displayUsing(IDisplayable.AUTO);
	}

	public void enableSelection() {
		boolean enabled = true;
		pageContainer.getSelectionStore().setCurrentActive(enabled);
		selectionCombo.getCombo().setEnabled(enabled);
		flavorCombo.getCombo().setEnabled(enabled);
		// FIXME: Make sure not to call useFlavor twice during initialization.
//		IItemStreamFlavor flavor = null;
//		if (enabled) {
//			flavor = getSelectedFlavor();
//		}
//		useFlavor(flavor);
	}

	public FlavorSelectorState getFlavorSelectorState() {
		return flavorSelectorState;
	}

	private IItemStreamFlavor getSelectedFlavor() {
		IItemStreamFlavor flavor = null;
		ISelection s = flavorCombo.getSelection();
		if (s instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) s).getFirstElement();
			if (obj instanceof IItemStreamFlavor) {
				flavor = (IItemStreamFlavor) obj;
			}
		}
		return flavor;
	}

	private static final class SelectionComboContentProvider implements IStructuredContentProvider {
		private SelectionStore store;

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof SelectionStore) {
				store = (SelectionStore) newInput;
			}
		}

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			// FIXME: This is for if we enable/disable selection usage outside of the combo
//			if (store.getSelections().count() == 0) {
//				return new String[] { "<No selection stored>" };
//			}
			return store.getSelections().toArray();
		}
	}

	public interface SelectionWithThreadAndRangeConsumer {
		public void accept(IItemCollection items, Set<String> threadNames, IRange<IQuantity> range);
	}

	private static final class SelectionComboLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof SelectionStoreEntry) {
				SelectionStoreEntry entry = (SelectionStoreEntry) element;
				return entry.getName();
			}
			return super.getText(element);
		}
	}

	private final class SelectionComboSelectionListener implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			flavorCombo.getCombo().removeAll();
			if (event.getSelection() instanceof IStructuredSelection) {
				Object obj = ((IStructuredSelection) (event.getSelection())).getFirstElement();
				if (obj instanceof SelectionStoreEntry) {
					SelectionStoreEntry entry = (SelectionStoreEntry) obj;
					IFlavoredSelection selection = entry.getSelection();
					pageContainer.getSelectionStore().setCurrent(selection);

					IItemStreamFlavor[] flavors = flavorSelectorState.calculatedFlavorsMap.get(selection);
					if (flavors == null) {
						flavors = selection.getFlavors(filter, items, attributes).toArray(IItemStreamFlavor[]::new);
						flavorSelectorState.calculatedFlavorsMap.put(selection, flavors);
					}
					flavorCombo.setInput(flavors);

					IItemStreamFlavor selectedFlavor = flavorSelectorState.selectedFlavorMap.get(selection);
					if (selectedFlavor == null) {
						if (flavors.length > 0) {
							selectedFlavor = flavors[0];
							flavorSelectorState.selectedFlavorMap.put(selection, selectedFlavor);
						}
					}
					if (selectedFlavor != null) {
						flavorCombo.setSelection(new StructuredSelection(selectedFlavor));
					}

					trimFlavorMaps();
				}
			}
		}
	}

	private static final class FlavorComboContentProvider implements IStructuredContentProvider {
		private IItemStreamFlavor[] flavors;

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof IItemStreamFlavor[]) {
				flavors = (IItemStreamFlavor[]) newInput;
			}
		}

		@Override
		public void dispose() {

		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (flavors == null || flavors.length == 0) {
				return new String[] {"<" + Messages.FlavorSelector_LABEL_NO_SELECTION + ">"}; //$NON-NLS-1$ //$NON-NLS-2$
			}
			return flavors;
		}
	}

	private static final class FlavorComboLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof IItemStreamFlavor) {
				IItemStreamFlavor sel = (IItemStreamFlavor) element;
				return sel.getName();
			}
			return super.getText(element);
		}
	}

	private final class FlavorComboSelectionListener implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			IItemStreamFlavor flavor = null;
			if (event.getSelection() instanceof IStructuredSelection) {
				Object obj = ((IStructuredSelection) event.getSelection()).getFirstElement();
				if (obj instanceof IItemStreamFlavor) {
					flavor = ((IItemStreamFlavor) obj);
					SelectionStoreEntry entry = pageContainer.getSelectionStore().getCurrentSelection();
					if (entry != null) {
						flavorSelectorState.selectedFlavorMap.put(entry.getSelection(), flavor);
					}
				}
			}
			useFlavor(flavor);
		}
	}

	private void useFlavor(IItemStreamFlavor flavor) {
		if (callbackActive) {
			Optional<IRange<IQuantity>> range = FlavorToolkit.getRange(flavor);
			painter.current = range.orElse(null);
			canvas.setVisible(painter.current != null);
			canvas.setToolTipText(range.map(FlavorSelector::formatRange).orElse(null));
			container.layout();

			// FIXME: Always use concurrent if (all?) items can't be displayed on page?
			IItemCollection itemsToUse = null;
			Set<IMCThread> threads = FlavorToolkit.getThreads(getSelectedFlavor(), flavorSelectorState.showConcurrent,
					flavorSelectorState.sameThreads);
			if (flavor != null && !flavor.isEmpty()) {
				IItemFilter rangeAndThreadFilter = FlavorToolkit.getRangeAndThreadFilter(range, threads,
						flavorSelectorState.showConcurrent, flavorSelectorState.concurrentContained,
						flavorSelectorState.sameThreads);
				if (rangeAndThreadFilter != null) {
					itemsToUse = items.apply(rangeAndThreadFilter);
				} else {
					itemsToUse = flavor.evaluate();
				}
			}
			Set<String> threadNames = FlavorToolkit.getThreadNames(threads, flavor);
			onSelectWithThreads.accept(itemsToUse, threadNames, range.orElse(pageContainer.getRecordingRange()));
		}
	}

	public void trimFlavorMaps() {
		// NOTE: It should be enough to keep the map sizes below 2 * storesize
		if (flavorSelectorState.calculatedFlavorsMap
				.size() > (2 * pageContainer.getSelectionStore().getSelections().count())) {

			List<IFlavoredSelection> storedSelections = pageContainer.getSelectionStore().getSelections()
					.map(sse -> sse.getSelection()).collect(Collectors.toList());

			for (Iterator<Entry<IFlavoredSelection, IItemStreamFlavor[]>> iterator = flavorSelectorState.calculatedFlavorsMap
					.entrySet().iterator(); iterator.hasNext();) {
				IFlavoredSelection selection = iterator.next().getKey();
				if (!storedSelections.contains(selection)) {
					iterator.remove();
				}
			}

			for (Iterator<Entry<IFlavoredSelection, IItemStreamFlavor>> iterator = flavorSelectorState.selectedFlavorMap
					.entrySet().iterator(); iterator.hasNext();) {
				IFlavoredSelection selection = iterator.next().getKey();
				if (!storedSelections.contains(selection)) {
					iterator.remove();
				}
			}
		}
	}

	public class ShowConcurrentSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			flavorSelectorState.showConcurrent = showConcurrentButton.getSelection();
			rangeStyleButton.setEnabled(flavorSelectorState.showConcurrent);
			sameThreadsButton.setEnabled(flavorSelectorState.showConcurrent);
			useFlavor(getSelectedFlavor());
		}
	}

	public class RangeStyleSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			flavorSelectorState.concurrentContained = rangeStyleButton.getSelection();
			useFlavor(getSelectedFlavor());
		}
	}

	public class SameThreadsSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			flavorSelectorState.sameThreads = sameThreadsButton.getSelection();
			useFlavor(getSelectedFlavor());
		}
	}

	private static class RangePainter implements PaintListener {

		private final Control canvas;
		private final IQuantity start;
		private final IQuantity end;

		IRange<IQuantity> current;

		RangePainter(Control canvas, IRange<IQuantity> fullRange) {
			this.canvas = canvas;
			start = fullRange.getStart();
			end = fullRange.getEnd();
			canvas.addPaintListener(this);
		}

		@Override
		public void paintControl(PaintEvent e) {
			if (current != null) {
				Point size = canvas.getSize();

				e.gc.setBackground(SWTColorToolkit.getColor(new RGB(200, 200, 200)));
				e.gc.setForeground(SWTColorToolkit.getColor(new RGB(120, 120, 120)));
				e.gc.fillRectangle(0, 0, size.x, size.y);
				e.gc.drawRectangle(0, 0, size.x - 1, size.y - 1);

				SubdividedQuantityRange fullRangeAxis = new SubdividedQuantityRange(start, end, size.x, 25);
				int x1 = (int) fullRangeAxis.getPixel(current.getStart());
				int x2 = (int) Math.ceil(fullRangeAxis.getPixel(current.getEnd()));
				e.gc.setForeground(SWTColorToolkit.getColor(new RGB(221, 58, 22)));
				e.gc.setBackground(SWTColorToolkit.getColor(new RGB(252, 128, 3)));
				e.gc.fillGradientRectangle(x1, 0, x2 - x1, size.y, true);
				e.gc.setForeground(SWTColorToolkit.getColor(new RGB(0, 0, 0)));
				e.gc.drawRectangle(x1, 0, x2 - x1 - 1, size.y - 1);
			}
		}
	}
}
