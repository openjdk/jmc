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

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;

import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;

class AttributeMenuFactory {
	// FIXME: Only create the menu and actions once, and just update the checkedState when the menu is displayed?
	public static MenuManager attributeMenu(
		boolean includeNone, BiConsumer<IAttribute<?>, Boolean> onSelection,
		Supplier<Stream<? extends IAttribute<?>>> commonAttributes, String menuOption,
		ImageDescriptor imageDescriptor) {
		return attributeMenu(includeNone, () -> Boolean.TRUE, onSelection, commonAttributes, () -> Stream.empty(),
				menuOption, imageDescriptor, false, o -> Boolean.FALSE);
	}

	public static MenuManager attributeMenu(
		boolean includeNone, Supplier<Boolean> actionEnabled, BiConsumer<IAttribute<?>, Boolean> onSelection,
		Supplier<Stream<? extends IAttribute<?>>> commonAttributes, String menuOption) {
		return attributeMenu(includeNone, actionEnabled, onSelection, commonAttributes, () -> Stream.empty(),
				menuOption, null, false, o -> Boolean.FALSE);
	}

	public static MenuManager attributeMenu(
		boolean includeNone, Supplier<Boolean> actionEnabled, BiConsumer<IAttribute<?>, Boolean> onSelection,
		Supplier<Stream<? extends IAttribute<?>>> commonAttributes,
		Supplier<Stream<? extends IAttribute<?>>> uncommonAttributes, String menuOption) {
		return attributeMenu(includeNone, actionEnabled, onSelection, commonAttributes, uncommonAttributes, menuOption,
				null, false, o -> Boolean.FALSE);
	}

	public static MenuManager attributeMenu(
		boolean includeNone, Supplier<Boolean> actionEnabled, BiConsumer<IAttribute<?>, Boolean> onSelection,
		Supplier<Stream<? extends IAttribute<?>>> commonAttributes,
		Supplier<Stream<? extends IAttribute<?>>> uncommonAttributes, String menuOption, boolean checkAction,
		Function<Object, Boolean> checkedState) {
		return attributeMenu(includeNone, actionEnabled, onSelection, commonAttributes, uncommonAttributes, menuOption,
				null, checkAction, checkedState);
	}

	public static MenuManager attributeMenu(
		boolean includeNone, BiConsumer<IAttribute<?>, Boolean> onSelection,
		Supplier<Stream<? extends IAttribute<?>>> commonAttributes,
		Supplier<Stream<? extends IAttribute<?>>> uncommonAttributes, String menuOption, boolean checkAction,
		Function<Object, Boolean> checkedState) {
		return attributeMenu(includeNone, () -> Boolean.TRUE, onSelection, commonAttributes, uncommonAttributes,
				menuOption, null, checkAction, checkedState);
	}

	public static MenuManager attributeMenu(
		boolean includeNone, Supplier<Boolean> actionEnabled, BiConsumer<IAttribute<?>, Boolean> onSelection,
		Supplier<Stream<? extends IAttribute<?>>> commonAttributes,
		Supplier<Stream<? extends IAttribute<?>>> uncommonAttributes, String menuOption, ImageDescriptor image,
		boolean checkAction, Function<Object, Boolean> checkedState) {
		// FIXME: Send in a real id, and make sure other parts of the code, like HistogramSequence doesn't have to know about it.
		MenuManager gbMenu = new MenuManager(menuOption, image, menuOption);
		gbMenu.setRemoveAllWhenShown(true);
		gbMenu.addMenuListener(attributeMenuListener(includeNone, actionEnabled, onSelection, commonAttributes,
				uncommonAttributes, checkAction, checkedState));
		return gbMenu;
	}

	public static IMenuListener attributeMenuListener(
		boolean includeNone, BiConsumer<IAttribute<?>, Boolean> onSelection,
		Supplier<Stream<? extends IAttribute<?>>> commonAttributes) {
		return attributeMenuListener(includeNone, () -> Boolean.TRUE, onSelection, commonAttributes,
				() -> Stream.empty(), false, o -> Boolean.FALSE);
	}

