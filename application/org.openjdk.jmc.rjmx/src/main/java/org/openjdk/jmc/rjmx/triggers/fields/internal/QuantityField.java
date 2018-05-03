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
package org.openjdk.jmc.rjmx.triggers.fields.internal;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.rjmx.triggers.ISetting;

public class QuantityField extends Field {

	private KindOfQuantity<?> kind;
	private IQuantity min;
	private IQuantity max;

	public QuantityField(String id, String label, String defaultValue, String description) throws Exception {
		super(id, label, defaultValue, description);
	}

	@Override
	public int getType() {
		return ISetting.QUANTITY;
	}

	@Override
	void initDefaultValue(String defaultValue) {
		if (!setValue(defaultValue)) {
			setValue(getKind().getDefaultUnit().quantity(0).interactiveFormat());
		}
	}

	@Override
	String parsedValue(String value) throws Exception {
		IQuantity quantity = kind.parseInteractive(value);
		if ((min == null || min.compareTo(quantity) <= 0) && (max == null || max.compareTo(quantity) >= 0)) {
			return quantity.interactiveFormat();
		}
		return null;
	}

	@Override
	public IQuantity getQuantity() {
		try {
			return kind.parseInteractive(getValue());
		} catch (QuantityConversionException e) {
			return null;
		}
	}

	public KindOfQuantity<?> getKind() {
		if (kind == null) {
			return UnitLookup.NUMBER;
		}
		return kind;
	}

	/**
	 * Initialize the kind of this field and set the value to a valid default.
	 *
	 * @param kind
	 *            Kind of quantity
	 * @param defaultValue
	 *            The value to assign to this field if possible
	 * @param min
	 *            Minimum value to accept
	 * @param max
	 *            Maximum value to accept
	 */
	public void initKind(KindOfQuantity<?> kind, String defaultValue, IQuantity min, IQuantity max) {
		if (!getKind().equals(kind)) {
			setValue(null);
		}
		this.kind = kind;
		this.min = min;
		this.max = max;
		initDefaultValue(defaultValue);
		initDefaultPreferenceValue();
		updateListener();
	}

	public void setKind(KindOfQuantity<?> kind) {
		this.kind = kind;
	}

}
