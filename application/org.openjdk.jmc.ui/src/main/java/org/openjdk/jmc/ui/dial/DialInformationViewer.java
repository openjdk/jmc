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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.openjdk.jmc.ui.accessibility.FocusTracker;
import org.openjdk.jmc.ui.accessibility.SimpleTraverseListener;

/**
 * Widget that shows information for {@link IDialInformationProvider}. Typically this widget is used
 * as a legend for a {@link DialViewer}
 */
final public class DialInformationViewer extends Composite {
	private static class DialInformationWidget extends CLabel {
		private final IDialInformationProvider m_provider;

		public DialInformationWidget(Composite parent, int style, IDialInformationProvider provider) {
			super(parent, style);
			setLeftMargin(5);
			setRightMargin(5);
			m_provider = provider;
		}

		public void setInput(Object input) {
			String text = m_provider.getName(input) + ' ' + m_provider.getValue(input);
			setText(text);
			setToolTipText(m_provider.getDescription(input));
			/*
			 * Provided that this method isn't called more than a few times per frame (ideally at
			 * most one), this actually improves performance enormously on OS X (10.10). This likely
			 * works since as a Canvas, its NSView returns true from isOpaque(), thus avoiding
			 * whatever SWT have done to slow down NSView.displayIfNeeded().
			 */
			update();
		}
	}

	final private Map<String, Object> m_inputs = new LinkedHashMap<>();
	final private FormToolkit m_toolkit;

	/**
	 * Create a {@link DialInformationViewer}, widget that shows information about dials.
	 *
	 * @param parent
	 *            the parent composite
	 * @param toolkit
	 *            the toolkit to construct the dial information text
	 * @param style
	 *            the style for the widget
	 */
	public DialInformationViewer(Composite parent, FormToolkit toolkit, int style) {
		super(parent, style);
		m_toolkit = toolkit;
		addTraverseListener(new SimpleTraverseListener(true));
	}

	public void setProviders(IDialInformationProvider ... providers) {
		disposeWidgets();
		createWidgets(providers);
		refresh();
	}

	/**
	 * Set the input to use for the {@link IDialInformationProvider}s
	 *
	 * @param input
	 *            the input
	 */
	public boolean setInput(String identifier, Object input) {
		return m_inputs.put(identifier, input) != input;
	}

	/**
	 * Refreshes with new data from all {@link IDialInformationProvider}s
	 */
	public void refresh() {
		for (Control widget : getChildren()) {
			if (widget instanceof DialInformationWidget) {
				DialInformationWidget info = (DialInformationWidget) widget;
				info.setInput(getInput(info.m_provider.getId()));
			}
		}
	}

	private Object getInput(String id) {
		return m_inputs.get(id);
	}

	private void createWidgets(IDialInformationProvider ... providers) {
		int columnIndex = 0;
		for (IDialInformationProvider info : providers) {
			TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP);
			Control dialInfoWidget = createWidget(calculateStyle(columnIndex, providers.length), info);
			dialInfoWidget.setLayoutData(td);
			columnIndex++;
		}
		setLayout(createWidgetLayout(providers.length));
		layout(true, true);
	}

	private Control createWidget(int style, IDialInformationProvider provider) {
		DialInformationWidget widget = new DialInformationWidget(this, style, provider);
		m_toolkit.adapt(widget);
		FocusTracker.enableFocusTracking(widget);
		return widget;
	}

	private static TableWrapLayout createWidgetLayout(int columnCount) {
		TableWrapLayout layout = new TableWrapLayout();
		layout.topMargin = 0;
		layout.bottomMargin = 0;
		layout.leftMargin = 0;
		layout.rightMargin = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.numColumns = columnCount;
		layout.makeColumnsEqualWidth = true;
		return layout;
	}

	private int calculateStyle(int columnIndex, int columnCount) {
		if (columnCount > 1) {
			if (columnIndex == 0) {
				return SWT.RIGHT;
			}
			if (columnIndex == columnCount - 1) {
				return SWT.LEFT;
			}
		}

		return SWT.CENTER;
	}

	private void disposeWidgets() {
		for (Control widget : getChildren()) {
			widget.dispose();
		}
	}
}
