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
package org.openjdk.jmc.rjmx.ui.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIMetadataToolkit;
import org.openjdk.jmc.rjmx.ui.celleditors.TypedEditingSupport;
import org.openjdk.jmc.rjmx.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.TypedLabelProvider;

public final class AttributeConfiguratorTableFactory {

	private AttributeConfiguratorTableFactory() {
		throw new UnsupportedOperationException("Should not be instantiated!"); //$NON-NLS-1$
	}

	public static TableViewer createAttributeConfiguratorTable(
		Composite parent, AttributeLabelProvider alp, AttributeSelectionViewModel viewModel,
		AttributeSelectionContentModel selectorModel, Runnable listener) {
		Map<KindOfQuantity<?>, List<IUnit>> types = getTypes();

		TableViewer viewer = new TableViewer(parent,
				SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		ColumnViewerToolTipSupport.enableFor(viewer);
		viewer.setContentProvider(ArrayContentProvider.getInstance());

		TableViewerColumn nameColumn = new TableViewerColumn(viewer, SWT.NONE);
		nameColumn.getColumn().setWidth(200);
		nameColumn.getColumn().setText(Messages.COLUMN_ATTRIBUTE_NAME);
		nameColumn.getColumn().setToolTipText(Messages.COLUMN_ATTRIBUTE_DESCRIPTION);
		nameColumn.setLabelProvider(alp);

		TableViewerColumn typeColumn = new TableViewerColumn(viewer, SWT.NONE);
		typeColumn.getColumn().setWidth(150);
		typeColumn.getColumn().setText(Messages.COLUMN_CONTENT_TYPE_NAME);
		typeColumn.getColumn().setToolTipText(Messages.COLUMN_CONTENT_TYPE_DESCRIPTION);
		typeColumn.setLabelProvider(createTypeLabelProvider(viewModel, selectorModel));
		typeColumn.setEditingSupport(createTypeEditingSupport(viewModel, selectorModel, types, viewer, listener));

		TableViewerColumn unitColumn = new TableViewerColumn(viewer, SWT.NONE);
		unitColumn.getColumn().setWidth(150);
		unitColumn.getColumn().setText(Messages.COLUMN_UNIT_TYPE_NAME);
		unitColumn.getColumn().setToolTipText(Messages.COLUMN_UNIT_TYPE_DESCRIPTION);
		unitColumn.setLabelProvider(createUnitLabelProvider(selectorModel));
		unitColumn.setEditingSupport(createUnitEditingSupport(viewModel, selectorModel, types, viewer, listener));

		MCContextMenuManager menu = MCContextMenuManager.create(viewer.getControl());
		types.entrySet().stream().sorted((e1, e2) -> e1.getKey().getName().compareTo(e2.getKey().getName()))
				.forEachOrdered(entry -> {
					KindOfQuantity<?> kind = entry.getKey();
					MenuManager typeUnit = new MenuManager(
							MessageFormat.format(Messages.CONTEXT_MENU_UNIT_TYPE_NAME, kind.getName()));
					for (IUnit unit : entry.getValue()) {
						typeUnit.add(ActionToolkit.action(() -> {
							Object[] array = ((StructuredSelection) viewer.getSelection()).toArray();
							for (int i = 0; i < array.length; i++) {
								if (array[i] instanceof MRI) {
									MRI attribute = (MRI) array[i];
									selectorModel.setAttributeUnit(attribute, unit);
									viewer.refresh();
									listener.run();
								}
							}
						}, unit.getLocalizedDescription()));
					}
					menu.add(typeUnit);
					typeUnit.setVisible(false);
					viewer.addSelectionChangedListener(e -> {
						typeUnit.setVisible(!e.getSelection().isEmpty());
						menu.update(true);
					});
				});
		return viewer;
	}

	// This class makes the selected value selected immediately when selecting with
	// the mouse. The default behavior for ComboBoxCellEditor is to apply the selection when
	// focus is lost.
	private static class AttributeComboBoxCellEditor extends ComboBoxCellEditor {
		AttributeComboBoxCellEditor(Composite parent, String[] items) {
			super(parent, items, SWT.READ_ONLY);
			setActivationStyle(
					ComboBoxCellEditor.DROP_DOWN_ON_KEY_ACTIVATION | ComboBoxCellEditor.DROP_DOWN_ON_MOUSE_ACTIVATION
							| ComboBoxCellEditor.DROP_DOWN_ON_TRAVERSE_ACTIVATION
							| ComboBoxCellEditor.DROP_DOWN_ON_PROGRAMMATIC_ACTIVATION);
		}

		@Override
		protected Control createControl(Composite parent) {
			final CCombo combo = (CCombo) super.createControl(parent);
			combo.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					// if the list is not visible, assume the user is done
					if (!combo.getListVisible()) {
						focusLost();
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			return combo;
		}
	}

	private static TypedLabelProvider<MRI> createTypeLabelProvider(
		final AttributeSelectionViewModel viewModel, final AttributeSelectionContentModel selectorModel) {
		return new TypedLabelProvider<MRI>(MRI.class) {
			@Override
			protected String getTextTyped(MRI attribute) {
				IUnit unit = selectorModel.getAttributeUnit(attribute);
				if (unit != null) {
					return unit.getContentType().getName();
				}
				if (viewModel.getContentType() == null) {
					return Messages.VALUE_NO_TYPE;
				}
				return viewModel.getContentType().getName();
			}
		};
	}

	private static TypedEditingSupport<MRI> createTypeEditingSupport(
		final AttributeSelectionViewModel viewModel, final AttributeSelectionContentModel selectorModel,
		final Map<KindOfQuantity<?>, List<IUnit>> types, final TableViewer table, final Runnable listener) {
		return new TypedEditingSupport<MRI>(table, MRI.class) {
			@Override
			public boolean canEdit(Object element) {
				if (viewModel.getContentType() != null) {
					return false;
				}
				if (element instanceof MRI) {
					MRI mri = (MRI) element;
					IMRIMetadata metadata = selectorModel.getMetadataService().getMetadata(mri);
					if (!MRIMetadataToolkit.isNumerical(metadata)) {
						return false;
					}
					return true;
				}
				return false;
			}

			@Override
			protected CellEditor getCellEditorTyped(MRI attribute) {
				return new AttributeComboBoxCellEditor(table.getTable(), getTypeDisplayNames());
			}

			private String[] getTypeDisplayNames() {
				if (viewModel.getContentType() != null) {
					return new String[] {viewModel.getContentType().getName()};
				}
				List<String> typeNames = new ArrayList<>(types.size());
				for (KindOfQuantity<?> type : types.keySet()) {
					typeNames.add(type.getName());
				}
				return typeNames.toArray(new String[typeNames.size()]);
			}

			@Override
			protected Object getValueTyped(MRI attribute) {
				IUnit unit = selectorModel.getAttributeUnit(attribute);
				if (unit == null) {
					return 0;
				}
				int index = 0;
				for (KindOfQuantity<?> type : types.keySet()) {
					if (type.equals(unit.getContentType())) {
						return index;
					}
					index++;
				}
				return 0;
			}

			@Override
			protected void setValueTyped(MRI attribute, Object value) {
				if (!(value instanceof Number)) {
					return;
				}
				ContentType<?> contentType = viewModel.getContentType();
				if (contentType != null) {
					if (contentType instanceof KindOfQuantity) {
						selectorModel.setAttributeUnit(attribute, ((KindOfQuantity<?>) contentType).getDefaultUnit());
						table.refresh();
					}
					return;
				}
				IUnit unit = null;
				int index = ((Number) value).intValue();
				int pos = 0;
				for (KindOfQuantity<?> type : types.keySet()) {
					if (pos == index) {
						unit = type.getDefaultUnit();
						break;
					}
					pos += 1;
				}
				selectorModel.setAttributeUnit(attribute, unit);
				table.refresh();
				listener.run();
			}
		};
	}

	private static TypedLabelProvider<MRI> createUnitLabelProvider(final AttributeSelectionContentModel selectorModel) {
		return new TypedLabelProvider<MRI>(MRI.class) {
			@Override
			protected String getTextTyped(MRI attribute) {
				IUnit unit = selectorModel.getAttributeUnit(attribute);
				if (unit != null) {
					return unit.getLocalizedDescription();
				}
				return Messages.VALUE_NO_UNIT;
			}
		};
	}

	private static TypedEditingSupport<MRI> createUnitEditingSupport(
		final AttributeSelectionViewModel viewModel, final AttributeSelectionContentModel selectorModel,
		final Map<KindOfQuantity<?>, List<IUnit>> types, final TableViewer table, final Runnable listener) {
		return new TypedEditingSupport<MRI>(table, MRI.class) {
			@Override
			public boolean canEdit(Object element) {
				if (element instanceof MRI) {
					MRI mri = (MRI) element;
					IMRIMetadata metadata = selectorModel.getMetadataService().getMetadata(mri);
					if (!MRIMetadataToolkit.isNumerical(metadata)) {
						return false;
					}
					return true;
				}
				return false;
			}

			@Override
			protected CellEditor getCellEditorTyped(MRI attribute) {
				return new AttributeComboBoxCellEditor(table.getTable(), getUnitsForType(attribute));
			}

			private String[] getUnitsForType(MRI attribute) {
				IUnit unit = selectorModel.getAttributeUnit(attribute);
				if (unit == null) {
					ContentType<?> contentType = viewModel.getContentType();
					if (contentType instanceof KindOfQuantity<?>) {
						unit = ((KindOfQuantity<?>) contentType).getDefaultUnit();
					} else {
						return new String[0];
					}
				}
				List<IUnit> units = types.get(unit.getContentType());
				String[] unitNames = new String[units.size()];
				for (int i = 0; i < units.size(); i += 1) {
					unitNames[i] = units.get(i).getLocalizedDescription();
				}
				return unitNames;
			}

			@Override
			protected Object getValueTyped(MRI attribute) {
				IUnit unit = selectorModel.getAttributeUnit(attribute);
				if (unit == null) {
					return 0;
				}
				List<IUnit> units = types.get(unit.getContentType());
				int index = units.indexOf(unit);
				if (index < 0) {
					index = 0;
				}
				return index;
			}

			@Override
			protected void setValueTyped(MRI attribute, Object value) {
				IUnit unit = selectorModel.getAttributeUnit(attribute);
				if (!(value instanceof Number) || (unit == null && viewModel.getContentType() == null)) {
					return;
				}
				int index = ((Number) value).intValue();
				List<IUnit> units = types.get((unit != null) ? unit.getContentType() : viewModel.getContentType());
				selectorModel.setAttributeUnit(attribute, units.get(index));
				table.refresh();
				listener.run();
			}
		};
	}

	private static Map<KindOfQuantity<?>, List<IUnit>> getTypes() {
		Map<KindOfQuantity<?>, List<IUnit>> types = new TreeMap<>(new Comparator<KindOfQuantity<?>>() {
			@Override
			public int compare(KindOfQuantity<?> o1, KindOfQuantity<?> o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		for (KindOfQuantity<?> type : UnitLookup.getKindsOfQuantity()) {
			List<IUnit> units = new ArrayList<>(type.getCommonUnits());
			types.put(type, units);
		}
		return types;
	}
}
