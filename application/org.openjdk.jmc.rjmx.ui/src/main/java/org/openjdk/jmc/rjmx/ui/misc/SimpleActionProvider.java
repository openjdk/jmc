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
package org.openjdk.jmc.rjmx.ui.misc;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.rjmx.ui.RJMXUIPlugin;
import org.openjdk.jmc.rjmx.ui.internal.IconConstants;
import org.openjdk.jmc.ui.common.action.IActionProvider;
import org.openjdk.jmc.ui.common.action.IUserAction;
import org.openjdk.jmc.ui.misc.IGraphical;
import org.openjdk.jmc.ui.misc.IPrintable;

public class SimpleActionProvider implements IActionProvider, IDescribable, IGraphical, IPrintable {

	private final String text;
	private final String description;
	private final ImageDescriptor icon;
	private final List<? extends IUserAction> actions;
	private final int doubleClickActionIndex;

	public SimpleActionProvider(String text, String description, ImageDescriptor icon,
			List<? extends IUserAction> actions, int doubleClickActionIndex) {
		this.text = text;
		this.description = description;
		this.actions = actions;
		this.icon = icon;
		this.doubleClickActionIndex = doubleClickActionIndex;
	}

	public SimpleActionProvider(String text, String description) {
		this(text, description, null, null, 0);
	}

	@Override
	public boolean hasChildren() {
		return false;
	}

	@Override
	public Collection<? extends IActionProvider> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public Collection<? extends IUserAction> getActions() {
		return actions != null ? actions : Collections.<IUserAction> emptyList();
	}

	@Override
	public IUserAction getDefaultAction() {
		return actions != null ? actions.get(doubleClickActionIndex) : null;
	}

	@Override
	public String getName() {
		return text;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return icon == null ? RJMXUIPlugin.getDefault().getMCImageDescriptor(IconConstants.ICON_REMOVE_OBJECT_DISABLED)
				: icon;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public FontDescriptor getFontDescriptor() {
		return JFaceResources.getFontRegistry().defaultFontDescriptor().setStyle(SWT.ITALIC);
	}
}
