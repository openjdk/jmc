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
package org.openjdk.jmc.rjmx.subscription;

import java.util.Properties;

/**
 * Interface for transformation factories. A transformation factory takes a set of properties and
 * creates an {@link IMRITransformation}.
 */
public interface IMRITransformationFactory {

	/**
	 * Invoked when the extension is read and the factory is created.
	 *
	 * @param properties
	 *            the properties to use for this factory
	 * @param transformationProperties
	 *            the properties to add to each transformation instance
	 */
	void setFactoryProperties(Properties properties, Properties transformationProperties);

	/**
	 * Creates a transformation for given properties.
	 *
	 * @param properties
	 *            the properties to use
	 * @return a transformation
	 */
	IMRITransformation createTransformation(Properties properties);

	/**
	 * Returns the label for a visualization menu item for the factory
	 *
	 * @return the visualization menu item
	 */
	String getVisualizationLabel();

	/**
	 * Creates a transformation MRI for given MRI. The resulting MRI can then be used to create the
	 * actual transformation instance.
	 *
	 * @param mri
	 *            the MRI to create a transformation MRI for
	 * @return the transformed MRI
	 */
	MRI createTransformationMRI(MRI mri);
}
