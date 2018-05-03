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

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.openjdk.jmc.common.item.IMemberAccessor;

public class DelegatingLabelProvider extends ColumnLabelProvider {

	private final IMemberAccessor<?, Object> cellAccessor;
	private final ColumnLabelProvider cellLabelProvider;

	public DelegatingLabelProvider(ColumnLabelProvider cellLabelProvider, IMemberAccessor<?, Object> cellAccessor) {
		this.cellLabelProvider = cellLabelProvider;
		this.cellAccessor = cellAccessor;
	}

	@Override
	public Font getFont(Object row) {
		return cellLabelProvider.getFont(cellAccessor.getMember(row));
	}

	@Override
	public Color getBackground(Object row) {
		return cellLabelProvider.getBackground(cellAccessor.getMember(row));
	}

	@Override
	public Color getForeground(Object row) {
		return cellLabelProvider.getForeground(cellAccessor.getMember(row));
	}

	@Override
	public Image getImage(Object row) {
		return cellLabelProvider.getImage(cellAccessor.getMember(row));
	}

	@Override
	public String getText(Object row) {
		return cellLabelProvider.getText(cellAccessor.getMember(row));
	}

	@Override
	public Image getToolTipImage(Object row) {
		return cellLabelProvider.getToolTipImage(cellAccessor.getMember(row));
	}

	@Override
	public String getToolTipText(Object row) {
		return cellLabelProvider.getToolTipText(cellAccessor.getMember(row));
	}

	@Override
	public Color getToolTipBackgroundColor(Object row) {
		return cellLabelProvider.getToolTipBackgroundColor(cellAccessor.getMember(row));
	}

	@Override
	public Color getToolTipForegroundColor(Object row) {
		return cellLabelProvider.getToolTipForegroundColor(cellAccessor.getMember(row));
	}

	@Override
	public Font getToolTipFont(Object row) {
		return cellLabelProvider.getToolTipFont(cellAccessor.getMember(row));
	}

	@Override
	public Point getToolTipShift(Object row) {
		return cellLabelProvider.getToolTipShift(cellAccessor.getMember(row));
	}

	@Override
	public boolean useNativeToolTip(Object row) {
		return cellLabelProvider.useNativeToolTip(cellAccessor.getMember(row));
	}

	@Override
	public int getToolTipTimeDisplayed(Object row) {
		return cellLabelProvider.getToolTipTimeDisplayed(cellAccessor.getMember(row));
	}

	@Override
	public int getToolTipDisplayDelayTime(Object row) {
		return cellLabelProvider.getToolTipDisplayDelayTime(cellAccessor.getMember(row));
	}

	@Override
	public int getToolTipStyle(Object row) {
		return cellLabelProvider.getToolTipStyle(cellAccessor.getMember(row));
	}

	@SuppressWarnings("unchecked")
	public static <V> ColumnLabelProvider build(
		ColumnLabelProvider cellLabelProvider, IMemberAccessor<?, V> cellAccessor) {
		return new DelegatingLabelProvider(cellLabelProvider, (IMemberAccessor<?, Object>) cellAccessor);
	}

	@SuppressWarnings("unchecked")
	public static <V> ColumnLabelProvider build(IMemberAccessor<String, V> textProvider) {
		return new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return textProvider.getMember((V) element);
			}
		};
	}

}
