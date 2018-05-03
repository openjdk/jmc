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
/**
 * JMX Console page support. Pages can be created and added by using the
 * {@code org.openjdk.jmc.console.ui.consolepage} extension. A number of resources are available
 * through dependency injection:
 * <ul>
 * <li>{@link org.openjdk.jmc.console.ui.editor.IConsolePageContainer} - an object working as an
 * interface for the Console page
 * <li>{@link org.eclipse.ui.forms.IManagedForm} - the managed form of the Console page, to add
 * section parts
 * <li>{@link org.eclipse.swt.widgets.Composite} - the actual body of the Console page, don't forget
 * to set appropriate layout manager
 * </ul>
 * This is in addition to the services injected by the {@code org.openjdk.jmc.rjmx} plugin. An
 * example:
 *
 * <pre>
public class SubscriptionExamplePage  {

   {@literal @}Inject
    private ISubscriptionService subscriptionService;

   {@literal @}Inject
    protected void createPageContent(IManagedForm managedForm, Container body) {
        managedForm.getToolkit().decorateFormHeading(managedForm.getForm().getForm());
        final Label valueLabel = managedForm.getToolkit().createLabel(
            body, "", SWT.CENTER);
        body.setLayout(new FillLayout());

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
 */
package org.openjdk.jmc.console.ui.editor;
