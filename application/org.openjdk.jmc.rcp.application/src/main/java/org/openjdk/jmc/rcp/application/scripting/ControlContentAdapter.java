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
package org.openjdk.jmc.rcp.application.scripting;

import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.fieldassist.IControlContentAdapter2;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;


// FIXME: clean up, we need to investigate further how this is supposed to work.
public final class ControlContentAdapter implements IControlContentAdapter, IControlContentAdapter2 {
	@Override
	public void setControlContents(Control control, String contents, int cursorPosition) {
		// ignore
	}

	int getMaxMatch(String contents, String buffer) {
		for (int n = 0; n < contents.length(); n++) {
			String s = contents.substring(0, contents.length() - n);
			if (buffer.endsWith(s)) {
				return n;
			}
		}
		return -1;
	}

	@Override
	public void insertControlContents(Control control, String contents, int cursorPosition) {
		StyledText st = (StyledText) control;
		Point selection = st.getSelection();
		if (selection.x == selection.y) {
			int start = Math.max(0, selection.x - contents.length());
			int maxMatch = getMaxMatch(contents, st.getText(start, selection.x - 1));
			String insert;
			if (maxMatch == -1) {
				insert = contents + ' ';
			} else {
				insert = contents.substring(contents.length() - maxMatch) + ' ';
			}
			st.insert(insert);
			st.setSelection(selection.x + insert.length());

		}
		setControlContents(control, contents, cursorPosition);
	}

	@Override
	public String getControlContents(Control control) {
		StyledText st = (StyledText) control;
		int lineIndex = st.getLineAtOffset(st.getSelection().x);
		return st.getLine(lineIndex);
	}

	@Override
	public int getCursorPosition(Control control) {
		return -1;
	}

	@Override
	public Rectangle getInsertionBounds(Control control) {
		StyledText st = (StyledText) control;
		int position = st.getSelection().x;
		Point location = st.getLocationAtOffset(position);
		int width = (int) (.9 * control.getDisplay().getBounds().width);
		return new Rectangle(0, location.y, width, 100);
	}

	@Override
	public void setCursorPosition(Control control, int index) {
		// ignore
	}

	@Override
	public Point getSelection(Control control) {
		StyledText st = (StyledText) control;
		return st.getSelection();
	}

	@Override
	public void setSelection(Control control, Point range) {
		StyledText st = (StyledText) control;
		st.setSelection(range);
	}
}
