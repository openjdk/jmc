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
package org.openjdk.jmc.greychart.ui.views.legend;

import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.ui.accessibility.AccessibilityConstants;
import org.openjdk.jmc.ui.accessibility.FocusTracker;
import org.openjdk.jmc.ui.accessibility.MCAccessibleListener;

/**
 * Composite for showing a legend consisting of a small filled square and a text.
 */
public class LegendViewer extends ContentViewer {
	final protected FormToolkit m_formToolkit;
	final private Composite m_container;

	public LegendViewer(Composite parent, FormToolkit formToolkit) {
		m_formToolkit = formToolkit;
		m_container = formToolkit.createComposite(parent);
		hookControl(m_container);
		m_container.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				dispose();
			}
		});
		adaptForAccessibility(m_container, "Legend"); //$NON-NLS-1$
	}

	public void dispose() {
		IBaseLabelProvider l = getLabelProvider();
		if (l != null) {
			l.dispose();
		}
	}

	private void adaptForAccessibility(Composite control, String name) {
		FocusTracker.enableFocusTracking(control);
		MCAccessibleListener accessibleListener = new MCAccessibleListener();
		accessibleListener.setComponentType(AccessibilityConstants.COMPONENT_TYPE_LEGEND);
		accessibleListener.setName(name);
		control.getAccessible().addAccessibleListener(accessibleListener);
	}

	@Override
	public void inputChanged(Object input, Object oldInput) {
		refresh();
	}

	@Override
	public void refresh() {
		ensureValid();
		destroy();

		if (hasElementsPositions()) {
			m_container.setLayout(createMarginFreeLayout(3));
			createMultiAxisLegend(m_container);
		} else {
			m_container.setLayout(createMarginFreeLayout(1));
			createAxisLegend(m_container, SWT.NONE);
		}
		m_container.layout(true, true);
	}

	private GridLayout createAxisLegendLayout(int itemCount) {
		// Works for the graphs we have today. We can provide a better approach
		// later /Erik
		boolean isMultiRow = itemCount > 5;
		int columnCount = isMultiRow ? 3 : itemCount;
		GridLayout layout = new GridLayout(columnCount, false);
		layout.marginHeight = 1;
		layout.marginWidth = 2;
		layout.verticalSpacing = isMultiRow ? 5 : 0;
		return layout;
	}

	private void createMultiAxisLegend(Composite container) {

		GridData gd1 = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
		Composite left = createAxisLegend(container, SWT.LEFT);
		left.setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false);
		Label l = m_formToolkit.createLabel(container, " "); //$NON-NLS-1$
		l.setLayoutData(gd2);

		GridData gd3 = new GridData(SWT.END, SWT.BEGINNING, false, false);
		Composite right = createAxisLegend(container, SWT.RIGHT);
		right.setLayoutData(gd3);
	}

	private Composite createAxisLegend(Composite parent, int position) {
		Composite container = m_formToolkit.createComposite(parent);

		int itemCount = 0;
		for (Object element : getElements()) {
			if (getPosition(element) == position) {
				GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
				Control item = createControl(container, element);
				item.setLayoutData(gd);
				itemCount++;
			}
		}
		container.setLayout(createAxisLegendLayout(itemCount));
		return container;
	}

	private Object[] getElements() {
		return getStructuredContentProvider().getElements(getInput());
	}

	private boolean hasElementsPositions() {
		return getLabelProvider() instanceof IPositionProvider;
	}

	private Layout createMarginFreeLayout(int columnCount) {
		GridLayout layout = new GridLayout(columnCount, false);
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		layout.marginWidth = 2;
		// if the height is zero the layout becomes larger
		layout.marginHeight = 1;
		return layout;
	}

	protected Control createControl(Composite parent, Object element) {
		Image image = getImage(element);
		String text = getText(element);

		FormText formText = m_formToolkit.createFormText(parent, false);
		formText.setImage("colorKey", image); //$NON-NLS-1$
		formText.setImage("backgroundimage", createBackgroundImage(m_formToolkit, formText, image.getBounds().height)); //$NON-NLS-1$
		formText.setText("<form><p>" + formatHTMLImageDescription("colorKey", text) + "</p></form>", true, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		formText.setData("name", text); //$NON-NLS-1$
		return formText;
	}

	private int getPosition(Object element) {
		if (hasElementsPositions()) {
			return ((IPositionProvider) getLabelProvider()).getPosition(element);
		}
		return SWT.NONE;
	}

	protected Image getImage(Object element) {
		return ((ILabelProvider) getLabelProvider()).getImage(element);
	}

	protected String getText(Object element) {
		return ((ILabelProvider) getLabelProvider()).getText(element);
	}

	private IStructuredContentProvider getStructuredContentProvider() {
		return (IStructuredContentProvider) getContentProvider();
	}

	private void ensureValid() {
		if (!(getContentProvider() instanceof IStructuredContentProvider)) {
			throw new IllegalStateException("Must have an IStructuredContentProvider."); //$NON-NLS-1$
		}
		if (!(getLabelProvider() instanceof ILabelProvider)) {
			throw new IllegalStateException("Missing ILabelProvider."); //$NON-NLS-1$
		}
	}

	private void destroy() {
		Control[] controls = m_container.getChildren();
		for (Control control : controls) {
			control.dispose();
		}
	}

	private static String formatHTMLImageDescription(String imageKey, String name) {
		String html = "<img align='middle'  href='" + imageKey + "'/>"; //$NON-NLS-1$ //$NON-NLS-2$
		html += "<img href='backgroundimage'/>"; //$NON-NLS-1$
		html += name;
		return html;
	}

	@Override
	public Control getControl() {
		return m_container;
	}

	@Override
	public ISelection getSelection() {
		throw new UnsupportedOperationException("Unsupported"); //$NON-NLS-1$
	}

	@Override
	public void setSelection(ISelection selection, boolean reveal) {
		throw new UnsupportedOperationException("Unsupported"); //$NON-NLS-1$
	}

	/*
	 * Workaround, there is a bug in the FormText widget (on Vista). The text overwrites the image.
	 * Therefore I create a thin image (of 5 pixel width) on right side of the image.
	 */
	private Image createBackgroundImage(FormToolkit toolkit, Composite parent, int size) {
		final Image image = new Image(parent.getDisplay(), 5, size);
		GC gc = new GC(image);
		gc.setBackground(toolkit.getColors().getBackground());
		gc.fillRectangle(image.getBounds());
		gc.dispose();
		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (image != null) {
					image.dispose();
				}
			}
		});
		return image;
	}
}
