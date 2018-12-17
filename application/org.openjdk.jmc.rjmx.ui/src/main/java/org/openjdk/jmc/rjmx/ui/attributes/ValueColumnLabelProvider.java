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
package org.openjdk.jmc.rjmx.ui.attributes;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.rjmx.services.IReadOnlyAttribute;
import org.openjdk.jmc.ui.misc.TypedLabelProvider;

public class ValueColumnLabelProvider extends TypedLabelProvider<IReadOnlyAttribute> {

	private static final String FIXED_TEXT_FONT = "org.openjdk.jmc.fixedtextfont"; //$NON-NLS-1$

	public ValueColumnLabelProvider() {
		super(IReadOnlyAttribute.class);
	}

	@Override
	protected Color getForegroundTyped(IReadOnlyAttribute attribute) {
		return isValid(attribute) ? null : JFaceResources.getColorRegistry().get(JFacePreferences.QUALIFIER_COLOR);
	}

	@Override
	protected Font getFontTyped(IReadOnlyAttribute attribute) {
		if (!JFaceResources.getFontRegistry().hasValueFor(FIXED_TEXT_FONT)) {
			createFixedSizeTextFont();
		}
		if (isValid(attribute)) {
			return JFaceResources.getFontRegistry().get(FIXED_TEXT_FONT);
		}
		return JFaceResources.getFontRegistry().getItalic(FIXED_TEXT_FONT);
	}

	private void createFixedSizeTextFont() {
		FontRegistry registry = JFaceResources.getFontRegistry();
		// Take the height of the first FontData. In practice, there should only be one.
		int defaultHeight = registry.get(JFaceResources.DEFAULT_FONT).getFontData()[0].getHeight();

		// Modify the TEXT_FONT to have the same height as the DEFAULT_FONT.
		FontDescriptor textFontDesc = registry.getDescriptor(JFaceResources.TEXT_FONT).setHeight(defaultHeight);
		registry.put(FIXED_TEXT_FONT, textFontDesc.getFontData());
	}

	@Override
	protected String getToolTipTextTyped(IReadOnlyAttribute attribute) {
		return attribute.getValue() == null ? Messages.AttributeInspector_VALUE_IS_NULL : null;
	}

	@Override
	protected String getTextTyped(IReadOnlyAttribute attribute) {
		if (attribute.getValue() == null) {
			return '<' + TypeHandling.simplifyType(attribute.getInfo().getType()) + '>';
		} else {
			return TypeHandling.getValueString(getValue(attribute));
		}
	}

	protected Object getValue(IReadOnlyAttribute attribute) {
		return attribute.getValue();
	}

	protected boolean isValid(IReadOnlyAttribute attribute) {
		return attribute.getValue() != null;
	}
}
