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
package org.openjdk.jmc.common.item;

import org.openjdk.jmc.common.unit.ContentType;

/**
 * Key used to get a {@link IMemberAccessor} from {@link IType#getAccessor(IAccessorKey)}. Normally
 * only used when introspecting an {@link IType} to get a list of all known accessors.
 *
 * @param <T>
 *            Class type of the content type. Also matches the class of the values returned by
 *            associated member accessors.
 */
public interface IAccessorKey<T> {
	/**
	 * The content type of this attribute. The type can be an opaque (or leaf) type in which case
	 * its instances can be of any class (but typically restricted according to the type). It can
	 * also be a structured type which has attributes (fields) of its own, in which case its
	 * instances currently must implement {@link IItem}.
	 *
	 * @return the content type of this attribute
	 */
	ContentType<T> getContentType();

	/**
	 * A identifier is a text string identifying the attribute. It must never be localized and it
	 * should only contain characters that are safe to use in various configuration files, e.g. as
	 * XML tags. (Analogous to {@link java.lang.reflect.Field#getName()}.)
	 *
	 * @return the attribute identifier
	 */
	String getIdentifier();
}
