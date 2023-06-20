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
final public class IntegerField extends Field {
	private int m_min;
	private int m_max;

	public IntegerField(String id, String label, String defaultValue, String description, String min, String max)
			throws Exception {
		super(id, label, defaultValue, description);

		m_min = getLimit(min, Integer.MIN_VALUE);
		m_max = getLimit(max, Integer.MAX_VALUE);

		// If a limit isn't valid, remove it.
		if (m_min > getInteger().intValue()) {
			m_min = Integer.MIN_VALUE;
		}
		if (m_max < getInteger().intValue()) {
			m_max = Integer.MAX_VALUE;
		}
	}

	@Override
	void initDefaultValue(String defaultValue) {
		// set largest possible boundaries.
		m_min = Integer.MIN_VALUE;
		m_max = Integer.MAX_VALUE;

		// Now, try to set default value from XML.
		if (!setValue(defaultValue)) {
			setValue(Integer.toString(0));
		}

	}

	@Override
	String parsedValue(String value) {
		int v = Integer.parseInt(value);
		if (v >= m_min && v <= m_max) {
			return Integer.toString(v);
		} else {
			return null;
		}
	}

	@Override
	public Integer getInteger() {
		return Integer.valueOf(getValue());
	}

	@Override
	public int getType() {
		return INTEGER;
	}

	int getLimit(String limit, int defaultValue) {
		try {
			return Integer.parseInt(limit);
		} catch (Exception e) {

		}
		return defaultValue;
	}
}
