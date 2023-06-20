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

import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ReflectionException;

/**
 * A synthetic attribute calculated as a difference between the specified minuend and subtrahend.
 */
public class LongDifferenceAttribute extends AbstractPropertySyntheticAttribute {

	private static String MINUEND = "minuend"; //$NON-NLS-1$
	private static String SUBTRAHEND = "subtrahend"; //$NON-NLS-1$

	@Override
	public Object getValue(MBeanServerConnection connection) throws MBeanException, ReflectionException {
		Map<String, Object> values = getPropertyAttributes(connection, new String[] {MINUEND, SUBTRAHEND});
		return getMinuend(values) - getSubtrahend(values);
	}

	private long getMinuend(Map<String, Object> values) throws MBeanException {
		return getLong(values, MINUEND);
	}

	private long getSubtrahend(Map<String, Object> values) throws MBeanException {
		return getLong(values, SUBTRAHEND);
	}

	private long getLong(Map<String, Object> values, String key) throws MBeanException {
		if (!values.containsKey(key)) {
			try {
				throw new AttributeNotFoundException("Attribute " + key + " not found!"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (AttributeNotFoundException e) {
				throw new MBeanException(e);
			}
		}
		return ((Number) values.get(key)).longValue();
	}

	@Override
	public boolean hasResolvedDependencies(MBeanServerConnection connection) {
		return hasResolvedAttribute(connection, MINUEND) && hasResolvedAttribute(connection, SUBTRAHEND);
	}
}
