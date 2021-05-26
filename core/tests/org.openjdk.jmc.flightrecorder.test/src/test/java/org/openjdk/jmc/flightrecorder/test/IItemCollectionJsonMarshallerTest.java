package org.openjdk.jmc.flightrecorder.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.openjdk.jmc.common.item.IItemCollection;

import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.json.AdHocIItemCollectionJsonMarshaller;
import org.openjdk.jmc.flightrecorder.json.FastJsonIItemCollectionJsonMarshaller;
import org.openjdk.jmc.flightrecorder.test.util.RecordingToolkit;

import com.alibaba.fastjson.JSONValidator;

public class IItemCollectionJsonMarshallerTest {

	@Test
	public void validateFastJsonMarshalling() throws IOException, CouldNotLoadRecordingException {
		IItemCollection events = RecordingToolkit.getNamedRecording("metadata_control.jfr");
		long start = System.nanoTime();
		String jsonEvents = FastJsonIItemCollectionJsonMarshaller.toJsonString(events);
		System.out.println("FastJSON marshalling time: " + (System.nanoTime() - start) / 1e6);
		JSONValidator validator = JSONValidator.from(jsonEvents);
		assertTrue(validator.validate());
	}

	@Test
	public void validateAdHocMarshalling() throws IOException, CouldNotLoadRecordingException {
		IItemCollection events = RecordingToolkit.getNamedRecording("metadata_control.jfr");
		long start = System.nanoTime();
		String jsonEvents = AdHocIItemCollectionJsonMarshaller.toJsonString(events);
		System.out.println("Ad-hoc JSON marshalling time: " + (System.nanoTime() - start) / 1e6);
		JSONValidator validator = JSONValidator.from(jsonEvents);
		assertTrue(validator.validate());
	}
}
