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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataProvider;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.IMRIService;
import org.openjdk.jmc.rjmx.subscription.MRI;

public class AttributeSelectionContentModel {

	private final IMRIService m_mriService;
	private final IMRIMetadataService m_mds;

	private final List<MRI> m_selectedAttributes = new ArrayList<>();
	private final Map<MRI, IUnit> m_alteredUnits = new HashMap<>();
	private final List<MRI> m_initialExpanded = new ArrayList<>();

	private final Set<IAttributeSelectionContentListener> listeners = new HashSet<>();

	public AttributeSelectionContentModel(IMRIService mriService, IMRIMetadataService mds, MRI[] selected,
			MRI[] expanded) {
		m_mriService = mriService;
		m_mds = mds;
		setSelectedAttributes(selected);
		if (expanded != null) {
			for (MRI attribute : expanded) {
				m_initialExpanded.add(attribute);
			}
		}
	}

	public void setSelectedAttributes(MRI[] selection) {
		m_selectedAttributes.clear();
		if (selection != null) {
			for (MRI selected : selection) {
				m_selectedAttributes.add(selected);
			}
		}
		fireSelectionChanged();
	}

	public MRI[] getSelectedAttributes() {
		return m_selectedAttributes.toArray(new MRI[m_selectedAttributes.size()]);
	}

	public MRI[] getInitialExpandedAttributes() {
		return m_initialExpanded.toArray(new MRI[m_initialExpanded.size()]);
	}

	public IMRIService getMRIService() {
		return m_mriService;
	}

	public Iterable<MRI> getAvailableAttributes() {
		return m_mriService.getMRIs();
	}

	public IMRIMetadataService getMetadataService() {
		return m_mds;
	}

	public IUnit getAttributeUnit(MRI attribute) {
		IUnit unit = m_alteredUnits.get(attribute);
		if (unit == null) {
			String unitString = m_mds.getMetadata(attribute).getUnitString();
			unit = UnitLookup.getUnitOrNull(unitString);
		}
		return unit;
	}

	public void setAttributeUnit(MRI attribute, IUnit unit) {
		m_alteredUnits.put(attribute, unit);
		fireSelectionChanged();
	}

	public void commitUnitChanges() {
		for (MRI attribute : m_selectedAttributes) {
			IUnit unit = m_alteredUnits.get(attribute);
			if (unit != null) {
				m_mds.setMetadata(attribute, IMRIMetadataProvider.KEY_UNIT_STRING, UnitLookup.getUnitIdentifier(unit));
			}
		}
	}

	public void addListener(IAttributeSelectionContentListener listener) {
		listeners.add(listener);
	}

	public void removeListener(IAttributeSelectionContentListener listener) {
		listeners.remove(listener);
	}

	private void fireSelectionChanged() {
		for (IAttributeSelectionContentListener listener : listeners) {
			listener.selectionChanged(this);
		}
	}

}
