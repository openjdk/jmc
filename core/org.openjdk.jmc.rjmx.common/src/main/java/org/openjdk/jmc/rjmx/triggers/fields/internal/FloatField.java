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

/**
 * Field for holding integer values
 */
public class FloatField extends Field {
	private float m_min;
	private float m_max;

	public FloatField(String fieldId, String fieldLabel, String fieldValue, String fieldDescription, String min,
			String max) throws Exception {
		super(fieldId, fieldLabel, fieldValue, fieldDescription);

		m_min = getLimit(min, Float.MIN_VALUE);
		m_max = getLimit(max, Float.MAX_VALUE);

		// If a limit isn't valid, remove it.
		if (m_min > getFloat().floatValue()) {
			m_min = Float.MIN_VALUE;
		}
		if (m_max < getFloat().floatValue()) {
			m_max = Float.MAX_VALUE;
		}

	}

	@Override
	void initDefaultValue(String defaultValue) {
		m_min = Float.MIN_VALUE;
		m_max = Float.MAX_VALUE;

		if (!setValue(defaultValue)) {
			setValue(Integer.toString(0));
		}

	}

	@Override
	protected String parsedValue(String value) {
		return Float.toString(Float.parseFloat(value));
	}

	@Override
	public Float getFloat() {
		return Float.valueOf((getValue()));
	}

	@Override
	public int getType() {
		return FLOAT;
	}

	float getLimit(String limit, float defaultValue) {
		try {
			return Float.parseFloat(limit);
		} catch (Exception e) {

		}
		return defaultValue;
	}

}
