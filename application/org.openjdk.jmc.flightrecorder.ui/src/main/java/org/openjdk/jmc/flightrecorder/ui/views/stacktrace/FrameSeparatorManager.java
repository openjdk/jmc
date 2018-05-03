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
package org.openjdk.jmc.flightrecorder.ui.views.stacktrace;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;

import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;

class FrameSeparatorManager implements IMenuListener {
	private final Runnable onChangeCallback;
	private FrameSeparator current;

	FrameSeparatorManager(Runnable onChangeCallback, FrameSeparator initial) {
		this.onChangeCallback = onChangeCallback;
		this.current = initial;
	}

	FrameSeparator getFrameSeparator() {
		return current;
	}

	MenuManager createMenu() {
		MenuManager menu = new MenuManager(Messages.STACKTRACE_VIEW_DISTINGUISH_FRAMES_BY);
		menu.setRemoveAllWhenShown(true);
		menu.addMenuListener(this);
		return menu;
	}

	@Override
	public void menuAboutToShow(IMenuManager manager) {
		Action typeAction = new Action(Messages.STACKTRACE_VIEW_OPTIMIZATION_TYPE, IAction.AS_CHECK_BOX) {

			@Override
			public void run() {
				current = new FrameSeparator(current.getCategorization(), isChecked());
				onChangeCallback.run();
			}
		};
		typeAction.setChecked(current.isDistinguishFramesByOptimization());
		manager.add(typeAction);
		manager.add(new Separator());
		for (FrameCategorization l : FrameCategorization.values()) {
			Action levelOption = new Action(l.getLocalizedName(), IAction.AS_RADIO_BUTTON) {

				@Override
				public void run() {
					if (isChecked() && current.getCategorization() != l) {
						current = new FrameSeparator(l, current.isDistinguishFramesByOptimization());
						onChangeCallback.run();
					}
				}
			};
			levelOption.setChecked(current.getCategorization() == l);
			manager.add(levelOption);
		}
	}
}
