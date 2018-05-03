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
package org.openjdk.jmc.ui.misc;

import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;

public abstract class BackgroundFractionDrawer implements Listener {
	private static final Color BG = SWTColorToolkit.getColor(new RGB(200, 100, 100));

	protected void draw(double fraction, Event event) {
		if (fraction > 0 && fraction <= 1) {
			event.gc.setBackground(BG);
			event.gc.fillRectangle(event.x, event.y, (int) (event.width * fraction), event.height);
			event.detail &= ~SWT.BACKGROUND;
		}
	}

	public static <T> Listener unchecked(Function<T, IQuantity> func) {
		return new BackgroundFractionDrawer() {

			@SuppressWarnings("unchecked")
			@Override
			public void handleEvent(Event event) {
				IQuantity q = func.apply((T) event.item.getData());
				if (q != null) {
					if (q.getType() == UnitLookup.NUMBER) {
						draw(q.doubleValueIn(UnitLookup.NUMBER_UNITY), event);
					} else if (q.getType() == UnitLookup.PERCENTAGE) {
						draw(q.doubleValueIn(UnitLookup.PERCENT_UNITY), event);
					}
				}
			};

		};
	}

	public static <T> Listener unchecked(ToDoubleFunction<T> func) {
		return new BackgroundFractionDrawer() {

			@SuppressWarnings("unchecked")
			@Override
			public void handleEvent(Event event) {
				draw(func.applyAsDouble((T) event.item.getData()), event);
			};

		};
	}
}
