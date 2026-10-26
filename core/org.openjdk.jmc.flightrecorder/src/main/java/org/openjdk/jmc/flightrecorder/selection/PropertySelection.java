/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.flightrecorder.selection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

/**
 * Selection of an attribute and its value, including aspects related through shared properties.
 */
public final class PropertySelection extends FlavoredSelectionBase {
	private final IAttribute<?> attribute;
	private final Object value;

	public PropertySelection(IAttribute<?> attribute, Object value, String name) {
		super(name);
		this.attribute = Objects.requireNonNull(attribute, "attribute"); //$NON-NLS-1$
		if (value instanceof Set<?>) {
			this.value = Collections.unmodifiableSet(new LinkedHashSet<>((Set<?>) value));
		} else if (value instanceof Collection<?>) {
			this.value = Collections.unmodifiableList(new ArrayList<>((Collection<?>) value));
		} else {
			this.value = value;
		}
	}

	public IAttribute<?> getAttribute() {
		return attribute;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public Stream<IItemStreamFlavor> getFlavors(
		IItemFilter dstFilter, IItemCollection items, List<IAttribute<?>> dstAttributes) {
		/*
		 * FIXME: Is this the desired behavior? Discuss and change if necessary.
		 *
		 * This most likely need more thought and discussion, but the implemented order of flavors
		 * is currently:
		 *
		 * For chart selections:
		 *
		 * 1: The selected events if any of them appear on the destination page
		 *
		 * 2: All events on the destination page in the selected range (if a range was selected)
		 *
		 * 3-n: All events on the destination page filtered on any of the attributes common to all
		 * selected events (excluding the range attribute if a range was selected)
		 *
		 * For histogram and list selections:
		 *
		 * 1: The selected events if any of them appear on the destination page
		 *
		 * 2-n: All events on the destination page filtered on any of the attributes common to all
		 * selected events (all will at least have (endTime))
		 *
		 * For properties view selections:
		 *
		 * 1: All events on the destination page filtered on the selected attribute:value if they
		 * all have the selected attribute
		 *
		 * 2: All events on the destination page filtered on the selected value if they all have an
		 * attribute with the same content type
		 *
		 * 3: All events on the destination page filtered on all common attributes with values from
		 * all events filtered on the selected attribute:value (see example)
		 *
		 * 4: All events on the destination page filtered on the selected attribute:value if there
		 * are any events with the attribute (and the attribute is not common to all events in which
		 * case this flavor has already been added in (1))
		 *
		 * Example of properties view selections (3):
		 *
		 * ECID:1-2-3-4 was selected and the user navigates to Java Application. All events on Java
		 * Application share (thread) and (endTime), so all events on the page are filtered on those
		 * properties. The values to include are collected from all events with the ECID attribute
		 * having value 1-2-3-4. The threads will be put in a set, the timestamps will form a range.
		 */
		IItemCollection filteredDstItems = ItemCollectionToolkit.filterIfNotNull(items, dstFilter);
		IPropertyFlavor relatedFilterFlavor = IPropertyFlavor.build(attribute, value, filteredDstItems);
		LinkedList<IItemStreamFlavor> flavors = new LinkedList<>();

		boolean anyRelatedOnDst = relatedFilterFlavor.evaluate().hasItems();
		IPropertyFlavor selectedPropertyFlavor = IPropertyFlavor.build(attribute, value, items);
		if (anyRelatedOnDst) {
			// prio1(a): Items related to the selected attribute if there are any
			flavors.add(selectedPropertyFlavor);
			selectedPropertyFlavor = null;
		}
		IItemCollection itemsRelatedToSelection = items.apply(relatedFilterFlavor.getFilter());
		if (dstAttributes == null || dstAttributes.isEmpty()) {
			dstAttributes = SelectionAspectToolkit.getCommonAttributes(filteredDstItems.iterator())
					.collect(Collectors.toList());
		}
		Iterator<IAttribute<?>> commonDstAttr = new LinkedHashSet<>(dstAttributes).iterator();
		List<IPropertyFlavor> relatedProperties = new ArrayList<>();
		while (commonDstAttr.hasNext()) {
			IAttribute<?> dstAttribute = commonDstAttr.next();
			if (!dstAttribute.equals(JfrAttributes.EVENT_TYPE)
					&& (!(dstAttribute.getContentType() instanceof KindOfQuantity)
							|| dstAttribute.equals(JfrAttributes.END_TIME))) {
				// FIXME: Collect type or quantity values?
				if (dstAttribute.equals(attribute)) {
					if (!anyRelatedOnDst && selectedPropertyFlavor != null) {
						// prio1(b): Related to the selected attribute even though it's empty, since the attribute is shared by all
						flavors.push(selectedPropertyFlavor);
						selectedPropertyFlavor = null;
					}
					relatedProperties = null;
				} else if (!dstAttribute.equals(attribute)
						&& dstAttribute.getContentType().equals(attribute.getContentType())) {
					// prio2: Destination items with an attribute of the selected content type and which equals the selected value
					IPropertyFlavor contentTypeFlavor = IPropertyFlavor.build(dstAttribute, value, items);
					flavors.add(IPropertyFlavor.withIdentifier("content-type:" + contentTypeFlavor.getIdentifier(), //$NON-NLS-1$
							contentTypeFlavor));
				}
				if (relatedProperties != null) {
					// Collect values from items related to selection (only items of types that has the attribute), and add as filter
					SelectionAspectToolkit.PropertySummary av = SelectionAspectToolkit.summarizeProperty(dstAttribute,
							itemsRelatedToSelection.stream()
									.filter(is -> dstAttribute.getAccessor(is.getType()) != null).iterator(),
							Integer.MAX_VALUE);
					if (av != null) {
						IPropertyFlavor relatedPropertyFlavor = IPropertyFlavor.build(av.attribute, av.value, items);
						relatedProperties.add(IPropertyFlavor.withIdentifier(
								"related:" + relatedPropertyFlavor.getIdentifier(), relatedPropertyFlavor)); //$NON-NLS-1$
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
