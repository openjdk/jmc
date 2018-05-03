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
package org.openjdk.jmc.flightrecorder.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.collection.IteratorToolkit;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.QuantitiesToolkit;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.RangeContentType;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.preferences.PreferenceKeys;
import org.openjdk.jmc.flightrecorder.ui.selection.FlavoredSelectionBase;
import org.openjdk.jmc.flightrecorder.ui.selection.IFilterFlavor;
import org.openjdk.jmc.flightrecorder.ui.selection.IFlavoredSelection;
import org.openjdk.jmc.flightrecorder.ui.selection.IItemStreamFlavor;
import org.openjdk.jmc.flightrecorder.ui.selection.IPropertyFlavor;
import org.openjdk.jmc.flightrecorder.ui.selection.ItemBackedSelection;
import org.openjdk.jmc.ui.TypeAppearance;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.accessibility.FocusTracker;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.column.TableSettings.ColumnSettings;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.TypedLabelProvider;

// FIXME: fields - units - filters - icons etc. should be handled more properly
public class JfrPropertySheet extends Page implements IPropertySheetPage {

	private static final String HELP_CONTEXT_ID = FlightRecorderUI.PLUGIN_ID + ".JfrPropertiesView"; //$NON-NLS-1$
	private static final Object TOO_MANY_VALUES = new Object();
	private static final PropertySheetRow CALCULATING = new PropertySheetRow(null, null);

	private static class PropertySheetRowSelection extends FlavoredSelectionBase {

		private final PropertySheetRow row;

		PropertySheetRowSelection(PropertySheetRow row) {
			super(MessageFormat.format(Messages.JFR_PROPERTIES_PROPERTY_SELECTION, row.attribute.getName()));
			this.row = row;
		}

