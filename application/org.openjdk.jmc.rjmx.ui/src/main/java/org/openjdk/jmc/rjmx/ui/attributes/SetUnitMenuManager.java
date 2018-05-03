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
package org.openjdk.jmc.rjmx.ui.attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.openjdk.jmc.common.unit.ITypedQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataProvider;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.MRIMetadataToolkit;

public class SetUnitMenuManager extends MenuManager {

	private final IConnectionHandle m_connection;
	IMenuManager m_supermenu;
	Collection<ReadOnlyMRIAttribute> m_selectedAttributes = Collections.emptyList();
	Map<String, Action> m_unitActions;
	private final Map<KindOfQuantity<?>, CustomUnitAction> m_customActions;
	Action m_rawAction;

	private class CustomUnitAction extends Action {
		private final LinearKindOfQuantity kindOfQuantity;
		private LinearUnit unit;

		private CustomUnitAction(LinearKindOfQuantity kindOfQuantity) {
			super(Messages.SetUnitMenuManager_CUSTOM_UNIT_MENU_ITEM, IAction.AS_CHECK_BOX);
			this.kindOfQuantity = kindOfQuantity;
		}

		private void setUnit(LinearUnit unit) {
			this.unit = unit;
			if (unit != null) {
				setText(NLS.bind(Messages.SetUnitMenuManager_CUSTOM_UNIT_MENU_ITEM_MSG,
						unit.getLocalizedDescription()));
			} else {
				setText(Messages.SetUnitMenuManager_CUSTOM_UNIT_MENU_ITEM);
			}
		}

		@Override
		public void run() {
			LinearUnit initialUnit = (unit != null) ? unit : kindOfQuantity.getDefaultUnit();
			ITypedQuantity<LinearUnit> initialQuantity = initialUnit.asWellKnownQuantity();
			Shell shell = Display.getCurrent().getActiveShell();
			ITypedQuantity<LinearUnit> quantity = QuantityInputDialog.promptForCustomUnit(shell, initialQuantity);
			if (quantity != null) {
				String id = quantity.persistableString();
				LinearUnit newUnit = kindOfQuantity.getCachedUnit(id);
				if (newUnit == null) {
					newUnit = kindOfQuantity.makeCustomUnit(quantity);
				}
				SetUnitMenuManager.this.setUnit(m_selectedAttributes, newUnit);
				uncheckAllUnitActions();
				setUnit(newUnit);
				setChecked(true);
			}
		}
	}

