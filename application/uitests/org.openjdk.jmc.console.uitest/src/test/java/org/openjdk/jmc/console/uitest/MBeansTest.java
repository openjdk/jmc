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
package org.openjdk.jmc.console.uitest;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.openjdk.jmc.console.uitest.mbeanhelpers.Mbean2Runner;
import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JmxConsole;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCButton;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCDialog;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCSection;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTabFolder;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCToolBar;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTree;

/**
 * Class for testing MBeans appearing and disappearing during a console session
 */
public class MBeansTest extends MCJemmyTestBase {
	private static final String NEW_CHART_DEFAULT_NAME = org.openjdk.jmc.rjmx.ui.attributes.Messages.VisualizeWizardPage_DEFAULT_CHART_NAME;
	private static final String ADD_CHART_TOOLITEM = org.openjdk.jmc.rjmx.ui.attributes.Messages.VisualizeWizardPage_CREATE_CHART_BUTTON_TEXT;
	private static final String MBEAN_NAME = "flickeringMBean";
	private static final String MBEAN_ATTRIBUTE_NAME = "BigInteger";
	private static final String MBEAN_PATH = "key1=" + MBEAN_NAME;
	private static final int sleepTime = 2000;
	private MCSection chartSection = null;
	private Mbean2Runner mBean2Runner = null;
	private static String savedConnectionName;

	@Rule
	public MCUITestRule testRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			// start the MBean server and register our MBean
			mBean2Runner = startMBeanServer();
			// register our MBean (assert that it does succeed)
			Assert.assertTrue("Problem registering MBean. Ending test", mBean2Runner.registerMBean(MBEAN_PATH));

			// open up the console and make sure that the overview tab is chosen
			MC.jvmBrowser.connect();
			JmxConsole.selectTab(JmxConsole.Tabs.OVERVIEW);

			// collapse the default charts and dials
			collapseDefaultCharts();

			// add a new chart to the overview tab
			MCToolBar.getByToolTip(ADD_CHART_TOOLITEM).clickToolItem(ADD_CHART_TOOLITEM);

			// add an attribute to the chart
			chartSection = MCSection.getByLabel(NEW_CHART_DEFAULT_NAME);
			addMBeanAttributeToChart("TestAgent", MBEAN_NAME, MBEAN_ATTRIBUTE_NAME, chartSection);
		}

		@Override
		public void after() {
			// clean-up
			// remove the custom chart
			if (chartSection != null) {
				chartSection.getHyperlink("Close").click();
			}
			// expand the default charts and dials
			expandDefaultCharts();
			// shut down the Mbean2Runner
			if (mBean2Runner != null) {
				mBean2Runner.stopRunning();
			}
		}
	};

	@ClassRule
	public static MCUITestRule classTestRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			// saving TEST_CONNECTION and setting it to this JVM to be able to successfully do the testing
			savedConnectionName = TEST_CONNECTION;
			TEST_CONNECTION = "The JVM Running Mission Control";
		}

		@Override
		public void after() {
			TEST_CONNECTION = savedConnectionName;
		}
	};

	/**
	 * This tests if charts are updated (and not updated) correctly when a MBean is de-registered at
	 * the server and re-registered again
	 */
	@Test
	public void intermittentMBeanTest() {
		MCTabFolder overviewTabFolder = MCTabFolder.getByTabName("Overview");

		overviewTabFolder.isWidgetUpdating(sleepTime);
		// verify that the chart updates itself with attribute data
		Assert.assertTrue("Chart " + NEW_CHART_DEFAULT_NAME
				+ " is not updated with new data after initial addition of attribute " + MBEAN_ATTRIBUTE_NAME,
				overviewTabFolder.isWidgetUpdating(sleepTime));

		// de-register the MBean
		mBean2Runner.unregisterMBean(MBEAN_PATH);

		// verify that the chart isn't updated now
		Assert.assertFalse(
				"Chart " + NEW_CHART_DEFAULT_NAME + " is still updated with new data after de-registration of MBean",
				overviewTabFolder.isWidgetUpdating(sleepTime));

		// re-register the MBean
		mBean2Runner.registerMBean(MBEAN_PATH);

		// verify that the chart is once again updated with attribute data
		Assert.assertTrue(
				"Chart " + NEW_CHART_DEFAULT_NAME + " is not updated with new data after re-registration of MBean",
				overviewTabFolder.isWidgetUpdating(sleepTime));
	}

	private void addMBeanAttributeToChart(
		final String MBEAN_NAMESPACE, final String MBEAN_NAME, final String MBEAN_ATTRIBUTE_NAME,
		MCSection chartSection) {
		// open the attribute dialog
		chartSection.getHyperlink("Add attributes").click();
		// first page
		MCDialog attributeShell = MCDialog.getByAnyDialogTitle("Select Attribute to Add");
		MCTree attributeTree = attributeShell.getFirstTree();
		attributeTree.select(MBEAN_NAMESPACE, MBEAN_NAME, MBEAN_ATTRIBUTE_NAME);
		attributeShell.clickButton(MCButton.Labels.NEXT);
		// second page
		MCTable attributeTable = attributeShell.getAllTables().get(0);
		attributeTable.select(MBEAN_ATTRIBUTE_NAME, "Content Type");
		attributeTable.contextChoose("Number, In Units Of", "1");
		attributeShell.closeWithButton(MCButton.Labels.FINISH);
	}

	private void expandDefaultCharts() {
		MCSection.getByLabel("Dashboard").expand();
		MCSection.getByLabel("Processor").expand();
		MCSection.getByLabel("Memory").expand();
	}

	private void collapseDefaultCharts() {
		MCSection.getByLabel("Dashboard").collapse();
		MCSection.getByLabel("Processor").collapse();
		MCSection.getByLabel("Memory").collapse();
	}

	private Mbean2Runner startMBeanServer() {
		mBean2Runner = new Mbean2Runner(false);
		Thread mBean2RunnerThread = new Thread(mBean2Runner);
		mBean2RunnerThread.start();
		while (!mBean2Runner.isUpAndRunning()) {
			try {
				Thread.sleep(500);
				System.out.println("Waiting for Runnables to start");
			} catch (InterruptedException ie) {
				// Do nothing
			}
		}
		return mBean2Runner;
	}
}
