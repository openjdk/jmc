package org.openjdk.jmc.flightrecorder.test;

import java.io.IOException;

import org.junit.Test;
import org.junit.Assert;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.test.util.RecordingToolkit;

public class OverlappingEventsTest {

	private IQuantity earliestGoldenValue(IItemCollection events, IAttribute<IQuantity> attr) {
		IQuantity earliest = UnitLookup.EPOCH_NS.quantity(Long.MAX_VALUE);
		for (IItemIterable eventIterable : events) {
			IMemberAccessor<IQuantity, IItem> accessor = attr.getAccessor(eventIterable.getType());
			for (IItem event : eventIterable) {
				IQuantity time = accessor.getMember(event);
				if (earliest.compareTo(time) > 0) {
					earliest = time;
				}
			}
		}
		return earliest.in(UnitLookup.EPOCH_NS);
	}

	private IQuantity latestGoldenValue(IItemCollection events, IAttribute<IQuantity> attr) {
		IQuantity earliest = UnitLookup.EPOCH_NS.quantity(0);
		for (IItemIterable eventIterable : events) {
			IMemberAccessor<IQuantity, IItem> accessor = attr.getAccessor(eventIterable.getType());
			for (IItem event : eventIterable) {
				IQuantity time = accessor.getMember(event);
				if (earliest.compareTo(time) < 0) {
					earliest = time;
				}
			}
		}
		return earliest.in(UnitLookup.EPOCH_NS);
	}

	@Test
	public void testStartTime() throws IOException, CouldNotLoadRecordingException {
		IItemCollection overlap = RecordingToolkit.getNamedRecording("overlap.jfr");
		IAggregator<IQuantity, ?> first = JdkAggregators.first(JfrAttributes.START_TIME);
		IQuantity expected = overlap.getAggregate(first);
		IQuantity actual = RulesToolkit.getEarliestStartTime(overlap).in(expected.getUnit());
		Assert.assertEquals("expected: " + expected.displayUsing(IDisplayable.AUTO) + ", actual: "
				+ actual.displayUsing(IDisplayable.AUTO), expected, actual);
	}

	@Test
	public void testFirstEndTime() throws IOException, CouldNotLoadRecordingException {
		IItemCollection overlap = RecordingToolkit.getNamedRecording("overlap.jfr");
		IQuantity expected = earliestGoldenValue(overlap, JfrAttributes.END_TIME);
		IQuantity actual = RulesToolkit.getEarliestEndTime(overlap).in(expected.getUnit());
		Assert.assertEquals("expected: " + expected.displayUsing(IDisplayable.AUTO) + ", actual: "
				+ actual.displayUsing(IDisplayable.AUTO), expected, actual);
	}

	@Test
	public void testLastEndTime() throws IOException, CouldNotLoadRecordingException {
		IItemCollection overlap = RecordingToolkit.getNamedRecording("overlap.jfr");
		IQuantity expected = latestGoldenValue(overlap, JfrAttributes.END_TIME);
		IQuantity actual = RulesToolkit.getLatestEndTime(overlap).in(expected.getUnit());
		Assert.assertEquals("expected: " + expected.displayUsing(IDisplayable.AUTO) + ", actual: "
				+ actual.displayUsing(IDisplayable.AUTO), expected, actual);
	}

}
