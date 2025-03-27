/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2022, 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.ui.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

public class AttributeSelection extends Action implements IMenuCreator {
	public static final String SAMPLES = "Samples"; //$NON-NLS-1$
	public static final String ATTRIBUTE_SELECTION_ID = "AttributeSelection"; //$NON-NLS-1$
	public static final String ATTRIBUTE_SELECTION_SEP_ID = "AttrSelectionSep"; //$NON-NLS-1$

	private Menu menu;
	private final Collection<Pair<String, IAttribute<IQuantity>>> items;
	private final Supplier<IAttribute<IQuantity>> getCurrentAttr;
	private final Consumer<IAttribute<IQuantity>> setCurrentAttr;
	private final Runnable onSet;

	public static List<Pair<String, IAttribute<IQuantity>>> extractAttributes(IItemCollection items) {
		Set<Pair<String, IAttribute<IQuantity>>> compatibleAttr = new HashSet<>();
		for (IItemIterable eventIterable : items) {
			List<IAttribute<?>> attributes = eventIterable.getType().getAttributes();
			if (eventIterable.getType().hasAttribute(JfrAttributes.EVENT_STACKTRACE)) {
				for (IAttribute<?> attr : attributes) {
					ContentType<?> contentType = attr.getContentType();
					if (contentType == UnitLookup.NUMBER || contentType == UnitLookup.MEMORY
							|| contentType == UnitLookup.TIMESPAN) {
						compatibleAttr.add(new Pair<>(attr.getName(), (IAttribute<IQuantity>) attr));
					}
				}
			}
		}
		List<Pair<String, IAttribute<IQuantity>>> sortedList = new ArrayList<>(compatibleAttr);
		sortedList.sort(Comparator.comparing(p -> p.left));
		sortedList.add(0, new Pair<>(SAMPLES, null));
		sortedList.add(1, new Pair<>("---", null));
		return sortedList;
	}

	public AttributeSelection(Collection<Pair<String, IAttribute<IQuantity>>> items, String attrName,
			Supplier<IAttribute<IQuantity>> getCurrentAttr, Consumer<IAttribute<IQuantity>> setCurrentAttr,
			Runnable onSet) {
		super(attrName != null ? attrName : SAMPLES, IAction.AS_DROP_DOWN_MENU);
		setId(ATTRIBUTE_SELECTION_ID);
		this.items = items;
		this.getCurrentAttr = getCurrentAttr;
		this.setCurrentAttr = setCurrentAttr;
		this.onSet = onSet;
		setMenuCreator(this);
	}

	@Override
	public void dispose() {
		// do nothing
	}

	@Override
	public Menu getMenu(Control parent) {
		if (menu == null) {
			menu = new Menu(parent);
			populate(items);
		}
		return menu;
	}

	@Override
	public Menu getMenu(Menu parent) {
		if (menu == null) {
			menu = new Menu(parent);
			populate(items);
		}
		return menu;
	}

	private void populate(Collection<Pair<String, IAttribute<IQuantity>>> attributes) {
		for (Pair<String, IAttribute<IQuantity>> item : attributes) {
			if (item.left.equals("---")) {
				(new Separator()).fill(menu, 1);
			} else {
				ActionContributionItem actionItem = new ActionContributionItem(
						new SetAttribute(item, item.right == getCurrentAttr.get()));
				actionItem.fill(menu, -1);
			}
		}
	}

	private class SetAttribute extends Action {
		private IAttribute<IQuantity> value;

		SetAttribute(Pair<String, IAttribute<IQuantity>> item, boolean isSelected) {
			super(item.left, IAction.AS_RADIO_BUTTON);
			this.value = item.right;
			setChecked(isSelected);
		}

		@Override
		public void run() {
			if (value != getCurrentAttr.get()) {
				setCurrentAttr.accept(value);
				onSet.run();
			}
		}
	}

}
