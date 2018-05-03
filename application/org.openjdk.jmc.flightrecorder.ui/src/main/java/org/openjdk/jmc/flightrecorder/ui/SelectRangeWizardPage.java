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
package org.openjdk.jmc.flightrecorder.ui;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.text.MessageFormat;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.flightrecorder.ui.common.JComponentNavigator;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.wizards.IPerformFinishable;

class SelectRangeWizardPage extends WizardPage implements IPerformFinishable {
	private JComponentNavigator m_slider;
	private final IRange<IQuantity> m_fullRange;
	private final IRange<IQuantity> m_range;
	private Label m_selectedStartLabel;
	private Label m_selectedEndLabel;
	private Label m_warningLabel;
	private FormToolkit m_toolkit;
	private IRange<IQuantity> m_selectedRange;
	private String recordingFileName;

	public SelectRangeWizardPage(IRange<IQuantity> range, IRange<IQuantity> fullRange, String recordingFileName) {
		super("SelectRangeWizardPage", Messages.SELECT_RANGE_WIZARD_TITLE, null); //$NON-NLS-1$
		m_fullRange = fullRange;
		m_range = range;
		this.recordingFileName = recordingFileName;
	}

	@Override
	public void createControl(Composite parent) {
		m_toolkit = new FormToolkit(parent.getDisplay());
		initializeDialogUnits(parent);
		int indent = 0; // convertWidthInCharsToPixels(4);

		setDescription(MessageFormat.format(Messages.SELECT_RANGE_WIZARD_DESCRIPTION, recordingFileName));

		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);

		createHeader(container);
		createTimeLabels(container);
		createSlider(container, indent);
		createSelectedTimeLabels(container, indent);
		m_warningLabel = new Label(container, SWT.LEFT);
		m_warningLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		m_warningLabel.setForeground(JFaceResources.getColorRegistry().get(JFacePreferences.ERROR_COLOR));
		setControl(container);

		m_slider.setNavigatorRange(m_fullRange);
		m_slider.setCurrentRange(m_range);
	}

	private void createHeader(Composite parent) {
		Label start = new Label(parent, SWT.CENTER);
		start.setText(Messages.SELECT_RANGE_WIZARD_TEXT);
		GridData g = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		g.horizontalSpan = 2;
		start.setLayoutData(g);
	}

	private void createTimeLabels(Composite parent) {
		Label start = new Label(parent, SWT.LEFT);
		start.setText(m_fullRange.getStart().displayUsing(IDisplayable.AUTO));
		start.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

		Label end = new Label(parent, SWT.RIGHT);
		end.setText(m_fullRange.getEnd().displayUsing(IDisplayable.AUTO));
		end.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
	}

	private void createSelectedTimeLabels(Composite parent, int indent) {
		m_selectedStartLabel = new Label(parent, SWT.LEFT);
		m_selectedStartLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		m_selectedEndLabel = new Label(parent, SWT.RIGHT);
		m_selectedEndLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
	}

	private GridData createGridData(boolean grabExcessHorizontalSpace, int horizontalSpan, int indent) {
		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, grabExcessHorizontalSpace, false);
		gridData.horizontalSpan = horizontalSpan;
		gridData.horizontalIndent = indent;
		return gridData;
	}

	private void createSlider(Composite parent, int indent) {
		m_slider = new JComponentNavigator(parent, m_toolkit) {

			private final Color RED = Color.getHSBColor(0.0f, 0.9f, 0.9f);
			private final Color ORANGE = Color.getHSBColor(0.1f, 0.9f, 0.8f);

			@Override
			protected void renderBackdrop(Graphics2D g2d, int w, int h) {
				g2d.setColor(Color.WHITE);
				g2d.fillRect(0, 0, w, h);
				GradientPaint gradient = new GradientPaint(0, h / 2.0f, RED, 0, h, ORANGE, false);
				g2d.setPaint(gradient);
				g2d.fillRect(2, h / 3, w - 4, h);
			}

			@Override
			protected void onRangeChange() {
				updateLabels(getCurrentRange());
			}

		};
		m_slider.setLayoutData(createGridData(true, 2, indent));
	}

	private void updateLabels(IRange<IQuantity> range) {
		m_selectedRange = range;
		m_selectedStartLabel.setText(range.getStart().displayUsing(IDisplayable.AUTO));
		m_selectedEndLabel.setText(range.getEnd().displayUsing(IDisplayable.AUTO));
		IQuantity warnExtent = m_range.getExtent().multiply(1.1); // 10% above initial
		if (range.getExtent().compareTo(warnExtent) > 0) {
			m_warningLabel.setText(Messages.SELECT_RANGE_WIZARD_TO_MUCH_SELECTED_WARNING);
		} else {
			m_warningLabel.setText(""); //$NON-NLS-1$
		}
		m_warningLabel.getParent().layout();
	}

	public IRange<IQuantity> getRange() {
		return m_selectedRange;
	}

	@Override
	public void dispose() {
		super.dispose();
		if (m_toolkit != null) {
			m_toolkit.dispose();
		}
	}

	@Override
	public boolean performFinish() {
		return true;
	}
}
