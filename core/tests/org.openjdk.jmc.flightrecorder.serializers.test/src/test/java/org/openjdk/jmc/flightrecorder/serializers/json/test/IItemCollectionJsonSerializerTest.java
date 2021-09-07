package org.openjdk.jmc.flightrecorder.serializers.json.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.test.io.IOResourceSet;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.serializers.json.FlameGraphJsonSerializer;
import org.openjdk.jmc.flightrecorder.serializers.json.IItemCollectionJsonSerializer;
import org.openjdk.jmc.flightrecorder.test.util.RecordingToolkit;
import org.openjdk.jmc.flightrecorder.test.util.StacktraceTestToolkit;

public class IItemCollectionJsonSerializerTest {

	private static IItemCollection testRecording;

	@BeforeClass
	public static void beforeAll() throws IOException, CouldNotLoadRecordingException {
		IOResourceSet[] testResources = StacktraceTestToolkit.getTestResources();
		IOResourceSet resourceSet = testResources[0];
		testRecording = RecordingToolkit.getFlightRecording(resourceSet);
	}

	@Test
	public void testSerializeKnownRecording() throws IOException {
		String expected = readResource("/iitemcollection.json");

		String actual = IItemCollectionJsonSerializer.toJsonString(testRecording);

		assertEquals(expected, actual);
	}

	private String readResource(String resourcePath) throws IOException {
		try (InputStream is = FlameGraphJsonSerializer.class.getResourceAsStream(resourcePath)) {
			if (is == null) {
				throw new IllegalArgumentException(resourcePath + " not found");
			}
			return StringToolkit.readString(is);
		}
	}
}
