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
package org.openjdk.jmc.rjmx.ui.celleditors;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.util.Date;

import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;

import org.openjdk.jmc.rjmx.ui.messages.internal.Messages;

public class DateCellEditor extends ParsingCellEditor {

	private static final int[] STYLES = new int[] {DateFormat.MEDIUM, DateFormat.FULL, DateFormat.LONG,
			DateFormat.SHORT};

	private DateFormat lastParsedFormat = null;

	public DateCellEditor(Composite parent) {
		super(parent);
	}

	@Override
	protected String format(Object value) {
		if (value instanceof Date) {
			return DateFormat.getDateTimeInstance().format(value);
		}
		return super.format(value);
	}

	@Override
	protected Date parse(String str) throws Exception {
		str = str.trim();
		ParsePosition pp = new ParsePosition(0);
		if (lastParsedFormat != null) {
			Date date = lastParsedFormat.parse(str, pp);
			if (date != null && pp.getIndex() == str.length()) {
				return date;
			}
		}
		for (int dateStyle : STYLES) {
			for (int timeStyle : STYLES) {
				DateFormat dateFormat = DateFormat.getDateTimeInstance(dateStyle, timeStyle);
				Date date = dateFormat.parse(str, pp);
				if (date != null && pp.getIndex() == str.length()) {
					lastParsedFormat = dateFormat;
					return date;
				}
			}
		}
		throw new ParseException(NLS.bind(Messages.DATE_PARSE_ERROR_MSG, str, format(new Date())));
	}
}
