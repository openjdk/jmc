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
package org.openjdk.jmc.ui.column;

import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;

import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.ui.handlers.CopySelectionAction;
import org.openjdk.jmc.ui.misc.CopySettings;
import org.openjdk.jmc.ui.misc.FormatToolkit;

class CopyColumnsAction implements Function<IStructuredSelection, Stream<String>> {

	private final Function<Boolean, Stream<? extends IColumn>> columns;

	CopyColumnsAction(Function<Boolean, Stream<? extends IColumn>> columns) {
		this.columns = columns;
	}

	static IAction build(StructuredViewer viewer, Function<Boolean, Stream<? extends IColumn>> columns) {
		return new CopySelectionAction(viewer, new CopyColumnsAction(columns));
	}

	@Override
	public Stream<String> apply(IStructuredSelection selection) {
		boolean shouldCopyOnlyVisible = CopySettings.getInstance().shouldCopyOnlyVisible();
		boolean raw = CopySettings.getInstance().shouldCopyAsRawData();
		Function<Stream<String>, String> rowFormatter = FormatToolkit.getPreferredRowFormatter();
		Function<Object, String> objectFormatter = o -> rowFormatter
				.apply(columns.apply(shouldCopyOnlyVisible).map(c -> formatObject(o, c, raw)));
		Stream<String> strings = FormatToolkit.formatSelection(selection, objectFormatter);
		if (CopySettings.getInstance().shouldCopyColumnHeaders()) {
			return Stream.concat(
					Stream.of(rowFormatter.apply(columns.apply(shouldCopyOnlyVisible).map(IColumn::getName))), strings);
		} else {
			return strings;
		}
	}

	private static String formatObject(Object o, IColumn column, boolean raw) {
		IMemberAccessor<?, Object> cellAccessor = column.getCellAccessor();
		if (raw && cellAccessor != null) {
			Object cell = cellAccessor.getMember(o);
			if (cell instanceof IQuantity) {
				return ((IQuantity) cell).persistableString();
			} else {
				return cell == null ? "" : TypeHandling.getValueString(cell); //$NON-NLS-1$
			}
		}
		return column.getLabelProvider().getText(o);
	}

}