		@Override
		public Stream<IItemStreamFlavor> getFlavors(
			IItemFilter dstFilter, IItemCollection items, List<IAttribute<?>> dstAttributes) {
			/*
			 * FIXME: Is this the desired behavior? Discuss and change if necessary.
			 * 
			 * This most likely need more thought and discussion, but the implemented order of
			 * flavors is currently:
			 * 
			 * For chart selections:
			 * 
			 * 1: The selected events if any of them appear on the destination page
			 * 
			 * 2: All events on the destination page in the selected range (if a range was selected)
			 * 
			 * 3-n: All events on the destination page filtered on any of the attributes common to
			 * all selected events (excluding the range attribute if a range was selected)
			 * 
			 * For histogram and list selections:
			 * 
			 * 1: The selected events if any of them appear on the destination page
			 * 
			 * 2-n: All events on the destination page filtered on any of the attributes common to
			 * all selected events (all will at least have (endTime))
			 * 
			 * For properties view selections:
			 * 
			 * 1: All events on the destination page filtered on the selected attribute:value if
			 * they all have the selected attribute
			 * 
			 * 2: All events on the destination page filtered on the selected value if they all have
			 * an attribute with the same content type
			 * 
			 * 3: All events on the destination page filtered on all common attributes with values
			 * from all events filtered on the selected attribute:value (see example)
			 * 
			 * 4: All events on the destination page filtered on the selected attribute:value if
			 * there are any events with the attribute (and the attribute is not common to all
			 * events in which case this flavor has already been added in (1))
			 * 
			 * Example of properties view selections (3):
			 * 
			 * ECID:1-2-3-4 was selected and the user navigates to Java Application. All events on
			 * Java Application share (thread) and (endTime), so all events on the page are filtered
			 * on those properties. The values to include are collected from all events with the
			 * ECID attribute having value 1-2-3-4. The threads will be put in a set, the timestamps
			 * will form a range.
			 */
			IItemCollection filteredDstItems = ItemCollectionToolkit.filterIfNotNull(items, dstFilter);
			IPropertyFlavor relatedFilterFlavor = IPropertyFlavor.build(row.attribute, row.value, filteredDstItems);
			LinkedList<IItemStreamFlavor> flavors = new LinkedList<>();

			boolean anyRelatedOnDst = relatedFilterFlavor.evaluate().hasItems();
			IPropertyFlavor selectedPropertyFlavor = IPropertyFlavor.build(row.attribute, row.value, items);
			if (anyRelatedOnDst) {
				// prio1(a): Items related to the selected attribute if there are any
				flavors.add(selectedPropertyFlavor);
				selectedPropertyFlavor = null;
			}
			IItemCollection itemsRelatedToSelection = items.apply(relatedFilterFlavor.getFilter());
			if (dstAttributes == null || dstAttributes.isEmpty()) {
				dstAttributes = commonAttributes(filteredDstItems.iterator()).collect(Collectors.toList());
			}
			Iterator<IAttribute<?>> commonDstAttr = dstAttributes.iterator();
			List<IPropertyFlavor> relatedProperties = new ArrayList<>();
			while (commonDstAttr.hasNext()) {
				IAttribute<?> dstAttribute = commonDstAttr.next();
				if (!dstAttribute.equals(JfrAttributes.EVENT_TYPE)
						&& (!(dstAttribute.getContentType() instanceof KindOfQuantity)
								|| dstAttribute.equals(JfrAttributes.END_TIME))) {
					// FIXME: Collect type or quantity values?
					if (dstAttribute.equals(row.attribute)) {
						if (!anyRelatedOnDst && selectedPropertyFlavor != null) {
							// prio1(b): Related to the selected attribute even though it's empty, since the attribute is shared by all
							flavors.push(selectedPropertyFlavor);
							selectedPropertyFlavor = null;
						}
						relatedProperties = null;
					} else if (!dstAttribute.equals(row.attribute)
							&& dstAttribute.getContentType().equals(row.attribute.getContentType())) {
						// prio2: Destination items with an attribute of the selected content type and which equals the selected value
						flavors.add(IPropertyFlavor.build(dstAttribute, row.value, items));
					}
					if (relatedProperties != null) {
						// Collect values from items related to selection (only items of types that has the attribute), and add as filter
						PropertySheetRow av = buildProperty(dstAttribute,
								ItemCollectionToolkit.stream(itemsRelatedToSelection)
										.filter(is -> dstAttribute.getAccessor(is.getType()) != null).iterator(),
								Integer.MAX_VALUE);
						if (av != null) {
							relatedProperties.add(IPropertyFlavor.build(av.attribute, av.value, items));
						}
					}
				}
			}
			if (relatedProperties != null) {
				if (relatedProperties.size() > 1) {
					// prio3: Destination items with properties shared with the items related to the selection
					flavors.add(IPropertyFlavor.combine(relatedProperties::stream, items));
				}

				// FIXME: Combinations with for example two properties if there are three properties in total shared?

				// prio4: Destination items with one property shared with the items related to the selection
				flavors.addAll(relatedProperties);
			}
			if (selectedPropertyFlavor != null) {
				// prio4: Items related to the selected attribute even if there aren't any
				flavors.add(selectedPropertyFlavor);
			}
			return flavors.stream();
		}
	}

	static class PropertySheetRow {
		final IAttribute<?> attribute;
		final Object value;

		PropertySheetRow(IAttribute<?> attribute, Object value) {
			this.attribute = attribute;
			this.value = value;
		}

		public IAttribute<?> getAttribute() {
			return attribute;
		}

		public Object getValue() {
			return value;
		}

	}

