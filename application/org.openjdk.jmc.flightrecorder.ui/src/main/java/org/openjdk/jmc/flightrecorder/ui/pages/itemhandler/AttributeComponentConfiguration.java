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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList;

/**
 * Holds type/attribute collection variants that can affect the configuration of a component.
 */
class AttributeComponentConfiguration {
	private final Map<String, IType<?>> allTypes;
	private final Map<String, IAttribute<?>> allAttributes;
	private final Map<String, IAttribute<?>> commonAttributes;
	private final Map<String, IAttribute<IQuantity>> commonChartableAttributes;
	private final Map<String, IAttribute<?>> uncommonAttributes;
	private final Map<String, IAttribute<IQuantity>> uncommonChartableAttributes;
	private final Map<String, IAttribute<IQuantity>> lineChartableAttributes;

	/**
	 * Creates a new configuration and populates it from the items.
	 *
	 * @param items
	 *            Items to take types and attributes from.
	 */
	public AttributeComponentConfiguration(IItemCollection items) {
		allTypes = new HashMap<>();
		allAttributes = new HashMap<>();
		commonAttributes = new HashMap<>();
		commonChartableAttributes = new HashMap<>();
		uncommonAttributes = new HashMap<>();
		uncommonChartableAttributes = new HashMap<>();
		lineChartableAttributes = new HashMap<>();
		forEachType(items);
		populateAttributeMaps(isSuitableForLineCharts(items, allTypes));
	}

	private void forEachType(IItemCollection items) {
		if (items != null) {
			ItemCollectionToolkit.stream(items).map(IItemIterable::getType)
					.forEach(type -> {
						allTypes.put(type.getIdentifier(), type);
						for (IAttribute<?> a : type.getAttributes()) {
							if (!a.equals(JfrAttributes.EVENT_STACKTRACE)) {
								allAttributes.put(ItemList.getColumnId(a), a);
							}
						}
					});
		}
	}

	private void populateAttributeMaps(boolean allowLineCharts) {
		for (Entry<String, IAttribute<?>> a : allAttributes.entrySet()) {
			if (!commonAttributes.containsKey(a.getKey()) && !uncommonAttributes.containsKey(a.getKey())
					&& allTypes.values().stream().allMatch(t -> {
						return t.getAttributes().contains(a.getValue());
					})) {
				commonAttributes.put(a.getKey(), a.getValue());
				if (a.getValue().getContentType() instanceof LinearKindOfQuantity) {
					@SuppressWarnings("unchecked")
					IAttribute<IQuantity> qa = (IAttribute<IQuantity>) a.getValue();
					commonChartableAttributes.put(a.getKey(), qa);
					if (allowLineCharts) {
						lineChartableAttributes.put(a.getKey(), qa);
					}
				}
			} else {
				uncommonAttributes.put(a.getKey(), a.getValue());
				if (a.getValue().getContentType() instanceof LinearKindOfQuantity) {
					@SuppressWarnings("unchecked")
					IAttribute<IQuantity> qa = (IAttribute<IQuantity>) a.getValue();
					uncommonChartableAttributes.put(a.getKey(), qa);
				}
			}
		}
	}

	private static boolean isSuitableForLineCharts(IItemCollection items, Map<String, IType<?>> types) {
		// NOTE: JMC-4520 - Only allowing line charts for one event type, which only has one event array. 
		if (types.values().size() == 1) {
			Iterator<IItemIterable> iterator = items.iterator();
			if (iterator.hasNext()) {
				iterator.next();
				return !iterator.hasNext();
			}
		}
		return false;
	}

	public Map<String, IAttribute<?>> getAllAttributes() {
		return allAttributes;
	}

	public Map<String, IAttribute<?>> getCommonAttributes() {
		return commonAttributes;
	}

	public Map<String, IAttribute<IQuantity>> getCommonChartableAttributes() {
		return commonChartableAttributes;
	}

	public Map<String, IAttribute<IQuantity>> getUncommonChartableAttributes() {
		return uncommonChartableAttributes;
	}

	public Map<String, IAttribute<IQuantity>> getLineChartableAttributes() {
		return lineChartableAttributes;
	}

	public Map<String, IType<?>> getAllTypes() {
		return allTypes;
	}

	public Map<String, IAttribute<?>> getUncommonAttributes() {
		return uncommonAttributes;
	}
}
