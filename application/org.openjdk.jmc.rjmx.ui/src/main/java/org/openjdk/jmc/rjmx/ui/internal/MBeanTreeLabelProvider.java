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
package org.openjdk.jmc.rjmx.ui.internal;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIMetadataToolkit;
import org.openjdk.jmc.rjmx.ui.RJMXUIPlugin;
import org.openjdk.jmc.rjmx.ui.internal.MBeanPropertiesOrderer.Property;
import org.openjdk.jmc.rjmx.ui.internal.MBeanPropertiesOrderer.PropertyWithMBean;
import org.openjdk.jmc.ui.common.tree.ITreeNode;

public class MBeanTreeLabelProvider extends ColumnLabelProvider {

	private final ContentType<?> allowedContentType;

	public MBeanTreeLabelProvider(ContentType<?> allowedContentType) {
		this.allowedContentType = allowedContentType;
	}

	private static Image getConsoleImage(String imageKey) {
		return RJMXUIPlugin.getDefault().getImage(imageKey);
	}

	@Override
	public Image getImage(Object element) {
		Object data = ((ITreeNode) element).getUserData();
		if (data instanceof PropertyWithMBean) {
			return getConsoleImage(IconConstants.ICON_MBEAN);
		} else if (data instanceof String || data instanceof Property) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
		} else if (data instanceof IMRIMetadata) {
			IMRIMetadata info = (IMRIMetadata) data;
			String unitString = info.getUnitString();
			if (allowedContentType != null && unitString != null
					&& !allowedContentType.equals(UnitLookup.getContentType(unitString))) {
				return null;
			} else if (MRIMetadataToolkit.isComposite(info)) {
				return getConsoleImage(IconConstants.ICON_ATTRIBUTE_COMPOSITE);
			} else if (MRIMetadataToolkit.isNumerical(info)) {
				return getConsoleImage(IconConstants.ICON_ATTRIBUTE_NUMERICAL);
			} else {
				return getConsoleImage(IconConstants.ICON_ATTRIBUTE_NORMAL);
			}
		} else {
			throw new IllegalArgumentException(
					"This label provider only supports the ObjectName and AttributeInfo types."); //$NON-NLS-1$
		}
	}

	@Override
	public String getText(Object element) {
		Object data = ((ITreeNode) element).getUserData();
		if (data instanceof String) {
			return (String) data;
		} else if (data instanceof Property) {
			return formatProperty((Property) data);
		} else if (data instanceof IMRIMetadata) {
			String dataPath = ((IMRIMetadata) data).getMRI().getDataPath();
			return dataPath.substring(dataPath.lastIndexOf(MRI.VALUE_COMPOSITE_DELIMITER) + 1);
		} else {
			throw new IllegalArgumentException(
					"This label provider only supports the ObjectName and AttributeInfo types: " + data); //$NON-NLS-1$
		}
	}

	@Override
	public Font getFont(Object element) {
		Object data = ((ITreeNode) element).getUserData();
		if (data instanceof IMRIMetadata && MRIMetadataToolkit.isNumerical((IMRIMetadata) data)
				&& ((IMRIMetadata) data).getUnitString() == null) {
			return JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);
		}
		return super.getFont(element);
	}

	@Override
	public Color getForeground(Object element) {
		Object data = ((ITreeNode) element).getUserData();
		if (data instanceof IMRIMetadata && allowedContentType != null) {
			String unitString = ((IMRIMetadata) data).getUnitString();
			if (unitString != null && !allowedContentType.equals(UnitLookup.getContentType(unitString))) {
				return JFaceResources.getColorRegistry().get(JFacePreferences.QUALIFIER_COLOR);
			}
		}
		return super.getForeground(element);
	}

	@Override
	public String getToolTipText(Object element) {
		Object data = ((ITreeNode) element).getUserData();
		if (data instanceof PropertyWithMBean) {
			return MBeanPropertiesOrderer.getMBeanPath(((PropertyWithMBean) data).getBean());
		} else if (data instanceof String) {
			return (String) data;
		} else if (data instanceof Property) {
			return ((Property) data).getStringRepresentation();
		} else if (data instanceof IMRIMetadata) {
			return ((IMRIMetadata) data).getDescription();
		}
		return null;
	}

	private static String formatProperty(Property data) {
		String value = data.getValue();
		if (value != null) {
			if ("class".equals(data.getKey())) { //$NON-NLS-1$
				return value.substring(value.lastIndexOf('.') + 1);
			}
		}
		return value;
	}
}
