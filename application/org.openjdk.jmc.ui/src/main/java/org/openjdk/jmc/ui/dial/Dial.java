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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.ui.accessibility.FocusTracker;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;
import org.openjdk.jmc.ui.wizards.OnePageWizardDialog;

/**
 */
public class Dial extends Composite {
	private static final String LAST_DIAL_ID = "last"; //$NON-NLS-1$
	private static final String MAX_DIAL_ID = "max"; //$NON-NLS-1$
	private DialConfiguration m_dialConfiguration;
	private final DialViewer m_dialViewer;
	private final DialInformationViewer m_dialInformationViewer;
	private final StackLayout stackLayout = new StackLayout();

	/**
	 * Creates the actual dial widget.
	 *
	 * @param parent
	 *            a widget which will be the parent of the new widget
	 * @param toolkit
	 *            the form toolkit in use
	 */
	public Dial(Composite parent, FormToolkit formToolkit, DialConfiguration dialConfiguration) {
		super(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);

		m_dialViewer = new DialViewer(this, SWT.NONE);
		FocusTracker.enableFocusTracking(m_dialViewer);
		m_dialViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		formToolkit.adapt(m_dialViewer);

		Composite infoPart = new Composite(this, SWT.NONE);
		infoPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		infoPart.setLayout(stackLayout);

		CLabel noValuesYetLabel = new CLabel(infoPart, SWT.CENTER);
		noValuesYetLabel.setText(Messages.DialViewer_NO_VALUE_YET_TEXT);
		formToolkit.adapt(noValuesYetLabel);
		stackLayout.topControl = noValuesYetLabel;

		m_dialInformationViewer = new DialInformationViewer(infoPart, formToolkit, SWT.NO_BACKGROUND);
		formToolkit.adapt(m_dialInformationViewer);

		formToolkit.paintBordersFor(this);
		formToolkit.adapt(this);

		setDialConfiguration(dialConfiguration);
	}

	public void setTitle(String title) {
		m_dialViewer.setTitle(title);
	}

	public void setUnit(IUnit unit) {
		m_dialViewer.setUnit(unit);
		updateDials();
	}

	private void updateDials() {
		IUnit unit = m_dialViewer.getUnit();
		if (unit != null) {
			if (m_dialConfiguration.getUseWatermark()) {
				m_dialInformationViewer.setProviders(
						new DialInformationProvider(LAST_DIAL_ID, unit, Messages.DIAL_LAST_VALUE_TEXT),
						new DialInformationProvider(MAX_DIAL_ID, unit, Messages.DIAL_MAX_VALUE_TEXT));
				m_dialViewer.setProviders(new MovingDial(MAX_DIAL_ID, m_dialConfiguration.getWatermarkColor()),
						new MovingDial(LAST_DIAL_ID));
			} else {
				m_dialInformationViewer
						.setProviders(new DialInformationProvider(LAST_DIAL_ID, unit, Messages.DIAL_LAST_VALUE_TEXT));
				m_dialViewer.setProviders(new MovingDial(LAST_DIAL_ID));
			}
		}
	}

	/**
	 * Sets the new configuration and a new attribute for the dial and reconfigures it accordingly.
	 *
	 * @param attribute
	 *            the new attribute to use
	 * @param dialConfiguration
	 *            the new configuration to use
	 */
	public void setDialConfiguration(DialConfiguration dialConfiguration) {
		m_dialConfiguration = dialConfiguration;
		m_dialViewer.setGradientRange(dialConfiguration.getGradientBeginValue(),
				dialConfiguration.getGradientEndValue(),
				SWTColorToolkit.asRGB(dialConfiguration.getGradientBeginColor()),
				SWTColorToolkit.asRGB(dialConfiguration.getGradientEndColor()));
		updateDials();

	}

	/**
	 * Update the {@code last} and {@code max} values of the dial, refresh the numerical display of
	 * these and optionally also the needles. Since the needles typically have momentum, they
	 * typically are externally refreshed periodically and don't need to be refreshed here too.
	 *
	 * @param last
	 * @param max
	 * @param refreshNeedles
	 *            if the needles should be redrawn
	 */
	public void setInput(Number last, Number max, boolean refreshNeedles) {
		m_dialViewer.setInput(LAST_DIAL_ID, last);
		m_dialInformationViewer.setInput(LAST_DIAL_ID, last);
		if (m_dialConfiguration.getUseWatermark()) {
			m_dialViewer.setInput(MAX_DIAL_ID, max);
			m_dialInformationViewer.setInput(MAX_DIAL_ID, max);
		}
		if (refreshNeedles) {
			m_dialViewer.refresh();
		}
		m_dialInformationViewer.refresh();

		if (stackLayout.topControl != m_dialInformationViewer) {
			stackLayout.topControl = m_dialInformationViewer;
			layout(true, true);
		}
	}

	@Override
	public boolean setFocus() {
		if (m_dialViewer != null && !m_dialViewer.isDisposed()) {
			return m_dialViewer.setFocus();
		}
		return setFocus();
	}

	public DialConfiguration getDialConfiguration() {
		return m_dialConfiguration;
	}

	public DialViewer getDialViewer() {
		return m_dialViewer;
	}

	public IAction getPropertiesAction() {
		return new Action(Messages.DIAL_PROPERTIES_ACTION_TEXT) {
			@Override
			public void run() {
				DialConfigurationPage dp = new DialConfigurationPage(getDialConfiguration(),
						m_dialViewer.getUnit().getContentType());
				OnePageWizardDialog dialog = new OnePageWizardDialog(getShell(), dp);
				dialog.setWidthConstraint(450, 450);
				dialog.setHeightConstraint(450, 450);
				if (dialog.open() == IDialogConstants.OK_ID) {
					setDialConfiguration(dp.getConfiguration());
				}
			}
		};
	}
}
