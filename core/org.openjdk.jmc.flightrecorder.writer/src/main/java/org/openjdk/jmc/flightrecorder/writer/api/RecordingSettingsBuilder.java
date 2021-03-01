package org.openjdk.jmc.flightrecorder.writer.api;

/**
 * A builder type for {@linkplain RecordingSettings}
 */
public interface RecordingSettingsBuilder {
	/**
	 * Set the recording timestamp in epoch nanoseconds (nanoseconds since 1970-01-01).
	 * 
	 * @param timestamp
	 *            the timestamp in epoch nanoseconds (nanoseconds since 1970-01-01)
	 * @return this instance for chaining
	 */
	RecordingSettingsBuilder withTimestamp(long timestamp);

	/**
	 * The recording will automatically initialize
	 * {@linkplain org.openjdk.jmc.flightrecorder.writer.api.Types.JDK} types.
	 * 
	 * @return this instance for chaining
	 */
	RecordingSettingsBuilder withJdkTypeInitialization();

	/**
	 * Build the settings instance.
	 * 
	 * @return the settings instance
	 */
	RecordingSettings build();
}
