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
package org.openjdk.jmc.rjmx.triggers.extension.internal;

import java.util.logging.Level;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.triggers.fields.internal.BooleanField;
import org.openjdk.jmc.rjmx.triggers.fields.internal.DateField;
import org.openjdk.jmc.rjmx.triggers.fields.internal.Field;
import org.openjdk.jmc.rjmx.triggers.fields.internal.FieldHolder;
import org.openjdk.jmc.rjmx.triggers.fields.internal.FileField;
import org.openjdk.jmc.rjmx.triggers.fields.internal.FloatField;
import org.openjdk.jmc.rjmx.triggers.fields.internal.IntegerField;
import org.openjdk.jmc.rjmx.triggers.fields.internal.PasswordField;
import org.openjdk.jmc.rjmx.triggers.fields.internal.QuantityField;
import org.openjdk.jmc.rjmx.triggers.fields.internal.StringField;
import org.openjdk.jmc.rjmx.triggers.fields.internal.TimeField;
import org.openjdk.jmc.ui.common.idesupport.IDESupportToolkit;
import org.openjdk.jmc.ui.common.resource.IImageResource;
import org.openjdk.jmc.ui.common.resource.MCFile;
import org.openjdk.jmc.ui.common.resource.Resource;

public class TriggerComponent implements IExecutableExtension, IImageResource {
	// extension
	private static final String XML_ATTRIBUTE_ICON = "icon"; //$NON-NLS-1$
	private static final String XML_ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	private static final String XML_ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
	private static final String XML_ATTRIBUTE_DESCRIPTION = "description"; //$NON-NLS-1$

	// field types
	private static final String XML_ELEMENT_INTEGER = "integer"; //$NON-NLS-1$
	private static final String XML_ELEMENT_FLOAT = "float"; //$NON-NLS-1$
	private static final String XML_ELEMENT_STRING = "string"; //$NON-NLS-1$
	private static final String XML_ELEMENT_FILE = "file"; //$NON-NLS-1$
	private static final String XML_ELEMENT_BOOLEAN = "boolean"; //$NON-NLS-1$
	private static final String XML_ELEMENT_DATE = "date"; //$NON-NLS-1$
	private static final String XML_ELEMENT_TIME = "time"; //$NON-NLS-1$
	private static final String XML_ELEMENT_TIMERANGE = "timerange"; //$NON-NLS-1$
	private static final String XML_ELEMENT_PASSWORD = "password"; //$NON-NLS-1$

	// field description
	private static final String XML_FIELD_DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String XML_FIELD_NAME = "name"; //$NON-NLS-1$
	private static final String XML_FIELD_ID = "id"; //$NON-NLS-1$
	private static final String XML_FIELD_DEFAULT_VALUE = "value"; //$NON-NLS-1$

	// numerical
	private static final String XML_FIELD_MIN = "min"; //$NON-NLS-1$
	private static final String XML_FIELD_MAX = "max"; //$NON-NLS-1$
	private String m_description = ""; //$NON-NLS-1$
	private String m_name = "Unknown name"; //$NON-NLS-1$
//	private String m_iconName = "";
	private String m_id = ""; //$NON-NLS-1$
	private Resource m_icon;
	private final FieldHolder m_fieldHolder;

	public TriggerComponent() {
		m_fieldHolder = new FieldHolder();
	}

	@Override
	final public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException {
		if (config != null) {
			try {
				m_name = getAttribute(config, XML_ATTRIBUTE_NAME, "Unknown name of trigger action"); //$NON-NLS-1$
				// m_iconName = getAttribute(config, XML_ATTRIBUTE_ICON, "Unknown name of trigger action");
				m_id = getAttribute(config, XML_ATTRIBUTE_ID, "Unknown name of trigger action"); //$NON-NLS-1$
				m_description = getAttribute(config, XML_ATTRIBUTE_DESCRIPTION, "Unknown name of trigger action"); //$NON-NLS-1$
				if (config != null) {
					m_icon = new Resource(config.getDeclaringExtension().getContributor().getName(),
							config.getAttribute(XML_ATTRIBUTE_ICON));
				}
				addFields(config);
			} catch (Exception e) {
				RJMXPlugin.getDefault().getLogger().log(Level.SEVERE, "Error initializing " + config, e); //$NON-NLS-1$
			}
		}
	}

	public FieldHolder getFieldHolder() {
		return m_fieldHolder;
	}

	protected String getAttribute(IConfigurationElement ice, String attribute, String defaultValue) {
		if (ice != null) {
			String temp = ice.getAttribute(attribute);
			if (temp != null) {
				return temp;
			}
		}

		return defaultValue;
	}

