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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Control;
import org.openjdk.jmc.commands.CommandsPlugin;
import org.openjdk.jmc.commands.Statement;
import org.openjdk.jmc.commands.Value;
import org.openjdk.jmc.rcp.application.scripting.model.OperatingSystem;

/**
 * Responsible syntax coloring
 */
final class ScriptLineStyleListener implements LineStyleListener {
	private final Font m_font;
	private final Color m_red;
	private final Color m_green;
	private final Color m_white;
	private final Color m_black;
	private final Color m_commandColor;
	private final Font m_boldFont;
	private final Color m_stringColor;

	public ScriptLineStyleListener(Control control, OperatingSystem os) {
		m_font = new Font(control.getDisplay(), "Courier New", 10, SWT.NORMAL); //$NON-NLS-1$
		m_boldFont = new Font(control.getDisplay(), "Courier New", 10, SWT.BOLD); //$NON-NLS-1$
		m_commandColor = new Color(control.getDisplay(), 127, 0, 85);
		m_red = new Color(control.getDisplay(), 255, 0, 0);
		m_black = new Color(control.getDisplay(), 0, 0, 0);
		m_white = new Color(control.getDisplay(), 255, 255, 255);
		m_green = new Color(control.getDisplay(), 63, 127, 95);
		m_stringColor = new Color(control.getDisplay(), 42, 0, 255);

		addResourceDisposer(control, m_font, m_red, m_black, m_white, m_green, m_commandColor, m_boldFont,
				m_stringColor);
	}

	private void addResourceDisposer(Control control, final Resource ... resources) {
		control.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				for (Resource resource : resources) {
					resource.dispose();
				}
			}
		});
	}

	@Override
	public void lineGetStyle(LineStyleEvent event) {
		StyledText s = (StyledText) event.widget;
		String text = event.lineText;
		event.styles = processLine(event.lineOffset, text, s.getLineAtOffset(event.lineOffset));
	}

	private StyleRange[] processLine(int lineOffset, String text, int lineNumber) {
		try {
			List<Statement> stats = CommandsPlugin.getDefault().parse(text);
			if (stats.isEmpty()) {
				return createCommentStyle(lineOffset, text.length());
			} else {
				return createOKStyle(stats.get(0), text, lineOffset, text.length());
			}
		} catch (ParseException pe) {
			return createCompileError(lineOffset, pe.getErrorOffset(), text.length());
		}
	}

	private StyleRange[] createCommentStyle(int offset, int length) {
		StyleRange style = new StyleRange(offset, offset + length, m_green, m_white);
		style.font = m_font;
		return new StyleRange[] {style};
	}

	private StyleRange[] createCompileError(int offset, int errorOffset, int length) {
		StyleRange style = new StyleRange(offset + errorOffset, length - errorOffset, m_black, m_white);
		style.underline = true;
		style.underlineStyle = SWT.UNDERLINE_SQUIGGLE;
		style.underlineColor = m_red;
		style.font = m_font;
		return new StyleRange[] {style};
	}

	private StyleRange[] createOKStyle(Statement statement, String text, int offset, int length) {
		List<StyleRange> srs = new ArrayList<>();
		srs.add(createCommandStyle(statement, text, offset));
		Value lastValue = null;
		for (Value current : statement.getValues()) {
			if (lastValue != null) {
				srs.add(createParameterStyle(current, lastValue.getPosition() + offset,
						current.getPosition() - lastValue.getPosition()));
			}
			lastValue = current;
		}
		if (lastValue != null) {
			srs.add(createParameterStyle(lastValue, lastValue.getPosition() + offset,
					text.length() - lastValue.getPosition()));
		}
		return srs.toArray(new StyleRange[srs.size()]);
	}

	private StyleRange createCommandStyle(Statement statement, String text, int offset) {
		String keyword = statement.getCommand().getIdentifier();
		int commandStart = offset + text.indexOf(keyword);
		StyleRange commandStyle = new StyleRange(commandStart, keyword.length(), m_black, m_white);
		commandStyle.font = m_font;
		return commandStyle;
	}

	private StyleRange createParameterStyle(Value v, int start, int length) {
		StyleRange parameterStyle = new StyleRange();
		parameterStyle.start = start;
		parameterStyle.length = length;
		parameterStyle.background = m_white;

		String type = v.getParameter().getType();
		if (type.equals("string")) //$NON-NLS-1$
		{
			parameterStyle.foreground = m_stringColor;
			parameterStyle.font = m_font;
		}
		if (type.equals("boolean")) //$NON-NLS-1$
		{
			parameterStyle.foreground = m_commandColor;
			parameterStyle.font = m_boldFont;
		}
		if (type.equals("number")) //$NON-NLS-1$
		{
			parameterStyle.foreground = m_black;
			parameterStyle.font = m_font;
		}

		return parameterStyle;
	}
}
