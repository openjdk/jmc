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
package org.openjdk.jmc.ui.misc;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Factory for creating Layout and LayoutData for mission control.
 * <p>
 * The purpose of this class is:
 * <ol>
 * <li>Simplify syntax. We should not need to write more lines of code then necessary.
 * <li>Promote a consistent user interface.
 * <li>Make it easier to change layout setting by either change the settings in the factory, or by
 * tracking who uses the current settings.
 * </ol>
 */
public class MCLayoutFactory {
	public static final int DEFAULT_LAYOUT_DATA_WIDTH = 50;
	public static final int DEFAULT_LAYOUT_DATA_HEIGHT = 50;
	public static final int VERTICAL_SECTION_SPACING = 6;
	public static final int HORIZONTAL_SECTION_SPACING = 6;
	public static final int MANAGED_FORM_MARGIN_WIDTH = 6;
	public static final int MANAGED_FORM_MARGIN_HEIGHT = 6;

	/**
	 * Layout that should be used form pages.
	 *
	 * @return the layout
	 */
	public static Layout createFormPageLayout() {
		return createFormPageLayout(MANAGED_FORM_MARGIN_WIDTH, MANAGED_FORM_MARGIN_HEIGHT);
	}

	public static Layout createFormPageLayout(int marginW, int marginH) {
		GridLayout layout = new GridLayout();
		layout.marginWidth = marginW;
		layout.marginHeight = marginH;
		layout.verticalSpacing = VERTICAL_SECTION_SPACING;
		layout.horizontalSpacing = HORIZONTAL_SECTION_SPACING;
		return layout;
	}

	public static Layout createMarginFreeFormPageLayout(int horizontalspan) {
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = VERTICAL_SECTION_SPACING;
		layout.horizontalSpacing = HORIZONTAL_SECTION_SPACING;
		layout.numColumns = horizontalspan;
		return layout;
	}

	public static Layout createPaintBordersMarginFreeFormPageLayout(int horizontalspan) {
		GridLayout layout = new GridLayout();
		layout.marginWidth = 2;
		layout.marginHeight = 2;
		layout.verticalSpacing = VERTICAL_SECTION_SPACING;
		layout.horizontalSpacing = HORIZONTAL_SECTION_SPACING;
		layout.numColumns = horizontalspan;
		return layout;
	}

	public static Layout createMarginFreeFormPageLayout() {
		return createMarginFreeFormPageLayout(1);
	}

	public static Layout createFormPageLayout(int columns) {
		GridLayout l = (GridLayout) createFormPageLayout();
		l.numColumns = columns;
		return l;
	}

	public static GridLayout createPackedLayout(int numColumns, boolean makeColumnsEqualWidth) {
		GridLayout layout = new GridLayout(numColumns, makeColumnsEqualWidth);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		return layout;
	}

	/**
	 * Factory method for Layout Data that should be used with a layout created
	 * createFormPageLayout()
	 *
	 * @return the layout data
	 */
	public static Object createFormPageLayoutData() {
		return createFormPageLayoutData(DEFAULT_LAYOUT_DATA_WIDTH, DEFAULT_LAYOUT_DATA_HEIGHT, 1, 1, true, true);
	}

	/**
	 * Factory method for creating layout data for a form page
	 *
	 * @param widthHint
	 *            a hint of the width
	 * @param heightHint
	 *            a hint of the height
	 * @return
	 */
	public static Object createFormPageLayoutData(
		int widthHint, int heightHint, int horizontalSpan, int vererticalSpan, boolean grabExcessiveHorizontal,
		boolean grabExcessiveVertical) {
		GridData layoutData = (GridData) createFormPageLayoutData(widthHint, heightHint, grabExcessiveHorizontal,
				grabExcessiveVertical);
		layoutData.widthHint = widthHint;
		layoutData.heightHint = heightHint;
		layoutData.horizontalSpan = horizontalSpan;
		layoutData.verticalSpan = vererticalSpan;
		return layoutData;
	}

	/**
	 * Factory method for creating layout data for a form page
	 *
	 * @param widthHint
	 *            a hint of the width
	 * @param heightHint
	 *            a hint of the height *
	 * @param grabExcessiveWidth
	 *            grab excessive horizontal space
	 * @param grabExcessiveHeight
	 *            grab excessive vertical space
	 * @return
	 */
	public static Object createFormPageLayoutData(
		int widthHint, int heightHint, boolean grabExcessiveWidth, boolean grabExcessiveHeight) {
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, grabExcessiveWidth, grabExcessiveHeight);
		layoutData.widthHint = widthHint;
		layoutData.heightHint = heightHint;
		return layoutData;
	}

	/**
	 * Factory method for creating layout data for a form page
	 *
	 * @param horisontalAlignment
	 *            horisontal alignment
	 * @param verticalAlignment
	 *            vertical alignment
	 * @param widthHint
	 *            a hint of the width
	 * @param heightHint
	 *            a hint of the height
	 * @return
	 */
	public static Object createFormPageLayoutData(
		int horisontalAlignment, int verticalAlignment, int widthHint, int heightHint) {
		GridData layoutData = new GridData(horisontalAlignment, verticalAlignment, false, false);
		layoutData.widthHint = widthHint;
		layoutData.heightHint = heightHint;
		return layoutData;
	}

	public static Object createFormPageLayoutData(int columns, int rows) {
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		layoutData.widthHint = DEFAULT_LAYOUT_DATA_WIDTH;
		layoutData.heightHint = DEFAULT_LAYOUT_DATA_HEIGHT;
		layoutData.horizontalSpan = columns;
		layoutData.verticalSpan = rows;
		return layoutData;
	}

	public static void addGrabOnExpandLayoutData(Section section) {
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, section.isExpanded());
		section.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanging(ExpansionEvent e) {
				gd.grabExcessVerticalSpace = Boolean.TRUE.equals(e.data);
			}
		});
		section.setLayoutData(gd);
	}

}
