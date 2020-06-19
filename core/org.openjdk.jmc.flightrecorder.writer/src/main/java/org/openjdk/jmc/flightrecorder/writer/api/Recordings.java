package org.openjdk.jmc.flightrecorder.writer.api;

import org.openjdk.jmc.flightrecorder.writer.RecordingImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A factory class to create new {@linkplain Recording} instances
 */
public final class Recordings {
	public static Recording newRecording(String path) throws IOException {
		return newRecording(Paths.get(path));
	}

	public static Recording newRecording(Path path) throws IOException {
		return newRecording(path.toFile());
	}

	public static Recording newRecording(File path) throws IOException {
		return new RecordingImpl(new FileOutputStream(path));
	}
}
