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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;

/**
 * A general formatting toolkit.<br>
 * Should be merged with org.openjdk.jmc.common.FormatToolkit when we can use java 8 in
 * common.
 */
public class FormatToolkit {

	private static final String CRLF = "\r\n"; //$NON-NLS-1$
	private static final String LINE_SEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$

	private static final String TAB = "\t"; //$NON-NLS-1$
	private static final String SEMI_COLON = ";"; //$NON-NLS-1$
	private static final String QUOTE = "\""; //$NON-NLS-1$
	private static final String DOUBLE_QUOTE = "\"\""; //$NON-NLS-1$
	private static final String SPACE_INDENT = "   "; //$NON-NLS-1$

	public static Function<Stream<String>, String> getPreferredRowFormatter() {
		return CopySettings.getInstance().shouldCopyAsCsv() ? FormatToolkit::formatRowCsv : FormatToolkit::formatRow;
	}

	public static String formatRow(Stream<String> cells) {
		return cells.collect(Collectors.joining(TAB)) + LINE_SEPARATOR;
	}

	/**
	 * Formatting a string stream to a string. CSV format is compliant with RFC 4180, but uses
	 * semi-colon as separator to allow Excel to open files without using data import, as Excel data
	 * import doesn't seem to support new lines in cell values even though they are escaped.
	 */
	public static String formatRowCsv(Stream<String> cells) {
		return cells.map(str -> QUOTE + str.replaceAll(QUOTE, DOUBLE_QUOTE) + QUOTE)
				.collect(Collectors.joining(SEMI_COLON)) + CRLF;
	}

	public static Function<IStructuredSelection, Stream<String>> selectionFormatter(ILabelProvider ... lps) {
		return selection -> {
			Function<Stream<String>, String> rowFormatter = getPreferredRowFormatter();
			Function<Object, String> objectFormatter = o -> rowFormatter.apply(Stream.of(lps).map(lp -> lp.getText(o)));
			return FormatToolkit.formatSelection(selection, objectFormatter);
		};
	}

	public static Stream<String> formatSelection(
		IStructuredSelection selection, Function<Object, String> objectFormatter) {
		if (selection instanceof ITreeSelection) {
			if (CopySettings.getInstance().shouldIndentForStructure()) {
				Builder<String> builder = Stream.builder();
				for (TreePath path : ((ITreeSelection) selection).getPaths()) {
					int indents = path.getSegmentCount() - 1;
					for (int n = 0; n < indents; n++) {
						builder.accept(SPACE_INDENT);
					}
					builder.accept(objectFormatter.apply(path.getLastSegment()));
				}
				return builder.build();
			}
			return Stream.of(((ITreeSelection) selection).getPaths()).map(TreePath::getLastSegment)
					.map(objectFormatter);
		} else {
			List<?> list = selection.toList();
			return list.stream().map(objectFormatter);
		}
	}
}