	private void addFields(IConfigurationElement ice) throws Exception {
		if (ice == null) {
			return;
		}

		IConfigurationElement[] ce = ice.getChildren();
		for (IConfigurationElement element : ce) {
			if (element != null) {
				try {
					String fieldDescription = getAttribute(element, XML_FIELD_DESCRIPTION, ""); //$NON-NLS-1$
					String fieldLabel = getAttribute(element, XML_FIELD_NAME, ""); //$NON-NLS-1$
					String fieldId = getAttribute(element, XML_FIELD_ID, ""); //$NON-NLS-1$
					String fieldValue = getAttribute(element, XML_FIELD_DEFAULT_VALUE, ""); //$NON-NLS-1$
					Field field = createField(element, fieldId, fieldLabel, fieldValue, fieldDescription);
					if (field != null) {
						m_fieldHolder.addField(field);
					} else {
						RJMXPlugin.getDefault().getLogger().severe("Extension XML-parse error " + element.getName()); //$NON-NLS-1$
					}
				} catch (Exception e) {
					RJMXPlugin.getDefault().getLogger().log(Level.SEVERE,
							"Extension XML-parse error " + element.getName(), e); //$NON-NLS-1$
				}
			}
		}
	}

	private Field createField(
		IConfigurationElement ice, String fieldId, String fieldLabel, String fieldValue, String fieldDescription)
			throws Exception {
		if (ice.getName().equalsIgnoreCase(XML_ELEMENT_STRING)) {
			return new StringField(fieldId, fieldLabel, fieldValue, fieldDescription);
		}
		if (ice.getName().equalsIgnoreCase(XML_ELEMENT_INTEGER)) {
			String min = getAttribute(ice, XML_FIELD_MIN, Integer.toString(Integer.MIN_VALUE));
			String max = getAttribute(ice, XML_FIELD_MAX, Integer.toString(Integer.MAX_VALUE));
			return new IntegerField(fieldId, fieldLabel, fieldValue, fieldDescription, min, max);
		}
		if (ice.getName().equalsIgnoreCase(XML_ELEMENT_FLOAT)) {
			String min = getAttribute(ice, XML_FIELD_MIN, Integer.toString(Integer.MIN_VALUE));
			String max = getAttribute(ice, XML_FIELD_MAX, Integer.toString(Integer.MAX_VALUE));
			return new FloatField(fieldId, fieldLabel, fieldValue, fieldDescription, min, max);
		}
		if (ice.getName().equalsIgnoreCase(XML_ELEMENT_FILE)) {
			MCFile defaultFile = IDESupportToolkit.createDefaultFileResource(fieldValue);
			return new FileField(fieldId, fieldLabel, defaultFile.getPath(), fieldDescription);
		}
		if (ice.getName().equalsIgnoreCase(XML_ELEMENT_DATE)) {
			return new DateField(fieldId, fieldLabel, fieldValue, fieldDescription);
		}
		if (ice.getName().equalsIgnoreCase(XML_ELEMENT_TIME)) {
			return new TimeField(fieldId, fieldLabel, fieldValue, fieldDescription);
		}
		if (ice.getName().equalsIgnoreCase(XML_ELEMENT_BOOLEAN)) {
			return new BooleanField(fieldId, fieldLabel, fieldValue, fieldDescription);
		}
		if (ice.getName().equalsIgnoreCase(XML_ELEMENT_TIMERANGE)) {
			/*
			 * FIXME: JMC-5329 - Clean up quantity field initialization.
			 * 
			 * Quantity field requires a default value. However, that default value cannot be parsed
			 * until we have initialized "kind", so we will internally get an NPE, which we will
			 * happily swallow, cause we will set the default, for real, when we initialize the
			 * "kind".
			 */
			QuantityField qf = new QuantityField(fieldId, fieldLabel, fieldValue, fieldDescription);
			// Here we initialize the kind, and set the default.
			qf.initKind(UnitLookup.TIMESPAN, fieldValue, UnitLookup.SECOND.quantity(0), null);
			return qf;
		}
		if (ice.getName().equalsIgnoreCase(XML_ELEMENT_PASSWORD)) {
			return new PasswordField(fieldId, fieldLabel, fieldValue, fieldDescription);
		}

		return null;
	}

	public final String getName() {
		return m_name;
	}

	public final String getDescription() {
		return m_description;
	}

	final private String getId() {
		return m_id;
	}

	@Override
	public Resource getImageResource() {
		return m_icon;
	}

	@Override
	public String toString() {
		return getName() + "[id: " + getId() + ']'; //$NON-NLS-1$
	}
}