	private static final IColumn FIELD_COLUMN = new ColumnBuilder(Messages.JFR_PROPERTY_SHEET_FIELD, "field", //$NON-NLS-1$
			new TypedLabelProvider<PropertySheetRow>(PropertySheetRow.class) {

				@Override
				protected String getTextTyped(PropertySheetRow p) {
					return p.attribute == null ? "" : p.attribute.getName(); //$NON-NLS-1$
				};

				@Override
				protected String getToolTipTextTyped(PropertySheetRow p) {
					// FIXME: This is duplicated in EventBrowserPage, where we also create a tooltip for an attribute.
					return p.attribute == null ? "" //$NON-NLS-1$
							: NLS.bind(Messages.ATTRIBUTE_ID_LABEL, p.attribute.getIdentifier())
									+ System.getProperty("line.separator") //$NON-NLS-1$
									+ NLS.bind(Messages.ATTRIBUTE_DESCRIPTION_LABEL, p.attribute.getDescription());
				};

				@Override
				protected Image getImageTyped(PropertySheetRow p) {
					if (p.attribute != null) {
						Image icon = TypeAppearance.getImage(p.attribute.getContentType().getIdentifier());
						return icon == null ? UIPlugin.getDefault().getImage(UIPlugin.ICON_PROPERTY_OBJECT) : icon;
					}
					return null;
				};
			}).build();

	private static final IColumn VALUE_COLUMN = new ColumnBuilder(Messages.JFR_PROPERTY_SHEET_VALUE, "value", //$NON-NLS-1$
			new TypedLabelProvider<PropertySheetRow>(PropertySheetRow.class) {
				@Override
				protected String getTextTyped(PropertySheetRow p) {
					Object value = p.getValue();
					if (p == CALCULATING) {
						return Messages.JFR_PROPERTIES_CALCULATING;
					} else if (value == TOO_MANY_VALUES) {
						return Messages.JFR_PROPERTIES_TOO_MANY_VALUES;
					}
					return getValueString(value);
				};

				// FIXME: Merge with TypeHandling.getValueString
				private String getValueString(Object value) {
					if (value instanceof IDisplayable) {
						return ((IDisplayable) value).displayUsing(IDisplayable.AUTO);
					} else if (value instanceof IItemCollection) {
						return itemCollectionDescription((IItemCollection) value);
					} else if (value instanceof IDescribable) {
						return ((IDescribable) value).getName();
					} else if (value instanceof IDescribable[] && ((IDescribable[]) value).length > 0) {
						IDescribable[] values = ((IDescribable[]) value);
						return "[" + values[0].getName() + " ... " //$NON-NLS-1$ //$NON-NLS-2$
								+ values[values.length - 1].getName() + "]"; //$NON-NLS-1$
					} else if (value instanceof Object[]) {
						return limitedDeepToString((Object[]) value, this::getValueString);
					} else if (value instanceof Collection) {
						return limitedDeepToString(((Collection<?>) value).toArray(), this::getValueString);
					}
					return TypeHandling.getValueString(value);
				}

				@Override
				protected String getToolTipTextTyped(PropertySheetRow p) {
					Object value = p.getValue();
					return JfrPropertySheet.getVerboseString(value);
				};

			}).build();

	private static String limitedDeepToString(Object[] array, Function<Object, String> valueToStringProvider) {
		return limitedDeepToString(array, new StringBuilder(), true, valueToStringProvider);
	}

	private static String limitedDeepToString(
		Object[] array, StringBuilder builder, boolean isRootArray, Function<Object, String> valueToStringProvider) {
		int maxCharacters = FlightRecorderUI.getDefault().getPreferenceStore()
				.getInt(PreferenceKeys.PROPERTY_MAXIMUM_PROPERTIES_ARRAY_STRING_SIZE);
		int omitted = 0;
		builder.append('[');
		for (int i = 0; i < array.length; i++) {
			Object element = array[i];
			if (element != null && element.getClass().isArray()) {
				limitedDeepToString((Object[]) element, builder, false, valueToStringProvider);
			} else {
				builder.append(valueToStringProvider.apply(element));
			}
			if ((i < (array.length - 1)) && builder.length() < maxCharacters) {
				builder.append(',');
				builder.append(' ');
			}
			if (isRootArray && (builder.length() > maxCharacters)) {
				builder.setLength(maxCharacters);
				builder.append(Messages.JFR_PROPERTIES_INSERTED_ELLIPSIS);
				omitted = (array.length - 1) - i;
				break;
			}
		}
		if (isRootArray && omitted > 0) {
			builder.append(' ');
			if (omitted > 1) {
				builder.append(MessageFormat.format(Messages.JFR_PROPERTIES_ARRAY_WITH_OMITTED_ELEMENTS, omitted));
			} else {
				builder.append(Messages.JFR_PROPERTIES_ARRAY_WITH_OMITTED_ELEMENT);
			}
		}
		builder.append(']');
		return builder.toString();
	}

