/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.serializers.json;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.FormatToolkit;

/**
 * This class is a hacked-up combination of the XML serializer in the JMC RecordingPrinter and the
 * `jfr` command's StructuredWriter.
 * <p/>
 * It provides a set of utility methods for serialising arbitrary values to JSON, with special logic
 * for JMC-specific types and Java primitives.
 * <p/>
 *
 * @see org.openjdk.jmc.flightrecorder.RecordingPrinter
 * @see <a href=
 *      "https://github.com/openjdk/jdk11/blob/master/src/jdk.jfr/share/classes/jdk/jfr/internal/cmd/StructuredWriter.java">jdk.jfr.internal.cmd.StructuredWriter</a>
 */
abstract class StructuredWriter {
	private final static String LINE_SEPARATOR = String.format("%n");

	private final Writer out;
	private final StringBuilder builder = new StringBuilder(4000);

	private char[] indentionArray = new char[0];
	private int indent = 0;
	private int column;

	StructuredWriter(Writer p) {
		out = p;
	}

	protected String stringify(Object value) {
		return stringify("", value, false);
	}

	protected String stringify(String indent, Object value) {
		return stringify(indent, value, true);
	}

	protected String stringify(String indent, Object value, boolean formatValues) {
		if (value instanceof IMCMethod) {
			return indent + stringifyMethod((IMCMethod) value);
		}
		if (value instanceof IMCType) {
			return indent + stringifyType((IMCType) value);
		}
		if (value instanceof IQuantity) {
			if (formatValues) {
				return ((IQuantity) value).displayUsing(IDisplayable.AUTO);
			} else {
				IQuantity quantity = (IQuantity) value;
				return String.valueOf(quantity.numberValue());
			}
		}
		// Workaround to maintain output after changed EventType.toString().
		if (value instanceof IDescribable) {
			String name = ((IDescribable) value).getName();
			return (name != null) ? name : value.toString();
		}
		if (value == null) {
			return "null"; //$NON-NLS-1$
		}
		if (value.getClass().isArray()) {
			StringBuilder buffer = new StringBuilder();
			Object[] values = (Object[]) value;
			buffer.append(" [").append(values.length).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
			for (Object o : values) {
				buffer.append(indent);
				buffer.append(stringify(indent + "  ", o)); //$NON-NLS-1$
			}
			return buffer.toString();
		}
		return value.toString();
	}

	private String stringifyType(IMCType type) {
		return formatPackage(type.getPackage()) + "." + //$NON-NLS-1$
				type.getTypeName();
	}

	protected String stringifyMethod(IMCMethod method) {
		return formatPackage(method.getType().getPackage()) + "." + //$NON-NLS-1$
				method.getType().getTypeName() + "#" + //$NON-NLS-1$
				method.getMethodName() + method.getFormalDescriptor();
	}

	private String formatPackage(IMCPackage mcPackage) {
		return FormatToolkit.getPackage(mcPackage);
	}

	protected final int getColumn() {
		return column;
	}

	// Flush to writer
	public final void flush() throws IOException {
		out.write(builder.toString());
		out.flush();
		builder.setLength(0);
	}

	public final void writeIndent() {
		builder.append(indentionArray, 0, indent);
		column += indent;
	}

	public final void writeln() {
		builder.append(LINE_SEPARATOR);
		column = 0;
	}

	public final void write(String ... texts) {
		for (String text : texts) {
			write(text);
		}
	}

	public final void writeAsString(Object o) {
		write(String.valueOf(o));
	}

	public final void write(String text) {
		builder.append(text);
		column += text.length();
	}

	public final void write(char c) {
		builder.append(c);
		column++;
	}

	public final void write(int value) {
		write(String.valueOf(value));
	}

	public final void indent() {
		indent += 2;
		updateIndent();
	}

	public final void retract() {
		indent -= 2;
		updateIndent();
	}

	public final void writeln(String text) {
		write(text);
		writeln();
	}

	private void updateIndent() {
		if (indent > indentionArray.length) {
			indentionArray = new char[indent];
			Arrays.fill(indentionArray, ' ');
		}
	}
}
