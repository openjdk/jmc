/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.ui.ai.views;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

/**
 * Strips markdown syntax and applies corresponding StyleRanges to a StyledText widget. Supports
 * bold, italic, inline code, code blocks, and headers. Markdown delimiters are removed from the
 * displayed text.
 */
public class MarkdownStyler {

	private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```\\w*\\n(.*?)```", Pattern.DOTALL); //$NON-NLS-1$
	private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,3})\\s+(.+)$", Pattern.MULTILINE); //$NON-NLS-1$
	private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*"); //$NON-NLS-1$
	private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)"); //$NON-NLS-1$
	private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`"); //$NON-NLS-1$

	private Font boldFont;
	private Font italicFont;
	private Font boldLargeFont;
	private Font monoFont;
	private Color codeBackground;

	public MarkdownStyler(Display display) {
		FontData[] defaultFontData = display.getSystemFont().getFontData();
		String defaultName = defaultFontData[0].getName();
		int defaultHeight = defaultFontData[0].getHeight();

		boldFont = new Font(display, defaultName, defaultHeight, SWT.BOLD);
		italicFont = new Font(display, defaultName, defaultHeight, SWT.ITALIC);
		boldLargeFont = new Font(display, defaultName, defaultHeight + 2, SWT.BOLD);
		monoFont = new Font(display, "Consolas", defaultHeight, SWT.NORMAL); //$NON-NLS-1$

		boolean dark = org.openjdk.jmc.ui.common.util.ThemeUtils.isDarkTheme();
		codeBackground = dark ? new Color(display, 50, 50, 50) : new Color(display, 235, 235, 235);
	}

	/**
	 * Processes the text in the widget starting at {@code offset}: strips markdown syntax
	 * characters and applies styled ranges for bold, italic, code, and headers.
	 */
	public void applyStyles(StyledText widget, int offset, Color baseColor) {
		String raw = widget.getText().substring(offset);
		StyledResult result = processMarkdown(raw, offset, baseColor);

		// Replace the raw markdown with the clean text
		widget.replaceTextRange(offset, raw.length(), result.cleanText);

		// Apply the styles
		for (StyleRange style : result.styles) {
			widget.setStyleRange(style);
		}
	}

	private StyledResult processMarkdown(String raw, int globalOffset, Color baseColor) {
		List<Span> spans = new ArrayList<>();

		// Pass 1: find code blocks (highest priority, content inside is not processed further)
		findSpans(CODE_BLOCK_PATTERN, raw, SpanType.CODE_BLOCK, spans);

		// Pass 2: find headers (only outside code blocks)
		findSpans(HEADER_PATTERN, raw, SpanType.HEADER, spans);

		// Pass 3: find bold (only outside code blocks)
		findSpans(BOLD_PATTERN, raw, SpanType.BOLD, spans);

		// Pass 4: find italic (only outside code blocks and bold)
		findSpans(ITALIC_PATTERN, raw, SpanType.ITALIC, spans);

		// Pass 5: find inline code (only outside code blocks)
		findSpans(INLINE_CODE_PATTERN, raw, SpanType.INLINE_CODE, spans);

		// Sort spans by their start position in the raw text
		spans.sort((a, b) -> Integer.compare(a.rawStart, b.rawStart));

		// Build the clean text and collect style ranges
		StringBuilder clean = new StringBuilder();
		List<StyleRange> styles = new ArrayList<>();
		int cursor = 0;

		for (Span span : spans) {
			if (span.rawStart < cursor) {
				continue; // overlapping span, skip
			}
			// Append text before this span
			clean.append(raw, cursor, span.rawStart);

			// Record the position in clean text where this span's content starts
			int cleanStart = globalOffset + clean.length();
			clean.append(span.content);

			// Create style range
			StyleRange style = new StyleRange();
			style.start = cleanStart;
			style.length = span.content.length();
			style.foreground = baseColor;
			applySpanStyle(span.type, style);
			styles.add(style);

			cursor = span.rawEnd;
		}

		// Append remaining text after last span
		clean.append(raw, cursor, raw.length());

		return new StyledResult(clean.toString(), styles);
	}

	private void applySpanStyle(SpanType type, StyleRange style) {
		switch (type) {
		case BOLD:
			style.font = boldFont;
			break;
		case ITALIC:
			style.font = italicFont;
			break;
		case HEADER:
			style.font = boldLargeFont;
			break;
		case INLINE_CODE:
			style.font = monoFont;
			style.background = codeBackground;
			break;
		case CODE_BLOCK:
			style.font = monoFont;
			style.background = codeBackground;
			break;
		}
	}

	private void findSpans(Pattern pattern, String text, SpanType type, List<Span> spans) {
		Matcher m = pattern.matcher(text);
		while (m.find()) {
			int rawStart = m.start();
			int rawEnd = m.end();
			if (isOverlapping(rawStart, rawEnd, spans)) {
				continue;
			}
			String content;
			switch (type) {
			case CODE_BLOCK:
				content = m.group(1); // content inside ``` ... ```
				break;
			case HEADER:
				content = m.group(2); // text after ### (strip the # prefix)
				break;
			default:
				content = m.group(1); // content inside delimiters
				break;
			}
			spans.add(new Span(type, rawStart, rawEnd, content));
		}
	}

	private boolean isOverlapping(int start, int end, List<Span> existing) {
		for (Span s : existing) {
			if (start < s.rawEnd && end > s.rawStart) {
				return true;
			}
		}
		return false;
	}

	public void dispose() {
		if (boldFont != null) {
			boldFont.dispose();
		}
		if (italicFont != null) {
			italicFont.dispose();
		}
		if (boldLargeFont != null) {
			boldLargeFont.dispose();
		}
		if (monoFont != null) {
			monoFont.dispose();
		}
		if (codeBackground != null) {
			codeBackground.dispose();
		}
	}

	private enum SpanType {
		BOLD, ITALIC, INLINE_CODE, CODE_BLOCK, HEADER
	}

	private static class Span {
		final SpanType type;
		final int rawStart;
		final int rawEnd;
		final String content;

		Span(SpanType type, int rawStart, int rawEnd, String content) {
			this.type = type;
			this.rawStart = rawStart;
			this.rawEnd = rawEnd;
			this.content = content;
		}
	}

	private static class StyledResult {
		final String cleanText;
		final List<StyleRange> styles;

		StyledResult(String cleanText, List<StyleRange> styles) {
			this.cleanText = cleanText;
			this.styles = styles;
		}
	}
}
