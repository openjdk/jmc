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

/**
 * The service from which to create and destroy subscriptions on MRIs. A subscription is created for
 * a value source when a listener is added to it. The subscription will be disposed when no listener
 * remains on a value source.
 * <p>
 * A subscription service can be injected into a Console tab:
 *
 * <pre>
public class SubscriptionExamplePage  {

    <b>{@literal @}Inject
   private ISubscriptionService subscriptionService;</b>

   {@literal @}Inject
    protected void createPageContent(IManagedForm managedForm) {
        managedForm.getToolkit().decorateFormHeading(managedForm.getForm().getForm());
        final Label valueLabel = managedForm.getToolkit().createLabel(
            managedForm.getForm().getBody(), "", SWT.CENTER);
        managedForm.getForm().getBody().setLayout(new FillLayout());

        subscriptionService.addMRIValueListener(
            new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem","ProcessCpuLoad"),
            new IMRIValueListener() {
                {@literal @}Override
                public void valueChanged(final MRIValueEvent event) {
                    DisplayToolkit.safeAsyncExec(label, new Runnable() {
                        {@literal @}Override
                        public void run() {
                            Double latestValue = (Double) event.getValue();
                            label.setText("CPU Load is: " + (latestValue.doubleValue() * 100) + "%");
                        }
                    });
                }
            });
    }
}
 * </pre>
 *
 * @see MRI
 */
public interface ISubscriptionService {
	/**
	 * Adds the listener to the subscription on given descriptor (possible creating it).
	 *
	 * @param mri
	 *            the MRI stating how and where to listen to value changes.
	 * @param listener
	 *            the listener to add.
	 */
	void addMRIValueListener(MRI mri, IMRIValueListener listener);

	/**
	 * Substitutes two listeners. The new listener will subscribe to all subscriptions that the old
	 * listener had and the old listener will unsubscribe from them.
	 *
	 * @param oldListener
	 *            the old listener to unsubscribe.
	 * @param newListener
	 *            the new listener to subscribe instead.
	 */
	void substituteMRIValueListener(IMRIValueListener oldListener, IMRIValueListener newListener);

	/**
	 * Removes the listener from all its subscriptions, possible disposing of some or all of the
	 * subscriptions if this was the last listener.
	 *
	 * @param listener
	 *            the listener to remove.
	 */
	void removeMRIValueListener(IMRIValueListener listener);

	/**
	 * Removes the listener from the subscription of given MRI, possible disposing of the
	 * subscription if this was the last listener.
	 *
	 * @param mri
	 *            the MRI on which to stop listen to value changes.
	 * @param listener
	 *            the value listener to remove.
	 */
	void removeMRIValueListener(MRI mri, IMRIValueListener listener);

	/**
	 * Returns the subscription associated with the {@link MRI}, if such a subscription exists, or
	 * {@code null} if no such subscription exists.
	 *
	 * @param mri
	 *            the MRI describing what data to retrieve.
	 * @return the subscription, if one already exists, or {@code null} if no subscription for the
	 *         MRI could be found.
	 */
	IMRISubscription getMRISubscription(MRI mri);

	/**
	 * Returns the last value event for the given MRI through its subscription.
	 *
	 * @param mri
	 *            the MRI for which to get the last value event.
	 * @return the last value event logged through the subscription with given descriptor. Returns
	 *         {@code null} if either the subscription does not exist or if it holds no value event.
	 */
	MRIValueEvent getLastMRIValueEvent(MRI mri);

	/**
	 * Returns whether a subscription on the MRI has been attempted but failed.
	 *
	 * @param mri
	 *            the MRI to check.
	 * @return {@code true} if a subscription on the MRI has been attempted but failed,
	 *         {@code false} if either subscribing on the MRI has not failed or never been
	 *         subscribed to.
	 */
	boolean isMRIUnavailable(MRI mri);
}
