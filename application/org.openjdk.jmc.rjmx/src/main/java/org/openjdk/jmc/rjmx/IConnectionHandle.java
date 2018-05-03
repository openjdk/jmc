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
package org.openjdk.jmc.rjmx;

import java.io.Closeable;

/**
 * A connection handle is a handle to an active connection. It provides services around the
 * connection. A connection handle can be injected into a Console page:
 *
 * <pre>
public class ConnectionExamplePage {

   <b>{@literal @}Inject
    private IConnectionHandle connectionHandle;</b>

   {@literal @}Inject
    protected void createPageContent(IManagedForm managedForm) {
        managedForm.getToolkit().decorateFormHeading(managedForm.getForm().getForm());
        final Label valueLabel = managedForm.getToolkit().createLabel(
            managedForm.getForm().getBody(), "", SWT.CENTER);
        <b>valueLabel.setText("isConnected: " + connectionHandle.isConnected());</b>
        managedForm.getForm().getBody().setLayout(new FillLayout());
    }
}
 * </pre>
 */
public interface IConnectionHandle extends Closeable {

	/**
	 * Gets the server descriptor for this connection handle.
	 *
	 * @return the server descriptor
	 */
	IServerDescriptor getServerDescriptor();

	/**
	 * Get a service implementation of {@code serviceClass}.
	 *
	 * @param <T>
	 *            the service type to look up
	 * @param serviceClass
	 *            the {@link Class} of the service
	 * @return either a proper service, or a dummy service that mostly throws exceptions, never
	 *         {@code null}.
	 * @throws IllegalArgumentException
	 *             if the service argument didn't represent an interface.
	 */
	<T> T getServiceOrDummy(Class<T> serviceClass) throws IllegalArgumentException;

	/**
	 * Get a proper service implementation of {@code serviceClass}, or {@code null} if no such
	 * service could be created or the connection has been closed.
	 *
	 * @param <T>
	 *            the service type to look up
	 * @param serviceClass
	 *            the {@link Class} of the service
	 * @return a proper service implementing {@code serviceClass}, or {@code null}.
	 */
	<T> T getServiceOrNull(Class<T> serviceClass);

	/**
	 * Get a proper service implementation of {@code serviceClass}
	 *
	 * @param <T>
	 *            the service type to look up
	 * @param serviceClass
	 *            the {@link Class} of the service
	 * @return a proper service implementing {@code serviceClass}
	 * @throws ConnectionException
	 *             if the connection handle has been closed
	 * @throws ServiceNotAvailableException
	 *             if the service is not available
	 */
	<T> T getServiceOrThrow(Class<T> serviceClass) throws ConnectionException, ServiceNotAvailableException;

	/**
	 * Returns whether there is a service registered for the given type.
	 *
	 * @param clazz
	 *            the {@link Class} of the service
	 * @return {@code true} if the service exist, {@code false} otherwise.
	 */
	boolean hasService(Class<?> clazz);

	/**
	 * Return whether this connection is live.
	 *
	 * @return {@code true} if this connection is live, {@code false} otherwise.
	 */
	boolean isConnected();

	/**
	 * Returns a description of the connection handle's intended use.
	 *
	 * @return the description of the connection handle's intended use.
	 */
	String getDescription();

}
