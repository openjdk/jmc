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

import java.util.function.Consumer;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.HistogramSelection;

public class ItemHistogramWithInput {
	private final ItemHistogram histogram;
	private IItemCollection input = ItemCollectionToolkit.EMPTY;

	public ItemHistogramWithInput(ItemHistogram histogram) {
		this.histogram = histogram;
	}

	public void setInput(IItemCollection items) {
		this.input = items;
		ColumnViewer viewer = histogram.getManager().getViewer();
		ISelection prevSelection = viewer.getSelection();
		histogram.show(items);
		if (viewer.getSelection().equals(prevSelection)) {
			// Send selection changed event even when the selection has not changed to trigger child.setInput
			viewer.setSelection(prevSelection);
		}
	}

	public void addListener(Consumer<? super IItemCollection> listener) {
		histogram.getManager().getViewer().addSelectionChangedListener(e -> listener.accept(getItems()));
	}

	public IItemCollection getItems() {
		HistogramSelection selection = histogram.getSelection();
		return selection.getRowCount() == 0 ? input : selection.getItems();
	}

	public static Consumer<IItemCollection> chain(
		ItemHistogram source, Consumer<? super IItemCollection> destination, ItemHistogram ... intermediate) {
		ItemHistogramWithInput top = new ItemHistogramWithInput(source);
		ItemHistogramWithInput prev = top;
		for (ItemHistogram ih : intermediate) {
			ItemHistogramWithInput current = new ItemHistogramWithInput(ih);
			prev.addListener(current::setInput);
			prev = current;
		}
		prev.addListener(destination);
		return top::setInput;
	}
}
