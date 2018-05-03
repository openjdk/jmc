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
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.ContentTypeDescriptor;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.DataStructure;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.EventTypeDescriptor;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.ProducerDescriptor;

/**
 * Parses a producer
 */
final class ProducerParser implements IArrayElementParser<ProducerDescriptor> {
	private static final TypedArrayParser<String> RELATIONS_PARSER = new TypedArrayParser<>(UTFStringParser.INSTANCE);

	@Override
	public ProducerDescriptor readElement(byte[] data, Offset offset) throws InvalidJfrFileException {
		int id = NumberReaders.readInt(data, offset);
		String name = UTFStringParser.readString(data, offset);
		String desc = UTFStringParser.readString(data, offset);
		String uri = UTFStringParser.readString(data, offset);
		String[] relations = RELATIONS_PARSER.read(data, offset);
		DataStructureParser dataStructureParser = new DataStructureParser(relations);
		DataStructure[] dataStructures = dataStructureParser.read(data, offset);
		TypedArrayParser<EventTypeDescriptor> eventTypeParser = new TypedArrayParser<>(
				new EventTypeParser(dataStructures));
		EventTypeDescriptor[] eventTypes = eventTypeParser.read(data, offset);
		TypedArrayParser<ContentTypeDescriptor> contentTypeParser = new TypedArrayParser<>(
				new ContentTypeParser(dataStructures));
		ContentTypeDescriptor[] contentTypes = contentTypeParser.read(data, offset);

		return new ProducerDescriptor(name, desc, uri, id, eventTypes, contentTypes);
	}

	@Override
	public ProducerDescriptor[] createArray(int length) {
		return new ProducerDescriptor[length];
	}

}
