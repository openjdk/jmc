/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.rjmx.common.subscription;

/**
 * The bare essence of providing metadata about a MRI.
 */
public interface IMRIMetadataProvider {

	public static final String KEY_UPDATE_TIME = "UpdateTime"; //$NON-NLS-1$
	public static final String KEY_UNIT_STRING = "UnitString"; //$NON-NLS-1$
	public static final String KEY_DISPLAY_NAME = "DisplayName"; //$NON-NLS-1$
	public static final String KEY_DESCRIPTION = "Description"; //$NON-NLS-1$
	public static final String KEY_VALUE_TYPE = "AttributeType"; //$NON-NLS-1$
	public static final String KEY_COMPOSITE = "composite"; //$NON-NLS-1$
	public static final String KEY_READABLE = "Readable"; //$NON-NLS-1$
	public static final String KEY_WRITABLE = "Writable"; //$NON-NLS-1$
	public static final String KEY_DESCRIPTOR = "Descriptor"; //$NON-NLS-1$
	public static final String KEY_COLOR = "color"; //$NON-NLS-1$

	/**
	 * Returns the {@link MRI} with which the metadata is associated.
	 *
	 * @return the {@link MRI}.
	 */
	public MRI getMRI();

	/**
	 * Convenience access to general getter.
	 *
	 * @param key
	 *            the key for the data to retrieve.
	 * @return the metadata resulting from the lookup, or {@code null} if no such data exist.
	 * @see IMRIMetadataService#getMetadata(MRI, String)
	 */
	public Object getMetadata(String key);

}
