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
package org.openjdk.jmc.rjmx.triggers.internal;

import java.io.PrintWriter;

import org.openjdk.jmc.rjmx.triggers.ITrigger;
import org.openjdk.jmc.rjmx.triggers.TriggerEvent;

/**
 * Utility methods for the notification framework.
 */
public final class NotificationToolkit {
	/**
	 * Private since we don't want any instances.
	 */
	private NotificationToolkit() {
	}

	/**
	 * Returns the notification event as a String, neatly formatted.
	 *
	 * @param e
	 *            the notification event to pretty print
	 * @return the String resulting from pretty-printing the event.
	 */
	public static String prettyPrint(TriggerEvent e) {
		StringBuffer buf = new StringBuffer(200);

		if (e.wasTriggered()) {
			buf.append("A notification event has been triggered!\n\n"); //$NON-NLS-1$
		}
		if (e.wasRecovered()) {
			buf.append("Your notification has recovered!\n\n"); //$NON-NLS-1$
		}
		buf.append("Notification creation time was: " + e.getCreationTime() + '\n'); //$NON-NLS-1$
		buf.append("The notification source is: " + e.getConnectorSourceDescription() + '\n'); //$NON-NLS-1$
		buf.append("The notification rule is: " + e.getRule().getName() + '\n'); //$NON-NLS-1$
		buf.append("Type description:\n" + e.getRule().getTrigger().getAttributeDescriptor() + '\n'); //$NON-NLS-1$
		buf.append("Rule trigger condition: " + e.getRule().getTrigger().getValueEvaluator()); //$NON-NLS-1$

		if (e.getSustainTime() > 0) {
			buf.append(" for " + e.getRule().getTrigger().getSustainTime() + " seconds.\n"); //$NON-NLS-1$ //$NON-NLS-2$
			buf.append("The condition was met for " + e.getSustainTime() / 1000 + " seconds.\n"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			buf.append("\nThe actual trigger value: " + e.getTriggerValue().toString()); //$NON-NLS-1$
		}
		return buf.toString();
	}

	/**
	 * Same as prettyPrint(NotificationEvent), but takes a PrintWriter instance that the result is
	 * printed on.
	 *
	 * @see #prettyPrint(TriggerEvent)
	 */
	public static void prettyPrint(PrintWriter writer, TriggerEvent e) {
		if (e.wasTriggered()) {
			writer.println("A notification event has been triggered!"); //$NON-NLS-1$
		}
		if (e.wasRecovered()) {
			writer.println("Your notification has recovered!"); //$NON-NLS-1$
		}
		writer.println("Notification creation time was: " + e.getCreationTime()); //$NON-NLS-1$
		writer.println("The notification source is: " + e.getConnectorSourceDescription()); //$NON-NLS-1$
		writer.println("The notification rule is: " + e.getRule().getName()); //$NON-NLS-1$
		writer.println("Type description:"); //$NON-NLS-1$
		writer.println(e.getRule().getTrigger().getAttributeDescriptor());
		writer.print("Rule trigger condition: " + e.getRule().getTrigger().getValueEvaluator()); //$NON-NLS-1$

		if (e.getSustainTime() > 0) {
			writer.println(" for " + e.getRule().getTrigger().getSustainTime() + " seconds."); //$NON-NLS-1$ //$NON-NLS-2$
			writer.println("The condition was met for " + e.getSustainTime() / 1000 + " seconds."); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			writer.println("\nThe actual trigger value: " + e.getTriggerValue().toString()); //$NON-NLS-1$
		}
		writer.println();
	}

	/**
	 * A trigger is complete if it has both a type and a value evaluator.
	 *
	 * @return true if the trigger is complete according to the definition above.
	 */
	public static boolean isComplete(ITrigger trigger) {
		return (trigger.getValueEvaluator() != null) && (trigger.getAttributeDescriptor() != null);
	}
}
