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
import java.util.logging.Level;

import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.subscription.IMRITransformation;
import org.openjdk.jmc.rjmx.subscription.IMRITransformationFactory;
import org.openjdk.jmc.rjmx.subscription.MRI;

/**
 * A transformation factory used be several of our own transformations.
 */
public class SingleMRITransformationFactory implements IMRITransformationFactory {

	private static final String TRANSFORMATION_CLASS_NAME_PROPERTY = "transformationClass"; //$NON-NLS-1$
	private Properties m_properties;
	private Properties m_transformationProperties;

	@Override
	public void setFactoryProperties(Properties properties, Properties transformationProperties) {
		m_properties = properties;
		m_transformationProperties = transformationProperties;
	}

	@Override
	public IMRITransformation createTransformation(Properties properties) {
		String className = m_properties.getProperty(TRANSFORMATION_CLASS_NAME_PROPERTY);
		if (className == null) {
			return null;
		}
		try {
			@SuppressWarnings("unchecked")
			Class<IMRITransformation> clz = (Class<IMRITransformation>) Class.forName(className);
			IMRITransformation transformation = clz.newInstance();
			properties.putAll(m_transformationProperties);
			transformation.setProperties(properties);
			return transformation;
		} catch (ClassNotFoundException e) {
			logException(e);
		} catch (InstantiationException e) {
			logException(e);
		} catch (IllegalAccessException e) {
			logException(e);
		}
		return null;
	}

	private void logException(Exception e) {
		RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Unable to create transformation!", e); //$NON-NLS-1$
	}

	@Override
	public String getVisualizationLabel() {
		return m_properties.getProperty("visualizeLabel"); //$NON-NLS-1$
	}

	@Override
	public MRI createTransformationMRI(MRI mri) {
		String transformationName = m_properties.getProperty(MRITransformationToolkit.TRANSFORMATION_NAME_ATTRIBUTE);
		// FIXME: Would be nice to not have to specify an ObjectName but until then let us use something meaningful.
		return new MRI(MRI.Type.TRANSFORMATION, "transformation:type=" + transformationName, //$NON-NLS-1$
				transformationName + "?attribute=" + mri); //$NON-NLS-1$
	}
}
