package org.openjdk.jmc.flightrecorder.writer;

import org.openjdk.jmc.flightrecorder.writer.api.RecordingSettings;
import org.openjdk.jmc.flightrecorder.writer.api.RecordingSettingsBuilder;

public final class RecordingSettingsBuilderImpl implements RecordingSettingsBuilder {
	private long timestamp = -1;
	private boolean initializeJdkTypes = false;

	@Override
	public RecordingSettingsBuilder withTimestamp(long timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	@Override
	public RecordingSettingsBuilder withJdkTypeInitialization() {
		initializeJdkTypes = true;
		return this;
	}

	@Override
	public RecordingSettings build() {
		return new RecordingSettings(timestamp > 0 ? timestamp : System.currentTimeMillis() * 1_000_000L,
				initializeJdkTypes);
	}
}
