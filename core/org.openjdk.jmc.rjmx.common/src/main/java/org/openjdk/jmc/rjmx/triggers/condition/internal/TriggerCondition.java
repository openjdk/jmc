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
package org.openjdk.jmc.rjmx.triggers.condition.internal;

import java.util.logging.Level;

import org.w3c.dom.Element;

import org.openjdk.jmc.common.unit.DecimalPrefix;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.triggers.IValueEvaluator;
import org.openjdk.jmc.rjmx.triggers.fields.internal.BooleanField;
import org.openjdk.jmc.rjmx.triggers.fields.internal.Field;
import org.openjdk.jmc.rjmx.triggers.fields.internal.Field.FieldValueChangeListener;
import org.openjdk.jmc.rjmx.triggers.fields.internal.FieldHolder;
import org.openjdk.jmc.rjmx.triggers.fields.internal.QuantityField;
import org.openjdk.jmc.rjmx.triggers.fields.internal.StringField;
import org.openjdk.jmc.rjmx.triggers.internal.INotificationFactory;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationTrigger;
import org.openjdk.jmc.rjmx.triggers.internal.ValueEvaluatorNumberMax;
import org.openjdk.jmc.rjmx.triggers.internal.ValueEvaluatorNumberMin;
import org.openjdk.jmc.rjmx.triggers.internal.ValueEvaluatorStringMatch;

/**
 * TriggerCondition currently doesn't use the extension point mechanism so I'll just add the
 * necessary fields here statically and add listeners for changes and propagate them into the base
 * class(NotificationTrigger)
 */
public class TriggerCondition extends NotificationTrigger {
	public static final String FIELD_SUSTAINED = "SUSTAINED"; //$NON-NLS-1$
	public static final String FIELD_LIMIT = "LIMIT"; //$NON-NLS-1$
	public static final String FIELD_ONTRIGGER = "ONTRIGGER"; //$NON-NLS-1$
	public static final String FIELD_ONFLANK = "ONFLANK"; //$NON-NLS-1$
	public static final String FIELD_EVAL_STRING = "EVAL_STRING"; //$NON-NLS-1$
	public static final String FIELD_EVAL_NUM_MAX = "EVAL_NUM_MAX"; //$NON-NLS-1$
	public static final String FIELD_EVAL_NUM_MIN = "EVAL_NUM_MIN"; //$NON-NLS-1$

	private FieldHolder m_fieldHolder;

	public TriggerCondition() {
		super();
	}

