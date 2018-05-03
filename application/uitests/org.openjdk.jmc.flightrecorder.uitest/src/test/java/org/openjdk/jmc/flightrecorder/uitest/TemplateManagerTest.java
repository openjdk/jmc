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
package org.openjdk.jmc.flightrecorder.uitest;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;

import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.helpers.ConnectionHelper;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrWizard;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCButton;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCDialog;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable.TableRow;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTemplateManager;

/**
 * Class for tests related to the JFR Template Manager
 */
public class TemplateManagerTest extends MCJemmyTestBase {

	private static JfrWizard jfrWizard;
	private static MCTemplateManager templateManager = null;
	private static boolean canRun = false;

	@ClassRule
	public static MCUITestRule classTestRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			canRun = ConnectionHelper.is7u40orLater(TEST_CONNECTION);
			if (!canRun) {
				System.out.println(
						"This test is silently skipped, since the Template Manger is only available on JDK7u40 or later");
			}
			// assumeTrue should skip the test if it cannot run, but it silently
			// ends the test, with no error.
			// When we move to JUnit 4.4, this can hopefully be fixed.
			Assume.assumeTrue("This feature is only valid on JDK7u40 or later.", canRun);
			// Open Template Manager against our JVM, if not open already
			if (templateManager == null) {
				jfrWizard = MC.jvmBrowser.startFlightRecordingWizard();
				templateManager = jfrWizard.openTemplateManager();
			}
		}

		@Override
		public void after() {
			if (canRun) {
				templateManager.close();
				jfrWizard.cancelWizard();
			}
		}
	};

	private String getTitleOfDuplicatedTemplate(String originalTemplateTitle) {
		return originalTemplateTitle + " (1)";
	}

	@Test
	public void testRegression16177543() {
		String firstName = "alpha";
		String secondName = "beta";

		// Duplicate any template with control elements
		// Only server templates currently, all with control elements, click the
		// first one.
		templateManager.templates.select(0);
		templateManager.duplicateButton.click();

		// Set the name to "Alpha" in the Template Event Details dialog
		MCDialog templateOptionsShell = templateManager.editSelected();
		templateOptionsShell.enterText(firstName);

		// Set the name to "Beta" in the Template Event Details dialog
		templateOptionsShell.clickButton("Advanced");
		MCDialog templateEventDetailsShell = new MCDialog("Template Event Details");
		templateEventDetailsShell.enterText(secondName);
		templateEventDetailsShell.closeWithButton(MCButton.Labels.OK);

		Assert.assertTrue("The template '" + secondName + "' was not found.",
				templateManager.templates.hasItem(secondName));

		Assert.assertFalse("A template named '" + firstName + "' should not exist, but was found.",
				templateManager.templates.hasItem(firstName));

		// Clean-up
		templateManager.removeTemplate(secondName);
	}

	/**
	 * Verify that VM-provided templates cannot be removed
	 */
	@Test
	public void testCannotDeleteServerTemplates() {
		// Can not delete server templates
		// Only server templates currently, click the first one.
		templateManager.templates.select(0);
		TableRow firstRow = templateManager.templates.getRows().get(0);
		String title = firstRow.getText().split("-")[0].trim();
		Assert.assertFalse("Remove button is not disabled for server templates.",
				templateManager.removeButton.isEnabled());

		// Can delete non-server templates
		templateManager.duplicateButton.click();
		String duplicatedTitle = getTitleOfDuplicatedTemplate(title);
		templateManager.templates.select(duplicatedTitle);
		Assert.assertTrue("Remove button is disabled for non-server templates.",
				templateManager.removeButton.isEnabled());

		// Clean-up
		templateManager.removeSelected();
	}

	/**
	 * Verify that the correct dialog is opened when a template has first been customized 
	 */
	@Test
	public void testModifiedTemplateOpensCorrectDialog() {
		// Duplicate the first server template. It should contain control elements.
		templateManager.templates.select(0);
		TableRow firstRow = templateManager.templates.getRows().get(0);
		String templateTitle = firstRow.getText().split("-")[0].trim();
		templateManager.duplicateButton.click();
		String duplicatedTemplateTitle = getTitleOfDuplicatedTemplate(templateTitle);

		// Editing should open the Template Options dialog.
		MCDialog templateOptionsShell = templateManager.editTemplate(duplicatedTemplateTitle);
		Assert.assertTrue("The Edit... button did not open the Template Options dialog.",
				templateOptionsShell.hasLabelText("Template Options"));

		// Save from Template Event Details dialog
		templateOptionsShell.clickButton("Advanced");
		MCDialog templateEventDetailsShell = new MCDialog("Template Event Details");
		templateEventDetailsShell.closeWithButton(MCButton.Labels.OK);

		// Editing should now open the Template Event Details dialog instead.
		templateEventDetailsShell = templateManager.editTemplate(duplicatedTemplateTitle);
		Assert.assertTrue("The Edit... button did not open the Template Event Details dialog.",
				templateEventDetailsShell.hasLabelText("Template Event Details"));

		// Clean-up
		templateEventDetailsShell.closeWithButton(MCButton.Labels.CANCEL);
		templateManager.removeTemplate(duplicatedTemplateTitle);
	}
}
