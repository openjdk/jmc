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
package org.openjdk.jmc.common.messages.internal;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.common.messages.internal.messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	public static final String FormatToolkit_DEFAULT_PACKAGE = "FormatToolkit_DEFAULT_PACKAGE"; //$NON-NLS-1$
	public static final String ItemAggregate_AVERAGE = "ItemAggregate_AVERAGE"; //$NON-NLS-1$
	public static final String ItemAggregate_COUNT = "ItemAggregate_COUNT"; //$NON-NLS-1$
	public static final String ItemAggregate_DISTINCT = "ItemAggregate_DISTINCT"; //$NON-NLS-1$
	public static final String ItemAggregate_FIRST = "ItemAggregate_FIRST"; //$NON-NLS-1$
	public static final String ItemAggregate_LAST = "ItemAggregate_LAST"; //$NON-NLS-1$
	public static final String ItemAggregate_LONGEST = "ItemAggregate_LONGEST"; //$NON-NLS-1$
	public static final String ItemAggregate_MAXIMUM = "ItemAggregate_MAXIMUM"; //$NON-NLS-1$
	public static final String ItemAggregate_MINIMUM = "ItemAggregate_MINIMUM"; //$NON-NLS-1$
	public static final String ItemAggregate_SHORTEST = "ItemAggregate_SHORTEST"; //$NON-NLS-1$
	public static final String ItemAggregate_STDDEV = "ItemAggregate_STDDEV"; //$NON-NLS-1$
	public static final String ItemAggregate_STDDEVP = "ItemAggregate_STDDEVP"; //$NON-NLS-1$
	public static final String ItemAggregate_TOTAL = "ItemAggregate_TOTAL"; //$NON-NLS-1$
	public static final String ItemAggregate_VARIANCE = "ItemAggregate_VARIANCE"; //$NON-NLS-1$
	public static final String ItemAggregate_VARIANCEP = "ItemAggregate_VARIANCEP"; //$NON-NLS-1$
	public static final String MISSING_VALUE = "MISSING_VALUE"; //$NON-NLS-1$
	public static final String MISSING_VALUE_TOOLTIP = "MISSING_VALUE_TOOLTIP"; //$NON-NLS-1$
	public static final String QuantityConversionException_CONSTRAINTS_DO_NOT_MATCH = "QuantityConversionException_CONSTRAINTS_DO_NOT_MATCH"; //$NON-NLS-1$
	public static final String QuantityConversionException_NO_UNIT_MSG = "QuantityConversionException_NO_UNIT_MSG"; //$NON-NLS-1$
	public static final String QuantityConversionException_TOO_HIGH_MSG = "QuantityConversionException_TOO_HIGH_MSG"; //$NON-NLS-1$
	public static final String QuantityConversionException_TOO_LOW_MSG = "QuantityConversionException_TOO_LOW_MSG"; //$NON-NLS-1$
	public static final String QuantityConversionException_TOO_SMALL_MAGNITUDE_MSG = "QuantityConversionException_TOO_SMALL_MAGNITUDE_MSG"; //$NON-NLS-1$
	public static final String QuantityConversionException_UNKNOWN_UNIT_MSG = "QuantityConversionException_UNKNOWN_UNIT_MSG"; //$NON-NLS-1$
	public static final String QuantityConversionException_UNPARSEABLE_MSG = "QuantityConversionException_UNPARSEABLE_MSG"; //$NON-NLS-1$
	public static final String RangeContentType_FIELD_CENTER = "RangeContentType_FIELD_CENTER"; //$NON-NLS-1$
	public static final String RangeContentType_FIELD_END = "RangeContentType_FIELD_END"; //$NON-NLS-1$
	public static final String RangeContentType_FIELD_EXTENT = "RangeContentType_FIELD_EXTENT"; //$NON-NLS-1$
	public static final String RangeContentType_FIELD_START = "RangeContentType_FIELD_START"; //$NON-NLS-1$
	public static final String RangeContentType_NAME = "RangeContentType_NAME"; //$NON-NLS-1$
	public static final String TimestampKind_SINCE_1970_MSG = "TimestampKind_SINCE_1970_MSG"; //$NON-NLS-1$
	public static final String TypeHandling_MESSAGE_SIZE = "TypeHandling_MESSAGE_SIZE"; //$NON-NLS-1$
	public static final String UnitLookup_TIMESTAMP_OUT_OF_RANGE = "UnitLookup_TIMESTAMP_OUT_OF_RANGE"; //$NON-NLS-1$

	private Messages() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	public static String getString(String key, String def) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return def;
		}
	}
}