	private static final IColumn VERBOSE_VALUE_COLUMN = new ColumnBuilder(Messages.JFR_PROPERTY_SHEET_VERBOSE_VALUE,
			"verboseValue", //$NON-NLS-1$
			new TypedLabelProvider<PropertySheetRow>(PropertySheetRow.class) {
				@Override
				protected String getTextTyped(PropertySheetRow p) {
					Object value = p.getValue();
					if (p == CALCULATING) {
						return Messages.JFR_PROPERTIES_CALCULATING;
					} else if (value == TOO_MANY_VALUES) {
						return Messages.JFR_PROPERTIES_TOO_MANY_VALUES;
					}
					return JfrPropertySheet.getVerboseString(value);
				};

				@Override
				protected String getToolTipTextTyped(PropertySheetRow p) {
					return getTextTyped(p);
				};

			}).build();

	// FIXME: Merge with TypeHandling.getVerboseString
	private static String getVerboseString(Object value) {
		if (value instanceof IDisplayable) {
			return ((IDisplayable) value).displayUsing(IDisplayable.VERBOSE);
		} else if (value instanceof IItemCollection) {
			return ItemCollectionToolkit.getDescription(((IItemCollection) value));
		} else if (value instanceof IDescribable) {
			return ((IDescribable) value).getDescription();
		} else if (value instanceof IDescribable[] && ((IDescribable[]) value).length > 0) {
			IDescribable[] values = ((IDescribable[]) value);
			return "[" + values[0].getDescription() + " ... " //$NON-NLS-1$ //$NON-NLS-2$
					+ values[values.length - 1].getDescription() + "]"; //$NON-NLS-1$
		} else if (value instanceof Object[]) {
			return limitedDeepToString((Object[]) value, JfrPropertySheet::getVerboseString);
		} else if (value instanceof Collection) {
			return limitedDeepToString(((Collection<?>) value).toArray(), JfrPropertySheet::getVerboseString);
		}

		return TypeHandling.getVerboseString(value);
	}

	private TableViewer viewer;
	private final IPageContainer controller;
	private CompletableFuture<Void> viewerUpdater;

	JfrPropertySheet(IPageContainer controller) {
		this.controller = controller;
	}

