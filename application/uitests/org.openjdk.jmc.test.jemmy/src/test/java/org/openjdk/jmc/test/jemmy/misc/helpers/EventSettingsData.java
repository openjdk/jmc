/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmc.test.jemmy.misc.helpers;

import static org.openjdk.jmc.common.unit.UnitLookup.TIMESPAN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.ITypedQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.TimestampUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints;
import org.openjdk.jmc.flightrecorder.configuration.internal.Messages;

/**
 * a utility class for testing flight recorder event settings
 */
public class EventSettingsData {
	private final HashMap<String, List<EventSettings>> settings = new HashMap<>();
	public static final String SETTING_THRESHOLD = "threshold";
	public static final String SETTING_PERIOD = "period";
	public static final String SETTING_STACKTRACE = "stacktrace";
	public static final String SETTING_ENABLED = "enabled";
	public static final List<String> PERIOD_END_CHUNK_VALUES = Arrays
			.asList(new String[] {Messages.getString(Messages.CommonConstraints_END_OF_EVERY_CHUNK), "endChunk"});
	public static final List<String> PERIOD_EVERY_CHUNK_VALUES = Arrays
			.asList(new String[] {Messages.getString(Messages.CommonConstraints_ONCE_EVERY_CHUNK), "everyChunk"});
	public static final List<String> PERIOD_BEGIN_CHUNK_VALUES = Arrays.asList(
			new String[] {Messages.getString(Messages.CommonConstraints_BEGINNING_OF_EVERY_CHUNK), "beginChunk"});
	public static final List<String> PERIOD_NULL_VALUES = Arrays.asList(new String[] {null, ""});
	private static List<String> PERIOD_ALL_SPECIAL_VALUES;

	static {
		PERIOD_ALL_SPECIAL_VALUES = new ArrayList<>(PERIOD_END_CHUNK_VALUES);
		PERIOD_ALL_SPECIAL_VALUES.addAll(PERIOD_EVERY_CHUNK_VALUES);
		PERIOD_ALL_SPECIAL_VALUES.addAll(PERIOD_BEGIN_CHUNK_VALUES);
		PERIOD_ALL_SPECIAL_VALUES.addAll(PERIOD_NULL_VALUES);
	};

	/**
	 * this class represents the actual settings for a single settings event
	 */
	public class EventSettings {

		private final String settingsForName;
		private final String endTime;
		private final String[] eventPath;
		private Map<String, String> settings;

		/**
		 * @param settingsForName
		 *            the name of the event this EventSettings object relates to
		 */
		public EventSettings(String settingsForName) {
			this(settingsForName, null, null);
		}

		/**
		 * @param settingsForName
		 *            the name of the event this EventSettings object relates to
		 * @param endTime
		 *            the End Time of this specific EventSettings object
		 */
		public EventSettings(String settingsForName, String endTime) {
			this(settingsForName, endTime, null);
		}

		/**
		 * @param settingsForName
		 *            the name of the event this EventSettings object relates to
		 * @param eventPath
		 *            the path to this event setting
		 */
		public EventSettings(String settingsForName, String[] eventPath) {
			this(settingsForName, null, eventPath);
		}

		/**
		 * @param settingsForName
		 *            the name of the event this EventSettings object relates to
		 * @param endTime
		 *            the End Time of this specific EventSettings object
		 * @param eventPath
		 *            the path to this event setting
		 */
		public EventSettings(String settingsForName, String endTime, String[] eventPath) {
			this.settingsForName = settingsForName;
			this.endTime = endTime;
			this.eventPath = eventPath;
			settings = new HashMap<>();
		}

		/**
		 * Adds a setting name/value pair to this EventSettings object
		 *
		 * @param name
		 *            the name of the setting
		 * @param value
		 *            the value of the setting
		 */
		public void add(String name, String value) {
			settings.put(name, value);
		}

		/**
		 * @return The End Time string of this EventSettings object. {@code null} if no End Time has
		 *         been specified
		 */
		public String getEndTimeString() {
			return endTime;
		}

		/**
		 * @return the name of the event this EventSettings object relates to
		 */
		public String getSettingsForName() {
			return settingsForName;
		}

		/**
		 * @return The path to this event setting. {@code null} if no such path has been specified
		 */
		public String[] getEventPath() {
			return eventPath;
		}

		/**
		 * @return a set of setting names
		 */
		public Set<String> getSettingNames() {
			return settings.keySet();
		}

		/**
		 * @param name
		 *            the name of the setting to get
		 * @return The value of the setting matching the name provided. {@code null} if no match
		 */
		public String getSetting(String name) {
			return settings.get(name);
		}

