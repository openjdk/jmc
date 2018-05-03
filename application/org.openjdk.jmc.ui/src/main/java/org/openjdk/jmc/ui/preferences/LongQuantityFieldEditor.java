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
package org.openjdk.jmc.ui.preferences;

import org.eclipse.swt.widgets.Composite;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.QuantityConversionException;

/**
 * A field editor that works with quantities while editing but stores the value as a primitive long.
 * It would be preferable to use quantities all the way but until a specific preference is migrated
 * to quantities all the way this class can be useful.
 */
public class LongQuantityFieldEditor extends QuantityFieldEditor {

	private final long maxIntegerValue;
	private final IUnit storageUnit;

	public LongQuantityFieldEditor(String name, String labelText, Composite parent, IUnit storageUnit) {
		this(name, labelText, parent, storageUnit, Long.MAX_VALUE);
	}

	public LongQuantityFieldEditor(String name, String labelText, Composite parent, IUnit storageUnit,
			long maxIntegerValue) {
		super(name, labelText, parent, storageUnit.getContentType());
		this.storageUnit = storageUnit;
		this.maxIntegerValue = maxIntegerValue;
	}

	@Override
	protected void validateQuantity(IQuantity value) throws QuantityConversionException {
		super.validateQuantity(value);
		IQuantity valueInStoragePrecision = storageUnit.quantity(value.longValueIn(storageUnit, maxIntegerValue));
		if (valueInStoragePrecision.compareTo(value) != 0) {
			/*
			 * FIXME: Should we report the closest valid value rather than the supported precision?
			 * See comment in belowPrecision().
			 */
			throw QuantityConversionException.belowPrecision(value, storageUnit.quantity(1));
		}
	}

	@Override
	protected IQuantity doGetQuantity(boolean defaultValue) {
		if (defaultValue) {
			return storageUnit.quantity(getPreferenceStore().getDefaultLong(getPreferenceName()));
		} else {
			return storageUnit.quantity(getPreferenceStore().getLong(getPreferenceName()));
		}

	}

	@Override
	protected void doSetQuantity(IQuantity value) throws QuantityConversionException {
		getPreferenceStore().setValue(getPreferenceName(), value.longValueIn(storageUnit, maxIntegerValue));
	}
}
