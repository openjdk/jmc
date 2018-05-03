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
package org.openjdk.jmc.flightrecorder.ui.selection;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;

public class StacktraceFrameSelection extends FlavoredSelectionBase {

	private IMCFrame frame;
	private IItemCollection selectedItems;

	public StacktraceFrameSelection(IMCFrame frame, IItemCollection selectedItems, String name) {
		super(name);
		this.frame = frame;
		this.selectedItems = selectedItems;
	}

	@Override
	public Stream<IItemStreamFlavor> getFlavors(
		IItemFilter filter, IItemCollection items, List<IAttribute<?>> attributes) {
		Builder<IItemStreamFlavor> builder = Stream.builder();
		builder.add(IItemStreamFlavor.build(MessageFormat.format(Messages.FLAVOR_SELECTED_EVENTS,
				ItemCollectionToolkit.getDescription(selectedItems)), selectedItems));
		builder.add(buildContainsMethodFlavor(frame.getMethod(), items));

		// FIXME: What more useful flavors can we create, can we use the frame, and how to we get the distinguish by option?
//		builder.add(IPropertyFlavor.build(JdkAttributes.STACK_TRACE_TOP_METHOD, frame.getMethod(), selectedItems));
		return builder.build();
	}

	private static IFilterFlavor buildContainsMethodFlavor(IMCMethod method, IItemCollection items) {
		return new IFilterFlavor() {

			@Override
			public String getName() {
				return MessageFormat.format(Messages.FLAVOR_CONTAINS, JdkAttributes.STACK_TRACE_STRING.getName(),
						FormatToolkit.getHumanReadable(method));
			}

			@Override
			public IItemCollection evaluate() {
				return items.apply(getFilter());
			}

			@Override
			public IItemFilter getFilter() {
				return ItemFilters.contains(JdkAttributes.STACK_TRACE_STRING, FormatToolkit.getHumanReadable(method));
			}
		};
	}
}
