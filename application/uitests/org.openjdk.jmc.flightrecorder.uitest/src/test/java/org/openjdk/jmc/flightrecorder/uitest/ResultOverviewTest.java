package org.openjdk.jmc.flightrecorder.uitest;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrNavigator;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrUi;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCText;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCToolBar;

public class ResultOverviewTest extends MCJemmyTestBase {

	private static final String PLAIN_JFR = "plain_recording.jfr";
	private static MCTable resultTable;
	private static MCToolBar tb;
	private static MCText text;

	@Rule
	public MCUITestRule testRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			JfrUi.openJfr(materialize("jfr", PLAIN_JFR, ResultOverviewTest.class));
			MC.setRecordingAnalysis(true);
			JfrNavigator.selectTab(JfrUi.Tabs.AUTOMATED_ANALYSIS_RESULTS);
		}

		@Override
		public void after() {
			MC.setRecordingAnalysis(false);
			MCMenu.closeActiveEditor();
		}
	};

	@Test
	public void searchTableTest() {
		tb = MCToolBar.getByToolTip(Messages.ResultOverview_DISPLAYMODE_REPORT);
		tb.clickToolItem(Messages.ResultOverview_DISPLAYMODE_TABLE);
		text = MCText.getByToolTip(org.openjdk.jmc.ui.Messages.SEARCH_KLEENE_OR_REGEXP_TOOLTIP);
		resultTable = MCTable.getByColumnHeader(Messages.ResultOverview_COLUMN_PAGE);

		// Verify that the table has elements
		final int totalEvents = resultTable.getItemCount();
		Assert.assertTrue(totalEvents > 0);

		// Verify that the search functionality works
		text.setText(Messages.SystemPage_SECTION_MEMORY);
		final int numEventsWithText = resultTable.getItemCount();
		Assert.assertTrue(totalEvents > numEventsWithText);

		// Verify that a the table is empty with a nonsense search text
		text.setText("ggvgaejltqcxcspninfh");
		final int numEmptyTable = resultTable.getItemCount();
		Assert.assertTrue(numEmptyTable == 0);
	}
}
