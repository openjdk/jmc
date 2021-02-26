package org.openjdk.jmc.flightrecorder.test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.json.IItemCollectionJsonSerializer;
import org.openjdk.jmc.flightrecorder.test.util.RecordingToolkit;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertTrue;

public class IItemCollectionJsonSerializerTest {
    private static IItemCollection testRecording;

    @BeforeClass
    public static void beforeAll() throws IOException, CouldNotLoadRecordingException {
        testRecording = RecordingToolkit.getNamedRecording("metadata_new.jfr");
    }

    // Ideally, we would test the output of the serializer against the output from the jfr command.

    // However, we lack JSON libraries (e.g. JSONAssert) to make comparing the output easier, and we don't exactly
    // respect the same format since jfr outputs nested objects for some fields (e.g. "eventThread") and JMC has
    // useful helpers for generating string representations (e.g. method descriptor).

    @Test
    public void testNewSerializer() throws IOException {
        StringWriter sw = new StringWriter();
        IItemCollectionJsonSerializer serializerV2 = new IItemCollectionJsonSerializer(sw);
        serializerV2.writeEventCollection(testRecording);
        assertTrue(sw.getBuffer().toString().length() > 0);
    }
}
