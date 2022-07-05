package org.openjdk.jmc.flightrecorder.serializers.stacktraces.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.test.TestToolkit;
import org.openjdk.jmc.common.test.io.IOResourceSet;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.serializers.dot.DotSerializer;
import org.openjdk.jmc.flightrecorder.serializers.dot.test.DotSerializerTest;
import org.openjdk.jmc.flightrecorder.serializers.json.FlameGraphJsonSerializer;
import org.openjdk.jmc.flightrecorder.serializers.json.test.FlameGraphJsonSerializerTest;
import org.openjdk.jmc.flightrecorder.serializers.stacktraces.CollapsedSerializer;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.graph.StacktraceGraphModel;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;
import org.openjdk.jmc.flightrecorder.test.util.RecordingToolkit;
import org.openjdk.jmc.flightrecorder.test.util.StacktraceTestToolkit;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class CollapsedSerializerTest {
	private static final boolean INVERTED_STACKS = true;
	private static final boolean REGULAR_STACKS = false;
	private static final FrameSeparator METHOD_SEPARATOR = new FrameSeparator(FrameSeparator.FrameCategorization.METHOD,
			false);

	private static IItemCollection testRecording;

	@BeforeClass
	public static void beforeAll() throws IOException, CouldNotLoadRecordingException {
		IOResourceSet[] testResources = StacktraceTestToolkit.getTestResources();
		IOResourceSet resourceSet = testResources[0];
		testRecording = RecordingToolkit.getFlightRecording(resourceSet);
	}

	@Test
	public void testSerializeKnownRecording() throws IOException, CouldNotLoadRecordingException {
		IItemCollection collection = RecordingToolkit.getFlightRecording(
				TestToolkit.getNamedResource(FlameGraphJsonSerializerTest.class, "recordings", "hotmethods.jfr"));
		assertNotNull(collection);
		String collapsed = CollapsedSerializer.toCollapsed(new StacktraceTreeModel(collection,
				new FrameSeparator(FrameSeparator.FrameCategorization.METHOD, false)));
		System.out.println(collapsed);
		try (Writer bw = new FileWriter("/tmp/hotmethods.collapsed")) {
			bw.write(collapsed);
		}
	}

}
