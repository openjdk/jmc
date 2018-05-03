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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.logging.Level;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IDataPageFactory;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.CoreImages;

public abstract class LabeledPageFactory implements IDataPageFactory {

	private static final String ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String ATTRIBUTE_ICON = "icon"; //$NON-NLS-1$

	@Override
	public String getName(IState state) {
		String name = state.getAttribute(ATTRIBUTE_NAME);
		return name != null ? name : getDefaultName(state);
	}

	protected String getDefaultName(IState state) {
		return Messages.PAGE_UNNAMED;
	}

	@Override
	public String getDescription(IState state) {
		return state.getAttribute(ATTRIBUTE_DESCRIPTION);
	}

	@Override
	public ImageDescriptor getImageDescriptor(IState state) {
		String iconStr = state.getAttribute(ATTRIBUTE_ICON);
		if (iconStr != null) {
			byte[] pngData = Base64.getDecoder().decode(iconStr);
			try {
				return ImageDescriptor.createFromImageData(new ImageData(new ByteArrayInputStream(pngData)));
			} catch (Exception e) {
				FlightRecorderUI.getDefault().getLogger().log(Level.WARNING,
						"Could not load icon for page: " + getName(state), e); //$NON-NLS-1$
			}
		}
		return getDefaultImageDescriptor(state);
	}

	protected ImageDescriptor getDefaultImageDescriptor(IState state) {
		return CoreImages.HELP;
	}

	public static void writeLabel(IWritableState to, String name, String description, ImageDescriptor image) {
		to.putString(ATTRIBUTE_NAME, name);
		to.putString(ATTRIBUTE_DESCRIPTION, description);
		if (image != null) {
			try {
				ImageLoader loader = new ImageLoader();
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				loader.data = new ImageData[] {image.getImageData()};
				loader.save(out, SWT.IMAGE_PNG);
				String iconStr = Base64.getEncoder().encodeToString(out.toByteArray());
				to.putString(LabeledPageFactory.ATTRIBUTE_ICON, iconStr);
			} catch (Exception e) {
				FlightRecorderUI.getDefault().getLogger().log(Level.WARNING, "Could not persist icon", e); //$NON-NLS-1$
			}
		}
	}

}
