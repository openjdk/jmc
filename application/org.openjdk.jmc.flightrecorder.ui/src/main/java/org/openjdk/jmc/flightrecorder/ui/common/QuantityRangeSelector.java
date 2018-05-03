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
package org.openjdk.jmc.flightrecorder.ui.common;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.function.Consumer;

import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;

public class QuantityRangeSelector extends JComponentNavigator {
	private final Color RED = Color.getHSBColor(0.0f, 0.9f, 0.9f);
	private final Color ORANGE = Color.getHSBColor(0.1f, 0.9f, 0.8f);
	private final DefaultToolTip tt;
	private final Consumer<IRange<IQuantity>>[] onChange;

	@SafeVarargs
	public QuantityRangeSelector(Composite parent, FormToolkit toolkit, IRange<IQuantity> fullRange,
			Consumer<IRange<IQuantity>> ... onChange) {
		super(parent, toolkit);
		this.onChange = onChange;
		tt = new DefaultToolTip(this);
		setNavigatorRange(fullRange);
	}

	@Override
	protected void renderBackdrop(Graphics2D g2d, int w, int h) {
		g2d.setPaint(new GradientPaint(0, 0, RED, 0, h, ORANGE, false));
		g2d.fillRect(0, 0, w, h);
	}

	@Override
	protected void onRangeChange() {
		IRange<IQuantity> range = getCurrentRange();
		String text = range.getStart().displayUsing(IDisplayable.AUTO) + " - ( " //$NON-NLS-1$
				+ range.getExtent().displayUsing(IDisplayable.AUTO) + " ) - " //$NON-NLS-1$
				+ range.getEnd().displayUsing(IDisplayable.AUTO);
		tt.setText(text);
		Arrays.stream(onChange).forEach(l -> l.accept(range));
	}
}
