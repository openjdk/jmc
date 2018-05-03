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
package org.openjdk.jmc.ui.dial;

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.ui.accessibility.MCAccessibleListener;
import org.openjdk.jmc.ui.misc.QuantityKindProposal;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;
import org.openjdk.jmc.ui.wizards.IPerformFinishable;

/**
 * Class for configuring a dial.
 */
public class DialConfigurationPage extends WizardPage implements IPerformFinishable {

	final private DialConfiguration m_dialConfiguration;
	// Widgets
	private Button m_waterMark;
	private Text m_gradientBeginValue;
	private Text m_gradientEndValue;
	private ColorSelector m_waterMarkColor;
	private ColorSelector m_gradientBeginColor;
	private ColorSelector m_gradientEndColor;
	private final KindOfQuantity<?> m_quantityKind;

	public DialConfigurationPage(DialConfiguration initial, KindOfQuantity<?> quantityKind) {
		super(Messages.DialConfigurationWizard_CONFIGURE_DIAL_WIZARD_PAGE);
		setTitle(Messages.DialConfigurationWizard_CONFIGURE_DIAL_WIZARD_PAGE);
		setDescription(Messages.DialConfigurationPage_DIAL_CONFIGURATION_TEXT);
		m_dialConfiguration = initial;
		m_quantityKind = quantityKind;
	}

	@Override
	public void createControl(Composite parent) {
		int columns = 2;
		GridLayout layout = new GridLayout(columns, false);
		parent.setLayout(layout);
		layout.marginWidth = 10;
		layout.marginHeight = 10;
		layout.horizontalSpacing = 10;

		createGradientBegin(parent, columns);
		createGradientEnd(parent, columns);
		createWatermark(parent, columns);
		setControl(parent);
		load();
		update();
	}

	private void createGradientEnd(Composite parent, int columns) {
		createLabel(parent, Messages.DialConfigurationPage_GRADIENT_END_VALUE_TEXT);
		m_gradientEndValue = createText(parent);
		QuantityKindProposal.install(m_gradientEndValue, m_quantityKind);
		createLabel(parent, Messages.DialConfigurationPage_GRADIENT_END_COLOR_TEXT);
		m_gradientEndColor = createColorSelector(parent);
	}

	protected void createGradientBegin(Composite parent, int columns) {
		createLabel(parent, Messages.DialConfigurationPage_GRADIENT_START_VALUE_TEXT);
		m_gradientBeginValue = createText(parent);
		QuantityKindProposal.install(m_gradientBeginValue, m_quantityKind);
		createLabel(parent, Messages.DialConfigurationPage_GRADIENT_START_COLOR_TEXT);
		m_gradientBeginColor = createColorSelector(parent);
	}

	protected void createWatermark(Composite parent, int columns) {
		createLabel(parent, Messages.DialConfigurationPage_SHOW_WATERMARK_CHECKBOX_TEXT);

		m_waterMark = new Button(parent, SWT.CHECK);
		m_waterMark.setText(""); //$NON-NLS-1$
		m_waterMark.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		MCAccessibleListener mcAccessibleListener = new MCAccessibleListener();
		mcAccessibleListener.setName(Messages.DialConfigurationPage_SHOW_WATERMARK_CHECKBOX_TEXT);
		mcAccessibleListener.setDescription(Messages.DialConfigurationPage_SHOW_WATERMARK_CHECKBOX_TEXT);
		m_waterMark.getAccessible().addAccessibleListener(mcAccessibleListener);

		createLabel(parent, Messages.DialConfigurationPage_WATERMARK_COLOR_TEXT);
		m_waterMarkColor = createColorSelector(parent);
	}

	private ColorSelector createColorSelector(Composite parent) {
		ColorSelector gradientEndColor = new ColorSelector(parent);
		gradientEndColor.getButton().setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		return gradientEndColor;
	}

	private Text createText(Composite parent) {
		Text gradientEndValue = new Text(parent, SWT.BORDER);
		gradientEndValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		gradientEndValue.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				update();
			}
		});
		return gradientEndValue;
	}

	private void createLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(text);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
	}

	protected boolean validates() {
		IQuantity start = null;
		try {
			String beginText = m_gradientBeginValue.getText();
			if (beginText.trim().length() != 0) {
				start = m_quantityKind.parseInteractive(beginText);
			}
		} catch (QuantityConversionException nfe) {
			setErrorMessage(Messages.DialConfigurationPage_GRADIENT_START_MUST_BE_NUMERIC_OR_EMPTY_TEXT + ": " //$NON-NLS-1$
					+ nfe.getLocalizedMessage());
			return false;
		}
		try {
			String endText = m_gradientEndValue.getText();
			if (endText.trim().length() > 0) {
				IQuantity end = m_quantityKind.parseInteractive(endText);
				if (start != null && start.compareTo(end) >= 0) {
					setErrorMessage(Messages.DialConfigurationPage_GRADIENT_END_MUST_BE_GREATER_THAN_START);
					return false;
				}
			}
		} catch (QuantityConversionException nfe) {
			setErrorMessage(Messages.DialConfigurationPage_GRADIENT_END_MUST_BE_NUMERIC_OR_EMPTY_TEXT + ": " //$NON-NLS-1$
					+ nfe.getLocalizedMessage());
			return false;
		}
		return true;
	}

	private String getGradientString(IQuantity q) {
		return q == null ? "" : q.displayUsing(IDisplayable.VERBOSE); //$NON-NLS-1$
	}

	private IQuantity getGradientValue(String interactiveQuantity) {
		try {
			return (interactiveQuantity.trim().length() == 0) ? null
					: m_quantityKind.parseInteractive(interactiveQuantity);
		} catch (QuantityConversionException e) {
			return null;
		}
	}

	protected void load() {
		m_waterMarkColor.setColorValue(SWTColorToolkit.asRGB(m_dialConfiguration.getWatermarkColor()));
		m_gradientBeginColor.setColorValue(SWTColorToolkit.asRGB(m_dialConfiguration.getGradientBeginColor()));
		m_gradientEndColor.setColorValue(SWTColorToolkit.asRGB(m_dialConfiguration.getGradientEndColor()));

		m_gradientBeginValue.setText(getGradientString(m_dialConfiguration.getGradientBeginValue()));
		m_gradientEndValue.setText(getGradientString(m_dialConfiguration.getGradientEndValue()));
		m_waterMark.setSelection(m_dialConfiguration.getUseWatermark());
	}

	protected void store() {
		m_dialConfiguration.setGradientBeginValue(getGradientValue(m_gradientBeginValue.getText()));
		m_dialConfiguration.setGradientEndValue((getGradientValue(m_gradientEndValue.getText())));
		m_dialConfiguration.setUseWatermark(m_waterMark.getSelection());
		m_dialConfiguration.setWatermarkColor(SWTColorToolkit.asAwtColor(m_waterMarkColor.getColorValue()));
		m_dialConfiguration.setGradientBeginColor(SWTColorToolkit.asAwtColor(m_gradientBeginColor.getColorValue()));
		m_dialConfiguration.setGradientEndColor(SWTColorToolkit.asAwtColor(m_gradientEndColor.getColorValue()));
	}

	public DialConfiguration getConfiguration() {
		return m_dialConfiguration;
	}

	public void update() {
		setPageComplete(validates());
		if (isPageComplete()) {
			setErrorMessage(null);
		}
	}

	@Override
	public boolean performFinish() {
		store();
		return true;
	}

}
