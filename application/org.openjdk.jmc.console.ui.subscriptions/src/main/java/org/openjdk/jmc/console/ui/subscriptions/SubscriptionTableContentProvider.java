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
package org.openjdk.jmc.console.ui.subscriptions;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.rjmx.subscription.internal.EmptySubscriptionDebugService;
import org.openjdk.jmc.rjmx.subscription.internal.IMRISubscriptionDebugInformation;
import org.openjdk.jmc.rjmx.subscription.internal.ISubscriptionDebugService;

public class SubscriptionTableContentProvider implements IStructuredContentProvider {

	private static ISubscriptionDebugService getSubscriptionDebugService(IConnectionHandle handle) {
		ISubscriptionService service = handle.getServiceOrNull(ISubscriptionService.class);
		if (service instanceof ISubscriptionDebugService) {
			return (ISubscriptionDebugService) service;
		}
		return new EmptySubscriptionDebugService();
	}

	private static Object[] getColumnObjects(IMRISubscriptionDebugInformation information) {
		return new Object[] {information.getMRI(), getState(information), information.getConnectionCount(),
				information.getDisconnectionCount(), information.getEventCount(), information.getRetainedEventCount(),
				getLastEventValue(information), getLastEventPayload(information), information.getConnectionLostCount(),
				information.getTriedReconnectionsCount(), information.getSucceededReconnectionsCount()};
	}

	private static Object getState(IMRISubscriptionDebugInformation information) {
		return information.getState();
	}

	private static Object getLastEventValue(IMRISubscriptionDebugInformation information) {
		MRIValueEvent event = information.getLastEvent();
		if (event != null) {
			return event.getValue();
		}
		return null;
	}

	private static String getLastEventPayload(IMRISubscriptionDebugInformation information) {
		MRIValueEvent event = information.getLastEvent();
		if (event == null) {
			return Messages.PAYLOAD_NO_EVENT_LABEL;
		}
		Object value = event.getValue();
		if (value == null) {
			return Integer.toString(0);
		}
		if (value instanceof Serializable) {
			return calculateSerializeableSize(value);
		}
		return Messages.PAYLOAD_NOT_SERIALIZEABLE_LABEL;
	}

	private static class CountOutputStream extends OutputStream {
		private int m_count;

		public int getCount() {
			return m_count;
		}

		@Override
		public void write(int b) throws IOException {
			m_count += 1;
		}

		@Override
		public void write(byte[] b, int off, int len) {
			if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
				throw new IndexOutOfBoundsException();
			}
			m_count += len;
		}
	}

	private static String calculateSerializeableSize(Object value) {
		CountOutputStream cout = new CountOutputStream();
		try (ObjectOutputStream oout = new ObjectOutputStream(cout)) {
			oout.writeObject(value);
			oout.close();
			return Integer.toString(cout.getCount());
		} catch (IOException e) {
			return Messages.PAYLOAD_IO_EXCEPTION_LABEL;
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (!(inputElement instanceof IConnectionHandle)) {
			return new Object[0];
		}
		Collection<IMRISubscriptionDebugInformation> debugInformation = getSubscriptionDebugService(
				(IConnectionHandle) inputElement).getDebugInformation();
		Object[][] cells = new Object[debugInformation.size()][];
		int i = 0;
		for (IMRISubscriptionDebugInformation info : debugInformation) {
			cells[i++] = getColumnObjects(info);
		}
		return cells;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

}
