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

import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;

/**
 * Takes an existing attribute as input and creates an averaging transformation. It is possible to
 * specify how many samples that should be included in the average.
 */
public class AverageTransformation extends AbstractSingleMRITransformation {

	private int m_maxTerms;
	private int m_terms = 0;
	private Double m_average = null;

	@Override
	public void setProperties(Properties properties) {
		super.setProperties(properties);
		m_maxTerms = Integer.parseInt(properties.getProperty("terms", "30")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public Object createSubscriptionValue(MRIValueEvent event) {
		if (event.getValue() instanceof Number) {
			double eventValue = ((Number) event.getValue()).doubleValue();
			if (m_average != null) {
				if (m_terms < m_maxTerms) {
					m_terms += 1;
				}
				double portion = 1d / m_terms;
				m_average = portion * eventValue + (1 - portion) * m_average;
			} else {
				m_average = eventValue;
				m_terms = 1;
			}
		}
		return (m_average == null) ? NO_VALUE : m_average;
	}

	@Override
	protected String getDisplayNamePattern() {
		return NLS.bind(super.getDisplayNamePattern(), m_maxTerms);
	}
}
