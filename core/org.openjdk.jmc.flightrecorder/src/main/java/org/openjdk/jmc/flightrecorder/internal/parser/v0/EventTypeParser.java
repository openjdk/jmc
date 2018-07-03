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
package org.openjdk.jmc.flightrecorder.internal.parser.v0;

import org.openjdk.jmc.flightrecorder.internal.InvalidJfrFileException;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.DataStructure;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.EventTypeDescriptor;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.ValueDescriptor;
import org.openjdk.jmc.flightrecorder.internal.util.ParserToolkit;

/**
 * Class responsible for reading an {@link EventTypeDescriptor}
 */
final class EventTypeParser implements IArrayElementParser<EventTypeDescriptor> {
	private final DataStructure[] dataStructures;

	public EventTypeParser(DataStructure[] dataStructures) {
		this.dataStructures = dataStructures;
	}

	@Override
	public EventTypeDescriptor readElement(byte[] data, Offset offset) throws InvalidJfrFileException {
		int id = NumberReaders.readInt(data, offset);
		String name = UTFStringParser.readString(data, offset);
		String description = UTFStringParser.readString(data, offset);
		String path = UTFStringParser.readString(data, offset);
		boolean hasStartTime = BooleanReader.readBoolean(data, offset);
		boolean hasThread = BooleanReader.readBoolean(data, offset);
		boolean canHaveStacktrace = BooleanReader.readBoolean(data, offset);
		boolean isRequestable = BooleanReader.readBoolean(data, offset);
		ValueDescriptor[] dataStructure = ParserToolkit.get(dataStructures, NumberReaders.readInt(data, offset))
				.getValueDescriptors();
		// expansion not used
		NumberReaders.readInt(data, offset);

		return new EventTypeDescriptor(id, name, hasStartTime, hasThread, canHaveStacktrace, isRequestable,
				dataStructure, description, path);
	}

	@Override
	public EventTypeDescriptor[] createArray(int length) {
		return new EventTypeDescriptor[length];
	}

}
