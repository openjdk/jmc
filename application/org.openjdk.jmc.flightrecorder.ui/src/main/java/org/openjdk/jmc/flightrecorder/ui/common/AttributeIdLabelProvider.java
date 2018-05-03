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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;

/**
 * Default {@link ColumnLabelProvider} using {@link IAttribute} identifiers as keys to obtain text
 * and image. Use directly instead of subclassing {@link ColumnLabelProvider}. Only need to subclass
 * if other keys are used in addition to {@link IAttribute} identifiers.
 */
public class AttributeIdLabelProvider extends ColumnLabelProvider {
	private final Map<String, IDescribable> describables;

	protected AttributeIdLabelProvider(IAttribute<?> ... attributes) {
		describables = new HashMap<>(attributes.length);
		for (IAttribute<?> attribute : attributes) {
			// NOTE: Use whatever mechanism we devise to obtain the IDescribable here instead.
			describables.put(attribute.getIdentifier(), attribute);
		}
	}

	@Override
	public String getText(Object element) {
		IDescribable describable = describables.get(element);
		return (describable != null) ? describable.getName() : super.getText(element);
	}

	@Override
	public String getToolTipText(Object element) {
		IDescribable describable = describables.get(element);
		return (describable != null) ? describable.getDescription() : super.getToolTipText(element);
	}

	@Override
	public Image getImage(Object element) {
		return SWTColorToolkit
				.getColorThumbnail(SWTColorToolkit.asRGB(DataPageToolkit.getFieldColor((String) element)));
	}
}
