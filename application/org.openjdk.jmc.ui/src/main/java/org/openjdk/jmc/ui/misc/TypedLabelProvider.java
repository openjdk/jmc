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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

public class TypedLabelProvider<T> extends ColumnLabelProvider {

	private final Class<T> elementClass;

	public TypedLabelProvider(Class<T> elementClass) {
		this.elementClass = elementClass;
	}

	private T getTyped(Object element) {
		return elementClass.isInstance(element) ? elementClass.cast(element) : null;
	}

	@Override
	public Font getFont(Object element) {
		T obj = getTyped(element);
		return obj == null ? JFaceResources.getDefaultFont() : getFontTyped(obj);
	}

	protected Font getFontTyped(T element) {
		// Always return a font, otherwise SWT will use the first columns font for all columns
		return JFaceResources.getDefaultFont();
	}

	@Override
	public Color getBackground(Object element) {
		T obj = getTyped(element);
		return obj == null ? super.getBackground(element) : getBackgroundTyped(obj);
	}

	protected Color getBackgroundTyped(T element) {
		return super.getBackground(element);
	}

	@Override
	public Color getForeground(Object element) {
		T obj = getTyped(element);
		return obj == null ? super.getForeground(element) : getForegroundTyped(obj);
	}

	protected Color getForegroundTyped(T element) {
		return super.getForeground(element);
	}

	@Override
	public Image getImage(Object element) {
		T obj = getTyped(element);
		return obj == null ? super.getImage(element) : getImageTyped(obj);
	}

	protected Image getImageTyped(T element) {
		return super.getImage(element);
	}

	@Override
	public String getText(Object element) {
		T obj = getTyped(element);
		return obj == null ? getDefaultText(element) : getTextTyped(obj);
	}

	protected String getTextTyped(T element) {
		return getDefaultText(element);
	}

	protected String getDefaultText(Object element) {
		return ""; //$NON-NLS-1$
	}

	@Override
	public String getToolTipText(Object element) {
		T obj = getTyped(element);
		return obj == null ? super.getToolTipText(element) : getToolTipTextTyped(obj);
	}

	protected String getToolTipTextTyped(T element) {
		return super.getToolTipText(element);
	}

	@Override
	public Image getToolTipImage(Object element) {
		T obj = getTyped(element);
		return obj == null ? super.getToolTipImage(element) : getToolTipImageTyped(obj);
	}

	protected Image getToolTipImageTyped(T element) {
		return super.getToolTipImage(element);
	}
}
