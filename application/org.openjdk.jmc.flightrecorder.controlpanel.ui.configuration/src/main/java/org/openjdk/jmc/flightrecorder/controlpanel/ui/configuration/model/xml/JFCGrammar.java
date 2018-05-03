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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Describes the XML grammar of a Flight Recorder Configuration. This should match with the XML
 * Schema {@code jfc_<version>.xsd}, to the extent that the grammar can be expressed in both.
 */
@SuppressWarnings("nls")
public final class JFCGrammar {
	/**
	 * XML elements that should be kept on one line when outputting the {@link XMLModel} as text.
	 */
	public static final Set<XMLTag> ONE_LINE_ELEMENTS;

	public static final String[] DATA_TYPE = {"int", "long", "string", "boolean"};
	public static final String FLAG_CONTENT_TYPE = "flag";
	public static final String TIMESPAN_CONTENT_TYPE = "timespan";
	public static final String[] CONTENT_TYPE = {FLAG_CONTENT_TYPE, TIMESPAN_CONTENT_TYPE};
	public static final String[] OPERATORS = {"equal"};

	public static final XMLAttribute ATTRIBUTE_LABEL_MANDATORY = new XMLAttribute("label", true, XMLNodeType.TEXT);
	public static final XMLAttribute ATTRIBUTE_LABEL = new XMLAttribute("label", false, XMLNodeType.TEXT);
	public static final XMLAttribute ATTRIBUTE_DESCRIPTION = new XMLAttribute("description", false, XMLNodeType.TEXT);
	public static final XMLAttribute ATTRIBUTE_NAME = new XMLAttribute("name", true, XMLNodeType.TEXT);
	public static final XMLAttribute ATTRIBUTE_NAME_DEFINITION = new XMLAttribute("name", true, XMLNodeType.DEFINITION);

	public static final XMLAttribute ATTRIBUTE_DEFAULT = new XMLAttribute("default", true, XMLNodeType.TEXT);
	public static final XMLAttribute ATTRIBUTE_VALUE = new XMLAttribute("value", false, XMLNodeType.TEXT);
	public static final XMLAttribute ATTRIBUTE_TRUE = new XMLAttribute("true", false, XMLNodeType.TEXT, "true");
	// NOTE: Do not provide a default value for false, since that would prevent multiple conditions for the same variable from working. 
	// Compare with the else attribute in Apache Ant's condition task.
	public static final XMLAttribute ATTRIBUTE_FALSE = new XMLAttribute("false", false, XMLNodeType.TEXT);
	public static final XMLAttribute ATTRIBUTE_PATH = new XMLAttribute("path", true, XMLNodeType.TEXT);
	public static final XMLAttribute ATTRIBUTE_VERSION = new XMLAttribute("version", true, XMLNodeType.TEXT);
	public static final XMLAttribute ATTRIBUTE_PROVIDER = new XMLAttribute("provider", false, XMLNodeType.TEXT);

	public static final XMLAttribute ATTRIBUTE_NAME_REFERENCE = new XMLAttribute("name", false, XMLNodeType.REFERENCE);
	public static final XMLAttribute ATTRIBUTE_CONTROL_REFERENCE = new XMLAttribute("control", false,
			XMLNodeType.REFERENCE);
	public static final XMLAttribute ATTRIBUTE_OPERATOR = new XMLAttribute("operator", true, XMLNodeType.TEXT,
			OPERATORS);
	public static final XMLAttribute ATTRIBUTE_URI = new XMLAttribute("uri", true, XMLNodeType.TEXT);
	public static final XMLAttribute ATTRIBUTE_CONTENT_TYPE = new XMLAttribute("contentType", false, XMLNodeType.TEXT);
	public static final XMLAttribute ATTRIBUTE_TYPE = new XMLAttribute("type", true, XMLNodeType.TEXT, DATA_TYPE);
	public static final XMLAttribute ATTRIBUTE_MINIMUM = new XMLAttribute("minimum", false, XMLNodeType.TEXT);
	public static final XMLAttribute ATTRIBUTE_MAXIMUM = new XMLAttribute("maximum", false, XMLNodeType.TEXT);

	public static final XMLTag ROOT = new XMLTag("root");
	public static final XMLTag TAG_CONFIGURATION_V1 = new XMLTag.Predicated("configuration", ATTRIBUTE_VERSION, "1.0");
	public static final XMLTag TAG_CONFIGURATION_V2 = new XMLTag("configuration");
	public static final XMLTag TAG_PRODUCER = new XMLTag("producer");
	public static final XMLTag TAG_CONTROL = new XMLTag("control");
	public static final XMLTag TAG_SELECTION = new XMLTag("selection");
	public static final XMLTag TAG_OPTION = new XMLTag("option", XMLNodeType.ELEMENT_WITH_CONTENT);
	public static final XMLTag TAG_FLAG = new XMLTag("flag", XMLNodeType.ELEMENT_WITH_CONTENT);
	public static final XMLTag TAG_TEXT = new XMLTag("text", XMLNodeType.ELEMENT_WITH_CONTENT);;
	public static final XMLTag TAG_VARIABLE = new XMLTag("variable", XMLNodeType.ELEMENT_WITH_CONTENT);
	public static final XMLTag TAG_CONDITION = new XMLTag("condition");
	public static final XMLTag TAG_AND = new XMLTag("and", XMLNodeType.ELEMENT_WITH_AT_LEAST_ONE_CHILD);
	public static final XMLTag TAG_OR = new XMLTag("or", XMLNodeType.ELEMENT_WITH_AT_LEAST_ONE_CHILD);
	public static final XMLTag TAG_NOT = new XMLTag("not", XMLNodeType.ELEMENT_WITH_AT_LEAST_ONE_CHILD);
	public static final XMLTag TAG_TEST = new XMLTag("test");
	public static final XMLTag TAG_EVENTTYPE_V1 = new XMLTag("event");
	public static final XMLTag TAG_EVENTTYPE_V2 = new XMLTag("event");
	public static final XMLTag TAG_SETTING = new XMLTag("setting", XMLNodeType.ELEMENT_WITH_CONTENT);
	public static final XMLTag TAG_CATEGORY = new XMLTag("category");