		// Not including endTime and eventPath in the equality check since these are to be considered meta data
		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj == this) {
				return true;
			}
			if (obj.getClass() != getClass()) {
				return false;
			}
			EventSettings other = (EventSettings) obj;

			// Checking that setting regards the same event
			if (!getSettingsForName().equals(other.getSettingsForName())) {
				return false;
			}

			Set<String> settingNames = getSettingNames();
			// comparing the equality of the setting map lengths
			if (settingNames.size() != other.getSettingNames().size()) {
				return false;
			}

			// Comparing each event setting
			for (String settingName : settingNames) {
				String otherValue = other.getSetting(settingName);
				if (otherValue != null) {
					if (!otherValue.equals(getSetting(settingName))) {
						return false;
					}
				} else {
					return false;
				}
			}

			return true;
		}

		@Override
		public String toString() {
			StringBuilder print = new StringBuilder();
			TreeSet<String> sortedSettingNames = new TreeSet<>(getSettingNames());
			for (String settingName : sortedSettingNames) {
				print.append(settingName + "=" + getSetting(settingName) + ", ");
			}
			print.append("End Time=" + getEndTimeString() + ", ");
			print.append(
					"Event Path=" + ((getEventPath() != null) ? Arrays.asList(getEventPath()).toString() : null) + ".");
			return print.toString();
		}

		/**
		 * Tests if this EventSettings object can be a result of the given. Currently, this only
		 * handles the known settings like "enabled", "stacktrace", "period" and "threshold"
		 *
		 * @param given
		 *            the EventSettings object to compare with
		 * @param isJfrNext
		 * @return {@code true} if this can be a result of the given
		 */
		public boolean canBeResultOf(EventSettings given, boolean isJfrNext) {
			// only compare settings if the given event is enabled
			boolean eventSettingsMatch = true;
			String eventName = given.getSettingsForName();

			if ("true".equals(given.getSetting(SETTING_ENABLED))) {
				for (String settingName : getSettingNames()) {
					String thisValue = getSetting(settingName);
					String givenValue = given.getSetting(settingName);

					// Enabled can never be null, must be equal
					if (settingName.equals(SETTING_ENABLED) && !thisValue.equals(givenValue)) {
						printComparisonErrorMessage(eventName, SETTING_ENABLED, givenValue, thisValue);
						eventSettingsMatch = false;
					}

					// If the given value is null, the actual value must be false.
					// Otherwise, the given and actual values must match
					if (settingName.equals(SETTING_STACKTRACE) && ((givenValue == null && thisValue.equals("true"))
							|| (givenValue != null && !thisValue.equals(givenValue)))) {
						printComparisonErrorMessage(eventName, SETTING_STACKTRACE, givenValue, thisValue);
						eventSettingsMatch = false;
					}

					if (settingName.equals(SETTING_PERIOD)) {
						if (isJfrNext && PERIOD_ALL_SPECIAL_VALUES.contains(givenValue)) {
							if (!isEqualSpecialValue(thisValue, givenValue)) {
								printComparisonErrorMessage(eventName, SETTING_PERIOD, givenValue, thisValue);
								eventSettingsMatch = false;
							}
						} else if (!isJfrNext && (given.getSetting(SETTING_PERIOD) == null
								|| given.getSetting(SETTING_PERIOD).equals(""))) {
							if (!thisValue.equals("0 s") && !thisValue.equals("everyChunk")
									&& !thisValue.equals("beginChunk") && !thisValue.equals("endChunk")) {
								printComparisonErrorMessage(eventName, SETTING_PERIOD, givenValue, thisValue);
								eventSettingsMatch = false;
							}
						} else {
							eventSettingsMatch = comparePeriod(isJfrNext, eventName, thisValue, givenValue) == 0;
						}
					}

					if (settingName.equals(SETTING_THRESHOLD)) {
						if (givenValue == null || givenValue.equals("") || givenValue.equals("N/A")) {
							if (!thisValue.equals("") && !thisValue.equals("0 ns")) {
								printComparisonErrorMessage(eventName, SETTING_THRESHOLD, givenValue, thisValue);
								eventSettingsMatch = false;
							}
						} else {
							try {
								if (TIMESPAN.parseInteractive(thisValue)
										.compareTo(TIMESPAN.parseInteractive(givenValue)) != 0) {
									System.out.println(eventNameErrorString(eventName) + "Threshold comparison failed: "
											+ TIMESPAN.parseInteractive(thisValue)
													.compareTo(TIMESPAN.parseInteractive(givenValue)));
									eventSettingsMatch = false;
								}
							} catch (QuantityConversionException e) {
								System.out.println(eventNameErrorString(eventName)
										+ "Could not parse one or both of the threshold values. This: \"" + thisValue
										+ "\", given: \"" + givenValue + "\"");
								e.printStackTrace();
								eventSettingsMatch = false;
							}
						}
					}
				}
			} else {
				if (!given.getSetting(SETTING_ENABLED).equals(getSetting(SETTING_ENABLED))) {
					printComparisonErrorMessage(eventName, SETTING_ENABLED, given.getSetting(SETTING_ENABLED),
							getSetting(SETTING_ENABLED));
					eventSettingsMatch = false;
				}
			}
			return eventSettingsMatch;
		}

	}

	/**
	 * Compare two strings interpreted as period values
	 *
	 * @param isJfrNext
	 *            true if the strings should be parsed as JFR next
	 * @param eventName
	 *            the name of the event setting
	 * @param thisValue
	 *            the String period value of this event setting
	 * @param givenValue
	 *            the other string period value to compare this to
	 * @return -1 if this value is lower than the other. 0 if equal. 1 if larger. 99 if comparison
	 *         failed
	 */
	public static int comparePeriod(boolean isJfrNext, String eventName, String thisValue, String givenValue) {
		int result = 99;
		try {
			IConstraint<IQuantity> constraint = isJfrNext ? CommonConstraints.PERIOD_V2 : CommonConstraints.PERIOD_V1;
			result = constraint.parseInteractive(thisValue).compareTo(constraint.parseInteractive(givenValue));
		} catch (QuantityConversionException e) {
			System.out.println(
					eventNameErrorString(eventName) + "Could not parse one or both of the period values. This: \""
							+ thisValue + "\", given: \"" + givenValue + "\"");
			e.printStackTrace();
		}
		return result;
	}

	private boolean isEqualSpecialValue(String first, String second) {
		return ((isEndChunkValue(first) && isEndChunkValue(second))
				|| (isEveryChunkValue(first) && isEveryChunkValue(second))
				|| (isBeginChunkValue(first) && isBeginChunkValue(second))
				|| (isNullValue(first) && isNullValue(second)));
	}

	private boolean isEndChunkValue(String value) {
		return PERIOD_END_CHUNK_VALUES.contains(value);
	}

	private boolean isBeginChunkValue(String value) {
		return PERIOD_BEGIN_CHUNK_VALUES.contains(value);
	}

	private boolean isEveryChunkValue(String value) {
		return PERIOD_EVERY_CHUNK_VALUES.contains(value);
	}

	private boolean isNullValue(String value) {
		return PERIOD_NULL_VALUES.contains(value);
	}

	private void printComparisonErrorMessage(String name, String setting, String given, String actual) {
		System.out.println(eventNameErrorString(name) + "\"" + setting + "\" setting differs: Given: " + given
				+ ", Actual: " + actual);
	}

	private static String eventNameErrorString(String name) {
		return "Event \"" + name + "\": ";
	}

	/**
	 * Adds a name/value pair for the setting provided
	 *
	 * @param settingsFor
	 *            the name of the event this EventSettings object relates to
	 * @param name
	 *            the name of the setting
	 * @param value
	 *            the value of the setting
	 */
	public void add(String settingsFor, String name, String value) {
		add(settingsFor, null, null, name, value);
	}

	/**
	 * Adds a name/value pair for the setting that matches the settingFor and eventPath provided
	 *
	 * @param settingsFor
	 *            the name of the event this EventSettings object relates to
	 * @param eventPath
	 *            the strings that build up the path to the setting
	 * @param name
	 *            the name of the setting
	 * @param value
	 *            the value of the setting
	 */
	public void add(String settingsFor, String[] eventPath, String name, String value) {
		add(settingsFor, null, eventPath, name, value);
	}

	/**
	 * Adds a name/value pair for the setting that matches the settingFor and endTime provided
	 *
	 * @param settingsFor
	 *            the name of the event this EventSettings object relates to
	 * @param endTime
	 *            the End Time of this setting
	 * @param name
	 *            the name of the setting
	 * @param value
	 *            the value of the setting
	 */
	public void add(String settingsFor, String endTime, String name, String value) {
		add(settingsFor, endTime, null, name, value);
	}

	/**
	 * Adds a name/value pair for the setting that matches the settingFor, endTime and eventPath
	 * provided
	 *
	 * @param settingsFor
	 *            the name of the event this EventSettings object relates to
	 * @param endTime
	 *            the End Time of this setting
	 * @param eventPath
	 *            the strings that build up the path to the setting
	 * @param name
	 *            the name of the setting
	 * @param value
	 *            the value of the setting
	 */
	public void add(String settingsFor, String endTime, String[] eventPath, String name, String value) {
		List<EventSettings> settingsForThisName = settings.get(settingsFor);
		EventSettings eventSettings = null;
		if (settingsForThisName == null) {
			settingsForThisName = new ArrayList<>();
		} else {
			for (EventSettings es : settingsForThisName) {
				if ((endTime != null && es.getEndTimeString().equals(endTime))
						|| (endTime == null && es.getEndTimeString() == null)) {
					eventSettings = es;
					break;
				}
			}
		}
		if (eventSettings == null) {
			eventSettings = new EventSettings(settingsFor, endTime, eventPath);
			settingsForThisName.add(eventSettings);
		}
		eventSettings.add(name, value);
		settings.put(settingsFor, settingsForThisName);
	}

	/**
	 * Returns a list of EventSettings that match the name provided
	 *
	 * @param settingsFor
	 *            the name of the event this EventSettings object relates to
	 * @return a list of {@link EventSettings}
	 */
	public List<EventSettings> get(String settingsFor) {
		return settings.get(settingsFor);
	}

	/**
	 * Returns the latest EventSettings that matches the name provided
	 *
	 * @param settingsFor
	 *            the name of the event this EventSettings object relates to
	 * @return the latest matching EventSettings
	 */
	public EventSettings getLatest(String settingsFor) {
		EventSettings latestEventSettings = null;
		if (get(settingsFor) != null) {
			for (EventSettings currentEventSettings : get(settingsFor)) {
				if (latestEventSettings != null && latestEventSettings.getEndTimeString() != null
						&& currentEventSettings.getEndTimeString() != null) {
					try {
						ITypedQuantity<TimestampUnit> resultDate = UnitLookup.TIMESTAMP
								.parseInteractive(latestEventSettings.getEndTimeString());
						ITypedQuantity<TimestampUnit> thisDate = UnitLookup.TIMESTAMP
								.parseInteractive(currentEventSettings.getEndTimeString());
						if (resultDate.compareTo(thisDate) < 0) {
							latestEventSettings = currentEventSettings;
						}
					} catch (QuantityConversionException e) {
						System.out.println("Problem parsing the date of one or both of the strings: \""
								+ latestEventSettings.getEndTimeString() + "\" and \""
								+ currentEventSettings.getEndTimeString() + "\"");
						e.printStackTrace();
					}
				} else {
					latestEventSettings = currentEventSettings;
				}
			}
		}
		return latestEventSettings;
	}

	/**
	 * @return a sorted list of all setting event names stored in this EventSettingsData
	 */
	public List<String> getAllEventNames() {
		List<String> allEventNames = new ArrayList<>(settings.keySet());
		java.util.Collections.sort(allEventNames);
		return allEventNames;
	}

	/**
	 * Returns the status of the supplied event name
	 *
	 * @param eventName
	 *            the name of the event
	 * @return {@code true} if enabled
	 */
	public boolean enabled(String eventName) {
		return "true".equals(getLatest(eventName).getSetting(SETTING_ENABLED));
	}

	@Override
	public String toString() {
		StringBuilder print = new StringBuilder();

		int settingsSize = 0;
		for (String eventName : getAllEventNames()) {
			StringBuilder values = new StringBuilder();
			for (EventSettings eventSettings : settings.get(eventName)) {
				values.append("\t" + eventSettings.toString() + "\n");
				settingsSize++;
			}
			print.append(eventName + ":\n" + values.toString() + "\n");
		}
		print.append("Total: " + settingsSize + "\n");
		return print.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		EventSettingsData other = (EventSettingsData) obj;
		return settings.equals(other.settings);
	}

	/**
	 * Determines if the current settings is an acceptable result of the given settings I.e. if you
	 * leave an empty string in a field, it is supposed to get a default value.
	 * 
	 * @param givenSettings
	 *            the settings to compare with
	 * @param verbose
	 *            {@code true} if verbose printout reporting is desired
	 * @param isJfrNext
	 *            {@code true} if the JFR event format is version 2
	 * @return {@code true} if it matches
	 */
	public boolean canBeResultOf(EventSettingsData givenSettings, boolean verbose, boolean isJfrNext) {
		boolean result = true;
		for (String eventName : getAllEventNames()) {
			EventSettings actual = getLatest(eventName);
			EventSettings given = givenSettings.getLatest(eventName);
			if (null == given) {
				// Found an event that we didn't have in the Wizard. This isn't necessarily wrong or bad but log it anyway
				System.out.println("INFO: The event '" + eventName
						+ "' was not found in the given settings (recording wizard), but exists in the recording.");
			} else {
				boolean thisEventResult = actual.canBeResultOf(given, isJfrNext);
				result = result && thisEventResult;
				if (!thisEventResult) {
					System.out.println("  Given settings:  " + given.toString());
					System.out.println("  Actual settings: " + actual.toString());
					result = false;
				} else {
					if (verbose) {
						System.out.println("OK: '" + eventName + "'");
					}
				}
			}
		}
		return result;
	}
}
