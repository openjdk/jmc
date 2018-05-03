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
package org.openjdk.jmc.flightrecorder.internal.parser.v0.factories;

import org.openjdk.jmc.common.IMCOldObject;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.MCOldObject;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.ValueDescriptor;

public class OldObjectFactory implements IPoolFactory<IMCOldObject> {

	private final int addressIndex;
	private final int typeIndex;
	private final int objectDescriptionIndex;
	private final int referrerIndex;
	private final int fieldNameIndex;
	private final int fieldModifierIndex;
	private final int arraySizeIndex;
	private final int arrayIndexIndex;

	public OldObjectFactory(ValueDescriptor[] descriptors) {
		addressIndex = ValueDescriptor.getIndex(descriptors, "address"); //$NON-NLS-1$
		typeIndex = ValueDescriptor.getIndex(descriptors, "objectClass"); //$NON-NLS-1$
		objectDescriptionIndex = ValueDescriptor.getIndex(descriptors, "objectDescription"); //$NON-NLS-1$
		referrerIndex = ValueDescriptor.getIndex(descriptors, "referrer"); //$NON-NLS-1$
		fieldNameIndex = ValueDescriptor.getIndex(descriptors, "fieldName"); //$NON-NLS-1$
		fieldModifierIndex = ValueDescriptor.getIndex(descriptors, "fieldModifier"); //$NON-NLS-1$
		arraySizeIndex = ValueDescriptor.getIndex(descriptors, "arraySize"); //$NON-NLS-1$
		arrayIndexIndex = ValueDescriptor.getIndex(descriptors, "arrayIndex"); //$NON-NLS-1$
	}

	@Override
	public IMCOldObject createObject(long identifier, Object source) {
		Object[] o = (Object[]) source;
		if (o != null) {
			IQuantity address = null;
			IMCType type = null;
			String objectDescription = null;
			IMCOldObject referrer = null;
			String fieldName = null;
			IQuantity fieldModifier = null;
			IQuantity arraySize = null;
			IQuantity arrayIndex = null;
			if (addressIndex != -1) {
				address = (IQuantity) o[addressIndex];
			}
			if (typeIndex != -1) {
				type = (IMCType) o[typeIndex];
			}
			if (objectDescriptionIndex != -1) {
				objectDescription = (String) o[objectDescriptionIndex];
			}
			if (referrerIndex != -1) {
				referrer = (IMCOldObject) o[referrerIndex];
			}
			if (fieldNameIndex != -1) {
				fieldName = (String) o[fieldNameIndex];
			}
			if (fieldModifierIndex != -1) {
				fieldModifier = (IQuantity) o[fieldModifierIndex];
			}
			if (arraySizeIndex != -1) {
				arraySize = (IQuantity) o[arraySizeIndex];
			}
			if (arrayIndexIndex != -1) {
				arrayIndex = (IQuantity) o[arrayIndexIndex];
			}
			return new MCOldObject(address, type, objectDescription, referrer, fieldName, fieldModifier, arraySize,
					arrayIndex);
		}
		return null;
	}

	@Override
	public ContentType<IMCOldObject> getContentType() {
		return UnitLookup.OLD_OBJECT;
	}
}
