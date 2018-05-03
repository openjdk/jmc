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
 * Interface for attribute transformations. Will typically generate and propagate a new value when
 * one of the value sources it depends on is updated.
 */
public interface IMRITransformation {

	/**
	 * Used to denote when no new value is available despite that value sources this transformation
	 * depend on have changed.
	 */
	public final Object NO_VALUE = new Object();

	/**
	 * Setter to initialize the transformation object after it has been created.
	 *
	 * @param properties
	 *            the properties to use
	 */
	void setProperties(Properties properties);

	/**
	 * Callback invoked when a value source it depends on has been updated.
	 *
	 * @param event
	 *            an updated attribute event
	 * @return a transformed value or {@link IMRITransformation#NO_VALUE}
	 */
	Object createSubscriptionValue(MRIValueEvent event);

	/**
	 * Getter for the value sources that this transformation depends on.
	 *
	 * @return the dependent value sources
	 */
	MRI[] getAttributes();

	/**
	 * Extends the metadata stored for this transformation.
	 *
	 * @param metadataService
	 *            the metadata service to use for lookup and metadata storage
	 * @param metadata
	 *            the current metadata of this transformation
	 */
	void extendMetadata(IMRIMetadataService metadataService, IMRIMetadata metadata);
}
