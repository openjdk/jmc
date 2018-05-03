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
package org.openjdk.jmc.rjmx.triggers.internal;

import java.util.logging.Logger;

import org.eclipse.osgi.util.NLS;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.rjmx.triggers.IValueEvaluator;
import org.w3c.dom.Element;

/**
 * Standard evaluator. Evaluates to true if the value is larger than max. The value must be a
 * Number.
 */
public final class ValueEvaluatorNumberMax implements IValueEvaluator {
	private final static Logger LOGGER = Logger.getLogger("org.openjdk.jmc.rjmx.triggers"); //$NON-NLS-1$

	private static final String XML_ELEMENT_MAXVALUE = "maxvalue"; //$NON-NLS-1$
	private static final String XML_ELEMENT_CONTENTTYPE = "contenttype"; //$NON-NLS-1$
	private IQuantity m_max;

	/**
	 * Constructor. Used when constructing from XML.
	 */
	public ValueEvaluatorNumberMax() {
	}

	/**
	 * Constructor.
	 *
	 * @param max
	 *            see class comment.
	 */
	public ValueEvaluatorNumberMax(IQuantity max) {
		m_max = max;
	}

	/**
	 * @see IValueEvaluator#triggerOn(Object)
	 */
	@Override
	public boolean triggerOn(Object val) throws Exception {
		if (!(val instanceof IQuantity)) {
			String logMessage = "ValueEvaluatorNumberMax: " + val + " does not have a content type set"; //$NON-NLS-1$ //$NON-NLS-2$
			LOGGER.info(logMessage);
			throw new ValueEvaluationException(logMessage,
					NLS.bind(Messages.ValueEvaluatorNumber_VALUE_NOT_A_QUANTITY, val));
		} else {
			return triggerOn((IQuantity) val);
		}
	}

	private boolean triggerOn(IQuantity val) throws Exception {
		if (!val.getUnit().getContentType().equals(m_max.getUnit().getContentType())) {
			String logMessage = "ValueEvaluatorNumberMax: " + val.persistableString() //$NON-NLS-1$
					+ " is not of the same content type as limit " + m_max.persistableString(); //$NON-NLS-1$
			LOGGER.info(logMessage);
			throw new ValueEvaluationException(logMessage,
					NLS.bind(Messages.ValueEvaluatorNumber_VALUE_NOT_OF_THE_SAME_TYPE,
							val.displayUsing(IDisplayable.EXACT), m_max.displayUsing(IDisplayable.EXACT)));
		}
		boolean result = (val.compareTo(m_max) > 0);
		LOGGER.info("ValueEvaluatorNumberMax: " + val.persistableString() + " > " + m_max.persistableString() + " = " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ result);
		return result;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "value > " + m_max.persistableString(); //$NON-NLS-1$
	}

	/**
	 * @return long the max to evaluate against.
	 */
	public IQuantity getMax() {
		return m_max;
	}

	/**
	 * Sets the max value to evaluate against.
	 *
	 * @param maxValue
	 *            the new max value.
	 */
	public void setMax(IQuantity maxValue) {
		m_max = maxValue;
	}

	@Override
	public void initializeEvaluatorFromXml(Element node) {
		ContentType<?> contentType = UnitLookup
				.getContentType(XmlToolkit.getSetting(node, XML_ELEMENT_CONTENTTYPE, "")); //$NON-NLS-1$
		// We need to set a content type on the evaluator if we are loading an old workspace that is lacking that information.
		if (!(contentType instanceof KindOfQuantity)) {
			contentType = UnitLookup.NUMBER;
		}
		String persistedQuantity = XmlToolkit.getSetting(node, XML_ELEMENT_MAXVALUE, "0"); //$NON-NLS-1$
		try {
			setMax(((KindOfQuantity<?>) contentType).parsePersisted(persistedQuantity));
		} catch (QuantityConversionException e) {
			LOGGER.warning(e.getMessage());
			setMax(((KindOfQuantity<?>) contentType).getDefaultUnit().quantity(0));
		}
	}

	@Override
	public void exportEvaluatorToXml(Element node) {
		XmlToolkit.setSetting(node, XML_ELEMENT_CONTENTTYPE, getMax().getUnit().getContentType().getIdentifier());
		XmlToolkit.setSetting(node, XML_ELEMENT_MAXVALUE, getMax().persistableString());
	}

	@Override
	public String getOperatorString() {
		return ">"; //$NON-NLS-1$
	}

	@Override
	public String getEvaluationConditionString() {
		return "> " + m_max.displayUsing(IDisplayable.EXACT); //$NON-NLS-1$
	}
}
