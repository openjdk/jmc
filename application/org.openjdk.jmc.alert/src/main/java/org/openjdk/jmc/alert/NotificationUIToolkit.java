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
package org.openjdk.jmc.alert;

import java.io.StringWriter;

import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.rjmx.triggers.TriggerEvent;

/**
 * Utility methods for the notification framework.
 */
public final class NotificationUIToolkit {
	/**
	 * Private since we don't want any instances.
	 */
	private NotificationUIToolkit() {
	}

	/**
	 * Returns the notification event as a String, neatly formatted.
	 *
	 * @param e
	 *            The notification event to pretty print
	 * @return the String resulting from pretty-printing the event.
	 */
	public static String prettyPrint(TriggerEvent e) {
		if (e.wasTriggered()) {
			return prettyPrint(e, Messages.NotificationUIToolkit_EVENT_TOOLKIT_NOTIFICATION_TRIGGERED);
		}
		if (e.wasRecovered()) {
			return prettyPrint(e, Messages.NotificationUIToolkit_EVENT_TOOLKIT_NOTIFICATION_RECOVERED);
		}
		return prettyPrint(e, null);
	}

	/**
	 * Returns the notification event as a String, neatly formatted.
	 *
	 * @param e
	 *            The notification event to pretty print
	 * @param title
	 *            A title for the event message
	 * @return the String resulting from pretty-printing the event.
	 */
	public static String prettyPrint(TriggerEvent e, String title) {
		StringWriter writer = new StringWriter();
		prettyPrint(writer, e, title);
		return writer.toString();
	}

	/**
	 * Same as prettyPrint(TriggerEvent, String), but takes a StringWriter instance that the result
	 * is appended to.
	 *
	 * @param writer
	 *            A StringWriter instance that the result is appended to
	 * @param e
	 *            The notification event to pretty print
	 * @param title
	 *            A title for the event message
	 * @see #prettyPrint(TriggerEvent, String)
	 */
	public static void prettyPrint(StringWriter writer, TriggerEvent e, String title) {
		if (title != null) {
			println(writer, title);
			writer.append("\n"); //$NON-NLS-1$
		}
		println(writer,
				NLS.bind(Messages.NotificationUIToolkit_EVENT_TOOLKIT_NOTIFICATION_CREATION_TIME, e.getCreationTime()));
		println(writer, NLS.bind(Messages.NotificationUIToolkit_EVENT_TOOLKIT_NOTIFICATION_SOURCE,
				e.getConnectorSourceDescription()));
		println(writer,
				NLS.bind(Messages.NotificationUIToolkit_EVENT_TOOLKIT_NOTIFICATION_RULE, e.getRule().getName()));
		println(writer, NLS.bind(Messages.NotificationUIToolkit_EVENT_TOOLKIT_TYPE_DESCRIPTION,
				e.getRule().getTrigger().getAttributeDescriptor()));

		if (e.getSustainTime() > 0) {
			println(writer,
					NLS.bind(Messages.NotificationUIToolkit_EVENT_TOOLKIT_TRIGGER_CONDITION_OPTIONAL_SUSTAIN,
							e.getRule().getTrigger().getValueEvaluator(),
							Double.valueOf(e.getRule().getTrigger().getSustainTime())));
		} else {
			println(writer, NLS.bind(Messages.NotificationUIToolkit_EVENT_TOOLKIT_RULE_TRIGGER_CONDITION,
					e.getRule().getTrigger().getValueEvaluator()));
		}
		println(writer, NLS.bind(Messages.NotificationUIToolkit_EVENT_TOOLKIT_ACTUAL_TRIGGER_VALUE,
				e.getTriggerValue().toString()));
		writer.append("\n"); //$NON-NLS-1$
	}

	private static void println(StringWriter writer, String string) {
		writer.append(string);
		writer.append("\n"); //$NON-NLS-1$
	}
}
