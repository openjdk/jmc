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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.IMRIService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIMetadataToolkit;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;
import org.openjdk.jmc.ui.misc.TypedLabelProvider;

public class AttributeLabelProvider extends TypedLabelProvider<MRI> {
	private final IMRIMetadataService m_mds;
	private final IMRIService m_availableAttributes;
	private static final RGB UNAVAILABLE_COLOR = new RGB(192, 192, 192);

	public AttributeLabelProvider(IMRIMetadataService mds, IMRIService availableAttributes) {
		super(MRI.class);
		m_mds = mds;
		m_availableAttributes = availableAttributes;
	}

	@Override
	protected String getTextTyped(MRI element) {
		return MRIMetadataToolkit.getDisplayName(m_mds, element);
	}

	@Override
	protected String getToolTipTextTyped(MRI element) {
		String path = MBeanPropertiesOrderer.mriAsTooltip(element);
		String desc = m_mds.getMetadata(element).getDescription();
		return desc == null ? path : path + "\n" + desc; //$NON-NLS-1$
	}

	@Override
	protected Image getImageTyped(MRI mri) {
		if (m_availableAttributes.isMRIAvailable(mri)) {
			java.awt.Color color = MRIMetadataToolkit.getColor(m_mds.getMetadata(mri));
			return SWTColorToolkit.getColorThumbnail(SWTColorToolkit.asRGB(color));
		} else {
			return SWTColorToolkit.getColorThumbnail(UNAVAILABLE_COLOR);
		}
	}

	@Override
	protected Color getForegroundTyped(MRI element) {
		if (m_availableAttributes.isMRIAvailable(element)) {
			return null;
		}
		return JFaceResources.getColorRegistry().get(JFacePreferences.QUALIFIER_COLOR);
	}

}
