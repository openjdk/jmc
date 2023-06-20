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
package org.openjdk.jmc.rjmx.subscription.internal;

import java.util.Properties;

import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.IMRITransformation;
import org.openjdk.jmc.rjmx.subscription.MRI;

/**
 * Base class for transformation depending on a single MRI for input.
 */
public abstract class AbstractSingleMRITransformation implements IMRITransformation {

	private static final String ATTRIBUTE_PROPERTY = "attribute"; //$NON-NLS-1$
	private static final String DISPLAY_NAME_PROPERTY = "displayName"; //$NON-NLS-1$
	private Properties m_transformationProperties;
	private MRI m_attribute;

	@Override
	public void setProperties(Properties properties) {
		m_transformationProperties = properties;
		m_attribute = MRI.createFromQualifiedName(m_transformationProperties.getProperty(ATTRIBUTE_PROPERTY));
	}

	@Override
	public MRI[] getAttributes() {
		return new MRI[] {m_attribute};
	}

	@Override
	public void extendMetadata(IMRIMetadataService metadataService, IMRIMetadata metadata) {
		MRITransformationToolkit.forwardMetadata(metadataService, metadata.getMRI(),
				metadataService.getMetadata(m_attribute), getDisplayNamePattern());
	}

	/**
	 * Will return the display name pattern for this transformation with a placeholder {0} for the
	 * attribute's display name.
	 *
	 * @return the display name pattern
	 */
	protected String getDisplayNamePattern() {
		return m_transformationProperties.getProperty(DISPLAY_NAME_PROPERTY);
	}

	/**
	 * Does a subtraction of the operands. Will try to use same type in return value as in
	 * parameters.
	 *
	 * @param minuend
	 *            the number to subtract from
	 * @param subtrahend
	 *            the number to subtract
	 * @return a number with the subtraction value
	 */
	protected Number subtract(Number minuend, Number subtrahend) {
		if (!minuend.getClass().equals(subtrahend.getClass())) {
			throw new IllegalArgumentException("Different type classes!"); //$NON-NLS-1$
		}
		if (minuend instanceof Integer) {
			return Integer.valueOf(minuend.intValue() - subtrahend.intValue());
		}
		if (minuend instanceof Long) {
			return Long.valueOf(minuend.longValue() - subtrahend.longValue());
		}
		if (minuend instanceof Byte) {
			return Byte.valueOf((byte) (minuend.intValue() - subtrahend.intValue()));
		}
		if (minuend instanceof Short) {
			return Short.valueOf((short) (minuend.intValue() - subtrahend.intValue()));
		}
		if (minuend instanceof Float) {
			return Float.valueOf(minuend.floatValue() - subtrahend.floatValue());
		}
		// for Double and other types, use double values
		return Double.valueOf(minuend.doubleValue() - subtrahend.doubleValue());
	}

}
