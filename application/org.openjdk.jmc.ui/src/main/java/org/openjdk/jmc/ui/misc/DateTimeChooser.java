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
package org.openjdk.jmc.ui.misc;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.function.LongUnaryOperator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;

/**
 * A combined date and time chooser. Uses the {@link DateTime} widget. Can be given constraints for
 * minimum and maximum values and that the time should not be lesser than or greater then another
 * {@link DateTimeChooser}.
 * <p>
 * Note that although this class is a subclass of {@link Composite}, it does not make sense to add
 * children to it, or set a layout on it.
 */
public class DateTimeChooser extends Composite {

	/*
	 * Intentionally kept state-less.
	 */
	private final DateTime m_dateWidget;
	private final DateTime m_timeWidget;
	private LongUnaryOperator m_timestampConstraint;

	/**
	 * Constructs a new instance of this class given its parent and a style value describing its
	 * behavior and appearance.
	 *
	 * @param parent
	 *            a widget which will be the parent of the new instance (cannot be null)
	 * @param style
	 *            the style of widget to construct
	 */
	public DateTimeChooser(Composite parent, int style) {
		super(parent, style);
		FillLayout layout = new FillLayout();
		layout.type = SWT.HORIZONTAL;
		setLayout(layout);

		SelectionListener selectionListener = createSelectionListener();
		m_dateWidget = new DateTime(this, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN);
		m_dateWidget.addSelectionListener(selectionListener);
		m_timeWidget = new DateTime(this, SWT.TIME | SWT.LONG);
		m_timeWidget.addSelectionListener(selectionListener);
	}

	@Override
	public void setEnabled(boolean enabled) {
		m_dateWidget.setEnabled(enabled);
		m_timeWidget.setEnabled(enabled);
		super.setEnabled(enabled);
	}

	private SelectionListener createSelectionListener() {
		return new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				constrainValue();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				constrainValue();
			}
		};
	}

	private void constrainValue() {
		if (m_timestampConstraint != null) {
			long currentTime = getTimestamp();
			long constrainedTime = m_timestampConstraint.applyAsLong(currentTime);
			if (currentTime != constrainedTime) {
				setTimestamp(constrainedTime);
			}
		}
	}

	/**
	 * @return the timestamp in UTC milliseconds from the epoch
	 */
	public long getTimestamp() {
		Calendar calendar = new GregorianCalendar();
		calendar.set(m_dateWidget.getYear(), m_dateWidget.getMonth(), m_dateWidget.getDay(), m_timeWidget.getHours(),
				m_timeWidget.getMinutes(), m_timeWidget.getSeconds());
		return calendar.getTimeInMillis();
	}

	/**
	 * @param time
	 *            the timestamp in UTC milliseconds from the epoch
	 */
	public void setTimestamp(long time) {
		Calendar calendar = new GregorianCalendar();
		calendar.setTimeInMillis(time);
		m_dateWidget.setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
		m_timeWidget.setTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
				calendar.get(Calendar.SECOND));
	}

	public void setConstraint(LongUnaryOperator timestampConstraint) {
		m_timestampConstraint = timestampConstraint;
	}

}
