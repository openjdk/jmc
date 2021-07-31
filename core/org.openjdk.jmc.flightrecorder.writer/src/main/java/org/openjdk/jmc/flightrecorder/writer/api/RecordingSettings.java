package org.openjdk.jmc.flightrecorder.writer.api;

/**
 * A settings data-class for a {@linkplain Recording} instance
 */
public final class RecordingSettings {
	private final long startTimestamp;
	private final boolean initializeJDKTypes;

	/**
	 * @param startTimestamp
	 *            the recording start timestamp in epoch nanoseconds (nanoseconds since 1970-01-01)
	 * @param initializeJDKTypes
	 *            should the {@linkplain org.openjdk.jmc.flightrecorder.writer.api.Types.JDK} types
	 *            be initialized
	 */
	public RecordingSettings(long startTimestamp, boolean initializeJDKTypes) {
		this.startTimestamp = startTimestamp;
		this.initializeJDKTypes = initializeJDKTypes;
	}

	/**
	 * Recording will use current time as its start timestamp
	 * 
	 * @param initializeJDKTypes
	 *            should the {@linkplain org.openjdk.jmc.flightrecorder.writer.api.Types.JDK} types
	 *            be initialized
	 */
	public RecordingSettings(boolean initializeJDKTypes) {
		this(-1, initializeJDKTypes);
	}

	/**
	 * Recording will initialize {@linkplain org.openjdk.jmc.flightrecorder.writer.api.Types.JDK}
	 * types.
	 * 
	 * @param startTimestamp
	 *            the recording start timestamp in epoch nanoseconds (nanoseconds since 1970-01-01)
	 */
	public RecordingSettings(long startTimestamp) {
		this(startTimestamp, true);
	}

	/**
	 * Recording will use current time as its start timestamp and will initialize
	 * {@linkplain org.openjdk.jmc.flightrecorder.writer.api.Types.JDK} types.
	 */
	public RecordingSettings() {
		this(-1, true);
	}

	/**
	 * @return recording timestamp in epoch nanoseconds (nanoseconds since 1970-01-01)
	 */
	public long getStartTimestamp() {
		return startTimestamp;
	}

	/**
	 * @return {@literal true} if {@linkplain org.openjdk.jmc.flightrecorder.writer.api.Types.JDK}
	 *         types are to be initialized
	 */
	public boolean shouldInitializeJDKTypes() {
		return initializeJDKTypes;
	}
}
