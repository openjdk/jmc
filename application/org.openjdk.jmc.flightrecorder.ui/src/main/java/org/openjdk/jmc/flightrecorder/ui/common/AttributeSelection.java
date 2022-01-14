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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.Pair;

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
			for (IAttribute<?> attr : attributes) {
				ContentType<?> contentType = attr.getContentType();
				if (contentType == UnitLookup.NUMBER || contentType == UnitLookup.MEMORY) {
					compatibleAttr.add(new Pair<>(attr.getName(), (IAttribute<IQuantity>) attr));
				}
			}
		}
		List<Pair<String, IAttribute<IQuantity>>> sortedList = new ArrayList<>(compatibleAttr);
		sortedList.add(new Pair<>(SAMPLES, null));
		sortedList.sort(Comparator.comparing(p -> p.left));
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
			ActionContributionItem actionItem = new ActionContributionItem(
					new SetAttribute(item, item.right == getCurrentAttr.get()));
			actionItem.fill(menu, -1);
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
				//triggerRebuildTask(currentItems);
			}
		}
	}

}