	public static IMenuListener attributeMenuListener(
		boolean includeNone, Supplier<Boolean> actionEnabled, BiConsumer<IAttribute<?>, Boolean> onSelection,
		Supplier<Stream<? extends IAttribute<?>>> commonAttributes,
		Supplier<Stream<? extends IAttribute<?>>> uncommonAttributes, boolean checkAction,
		Function<Object, Boolean> checkedState) {
		return manager -> {
			if (actionEnabled.get()) {
				if (includeNone) {
					manager.add(new Action(Messages.TABLECOMPONENT_NONE) {
						@Override
						public void run() {
							onSelection.accept(null, true);
						}
					});
				}
				commonAttributes.get().map(getAttributeAction(onSelection, checkAction, checkedState))
						.forEach(manager::add);
				if (uncommonAttributes.get().findAny().isPresent()) {
					MenuManager uncommonMenu = new MenuManager(Messages.ATTRIBUTE_NOT_SHARED,
							Messages.ATTRIBUTE_NOT_SHARED);
					uncommonMenu.setRemoveAllWhenShown(true);
					uncommonMenu.addMenuListener(new IMenuListener() {
						@Override
						public void menuAboutToShow(IMenuManager innerManager) {
							uncommonAttributes.get().map(getAttributeAction(onSelection, checkAction, checkedState))
									.forEach(innerManager::add);
						}
					});
					manager.add(uncommonMenu);
				}
			}
		};
	}

	public static MenuManager aggregatorMenu(
		BiConsumer<IAggregator<IQuantity, ?>, Boolean> onAggregateSelection,
		TriConsumer<IAttribute<?>, IAggregator<IQuantity, ?>, Boolean> onAttributeSelection,
		TriConsumer<IType<?>, IAggregator<IQuantity, ?>, Boolean> onTypeSelection,
		Supplier<Stream<? extends IAttribute<?>>> commonAttributes,
		Supplier<Stream<? extends IAttribute<?>>> uncommonAttributes,
		List<Function<IAttribute<?>, IAggregator<IQuantity, ?>>> attributeAggregators, Supplier<Stream<IType<?>>> types,
		Function<IType<?>, IAggregator<IQuantity, ?>> typeAggregator, List<IAggregator<IQuantity, ?>> aggregators,
		String menuOption, boolean checkAction, Function<Object, Boolean> checkedState) {
		MCContextMenuManager aggregatorMenu = new MCContextMenuManager(menuOption, menuOption);

		aggregatorMenu.setRemoveAllWhenShown(true);
		aggregatorMenu.addMenuListener(new IMenuListener() {

			@Override
			public void menuAboutToShow(IMenuManager manager) {
				aggregators.stream().map(this::getAggregateAction).forEach(manager::add);
				types.get().map(type -> getTypeAggregateAction(type, typeAggregator)).forEach(manager::add);
				commonAttributes.get().map(getAttributeAggregatorMenu(onAttributeSelection, attributeAggregators,
						checkAction, checkedState)).forEach(manager::add);
				if (uncommonAttributes.get().findAny().isPresent()) {
					MenuManager uncommonMenu = new MenuManager(Messages.ATTRIBUTE_NOT_SHARED,
							Messages.ATTRIBUTE_NOT_SHARED);
					uncommonMenu.setRemoveAllWhenShown(true);
					uncommonMenu.addMenuListener(new IMenuListener() {

						@Override
						public void menuAboutToShow(IMenuManager innerManager) {
							uncommonAttributes.get().map(getAttributeAggregatorMenu(onAttributeSelection,
									attributeAggregators, checkAction, checkedState)).forEach(innerManager::add);
						}

					});
					manager.add(uncommonMenu);
				}

			}

			private Action getAggregateAction(IAggregator<IQuantity, ?> aggregator) {
				return new Action(aggregator.getName(), checkAction ? IAction.AS_CHECK_BOX : SWT.NONE) {
					{
						if (checkAction) {
							setChecked(checkedState.apply(aggregator));
						}
					}

					@Override
					public void run() {
						onAggregateSelection.accept(aggregator, checkAction ? isChecked() : true);
					}
				};
			}

			private Action getTypeAggregateAction(
				IType<?> type, Function<IType<?>, IAggregator<IQuantity, ?>> aggregatorSupplier) {
				IAggregator<IQuantity, ?> aggregator = aggregatorSupplier.apply(type);
				return new Action(aggregator.getName(), checkAction ? IAction.AS_CHECK_BOX : SWT.NONE) {
					{
						if (checkAction) {
							setChecked(checkedState.apply(new Pair<IType<?>, IAggregator<?, ?>>(type, aggregator)));
						}
					}

					@Override
					public void run() {
						onTypeSelection.accept(type, aggregator, checkAction ? isChecked() : true);
					}
				};
			}

		});
		return aggregatorMenu;
	}

