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
package org.openjdk.jmc.common.unit;

import org.openjdk.jmc.common.util.TypeHandling;

/**
 * A display formatter is basically an identifier that tells a user interface widget how the unit
 * should be formatted, e.g. as a kilobytes. Currently the display unit doesn't contain any
 * information about what makes the unit, for instance 1 kilobyte is 1024 times as much as a byte or
 * the precision. This could be added on later by sub-classing {@link DisplayFormatter} and by
 * adding properties specific for the {@link DisplayFormatter}.
 * <p>
 * A display formatter with the magic identifier "auto" should be used to tells the user interface
 * widget that it can decide by itself how the unit should be formatted. E.g, if it is a large
 * number it may decide show it as GiB, but if it is a low number it may choose bytes.
 *
 * @param <T>
 *            the type of values that can be formatted
 */
/*
 * FIXME: Rewrite! This class is responsible for holding configuration details about which kind of
 * display formatters are available in Mission Control. It's a configuration class, and it should
 * not be instantiated or persisted by a client.
 */
public class DisplayFormatter<T> implements IFormatter<T> {
	public static final String ENGINEERING_NOTATION_IDENTIFIER = "engineeringNotation"; //$NON-NLS-1$
	public static final String SCIENTIFIC_NOTATION_IDENTIFIER = "scientificNotation"; //$NON-NLS-1$

	final private String m_name;
	final private String m_identifier;
	final private ContentType<T> m_contentType;

	// NOTE: Name is not used anywhere at the moment so it is not necessary to localize
	protected DisplayFormatter(ContentType<T> contentType, String identifier, String name) {
		m_name = name;
		m_contentType = contentType;
		m_identifier = identifier;
	}

	public String getName() {
		return m_name;
	}

	public String getIdentifier() {
		return m_identifier;
	}

	public ContentType<T> getContentType() {
		return m_contentType;
	}

	@Override
	public String format(T o) {
		return TypeHandling.getValueString(o);
	}
}
