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
package org.openjdk.jmc.flightrecorder.ext.jfx;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

final class JfxConstants {

	static final String TYPE_ID_PULSE_PHASE_8 = "http://www.oracle.com/technetwork/java/javafx/index.html/javafx/pulse"; //$NON-NLS-1$
	static final String TYPE_ID_INPUT_8 = "http://www.oracle.com/technetwork/java/javafx/index.html/javafx/input"; //$NON-NLS-1$
	static final String TYPE_ID_PULSE_PHASE_12 = "javafx.PulsePhase"; //$NON-NLS-1$
	static final String TYPE_ID_INPUT_12 = "javafx.Input"; //$NON-NLS-1$

	static final String JFX_RULE_PATH = "javaFx"; //$NON-NLS-1$

	static final IItemFilter JFX_PULSE_FILTER = ItemFilters.or(ItemFilters.type(TYPE_ID_PULSE_PHASE_8), ItemFilters.type(TYPE_ID_PULSE_PHASE_12));
	static final IItemFilter JFX_INPUT_FILTER = ItemFilters.or(ItemFilters.type(TYPE_ID_INPUT_8), ItemFilters.type(TYPE_ID_INPUT_12));

	static final IItemFilter JFX_FILTER = ItemFilters.or(JFX_INPUT_FILTER, JFX_PULSE_FILTER);

	static final IAttribute<IQuantity> ATTRIBUTE_PULSE_ID_8 = Attribute.attr("pulseNumber", Messages.JfxConstants_PULSE_ID, //$NON-NLS-1$
			UnitLookup.NUMBER);
	static final IAttribute<IQuantity> ATTRIBUTE_PULSE_ID_12 = Attribute.attr("pulseId", Messages.JfxConstants_PULSE_ID, //$NON-NLS-1$
			UnitLookup.NUMBER);
	static final IAttribute<String> ATTRIBUTE_PHASE_NAME_8 = Attribute.attr("phase", Messages.JfxConstants_PHASE_NAME, //$NON-NLS-1$
			UnitLookup.PLAIN_TEXT);
	static final IAttribute<String> ATTRIBUTE_PHASE_NAME_12 = Attribute.attr("phaseName", Messages.JfxConstants_PHASE_NAME, //$NON-NLS-1$
			UnitLookup.PLAIN_TEXT);
	static final IAttribute<String> INPUT_TYPE = Attribute.attr("input", Messages.JfxConstants_INPUT_TYPE, //$NON-NLS-1$
			UnitLookup.PLAIN_TEXT);

	static final IAggregator<IQuantity, ?> MAX_PULSE_DURATION = Aggregators.max(TYPE_ID_PULSE_PHASE_12, JfrAttributes.DURATION);
	static final IAggregator<IQuantity, ?> PULSE_START = Aggregators.min(Messages.JfxConstants_PULSE_START,
			Messages.JfxConstants_PULSE_START_DESCRIPTION, JfxConstants.TYPE_ID_PULSE_PHASE_12, JfrAttributes.START_TIME);

}
