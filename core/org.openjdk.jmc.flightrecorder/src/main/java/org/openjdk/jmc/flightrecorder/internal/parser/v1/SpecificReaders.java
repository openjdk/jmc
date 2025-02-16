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
package org.openjdk.jmc.flightrecorder.internal.parser.v1;

import java.io.IOException;
import java.util.logging.Logger;

import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.flightrecorder.internal.InvalidJfrFileException;

public class SpecificReaders {
	private static Logger LOG = Logger.getLogger(SpecificReaders.class.getName());

	static class StackFrame2Reader extends ValueReaders.ReflectiveReader {
		boolean fallback;
		int methodIdx;
		int lineNumberIdx;
		int bytecodeIndexIdx;
		int typeIdx;

		<T> StackFrame2Reader(Class<T> klass, int fieldCount, ContentType<? super T> ct) {
			super(klass, fieldCount, ct);
		}

		@Override
		void addField(String identifier, String name, String description, ValueReaders.IValueReader reader)
				throws InvalidJfrFileException {
			super.addField(identifier, name, description, reader);
			int currentIdx = valueReaders.size() - 1;
			switch (identifier) {
			case "method":
				methodIdx = currentIdx;
				break;
			case "lineNumber":
				lineNumberIdx = currentIdx;
				break;
			case "bytecodeIndex":
				bytecodeIndexIdx = currentIdx;
				break;
			case "type":
				typeIdx = currentIdx;
				break;
			default:
				fallback = true; // invalid expected format, falling back to ReflectiveReader
				LOG.warning("unexpected fields for StackFrame2Reader: " + identifier);
			}
		}

		@Override
		public Object read(IDataInput in, boolean allowUnresolvedReference)
				throws IOException, InvalidJfrFileException {
			if (fallback) {
				return super.read(in, allowUnresolvedReference);
			}
			StructTypes.JfrFrame jfrFrame = new StructTypes.JfrFrame();
			jfrFrame.method = valueReaders.get(methodIdx).read(in, allowUnresolvedReference);
			jfrFrame.lineNumber = valueReaders.get(lineNumberIdx).read(in, allowUnresolvedReference);
			jfrFrame.bytecodeIndex = valueReaders.get(bytecodeIndexIdx).read(in, allowUnresolvedReference);
			jfrFrame.type = valueReaders.get(typeIdx).read(in, allowUnresolvedReference);
			return jfrFrame;
		}

		@Override
		public Object resolve(Object value) throws InvalidJfrFileException {
			if (!(value instanceof StructTypes.JfrFrame))
				throw new RuntimeException("Invalid object type, expected JfrFrame");
			StructTypes.JfrFrame jfrFrame = (StructTypes.JfrFrame) value;
			jfrFrame.method = valueReaders.get(0).resolve(jfrFrame.method);
			jfrFrame.lineNumber = valueReaders.get(1).resolve(jfrFrame.lineNumber);
			jfrFrame.bytecodeIndex = valueReaders.get(2).resolve(jfrFrame.bytecodeIndex);
			jfrFrame.type = valueReaders.get(3).resolve(jfrFrame.type);
			return value;
		}
	}
}
