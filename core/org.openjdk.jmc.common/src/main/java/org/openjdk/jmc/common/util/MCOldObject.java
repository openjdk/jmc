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
package org.openjdk.jmc.common.util;

import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCOldObject;
import org.openjdk.jmc.common.IMCOldObjectArray;
import org.openjdk.jmc.common.IMCOldObjectField;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;

/**
 * Base implementation of the {@link IMCOldObject} interface.
 */
// FIXME: Move MC* classes and related toolkits to a separate package?
public class MCOldObject implements IMCOldObject {

	private final IQuantity address;
	private final MCOldObjectArray array;
	private final MCOldObjectField field;
	private final String objectDescription;
	private final IMCOldObject referrer;
	private final IMCType type;

	private static class MCOldObjectArray implements IMCOldObjectArray {

		Long arrayIndex;
		Long arraySize;

		public MCOldObjectArray(IQuantity arrayIndex, IQuantity arraySize) {
			try {
				this.arrayIndex = arrayIndex.longValueIn(UnitLookup.NUMBER_UNITY);
				this.arraySize = arraySize.longValueIn(UnitLookup.NUMBER_UNITY);
			} catch (QuantityConversionException e) {
				Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
						"Could not convert number to number for array info", e); //$NON-NLS-1$
				this.arrayIndex = null;
				this.arraySize = null;
			}
		}

		@Override
		public Long getSize() {
			return arraySize;
		}

		@Override
		public Long getIndex() {
			return arrayIndex;
		}

	}

	private static class MCOldObjectField implements IMCOldObjectField {

		String fieldName;
		Integer fieldModifier;

		public MCOldObjectField(String fieldName, Integer fieldModifier) {
			this.fieldName = fieldName;
			this.fieldModifier = fieldModifier;
		}

		@Override
		public String getName() {
			return fieldName;
		}

		@Override
		public Integer getModifier() {
			return fieldModifier;
		}

	}

	/**
	 * Create a new old object instance.
	 *
	 * @param address
	 *            object address, see {@link IMCOldObject#getAddress()}
	 * @param type
	 *            object type
	 * @param objectDescription
	 *            object description
	 * @param referrer
	 *            Referrer object, see {@link IMCOldObject#getReferrer()}. {@code null} if there is
	 *            no referring object.
	 * @param field
	 *            Name of the referring field, see {@link IMCOldObjectField#getName()}. {@code null}
	 *            if there is no referring field.
	 * @param fieldModifier
	 *            Modifier of the referring field, see {@link IMCOldObjectField#getModifier()}.
	 *            {@code null} if there is no referring field.
	 * @param arraySize
	 *            The array size if {@code field} refers to an array, see
	 *            {@link IMCOldObjectArray#getSize()}. {@code null} if the field is not an array.
	 * @param arrayIndex
	 *            The array index that refers to this object if {@code field} refers to an array,
	 *            see {@link IMCOldObjectArray#getIndex()}. {@code null} if the field is not an
	 *            array.
	 */
	public MCOldObject(IQuantity address, IMCType type, String objectDescription, IMCOldObject referrer, String field,
			IQuantity fieldModifier, IQuantity arraySize, IQuantity arrayIndex) {
		this.address = address;
		this.type = type;
		this.objectDescription = objectDescription;
		this.referrer = referrer;
		this.field = fieldModifier != null ? new MCOldObjectField(field, (int) fieldModifier.longValue()) : null;
		this.array = (arraySize != null && arraySize.longValue() >= 0) ? new MCOldObjectArray(arrayIndex, arraySize)
				: null;
	}

	@Override
	public IQuantity getAddress() {
		return address;
	}

	@Override
	public IMCOldObjectArray getReferrerArray() {
		return array;
	}

	@Override
	public IMCOldObjectField getReferrerField() {
		return field;
	}

	@Override
	public String getDescription() {
		return objectDescription;
	}

	@Override
	public IMCOldObject getReferrer() {
		return referrer;
	}

	@Override
	public IMCType getType() {
		return type;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IMCOldObject) {
			return Objects.equals(((IMCOldObject) obj).getAddress(), address);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(address);
	}

	@Override
	public String toString() {
		String s = getType().getFullName();
		if (getReferrerArray() != null) {
			s = s.substring(0, s.length() - 1) + getReferrerArray().getIndex().longValue() + "]"; //$NON-NLS-1$
		}
		if (getReferrerField() != null) {
			Integer modifier = getReferrerField().getModifier();
			if (modifier != null) {
				if (modifier == 0) {
					s += "." + getReferrerField().getName(); //$NON-NLS-1$
				} else {
					s = Modifier.toString(modifier) + " " + s + "." + getReferrerField().getName(); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		s += " @ " + getAddress().displayUsing(IDisplayable.AUTO); //$NON-NLS-1$
		return s;
	}

	@Override
	public int getReferrerSkip() {
		return 0;
	}
}
