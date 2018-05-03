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
package org.openjdk.jmc.flightrecorder.configuration.internal;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.QuantityConversionException;

/**
 * Helpers for validating entire settings maps.
 */
public final class ValidationToolkit {

	/**
	 * Will validate the entire map and collect all validation problems in a big validation
	 * exception. If there are no errors, no exception will be thrown
	 *
	 * @param options
	 * @throws ValidationException
	 */
	public static <K> void validate(IConstrainedMap<K> options) throws ValidationException {
		StringBuilder messageBuilder = new StringBuilder();
		StringBuilder localizedMessageBuilder = new StringBuilder();
		boolean caught = false;
		try {
			for (K key : options.keySet()) {
				Object value = options.get(key);
				if (value != null) {
					validate(options.getConstraint(key), value);
				}
			}
		} catch (QuantityConversionException e) {
			caught = true;
			messageBuilder.append(e.getMessage());
			messageBuilder.append(System.getProperty("line.separator")); //$NON-NLS-1$
			localizedMessageBuilder.append(e.getLocalizedMessage());
			localizedMessageBuilder.append(System.getProperty("line.separator")); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			caught = true;
			messageBuilder.append(e.getMessage());
			messageBuilder.append(System.getProperty("line.separator")); //$NON-NLS-1$
			localizedMessageBuilder.append(e.getLocalizedMessage());
			localizedMessageBuilder.append(System.getProperty("line.separator")); //$NON-NLS-1$
		}
		if (caught) {
			throw new ValidationException(messageBuilder.toString(), localizedMessageBuilder.toString());
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> void validate(IConstraint<T> constraint, Object value) throws QuantityConversionException {
		constraint.validate((T) value);
	}
}
