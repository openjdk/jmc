package org.openjdk.jmc.flightrecorder.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.test.util.RecordingToolkit;

public class OverlappingEventsTest {

	@Test
	public void testStartTime() throws IOException, CouldNotLoadRecordingException {
		IItemCollection overlap = RecordingToolkit.getNamedRecording("overlap.jfr")
				.apply(ItemFilters.type("org.openjdk.jmc.test.OverlappingEvent"));
		IAggregator<IQuantity, ?> first = Aggregators.min(JfrAttributes.START_TIME);
		IQuantity expected = overlap.getAggregate(first);
		IQuantity actual = RulesToolkit.getEarliestStartTime(overlap).in(expected.getUnit());
		Assert.assertEquals("expected: " + expected.displayUsing(IDisplayable.AUTO) + ", actual: "
				+ actual.displayUsing(IDisplayable.AUTO), expected, actual);
	}

	@Test
	public void testFirstEndTime() throws IOException, CouldNotLoadRecordingException {
		IItemCollection overlap = RecordingToolkit.getNamedRecording("overlap.jfr")
				.apply(ItemFilters.type("org.openjdk.jmc.test.OverlappingEvent"));
		IAggregator<IQuantity, ?> min = Aggregators.min(JfrAttributes.END_TIME);
		IQuantity expected = overlap.getAggregate(min);
		IQuantity actual = RulesToolkit.getEarliestEndTime(overlap).in(expected.getUnit());
		Assert.assertEquals("expected: " + expected.displayUsing(IDisplayable.AUTO) + ", actual: "
				+ actual.displayUsing(IDisplayable.AUTO), expected, actual);
	}

	@Test
	public void testLastEndTime() throws IOException, CouldNotLoadRecordingException {
		IItemCollection overlap = RecordingToolkit.getNamedRecording("overlap.jfr")
				.apply(ItemFilters.type("org.openjdk.jmc.test.OverlappingEvent"));
		IAggregator<IQuantity, ?> last = Aggregators.max(JfrAttributes.END_TIME);
		IQuantity aggregatedLast = overlap.getAggregate(last).in(UnitLookup.EPOCH_NS);
		IQuantity actual = RulesToolkit.getLatestEndTime(overlap).in(UnitLookup.EPOCH_NS);
		Assert.assertEquals("expected: " + aggregatedLast.displayUsing(IDisplayable.AUTO) + ", actual: "
				+ actual.displayUsing(IDisplayable.AUTO), aggregatedLast, actual);
	}

}
