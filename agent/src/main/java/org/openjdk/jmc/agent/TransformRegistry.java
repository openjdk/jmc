/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.agent;

import java.util.List;
import java.util.Set;

public interface TransformRegistry {
	/**
	 * The named class has transforms that have not been executed yet.
	 *
	 * @param className
	 *            the name of the class to be transformed.
	 * @return true if the class has transforms scheduled. false if not.
	 */
	boolean hasPendingTransforms(String className);

	/**
	 * Returns the unmodifiable list of {@link TransformDescriptor}s for the named class.
	 *
	 * @param className
	 *            the class for which to retrieve the transformation metadata.
	 * @return the list of transformation metadata for the named class; may be empty but never
	 *         {@code null}.
	 */
	List<TransformDescriptor> getTransformData(String className);

	/**
	 * Returns the names of all classes stored in the registry.
	 *
	 * @return the unmodifiable set of class names.
	 */
	Set<String> getClassNames();

	/**
	 * Returns the currently instrumented configuration.
	 *
	 * @return an XML snippet of the configuration.
	 */
	String getCurrentConfiguration();

	/**
	 * Set the current configuration that will be instrumented
	 *
	 * @param xmlDescription
	 *            an XML snippet describing the current configuration
	 */
	void setCurrentConfiguration(String xmlDescription);

	/**
	 * Modifies class information in the registry according to the xml description.
	 *
	 * @param xmlDescription
	 *            an XML snippet describing the wanted modifications.
	 * @return a set of class names associated with modified {@link TransformDescriptor}s.
	 */
	Set<String> modify(String xmlDescription);

	/**
	 * Clears all classes and their corresponding transforms in the registry.
	 *
	 * @return the set of class names that were cleared.
	 */
	Set<String> clearAllTransformData();

	/**
	 * Signify classes are or are not being reverted to their pre instrumentation versions.
	 *
	 * @param shouldRevert
	 *            true if class instrumentation should be reverted, false otherwise.
	 */
	void setRevertInstrumentation(boolean shouldRevert);

	/**
	 * Determines if classes should be reverted to their pre instrumentation versions.
	 *
	 * @return true, if classes should be reverted and false otherwise.
	 */
	boolean isRevertIntrumentation();

}