	/*
	 * Currently used values for the "name" attribute of <setting> elements. With the exception of
	 * "enabled", these shouldn't really be treated specially anywhere, since more will be added in
	 * "JFR 2.0". However, since some currently are special treated, we should at least have
	 * constants for them, so their usage can be tracked.
	 * 
	 * FIXME: These should probably not be defined in multiple places.
	 * 
	 * But since they were only defined in the RJMX project, which is utterly wrong (in
	 * org.openjdk.jmc.rjmx.services.flr.internal.RecordingSettingsToolkit), they are temporarily
	 * defined here.
	 */
	public static final String SETTING_ENABLED = "enabled";
	public static final String SETTING_STACKTRACE = "stackTrace";
	public static final String SETTING_THRESHOLD = "threshold";
	public static final String SETTING_PERIOD = "period";

	public static final String PERIOD_EVERY_CHUNK = "everyChunk";

	static {
		// Wire it up!
		ROOT.add(TAG_CONFIGURATION_V1, TAG_CONFIGURATION_V2);

		TAG_CONFIGURATION_V1.add(TAG_PRODUCER, ATTRIBUTE_VERSION, ATTRIBUTE_NAME, ATTRIBUTE_DESCRIPTION,
				ATTRIBUTE_PROVIDER);

		TAG_PRODUCER.add(TAG_CONTROL, TAG_SETTING, TAG_EVENTTYPE_V1, ATTRIBUTE_URI, ATTRIBUTE_LABEL,
				ATTRIBUTE_DESCRIPTION);

		TAG_EVENTTYPE_V1.add(TAG_SETTING, ATTRIBUTE_PATH, ATTRIBUTE_LABEL, ATTRIBUTE_DESCRIPTION);

		TAG_CONFIGURATION_V2.add(TAG_CONTROL, TAG_CATEGORY, TAG_EVENTTYPE_V2, ATTRIBUTE_VERSION,
				ATTRIBUTE_LABEL_MANDATORY, ATTRIBUTE_DESCRIPTION, ATTRIBUTE_PROVIDER);

		TAG_CATEGORY.add(ATTRIBUTE_LABEL_MANDATORY, TAG_CATEGORY, TAG_EVENTTYPE_V2);

		TAG_EVENTTYPE_V2.add(TAG_SETTING, ATTRIBUTE_NAME, ATTRIBUTE_LABEL, ATTRIBUTE_DESCRIPTION);

		TAG_CONTROL.add(TAG_FLAG, TAG_TEXT, TAG_SELECTION, TAG_VARIABLE, TAG_CONDITION);

		TAG_SELECTION.add(TAG_OPTION, ATTRIBUTE_NAME_DEFINITION, ATTRIBUTE_DEFAULT, ATTRIBUTE_LABEL_MANDATORY,
				ATTRIBUTE_DESCRIPTION);

		TAG_OPTION.add(ATTRIBUTE_LABEL_MANDATORY, ATTRIBUTE_DESCRIPTION, ATTRIBUTE_NAME);

		TAG_TEXT.add(ATTRIBUTE_NAME_DEFINITION, ATTRIBUTE_LABEL_MANDATORY, ATTRIBUTE_DESCRIPTION,
				ATTRIBUTE_CONTENT_TYPE, ATTRIBUTE_MINIMUM, ATTRIBUTE_MAXIMUM);

		TAG_VARIABLE.add(ATTRIBUTE_NAME_DEFINITION);

		TAG_CONDITION.add(TAG_OR, TAG_AND, TAG_NOT, TAG_TEST, ATTRIBUTE_NAME_DEFINITION, ATTRIBUTE_TRUE,
				ATTRIBUTE_FALSE);

		TAG_TEST.add(ATTRIBUTE_NAME_REFERENCE, ATTRIBUTE_CONTENT_TYPE, ATTRIBUTE_OPERATOR, ATTRIBUTE_VALUE);

		TAG_OR.add(TAG_AND, TAG_NOT, TAG_TEST, TAG_OR);

		TAG_NOT.add(TAG_OR, TAG_AND, TAG_TEST, TAG_NOT);

		TAG_AND.add(TAG_OR, TAG_NOT, TAG_TEST, TAG_AND);

		TAG_FLAG.add(ATTRIBUTE_NAME_DEFINITION, ATTRIBUTE_LABEL_MANDATORY, ATTRIBUTE_DESCRIPTION);

		TAG_SETTING.add(ATTRIBUTE_NAME, ATTRIBUTE_CONTROL_REFERENCE, ATTRIBUTE_LABEL, ATTRIBUTE_DESCRIPTION,
				ATTRIBUTE_CONTENT_TYPE);

		{
			Set<XMLTag> oneLineElements = new HashSet<>();
			oneLineElements.add(TAG_SETTING);
			oneLineElements.add(TAG_OPTION);
			oneLineElements.add(TAG_NOT);
			oneLineElements.add(TAG_AND);
			oneLineElements.add(TAG_OR);
			oneLineElements.add(TAG_TEST);
			ONE_LINE_ELEMENTS = Collections.unmodifiableSet(oneLineElements);
		}
	}
}
