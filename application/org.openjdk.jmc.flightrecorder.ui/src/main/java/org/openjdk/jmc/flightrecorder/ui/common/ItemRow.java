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

import java.awt.Graphics2D;
import java.awt.Point;
import java.util.stream.Stream;

import org.openjdk.jmc.common.item.ICanonicalAccessorFactory;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.RangeMatchPolicy;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.ui.charts.IChartInfoVisitor;
import org.openjdk.jmc.ui.charts.IRenderedRow;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.RenderedRowBase;
import org.openjdk.jmc.ui.charts.SubdividedQuantityRange;
import org.openjdk.jmc.ui.charts.XYChart;

public class ItemRow implements IXDataRenderer {
	private final IItemCollection items;
	private final IXDataRenderer renderer;
	private final String name;
	private final String description;

	public ItemRow(IXDataRenderer renderer, IItemCollection items) {
		this(null, null, renderer, items);
	}

	public ItemRow(String name, String description, IXDataRenderer renderer, IItemCollection items) {
		this.name = name;
		this.description = description;
		this.items = items;
		this.renderer = renderer;
	}

	public static IItemCollection getSelection(XYChart chart) {
		Object[] selectedRows = chart.getSelectedRows();
		if (selectedRows.length == 0) {
			return ItemCollectionToolkit.EMPTY;
		} else {
			return ItemCollectionToolkit.merge(() -> Stream.of(selectedRows).filter(row -> row instanceof ItemRow)
					.map(row -> ((ItemRow) row).items));
		}
	}

	public static IItemCollection getSelection(XYChart chart, ICanonicalAccessorFactory<IQuantity> attribute) {
		IQuantity start = chart.getSelectionStart();
		IQuantity end = chart.getSelectionEnd();
		if (start != null && end != null) {
			IItemFilter rangeFilter = ItemFilters.interval(attribute, start, true, end, true);
			return getSelection(chart).apply(rangeFilter);
		} else {
			return getSelection(chart);
		}
	}

	public static IItemCollection getRangeSelection(
		XYChart chart, ICanonicalAccessorFactory<IRange<IQuantity>> attribute) {
		IRange<IQuantity> range = chart.getSelectionRange();
		if (range != null) {
			IItemFilter rangeFilter = ItemFilters.matchRange(RangeMatchPolicy.CONTAINED_IN_CLOSED, attribute, range);
			return getSelection(chart).apply(rangeFilter);
		} else {
			return getSelection(chart);
		}
	}

	@Override
	public IRenderedRow render(Graphics2D context, SubdividedQuantityRange xRange, int height) {
		IRenderedRow render = renderer.render(context, xRange, height);
		return new RenderedRowBase(render.getNestedRows(), height, name == null ? render.getName() : name,
				description == null ? render.getDescription() : description, this) {

			@Override
			public void infoAt(IChartInfoVisitor visitor, int x, int y, Point offset) {
				render.infoAt(visitor, x, y, offset);
			}
		};
	}
}
