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
package org.openjdk.jmc.flightrecorder.internal.parser.v0.model;

import org.openjdk.jmc.flightrecorder.internal.InvalidJfrFileException;
import org.openjdk.jmc.flightrecorder.internal.util.ParserToolkit;

/**
 * Class responsible for holding metadata about an event value
 */
public final class ValueDescriptor implements Cloneable {
	private final String identifier;
	private final String name;
	private final String description;
	private final Transition transition;
	private final DataType dataType;
	private final String relationalKey;
	private final int contentType;
	private final DataStructure[] dataStructures;
	private final int structureIndex;

	public ValueDescriptor(String identifier, String name, String description, Transition transition, DataType dataType,
			String relationalKey, int contentType, DataStructure[] dataStructures, int structureIndex) {
		this.identifier = identifier;
		this.name = name;
		this.description = description;
		this.transition = transition;
		this.dataType = dataType;
		this.relationalKey = relationalKey;
		this.contentType = contentType;
		this.dataStructures = dataStructures;
		this.structureIndex = structureIndex;
	}

	public String getIdentifier() {
		return identifier;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Transition getTransition() {
		return transition;
	}

	public DataType getDataType() {
		return dataType;
	}

	public String getRelationalKey() {
		return relationalKey;
	}

	public int getContentType() {
		return contentType;
	}

	public DataType getInnerDataType() throws InvalidJfrFileException {
		ParserToolkit.assertValue(dataType, DataType.ARRAY);
		return ParserToolkit.get(DataType.values(), structureIndex);
	}

	public ValueDescriptor[] getChildren() throws InvalidJfrFileException {
		ParserToolkit.assertValue(dataType, DataType.STRUCT, DataType.STRUCTARRAY);
		return ParserToolkit.get(dataStructures, structureIndex).getValueDescriptors();
	}

	@Override
	public String toString() {
		String toString = ""; //$NON-NLS-1$

		toString += "Name: " + getName() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		toString += "Identifier: " + getIdentifier() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		toString += "Datatype: " + getDataType() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		toString += "Description: " + getDescription() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		toString += "Transition : " + getTransition() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		toString += "Relation Key: " + getRelationalKey() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$

		return toString;
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new Error("Can't happen"); //$NON-NLS-1$
		}
	}

	public static int getIndex(ValueDescriptor[] descriptors, String identifier) {
		for (int n = 0; n < descriptors.length; n++) {
			if (identifier.equals(descriptors[n].getIdentifier())) {
				return n;
			}
		}
		return -1;
	}
}
