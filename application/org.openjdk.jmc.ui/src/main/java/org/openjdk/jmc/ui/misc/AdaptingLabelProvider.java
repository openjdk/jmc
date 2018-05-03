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

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.ui.common.util.AdapterUtil;

public class AdaptingLabelProvider extends ColumnLabelProvider {

	private LocalResourceManager localResourceManager;

	@Override
	public Image getImage(Object element) {
		ImageDescriptor image = AdapterUtil.getAdapter(element, ImageDescriptor.class);
		return image == null ? null : (Image) getResourceManager().get(image);
	}

	@Override
	public String getText(Object element) {
		IDescribable d = AdapterUtil.getAdapter(element, IDescribable.class);
		return d == null ? null : d.getName();
	}

	@Override
	public String getToolTipText(Object element) {
		IDescribable d = AdapterUtil.getAdapter(element, IDescribable.class);
		return d == null ? null : d.getDescription();
	}

	protected LocalResourceManager getResourceManager() {
		if (localResourceManager == null) {
			localResourceManager = new LocalResourceManager(JFaceResources.getResources());
		}
		return localResourceManager;
	}

	@Override
	public Font getFont(Object o) {
		IPrintable p = AdapterUtil.getAdapter(o, IPrintable.class);
		if (p == null) {
			return null;
		}
		FontDescriptor fontDescriptor = p.getFontDescriptor();
		return fontDescriptor == null ? null : (Font) getResourceManager().get(fontDescriptor);
	}

	@Override
	public void dispose() {
		if (localResourceManager != null) {
			localResourceManager.dispose();
			localResourceManager = null;
		}
	}
}