	private static Function<IAttribute<?>, Action> getAttributeAction(
		BiConsumer<IAttribute<?>, Boolean> onSelection, boolean checkAction, Function<Object, Boolean> checkedState) {
		return new Function<IAttribute<?>, Action>() {

			@Override
			public Action apply(IAttribute<?> attribute) {
				return new Action(attribute.getName(), checkAction ? IAction.AS_CHECK_BOX : SWT.NONE) {
					{
						if (checkAction) {
							setChecked(checkedState.apply(attribute));
						}
					}

					@Override
					public void run() {
						onSelection.accept(attribute, checkAction ? isChecked() : true);
					}
				};
			}
		};
	}

	private static Function<IAttribute<?>, MenuManager> getAttributeAggregatorMenu(
		TriConsumer<IAttribute<?>, IAggregator<IQuantity, ?>, Boolean> onSelection,
		List<Function<IAttribute<?>, IAggregator<IQuantity, ?>>> attributeAggregators, boolean checkAction,
		Function<Object, Boolean> checkedState) {
		return new Function<IAttribute<?>, MenuManager>() {
			@Override
			public MenuManager apply(IAttribute<?> attribute) {
				MenuManager aggregatorMenu = new MenuManager(attribute.getName(), attribute.getName());

				aggregatorMenu.setRemoveAllWhenShown(true);
				aggregatorMenu.addMenuListener(new IMenuListener() {

					@Override
					public void menuAboutToShow(IMenuManager manager) {
						attributeAggregators.stream().map(this::attributeAggregatorMenu).forEach(manager::add);
					}

					Action attributeAggregatorMenu(
						Function<IAttribute<?>, IAggregator<IQuantity, ?>> aggregatorFunction) {
						IAggregator<IQuantity, ?> aggregator = aggregatorFunction.apply(attribute);
						return new AggregatorAction(aggregator, attribute, checkAction, checkedState, onSelection);
					}
				});
				return aggregatorMenu;
			}
		};
	}

	private static final class AggregatorAction extends Action {
		private final IAttribute<?> attribute;
		private final IAggregator<IQuantity, ?> aggregator;
		private final boolean checkAction;
		private final TriConsumer<IAttribute<?>, IAggregator<IQuantity, ?>, Boolean> onSelection;

		public AggregatorAction(IAggregator<IQuantity, ?> aggregator, IAttribute<?> attribute, boolean checkAction,
				Function<Object, Boolean> checkedState,
				TriConsumer<IAttribute<?>, IAggregator<IQuantity, ?>, Boolean> onSelection) {
			super(aggregator.getName(), checkAction ? IAction.AS_CHECK_BOX : SWT.NONE);
			this.aggregator = aggregator;
			this.attribute = attribute;
			this.checkAction = checkAction;
			this.onSelection = onSelection;
			if (checkAction) {
				setChecked(checkedState.apply(new Pair<IAttribute<?>, IAggregator<?, ?>>(attribute, aggregator)));
			}
		}

		@Override
		public void run() {
			onSelection.accept(attribute, aggregator, checkAction ? isChecked() : true);
		}
	}
}
