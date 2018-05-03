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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration;

import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.PERIOD_EVERY_CHUNK;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.SETTING_PERIOD;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.PathElement.PathElementKind;
import org.openjdk.jmc.flightrecorder.ui.common.TypeLabelProvider;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.misc.AdaptingLabelProvider;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;

/**
 * Class responsible for providing images and text for a {@link Property} or a
 * {@link PropertyContainer} for a JFace viewer.
 */
final class PropertyLabelProvider extends AdaptingLabelProvider {

	@Override
	public Image getImage(Object element) {
		if (element instanceof PropertyContainer.FolderNode) {
			return UIPlugin.getDefault().getImage(UIPlugin.ICON_FOLDER);
		} else if (element instanceof PropertyContainer.EventNode) {
			return getEventImage((PropertyContainer.EventNode) element);
		} else {
			assert element instanceof Property;
			return UIPlugin.getDefault().getImage(UIPlugin.ICON_PROPERTY_OBJECT);
		}
	}

	private static Image getEventImage(PropertyContainer.EventNode container) {
		String key = container.getEventTypeID().getFullKey();
		return SWTColorToolkit.getColorThumbnail(SWTColorToolkit.asRGB(TypeLabelProvider.getColorOrDefault(key)));
	}

	@Override
	public String getText(Object element) {
		if (element instanceof PathElement) {
			String text = ((PathElement) element).getName();
			if (element instanceof Property) {
				// FIXME: Should be handled in RJMX layer, all times as nanos
				Property property = (Property) element;
				String value = property.getValue();
				// FIXME: Check or ideally delegate to "content type".
				if (SETTING_PERIOD.equals(text) && PERIOD_EVERY_CHUNK.equals(value)) {
					value = Messages.RECORDING_TEMPLATE_ONCE_PER_CHUNK;
				}
				text += '=' + value;
			}
			return text;
		}
		assert false;
		return super.getText(element);
	}

	@Override
	public Font getFont(Object element) {
		if (element instanceof PathElement) {
			return getFont((PathElement) element);
		}
		return null;
	}

	private Font getFont(PathElement pathElement) {
		switch (pathElement.getKind()) {
		case IN_CONFIGURATION:
			return getPropertyDoesNotExistFont();
		case IN_BOTH:
			return getPropertyPartOfTemplateFont();
		case IN_SERVER:
			return getNormalFont();
		default:
			throw new IllegalArgumentException("Illegal type " + pathElement.getKind() + '!'); //$NON-NLS-1$
		}
	}

	private Font getPropertyDoesNotExistFont() {
		FontDescriptor fd = JFaceResources.getFontRegistry().defaultFontDescriptor().setStyle(SWT.BOLD | SWT.ITALIC);
		return (Font) JFaceResources.getResources().get(fd);
	}

	private Font getPropertyPartOfTemplateFont() {
		return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
	}

	private Font getNormalFont() {
		return JFaceResources.getDefaultFont();
	}

	@Override
	public Color getForeground(Object element) {
		if ((element instanceof PathElement) && ((PathElement) element).getKind() == PathElementKind.IN_CONFIGURATION) {
			return JFaceResources.getColorRegistry().get(JFacePreferences.QUALIFIER_COLOR);
		}
		return null;
	}

	@Override
	public Color getBackground(Object element) {
		// Always use the default
		return null;
	}
}