	void initEvaluator() {
		try {
			IValueEvaluator ve = getValueEvaluator();
			if (ve instanceof ValueEvaluatorNumberMax) {
				createNumberMaxEvaluatorField(((ValueEvaluatorNumberMax) ve).getMax());
			} else {
				createNumberMaxEvaluatorField(UnitLookup.NUMBER.getDefaultUnit().quantity(0));
			}
			if (ve instanceof ValueEvaluatorNumberMin) {
				createNumberMinEvaluatorField(((ValueEvaluatorNumberMin) ve).getMin());
			} else {
				createNumberMinEvaluatorField(UnitLookup.NUMBER.getDefaultUnit().quantity(0));
			}
			if (ve instanceof ValueEvaluatorStringMatch) {
				createStringEvaluatorField(((ValueEvaluatorStringMatch) ve).getMatchString());
			} else {
				createStringEvaluatorField("*"); //$NON-NLS-1$
			}
		} catch (Exception e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, e.getMessage(), e);
		}

	}

	public void initFieldHolder() {
		m_fieldHolder = new FieldHolder();
		try {
			initEvaluator();
			createSustained();
			createLimit();
			createFlankBegin();
			createFlankEnd();
		} catch (Exception e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, e.getMessage(), e);
		}
	}

	@Override
	public void initializeFromXml(Element node, INotificationFactory factory) {
		// currently we use the base and listeners but we could make
		// a complete override and store everything in the FieldHolder instead
		super.initializeFromXml(node, factory);

		initFieldHolder();
	}

	public FieldHolder getFieldHolder() {
		return m_fieldHolder;
	}

	private void createFlankEnd() throws Exception {
		boolean descendingFlank = (getTriggerOn() & NotificationTrigger.TRIGGER_ON_RULE_TRIGGERED) != 0;

		Field triggeronFlankEnd = new BooleanField(FIELD_ONFLANK, Messages.TriggerCondition_DESCENDING_FLANK_CAPTION,
				Boolean.toString(descendingFlank), Messages.TriggerCondition_DESCENDING_FLANK_TOOLTIP);
		m_fieldHolder.addField(triggeronFlankEnd);
		triggeronFlankEnd.addFieldValueListener(new FieldValueChangeListener() {
			@Override
			public void onChange(Field field) {
				setTriggerFlag(NotificationTrigger.TRIGGER_ON_RULE_RECOVERED, field.getBoolean().booleanValue());
			}
		});
	}

	private void createFlankBegin() throws Exception {
		boolean ascendingFlank = (getTriggerOn() & NotificationTrigger.TRIGGER_ON_RULE_TRIGGERED) != 0;

		Field triggeronFlankStart = new BooleanField(FIELD_ONTRIGGER, Messages.TriggerCondition_ASCENDING_FLANK_CAPTION,
				Boolean.toString(ascendingFlank), Messages.TriggerCondition_ASCENDING_FLANK_TOOLTIP);
		m_fieldHolder.addField(triggeronFlankStart);
		triggeronFlankStart.addFieldValueListener(new FieldValueChangeListener() {
			@Override
			public void onChange(Field field) {
				setTriggerFlag(NotificationTrigger.TRIGGER_ON_RULE_TRIGGERED, field.getBoolean().booleanValue());
			}
		});
	}

	private void createLimit() throws Exception {
		final IUnit second = UnitLookup.TIMESPAN.getUnit(DecimalPrefix.NONE);
		IQuantity zeroQuantity = second.quantity(0);
		QuantityField limitField = new QuantityField(FIELD_LIMIT, Messages.TriggerCondition_LIMIT_PERIOD_CAPTION, "0", //$NON-NLS-1$
				Messages.TriggerCondition_LIMIT_PERIOD_TOOLTIP);
		limitField.initKind(second.getContentType(), second.quantity(getLimitTime()).interactiveFormat(), zeroQuantity,
				null);
		m_fieldHolder.addField(limitField);
		limitField.addFieldValueListener(new FieldValueChangeListener() {
			@Override
			public void onChange(Field field) {
				try {
					setLimitTime((int) field.getQuantity().longValueIn(second, Integer.MAX_VALUE));
				} catch (QuantityConversionException e) {
				}
			}
		});
	}

	private void createSustained() throws Exception {
		final IUnit second = UnitLookup.TIMESPAN.getUnit(DecimalPrefix.NONE);
		IQuantity zeroQuantity = second.quantity(0);
		QuantityField sustained = new QuantityField(FIELD_SUSTAINED, Messages.TriggerCondition_SUSTAINED_CAPTION, "0", //$NON-NLS-1$
				Messages.TriggerCondition_SUSTAINED_TOOLTIP);
		sustained.initKind(second.getContentType(), second.quantity(getSustainTime()).interactiveFormat(), zeroQuantity,
				null);
		m_fieldHolder.addField(sustained);
		sustained.addFieldValueListener(new FieldValueChangeListener() {
			@Override
			public void onChange(Field field) {
				try {
					setSustainTime((int) field.getQuantity().longValueIn(second, Integer.MAX_VALUE));
				} catch (QuantityConversionException e) {
				}
			}
		});
	}

	private void createStringEvaluatorField(String matchString) throws Exception {
		Field evaluatorField = new StringField(FIELD_EVAL_STRING, Messages.TriggerCondition_MATCH_STRING_CAPTION,
				matchString, Messages.TriggerCondition_MATCH_STRING_TOOLTIP);
		m_fieldHolder.addField(evaluatorField);
		evaluatorField.addFieldValueListener(new FieldValueChangeListener() {
			@Override
			public void onChange(Field field) {
				setValueEvaluator(new ValueEvaluatorStringMatch(field.getString()));
			}
		});
	}

	private void createNumberMaxEvaluatorField(IQuantity maxValue) throws Exception {
		QuantityField evaluatorField = new QuantityField(FIELD_EVAL_NUM_MAX,
				Messages.TriggerCondition_MAX_TRIGGER_CAPTION, "0", Messages.TriggerCondition_MAX_TRIGGER_TOOLTIP); //$NON-NLS-1$
		evaluatorField.initKind(maxValue.getUnit().getContentType(), maxValue.interactiveFormat(), null, null);
		m_fieldHolder.addField(evaluatorField);
		evaluatorField.addFieldValueListener(new FieldValueChangeListener() {
			@Override
			public void onChange(Field field) {
				IQuantity quantity = field.getQuantity();
				if (quantity != null) {
					setValueEvaluator(new ValueEvaluatorNumberMax(quantity));
				}
			}
		});
	}

	private void createNumberMinEvaluatorField(IQuantity minValue) throws Exception {
		QuantityField evaluatorField = new QuantityField(FIELD_EVAL_NUM_MIN,
				Messages.TriggerCondition_MIN_TRIGGER_CAPTION, "0", Messages.TriggerCondition_MIN_TRIGGER_TOOLTIP); //$NON-NLS-1$
		evaluatorField.initKind(minValue.getUnit().getContentType(), minValue.interactiveFormat(), null, null);
		m_fieldHolder.addField(evaluatorField);
		evaluatorField.addFieldValueListener(new FieldValueChangeListener() {
			@Override
			public void onChange(Field field) {
				IQuantity quantity = field.getQuantity();
				if (quantity != null) {
					setValueEvaluator(new ValueEvaluatorNumberMin(quantity));
				}
			}
		});
	}

	public void setTriggerFlag(int flag, boolean value) {
		int triggerOn = getTriggerOn();
		triggerOn = value ? triggerOn | flag : triggerOn & ~flag;
		setTriggerOn(triggerOn);
	}
}