	@Override
	public void createControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.FULL_SELECTION);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		// FIXME: Should we keep a state for the properties view?
		ColumnManager manager = ColumnManager.build(viewer,
				Arrays.asList(FIELD_COLUMN, VALUE_COLUMN, VERBOSE_VALUE_COLUMN), getTableSettings(null));
		MCContextMenuManager mm = MCContextMenuManager.create(viewer.getControl());
		ColumnMenusFactory.addDefaultMenus(manager, mm);
		Function<Consumer<IFlavoredSelection>, Function<List<PropertySheetRow>, Runnable>> actionProvider = flavorConsumer -> selected -> {
			if (selected.size() == 1 && selected.get(0).value != TOO_MANY_VALUES) {
				if (selected.get(0).attribute != null) {
					return () -> flavorConsumer.accept(new PropertySheetRowSelection(selected.get(0)));
				} else if (selected.get(0).value instanceof IItemCollection) {
					IItemCollection items = (IItemCollection) selected.get(0).value;
					String selectionName = itemCollectionDescription(items);
					return () -> flavorConsumer.accept(new ItemBackedSelection(items, selectionName));
				}
			}
			return null;
		};
		// FIXME: Break out to other place where these actions are added to menus
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT,
				ActionToolkit.forListSelection(viewer, Messages.STORE_SELECTION_ACTION, false,
						actionProvider.apply(controller.getSelectionStore()::addSelection)));
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT,
				ActionToolkit.forListSelection(viewer, Messages.STORE_AND_ACTIVATE_SELECTION_ACTION, false,
						actionProvider.apply(controller.getSelectionStore()::addAndSetAsCurrentSelection)));
		ColumnViewerToolTipSupport.enableFor(viewer);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), HELP_CONTEXT_ID);

		if (UIPlugin.getDefault().getAccessibilityMode()) {
			FocusTracker.enableFocusTracking(viewer.getTable());
		}
	}

	private static TableSettings getTableSettings(IState state) {
		if (state == null) {
			return new TableSettings(null,
					Arrays.asList(new ColumnSettings(FIELD_COLUMN.getId(), false, 120, null),
							new ColumnSettings(VALUE_COLUMN.getId(), false, 120, null),
							new ColumnSettings(VERBOSE_VALUE_COLUMN.getId(), true, 400, null)));
		} else {
			return new TableSettings(state);
		}
	}

	private static String itemCollectionDescription(IItemCollection items) {
		IQuantity count = items.getAggregate(Aggregators.count());
		return NLS.bind(Messages.JFR_PROPERTY_SHEET_EVENTS, count == null ? 0 : count.displayUsing(IDisplayable.AUTO));
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object first = ((IStructuredSelection) selection).getFirstElement();
			IItemCollection items = AdapterUtil.getAdapter(first, IItemCollection.class);
			if (items != null) {
				show(items);
			}
		}
	}

	private void show(IItemCollection items) {
		if (viewerUpdater != null) {
			viewerUpdater.complete(null);
		}
		CompletableFuture<PropertySheetRow[]> modelBuilder = CompletableFuture.supplyAsync(() -> buildRows(items));
		viewerUpdater = modelBuilder.thenAcceptAsync(this::setViewerInput, DisplayToolkit.inDisplayThread());
		viewerUpdater.exceptionally(JfrPropertySheet::handleModelBuildException);
		DisplayToolkit.safeTimerExec(Display.getCurrent(), 300, this::showCalculationFeedback);
	}

	private void setViewerInput(PropertySheetRow[] rows) {
		if (!viewer.getControl().isDisposed()) {
			viewer.setInput(rows);
		}
		viewerUpdater = null;
	}

	private void showCalculationFeedback() {
		if (viewerUpdater != null && !viewer.getControl().isDisposed()) {
			viewer.setInput(new PropertySheetRow[] {CALCULATING});
		}
	}

	private static Void handleModelBuildException(Throwable ex) {
		FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE, "Failed to build properties view model", ex); //$NON-NLS-1$
		return null;
	}

	private static PropertySheetRow[] buildRows(IItemCollection items) {
		Iterator<? extends IItemIterable> nonEmpty = IteratorToolkit.filter(items.iterator(),
				i -> i.iterator().hasNext());
		// FIXME: Would it be interesting to add derived attributes here as well?
		Stream<PropertySheetRow> rows = commonAttributes(nonEmpty)
				.map(attr -> buildProperty(attr, items.iterator(), MAX_DISTINCT_VALUES)).filter(Objects::nonNull);
		return Stream.concat(rows, Stream.of(new PropertySheetRow(null, items))).toArray(PropertySheetRow[]::new);
	}

	private static Stream<IAttribute<?>> commonAttributes(Iterator<? extends IItemIterable> iterables)
			throws IllegalArgumentException {
		// FIXME: List of attributes for the item collection should be provided from elsewhere
		if (!iterables.hasNext()) {
			return Stream.empty();
		} else {
			IItemIterable single = iterables.next();
			List<IAttribute<?>> attributes = single.getType().getAttributes();
			if (iterables.hasNext()) {
				attributes = new ArrayList<>(attributes); // modifiable copy
				while (iterables.hasNext()) {
					IType<?> otherType = iterables.next().getType();
					// FIXME: Use a Set<IType<?>> to avoid going through any type more than once.
					Iterator<IAttribute<?>> aIterator = attributes.iterator();
					while (aIterator.hasNext()) {
						if (!otherType.hasAttribute(aIterator.next())) {
							aIterator.remove();
						}
					}
				}
			}
			// FIXME: Possible remove this filter if we convert this to persistable attributes.
			return attributes.stream().filter(a -> a.getContentType() != UnitLookup.STACKTRACE);
		}
	}

	public static Stream<IFilterFlavor> calculatePersistableFilterFlavors(
		IItemCollection srcItems, IItemCollection dstItems, IItemCollection allItems,
		List<IAttribute<?>> dstAttributes) {
		return calculatePersistableFilterFlavors(srcItems, dstItems, allItems, dstAttributes, a -> true);
	}

	public static Stream<IFilterFlavor> calculatePersistableFilterFlavors(
		IItemCollection srcItems, IItemCollection dstItems, IItemCollection allItems, List<IAttribute<?>> dstAttributes,
		Predicate<IAttribute<?>> include) {
		// FIXME: Calculate common content types from the dstItems, and see if any of the srcItems can deliver them?
		Stream<IAttribute<?>> commonAttributes = null;
		if (dstAttributes != null && !dstAttributes.isEmpty()) {
			commonAttributes = commonAttributes(srcItems.iterator()).filter(a -> dstAttributes.contains(a));
		} else {
			Stream<? extends IItemIterable> items = Stream.concat(ItemCollectionToolkit.stream(srcItems),
					ItemCollectionToolkit.stream(dstItems));
			commonAttributes = commonAttributes(items.iterator());
		}
		Stream<IAttribute<?>> persistableAttributes = DataPageToolkit.getPersistableAttributes(commonAttributes)
				.filter(include::test);
		// FIXME: Add combinations here as well, similar to PropertySheetRowSelection.buildFlavors
		// FIXME: Can we get construct a life time filter from start and end times?
		return persistableAttributes.map(attr -> buildProperty(attr, srcItems.iterator(), MAX_DISTINCT_VALUES))
				.filter(p -> p != null && p.value != TOO_MANY_VALUES).sorted(RELEVANCE_ORDER)
				.map(p -> IPropertyFlavor.build(p.attribute, p.value, allItems));
	}

	private static final int MAX_DISTINCT_VALUES = 10;

	// FIXME: How to order? (currently quantity attributes last). Should we involve relational key attributes?
	private static final Comparator<PropertySheetRow> RELEVANCE_ORDER = new Comparator<PropertySheetRow>() {

		@Override
		public int compare(PropertySheetRow o1, PropertySheetRow o2) {
			int a1c = getAttributeCategory(o1.getAttribute());
			int a2c = getAttributeCategory(o2.getAttribute());
			if (a1c == a2c) {
				return o1.getAttribute().getIdentifier().compareTo(o2.getAttribute().getIdentifier());
			}
			return Integer.compare(a1c, a2c);
		}

		private int getAttributeCategory(IAttribute<?> attr) {
			ContentType<?> ct = attr.getContentType();
			if (ct.equals(UnitLookup.TIMESTAMP)) {
				return 0;
			} else if (ct instanceof KindOfQuantity) {
				return 2;
			}
			return 1;
		}

	};

	private static <M> PropertySheetRow buildProperty(
		IAttribute<M> attribute, Iterator<? extends IItemIterable> iterables, int maxDistinct) {
		ContentType<M> contentType = attribute.getContentType();
		if (contentType instanceof KindOfQuantity) {
			@SuppressWarnings("unchecked")
			IAttribute<IQuantity> qAttribute = (IAttribute<IQuantity>) attribute;
			IQuantity minValue = null;
			IQuantity maxValue = null;
			while (iterables.hasNext()) {
				IItemIterable ii = iterables.next();
				IMemberAccessor<IQuantity, IItem> accessor = qAttribute.getAccessor(ii.getType());
				Iterator<? extends IItem> items = ii.iterator();
				while (items.hasNext()) {
					IQuantity val = accessor.getMember(items.next());
					if (val == null) {
						// FIXME: Should null values be expected/accepted?
//						FlightRecorderUI.getDefault().getLogger().warning("Null value in " + qAttribute.getIdentifier() + " field"); //$NON-NLS-1$ //$NON-NLS-2$
					} else if (minValue == null) {
						minValue = maxValue = val;
					} else {
						minValue = QuantitiesToolkit.min(val, minValue);
						maxValue = QuantitiesToolkit.max(val, maxValue);
					}
				}
			}

			if (minValue != null) {
				if (minValue == maxValue) {
					return new PropertySheetRow(qAttribute, minValue);
				} else {
					return new PropertySheetRow(qAttribute, QuantityRange.createWithEnd(minValue, maxValue));
				}
			}
		} else if (contentType instanceof RangeContentType) {
			if (((RangeContentType<?>) contentType).getEndPointContentType() instanceof KindOfQuantity) {
				@SuppressWarnings("unchecked")
				IAttribute<IRange<IQuantity>> rangeAttribute = (IAttribute<IRange<IQuantity>>) attribute;
				IQuantity minValue = null;
				IQuantity maxValue = null;
				while (iterables.hasNext()) {
					IItemIterable ii = iterables.next();
					IMemberAccessor<IRange<IQuantity>, IItem> accessor = rangeAttribute.getAccessor(ii.getType());
					Iterator<? extends IItem> items = ii.iterator();
					while (items.hasNext()) {
						IRange<IQuantity> range = accessor.getMember(items.next());
						if (range == null) {
							// FIXME: Should null values be expected/accepted?
//							FlightRecorderUI.getDefault().getLogger().warning("Null value in " + rangeAttribute.getIdentifier() + " field"); //$NON-NLS-1$ //$NON-NLS-2$
						} else if (minValue == null) {
							minValue = range.getStart();
							maxValue = range.getEnd();
						} else {
							minValue = QuantitiesToolkit.min(range.getStart(), minValue);
							maxValue = QuantitiesToolkit.max(range.getEnd(), maxValue);
						}
					}
				}

				if (minValue != null) {
					if (minValue == maxValue) {
						return new PropertySheetRow(rangeAttribute, minValue);
					} else {
						return new PropertySheetRow(rangeAttribute, QuantityRange.createWithEnd(minValue, maxValue));
					}
				}
			}
		}

		Set<M> keys = new HashSet<>();
		while (iterables.hasNext()) {
			IItemIterable ii = iterables.next();
			IMemberAccessor<M, IItem> accessor = attribute.getAccessor(ii.getType());
			Iterator<? extends IItem> items = ii.iterator();
			while (items.hasNext()) {
				if (keys.size() > maxDistinct) {
					return new PropertySheetRow(attribute, TOO_MANY_VALUES);
				}
				// FIXME: Add more limitations if there are a lot of items?
				keys.add(accessor.getMember(items.next()));
			}
		}
		if (keys.size() == 0) {
			return null;
		} else if (keys.size() == 1) {
			return new PropertySheetRow(attribute, keys.iterator().next());
		} else {
			return new PropertySheetRow(attribute, keys);
		}

	}

	@Override
	public Control getControl() {
		return viewer.getControl();
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

}
