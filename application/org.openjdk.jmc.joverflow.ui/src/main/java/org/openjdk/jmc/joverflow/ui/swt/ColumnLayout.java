/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.joverflow.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

public class ColumnLayout extends Layout {
	// fixed margin and spacing
	private static final int MARGIN = 4;
	private static final int SPACING = 2;

	// cache
	private Point[] sizes;
	private int maxWidth, totalHeight;

	protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
		Control[] children = composite.getChildren();
		if (flushCache || sizes == null || sizes.length != children.length) {
			initialize(children);
		}
		int width = wHint, height = hHint;
		if (wHint == SWT.DEFAULT)
			width = maxWidth;
		if (hHint == SWT.DEFAULT)
			height = totalHeight;
		return new Point(width + 2 * MARGIN, height + 2 * MARGIN);
	}

	protected void layout(Composite composite, boolean flushCache) {
		Control[] children = composite.getChildren();
		if (flushCache || sizes == null || sizes.length != children.length) {
			initialize(children);
		}
		Rectangle rect = composite.getClientArea();
		int y = MARGIN;
		int width = Math.max(rect.width - 2 * MARGIN, maxWidth);
		for (int i = 0; i < children.length; i++) {
			int height = sizes[i].y;
			children[i].setBounds(MARGIN, y, width, height);
			y += height + SPACING;
		}
	}

	private void initialize(Control[] children) {
		maxWidth = 0;
		totalHeight = 0;
		sizes = new Point[children.length];
		for (int i = 0; i < children.length; i++) {
			sizes[i] = children[i].computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
			maxWidth = Math.max(maxWidth, sizes[i].x);
			totalHeight += sizes[i].y;
		}
		totalHeight += (children.length - 1) * SPACING;
	}
}
