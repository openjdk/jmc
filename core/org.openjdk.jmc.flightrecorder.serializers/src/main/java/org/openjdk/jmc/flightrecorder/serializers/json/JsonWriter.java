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

import java.io.Writer;

/**
 * This class provides a set of utility methods for serialising arbitrary objects to JSON, with
 * special logic for stringifying JMC types and Java primitives inherited from
 * {@link StructuredWriter}.
 * <p/>
 * It's a slightly modified version of the `jfr` command's JSONWriter.
 * <p/>
 *
 * @see <a href=
 *      "https://github.com/openjdk/jdk11/blob/master/src/jdk.jfr/share/classes/jdk/jfr/internal/cmd/JSONWriter.java">jdk.jfr.internal.cmd.JSONWriter</a>
 */
abstract class JsonWriter extends StructuredWriter {

	JsonWriter(Writer p) {
		super(p);
	}

	protected void writeField(boolean first, String fieldName, Object value) {
		nextField(first, fieldName);
		if (!writeIfNull(value)) {
			if (value instanceof Boolean) {
				writeAsString(value);
				return;
			}
			if (value instanceof Double) {
				double dValue = (Double) value;
				if (Double.isNaN(dValue) || Double.isInfinite(dValue)) {
					writeNull();
					return;
				}
				writeAsString(value);
				return;
			}
			if (value instanceof Float) {
				float fValue = (Float) value;
				if (Float.isNaN(fValue) || Float.isInfinite(fValue)) {
					writeNull();
					return;
				}
				writeAsString(value);
				return;
			}
			if (value instanceof Number) {
				write(stringify("", value, false));
				return;
			}
			writeStringValue(stringify(value));
		}
	}

	protected void nextElement(boolean first) {
		if (!first) {
			write(", ");
		}
	}

	protected void nextField(boolean first, String fieldName) {
		if (!first) {
			writeln(", ");
		}
		writeFieldName(fieldName);
	}

	protected boolean writeIfNull(Object value) {
		if (value == null) {
			writeNull();
			return true;
		}
		return false;
	}

	protected void writeNull() {
		write("null");
	}

	protected void writeObjectBegin() {
		writeln("{");
		indent();
	}

	protected void writeObjectEnd() {
		retract();
		writeln();
		writeIndent();
		write("}");
	}

	protected void writeArrayEnd() {
		write("]");
	}

	protected void writeArrayBegin() {
		write("[");
	}

	protected void writeStringValue(String value) {
		write("\"");
		writeEscaped(value);
		write("\"");
	}

	private void writeFieldName(String text) {
		writeIndent();
		write("\"");
		write(text);
		write("\": ");
	}

	private void writeEscaped(String text) {
		for (int i = 0; i < text.length(); i++) {
			writeEscaped(text.charAt(i));
		}
	}

	private void writeEscaped(char c) {
		if (c == '\b') {
			write("\\b");
			return;
		}
		if (c == '\n') {
			write("\\n");
			return;
		}
		if (c == '\t') {
			write("\\t");
			return;
		}
		if (c == '\f') {
			write("\\f");
			return;
		}
		if (c == '\r') {
			write("\\r");
			return;
		}
		if (c == '\"') {
			write("\\\"");
			return;
		}
		if (c == '\\') {
			write("\\\\");
			return;
		}
		/*
		 * we don't need to escape slashes if (c == '/') { print("\\/"); return; }
		 */
		if (c > 0x7F || c < 32) {
			write("\\u");
			// 0x10000 will pad with zeros.
			write(Integer.toHexString(0x10000 + c).substring(1));
			return;
		}
		write(c);
	}
}
