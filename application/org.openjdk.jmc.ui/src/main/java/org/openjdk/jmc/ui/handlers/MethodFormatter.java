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
package org.openjdk.jmc.ui.handlers;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IMemento;

import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.ui.misc.MementoToolkit;

public class MethodFormatter {
	// FIXME: Move the option part to a non-UI bundle?

	private static final String RETURN_VALUE = "returnValue"; //$NON-NLS-1$
	private static final String CLASS = "class"; //$NON-NLS-1$
	private static final String PARAMETER = "parameter"; //$NON-NLS-1$
	private static final int RETURN_VALUE_INDEX = 0;
	private static final int CLASS_INDEX = 1;
	private static final int PARAMETER_INDEX = 2;

	public enum Option {
		HIDDEN(Messages.MethodFormatter_OPTION_HIDDEN),
		CLASS(Messages.MethodFormatter_OPTION_CLASS),
		QUALIFIED(Messages.MethodFormatter_OPTION_CLASS_AND_PACKAGE);

		private final String localizedName;

		Option(String localizedName) {
			this.localizedName = localizedName;
		}
	}

	private class OptionAction extends Action {
		private final int index;
		private final Option value;

		OptionAction(int index, Option value) {
			super(value.localizedName, IAction.AS_RADIO_BUTTON);
			this.value = value;
			this.index = index;
		}

		@Override
		public void run() {
			options[index] = value;
			onUpdate.run();
		}
	}

	private final Option[] options = new Option[3];
	private final Runnable onUpdate;

	public MethodFormatter(IMemento memento, Runnable onUpdate) {
		IState state = MementoToolkit.asState(memento);
		options[RETURN_VALUE_INDEX] = StateToolkit.readEnum(state, RETURN_VALUE, Option.CLASS, Option.class);
		options[CLASS_INDEX] = StateToolkit.readEnum(state, CLASS, Option.QUALIFIED, Option.class);
		options[PARAMETER_INDEX] = StateToolkit.readEnum(state, PARAMETER, Option.CLASS, Option.class);
		this.onUpdate = onUpdate;
	}

	public MenuManager createMenu() {
		MenuManager menu = new MenuManager(Messages.MethodFormatter_FORMATTING_OPTIONS);
		// Return value menu
		MenuManager returnValueMenu = new MenuManager(Messages.MethodFormatter_RETURN_VALUE_MENU_TEXT);
		returnValueMenu.setRemoveAllWhenShown(true);
		returnValueMenu.addMenuListener(createMenuListener(RETURN_VALUE_INDEX));
		menu.add(returnValueMenu);

		// Class menu
		MenuManager classMenu = new MenuManager(Messages.MethodFormatter_CLASS_MENU_TEXT);
		classMenu.setRemoveAllWhenShown(true);
		classMenu.addMenuListener(createMenuListener(CLASS_INDEX));
		menu.add(classMenu);

		// Parameter menu
		MenuManager parameterMenu = new MenuManager(Messages.MethodFormatter_PARAMETERS_MENU_TEXT);
		parameterMenu.setRemoveAllWhenShown(true);
		parameterMenu.addMenuListener(createMenuListener(PARAMETER_INDEX));
		menu.add(parameterMenu);
		return menu;
	}

	private IMenuListener createMenuListener(final int optionIndex) {
		return new IMenuListener() {

			@Override
			public void menuAboutToShow(IMenuManager manager) {
				Option selected = options[optionIndex];
				for (Option o : Option.values()) {
					OptionAction a = new OptionAction(optionIndex, o);
					a.setChecked(selected == o);
					manager.add(a);
				}
			}
		};

	}

	private boolean show(int optionIndex) {
		return options[optionIndex] != Option.HIDDEN;
	}

	private boolean qualified(int optionIndex) {
		return options[optionIndex] == Option.QUALIFIED;
	}

	public String format(IMCMethod method) {
		return FormatToolkit.getHumanReadable(method, showReturnValue(), showReturnValuePackage(), showClassName(),
				showClassPackageName(), showArguments(), showArgumentsPackage());
	}

	public String format(IMCType type) {
		return FormatToolkit.getType(type, showClassPackageName());
	}

	public boolean showReturnValue() {
		return show(RETURN_VALUE_INDEX);
	}

	public boolean showReturnValuePackage() {
		return qualified(RETURN_VALUE_INDEX);
	}

	public boolean showClassName() {
		return show(CLASS_INDEX);
	}

	public boolean showClassPackageName() {
		return qualified(CLASS_INDEX);
	}

	public boolean showArguments() {
		return show(PARAMETER_INDEX);
	}

	public boolean showArgumentsPackage() {
		return qualified(PARAMETER_INDEX);
	}

	public void saveState(IMemento state) {
		state.putString(RETURN_VALUE, options[RETURN_VALUE_INDEX].name());
		state.putString(CLASS, options[CLASS_INDEX].name());
		state.putString(PARAMETER, options[PARAMETER_INDEX].name());
	}
}