	public SetUnitMenuManager(ISelectionProvider selectionProvider, IConnectionHandle connection,
			IMenuManager supermenu) {
		super(Messages.SetUnitMenuManager_SET_UNIT_MENU_ITEM);
		setVisible(false);

		m_connection = connection;
		m_supermenu = supermenu;
		m_unitActions = new HashMap<>();
		m_customActions = new HashMap<>();

		m_rawAction = new Action(Messages.SetUnitMenuManager_RAW_VALUE_MENU_ITEM) {
			@Override
			public void run() {
				setUnit(m_selectedAttributes, null);
				uncheckAllUnitActions();
				setChecked(true);
			}
		};

		add(m_rawAction);

		for (KindOfQuantity<?> contentType : UnitLookup.getKindsOfQuantity()) {
			String label = NLS.bind(Messages.SetUnitMenuManager_KIND_OF_QUANTITY_BY_MULTIPLYING_WITH_MSG,
					contentType.getName());

			MenuManager typeManager = new MenuManager(label);
			for (final IUnit unit : contentType.getCommonUnits()) {
				Action action = new Action(unit.getLocalizedDescription(), IAction.AS_CHECK_BOX) {
					@Override
					public void run() {
						setUnit(m_selectedAttributes, unit);
						uncheckAllUnitActions();
						setChecked(true);
					}
				};
				m_unitActions.put(UnitLookup.getUnitIdentifier(unit), action);
				typeManager.add(action);
			}
			if (contentType instanceof LinearKindOfQuantity) {
				final LinearKindOfQuantity kindOfQuantity = (LinearKindOfQuantity) contentType;
				CustomUnitAction action = new CustomUnitAction(kindOfQuantity);
				m_customActions.put(kindOfQuantity, action);
				typeManager.add(action);
			}
			add(typeManager);
		}

		selectionProvider.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IMRIMetadataService infoService = m_connection.getServiceOrNull(IMRIMetadataService.class);
				if (infoService != null && selectionIsNumeric(infoService, event)) {
					m_selectedAttributes = selectedAttributes(event);
					checkSelectedUnitAction(infoService);
					setVisible(true);
				} else {
					setVisible(false);
				}
				m_supermenu.update(true);
			}

		});
	}

	private void setUnit(Collection<ReadOnlyMRIAttribute> m_selectedAttributes, IUnit unit) {
		// FIXME: This should preferably be abstracted away to e.g. MRIMetadataToolkit
		String unitIdentifier = null;
		if (unit != null) {
			unitIdentifier = UnitLookup.getUnitIdentifier(unit);
		}
		IMRIMetadataService infoService = m_connection.getServiceOrNull(IMRIMetadataService.class);
		if (infoService != null) {
			for (ReadOnlyMRIAttribute attribute : m_selectedAttributes) {
				infoService.setMetadata(attribute.getMRI(), IMRIMetadataProvider.KEY_UNIT_STRING, unitIdentifier);
			}
		}
	}

	private Collection<ReadOnlyMRIAttribute> selectedAttributes(SelectionChangedEvent event) {
		List<ReadOnlyMRIAttribute> attributes = new ArrayList<>();
		StructuredSelection selection = (StructuredSelection) event.getSelection();
		Iterator<?> selectionIterator = selection.iterator();
		while (selectionIterator.hasNext()) {
			Object selectedObject = selectionIterator.next();
			if (selectedObject instanceof ReadOnlyMRIAttribute) {
				attributes.add((ReadOnlyMRIAttribute) selectedObject);
			}
		}
		return attributes;
	}

	private boolean selectionIsNumeric(IMRIMetadataService infoService, SelectionChangedEvent event) {
		StructuredSelection selection = (StructuredSelection) event.getSelection();
		Iterator<?> selectionIterator = selection.iterator();
		if (!selectionIterator.hasNext()) {
			return false;
		}

		while (selectionIterator.hasNext()) {
			Object selectedObject = selectionIterator.next();
			if (selectedObject instanceof ReadOnlyMRIAttribute) {
				ReadOnlyMRIAttribute attribute = (ReadOnlyMRIAttribute) selectedObject;
				if (!MRIMetadataToolkit.isNumerical(infoService.getMetadata(attribute.getMRI()))) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	private void checkSelectedUnitAction(IMRIMetadataService infoService) {
		Set<String> selectedUnits = new HashSet<>();
		for (ReadOnlyMRIAttribute attribute : m_selectedAttributes) {
			selectedUnits.add(infoService.getMetadata(attribute.getMRI()).getUnitString());
		}
		uncheckAllUnitActions();
		// Only check if selected items are of the same unit
		if (selectedUnits.size() == 1) {
			for (String selectedUnit : selectedUnits) {
				Action action = (selectedUnit == null) ? m_rawAction : m_unitActions.get(selectedUnit);
				if (action == null) {
					IUnit unit = UnitLookup.getUnitOrNull(selectedUnit);
					if (unit instanceof LinearUnit) {
						CustomUnitAction custAction = m_customActions.get(unit.getContentType());
						if (custAction != null) {
							custAction.setUnit((LinearUnit) unit);
							action = custAction;
						}
					}
				}
				if (action != null) {
					action.setChecked(true);
				}
			}
		}
	}

	private void uncheckAllUnitActions() {
		m_rawAction.setChecked(false);
		for (Action action : m_unitActions.values()) {
			action.setChecked(false);
		}
		for (Action action : m_customActions.values()) {
			action.setChecked(false);
		}
	}
}
