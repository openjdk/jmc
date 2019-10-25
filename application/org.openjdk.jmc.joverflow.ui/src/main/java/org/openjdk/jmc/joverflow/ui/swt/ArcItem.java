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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Item;

// An ArcItem represents an arc in a PieChart
public class ArcItem extends Item {
	private final PieChart mParent;
	private final int mStyle;

	private int mAngle = 0;
	private Color mColor;

	public ArcItem(PieChart parent, int style) {
		this(parent, style, parent.getItemCount());
	}

	public ArcItem(PieChart parent, int style, int index) {
		super(parent, style, index);
		this.mParent = parent;
		this.mStyle = style;

		parent.createItem(this, index);
	}

	// ArcItem and PieChart doesn't check if sum of all arcs' angle adds to more than 360
	public void setAngle(int angle) {
		if (angle < 0) {
			SWT.error(SWT.ERROR_INVALID_RANGE);
			return;
		}

		if (angle > 360) {
			mAngle = angle % 360;
		} else {
			mAngle = angle;
		}
		mParent.redraw();
	}

	public int getAngle() {
		return mAngle;
	}

	public void setColor(Color mColor) {
		this.mColor = mColor;
		mParent.redraw();
	}

	public Color getColor() {
		return mColor;
	}

	void paintArc(GC gc, Point center, int radius, int startAngle, double zoomRatio, boolean paintArcBorder) {
		if (mAngle < 0) {
			SWT.error(SWT.ERROR_INVALID_RANGE);
		}

		if (mColor != null) {
			gc.setBackground(this.mColor);
		}

		int outerRadius = (int) (radius * zoomRatio);

		gc.fillArc(center.x - outerRadius, center.y - outerRadius, outerRadius * 2, outerRadius * 2, startAngle,
				mAngle);

		if (paintArcBorder) {
			gc.drawArc(center.x - outerRadius, center.y - outerRadius, outerRadius * 2, outerRadius * 2, startAngle,
					mAngle);
			if (zoomRatio != 1 && mAngle < 360) {
				gc.drawLine((int) (center.x + Math.cos(Math.toRadians(startAngle)) * radius),
						(int) (center.y - Math.sin(Math.toRadians(startAngle)) * radius),
						(int) (center.x + Math.cos(Math.toRadians(startAngle)) * outerRadius),
						(int) (center.y - Math.sin(Math.toRadians(startAngle)) * outerRadius));
				gc.drawLine((int) (center.x + Math.cos(Math.toRadians(startAngle + mAngle)) * radius),
						(int) (center.y - Math.sin(Math.toRadians(startAngle + mAngle)) * radius),
						(int) (center.x + Math.cos(Math.toRadians(startAngle + mAngle)) * outerRadius),
						(int) (center.y - Math.sin(Math.toRadians(startAngle + mAngle)) * outerRadius));
			}
		}

		if ((mStyle & SWT.BORDER) == SWT.BORDER && mAngle < 360) {
			gc.drawLine(center.x, center.y, (int) (center.x + Math.cos(Math.toRadians(startAngle)) * radius),
					(int) (center.y - Math.sin(Math.toRadians(startAngle)) * radius));
			gc.drawLine(center.x, center.y, (int) (center.x + Math.cos(Math.toRadians(startAngle + mAngle)) * radius),
					(int) (center.y - Math.sin(Math.toRadians(startAngle + mAngle)) * radius));
		}
	}
}
