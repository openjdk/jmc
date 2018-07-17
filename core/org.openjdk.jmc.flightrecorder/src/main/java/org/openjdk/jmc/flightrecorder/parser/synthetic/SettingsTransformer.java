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
package org.openjdk.jmc.flightrecorder.parser.synthetic;

import static org.openjdk.jmc.common.item.Attribute.attr;
import static org.openjdk.jmc.common.unit.UnitLookup.FLAG;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESPAN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.LabeledIdentifier;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.parser.IEventSink;
import org.openjdk.jmc.flightrecorder.parser.IEventSinkFactory;
import org.openjdk.jmc.flightrecorder.parser.ValueField;

/**
 * Event sink that transforms pre JDK 9 event types to their equivalent JDK 9 types. JDK 9 input
 * data will be passed through mostly untouched.
 */
class SettingsTransformer implements IEventSink {

	/**
	 * Fix for JDK-8157024, the code cache stats unallocatedCapacity event is written as KiB but
	 * reported as B. This is fixed in JDK9, but we need to perform this transformation for
	 * recordings from JDK8 and earlier.
	 */
	private static class FixCodeCacheSink implements IEventSink {

		private int unallocatedFieldIndex;
		private IEventSink subSink;

		public FixCodeCacheSink(int unallocatedFieldIndex, IEventSink subSink) {
			this.unallocatedFieldIndex = unallocatedFieldIndex;
			this.subSink = subSink;
		}

		@Override
		public void addEvent(Object[] values) {
			if (values[unallocatedFieldIndex] instanceof IQuantity) {
				values[unallocatedFieldIndex] = ((IQuantity) values[unallocatedFieldIndex]).multiply(1024);
			}
			subSink.addEvent(values);
		}

	}

	// TODO: Break out constants?
	static final String REC_SETTING_NAME_ENABLED = "enabled"; //$NON-NLS-1$
	static final String REC_SETTING_NAME_STACKTRACE = "stacktrace"; //$NON-NLS-1$
	static final String REC_SETTING_NAME_THRESHOLD = "threshold"; //$NON-NLS-1$
	static final String REC_SETTING_NAME_PERIOD = "period"; //$NON-NLS-1$

	static final String REC_SETTING_PERIOD_EVERY_CHUNK = "everyChunk"; //$NON-NLS-1$

	private static final IAttribute<Boolean> REC_SETTINGS_ATTR_ENABLED = attr("enabled", //$NON-NLS-1$
			Messages.getString(Messages.SettingsTransformer_REC_SETTINGS_ATTR_ENABLED), FLAG);
	private static final IAttribute<Boolean> REC_SETTINGS_ATTR_STACKTRACE = attr("stacktrace", //$NON-NLS-1$
			Messages.getString(Messages.SettingsTransformer_REC_SETTINGS_ATTR_STACKTRACE), FLAG);
	static final IAttribute<IQuantity> REC_SETTINGS_ATTR_THRESHOLD = attr("threshold", //$NON-NLS-1$
			Messages.getString(Messages.SettingsTransformer_REC_SETTINGS_ATTR_THRESHOLD), TIMESPAN);
	static final IAttribute<IQuantity> REC_SETTINGS_ATTR_PERIOD = attr("period", //$NON-NLS-1$
			Messages.getString(Messages.SettingsTransformer_REC_SETTINGS_ATTR_PERIOD), TIMESPAN);
	private static final List<ValueField> FIELDS = Arrays.asList(new ValueField(JfrAttributes.END_TIME),
			new ValueField(SyntheticAttributeExtension.REC_SETTING_EVENT_ID_ATTRIBUTE),
			new ValueField(JdkAttributes.REC_SETTING_NAME), new ValueField(JdkAttributes.REC_SETTING_VALUE));

	// Renamed attributes from pre JDK 9: <event id, <pre 9 attribute id, 9 attribute id>>
	private static final Map<String, Map<String, String>> attributeRenameMap;

	// JDK-8157024 constant for the field id
	private static final String UNALLOCATED_CAPACITY_FIELD_ID = "unallocatedCapacity"; //$NON-NLS-1$

	private final IEventSink sink;
	private final Object[] reusableArray = new Object[FIELDS.size()];
	private int endTimeIndex = -1;
	private int typeIndex = -1;
	private int enabledIndex = -1;
	private int stacktraceIndex = -1;
	private int thresholdIndex = -1;
	private int periodIndex = -1;

	static {
		attributeRenameMap = buildRenameMap();
	}

	@SuppressWarnings("nls")
	private static HashMap<String, Map<String, String>> buildRenameMap() {
		// NOTE: Replace the last string argument with an identifier reference if a matching one is added to JfrAttributes.
		HashMap<String, Map<String, String>> map = new HashMap<>();
		addRenameEntry(map, JdkTypeIDsPreJdk11.THREAD_PARK, "klass", "parkedClass");
		addRenameEntry(map, JdkTypeIDsPreJdk11.MONITOR_ENTER, "klass", JdkAttributes.MONITOR_CLASS.getIdentifier());
		addRenameEntry(map, JdkTypeIDsPreJdk11.MONITOR_WAIT, "klass", JdkAttributes.MONITOR_CLASS.getIdentifier());
		addRenameEntry(map, JdkTypeIDsPreJdk11.INT_FLAG_CHANGED, "old_value", "oldValue");
		addRenameEntry(map, JdkTypeIDsPreJdk11.INT_FLAG_CHANGED, "new_value", "newValue");
		addRenameEntry(map, JdkTypeIDsPreJdk11.UINT_FLAG_CHANGED, "old_value", "oldValue");
		addRenameEntry(map, JdkTypeIDsPreJdk11.UINT_FLAG_CHANGED, "new_value", "newValue");
		addRenameEntry(map, JdkTypeIDsPreJdk11.LONG_FLAG_CHANGED, "old_value", "oldValue");
		addRenameEntry(map, JdkTypeIDsPreJdk11.LONG_FLAG_CHANGED, "new_value", "newValue");
		addRenameEntry(map, JdkTypeIDsPreJdk11.ULONG_FLAG_CHANGED, "old_value", "oldValue");
		addRenameEntry(map, JdkTypeIDsPreJdk11.ULONG_FLAG_CHANGED, "new_value", "newValue");
		addRenameEntry(map, JdkTypeIDsPreJdk11.DOUBLE_FLAG_CHANGED, "old_value", "oldValue");
		addRenameEntry(map, JdkTypeIDsPreJdk11.DOUBLE_FLAG_CHANGED, "new_value", "newValue");
		addRenameEntry(map, JdkTypeIDsPreJdk11.BOOLEAN_FLAG_CHANGED, "old_value", "oldValue");
		addRenameEntry(map, JdkTypeIDsPreJdk11.BOOLEAN_FLAG_CHANGED, "new_value", "newValue");
		addRenameEntry(map, JdkTypeIDsPreJdk11.STRING_FLAG_CHANGED, "old_value", "oldValue");
		addRenameEntry(map, JdkTypeIDsPreJdk11.STRING_FLAG_CHANGED, "new_value", "newValue");
		addRenameEntry(map, JdkTypeIDsPreJdk11.GC_DETAILED_EVACUATION_INFO, "allocRegionsUsedBefore",
				"allocationRegionsUsedBefore");
		addRenameEntry(map, JdkTypeIDsPreJdk11.GC_DETAILED_EVACUATION_INFO, "allocRegionsUsedAfter",
				"allocationRegionsUsedAfter");
		addRenameEntry(map, JdkTypeIDsPreJdk11.SWEEP_CODE_CACHE, "sweepIndex", "sweepId");
		addRenameEntry(map, JdkTypeIDsPreJdk11.ALLOC_INSIDE_TLAB, "class",
				JdkAttributes.ALLOCATION_CLASS.getIdentifier());
		addRenameEntry(map, JdkTypeIDsPreJdk11.ALLOC_OUTSIDE_TLAB, "class",
				JdkAttributes.ALLOCATION_CLASS.getIdentifier());
		addRenameEntry(map, JdkTypeIDsPreJdk11.OBJECT_COUNT, "class", JdkAttributes.OBJECT_CLASS.getIdentifier());
		addRenameEntry(map, JdkTypeIDsPreJdk11.COMPILER_PHASE, "compileID",
				JdkAttributes.COMPILER_COMPILATION_ID.getIdentifier());
		addRenameEntry(map, JdkTypeIDsPreJdk11.COMPILATION, "compileID",
				JdkAttributes.COMPILER_COMPILATION_ID.getIdentifier());
		addRenameEntry(map, JdkTypeIDsPreJdk11.COMPILER_FAILURE, "compileID",
				JdkAttributes.COMPILER_COMPILATION_ID.getIdentifier());
		addRenameEntry(map, JdkTypeIDsPreJdk11.COMPILER_FAILURE, "failure",
				JdkAttributes.COMPILER_FAILED_MESSAGE.getIdentifier());
		addRenameEntry(map, JdkTypeIDsPreJdk11.GC_DETAILED_OBJECT_COUNT_AFTER_GC, "class",
				JdkAttributes.OBJECT_CLASS.getIdentifier());
		return map;
	}

	private static void addRenameEntry(
		Map<String, Map<String, String>> renameMap, String eventId, String pre9AttrId, String attrId) {
		Map<String, String> attrMap = renameMap.get(eventId);
		if (attrMap == null) {
			attrMap = new HashMap<>();
			renameMap.put(eventId, attrMap);
		}
		attrMap.put(pre9AttrId, attrId);
	}

	SettingsTransformer(IEventSinkFactory sinkFactory, String label, String[] category, String description,
			List<ValueField> dataStructure) {
		for (int i = 0; i < dataStructure.size(); i++) {
			ValueField vf = dataStructure.get(i);
			if (vf.matches(JfrAttributes.END_TIME)) {
				endTimeIndex = i;
			} else if (vf.matches(SyntheticAttributeExtension.REC_SETTING_EVENT_ID_ATTRIBUTE)) {
				typeIndex = i;
			} else if (vf.matches(REC_SETTINGS_ATTR_ENABLED)) {
				enabledIndex = i;
			} else if (vf.matches(REC_SETTINGS_ATTR_STACKTRACE)) {
				stacktraceIndex = i;
			} else if (vf.matches(REC_SETTINGS_ATTR_THRESHOLD)) {
				thresholdIndex = i;
			} else if (vf.matches(REC_SETTINGS_ATTR_PERIOD)) {
				periodIndex = i;
			}
		}
		if (endTimeIndex >= 0) {
			sink = sinkFactory.create(JdkTypeIDs.RECORDING_SETTING, label, category, description, FIELDS);
		} else {
			sink = sinkFactory.create(JdkTypeIDs.RECORDING_SETTING, label, category, description, dataStructure);
		}
	}

	boolean isValid() {
		return endTimeIndex >= 0 && typeIndex >= 0 && enabledIndex >= 0 && stacktraceIndex >= 0 && thresholdIndex >= 0
				&& periodIndex >= 0;
	}

	boolean isValidV1() {
		return typeIndex >= 0;
	}

	@Override
	public void addEvent(Object[] values) {
		LabeledIdentifier type = (LabeledIdentifier) values[typeIndex];
		if (type != null) {
			if (JdkTypeIDsPreJdk11.needTransform(type.getInterfaceId())) {
				type = new LabeledIdentifier(JdkTypeIDsPreJdk11.translate(type.getInterfaceId()),
						type.getImplementationId(), type.getName(), type.getDeclaredDescription());
			}
			if (endTimeIndex < 0) {
				values[typeIndex] = type;
				sink.addEvent(values);
				return;
			}
		}
		Object startTime = values[endTimeIndex];

		addSettingEvent(startTime, type, REC_SETTING_NAME_ENABLED, values[enabledIndex]);
		addSettingEvent(startTime, type, REC_SETTING_NAME_STACKTRACE, values[stacktraceIndex]);
		addThresholdSettingEvent(startTime, type, (IQuantity) values[thresholdIndex]);
		addPeriodSettingEvent(startTime, type, (IQuantity) values[periodIndex]);
	}

	private boolean addThresholdSettingEvent(Object startTime, LabeledIdentifier type, IQuantity quantity) {
		// Remove thresholds with Long.MIN_VALUE ns duration as these are just padding for
		// event types that cannot have thresholds. (At least JDK 7u75 used Long.MAX_VALUE.)
		if (quantity != null) {
			long numQuantity = quantity.longValue();
			if ((numQuantity != Long.MIN_VALUE) && (numQuantity != Long.MAX_VALUE)) {
				addSettingEvent(startTime, type, REC_SETTING_NAME_THRESHOLD, quantity.persistableString());
			}
		}
		return false;
	}

	private boolean addPeriodSettingEvent(Object startTime, LabeledIdentifier type, IQuantity quantity) {
		// Similar to threshold. Seems to be similar but almost the opposite for period, which at least in JDK 8u40
		// uses Long.MIN_VALUE ms for event types that can have a period, but none have been defined.
		if (quantity != null) {
			long numQuantity = quantity.longValue();
			if (numQuantity == 0L) {
				addSettingEvent(startTime, type, REC_SETTING_NAME_PERIOD, REC_SETTING_PERIOD_EVERY_CHUNK);
			} else if ((numQuantity != Long.MIN_VALUE) && (numQuantity != Long.MAX_VALUE)) {
				addSettingEvent(startTime, type, REC_SETTING_NAME_PERIOD, quantity.persistableString());
			}
		}
		return false;
	}

	private void addSettingEvent(Object startTime, LabeledIdentifier type, String settingName, Object settingValue) {
		reusableArray[0] = startTime;
		reusableArray[1] = type;
		reusableArray[2] = settingName;
		reusableArray[3] = settingValue == null ? null : settingValue.toString();
		sink.addEvent(reusableArray);
	}

	/*
	 * FIXME: Weird to explicitly wrap when the parser does exactly that.
	 *
	 * This class should be refactored into a parser extension although this may require a change to
	 * the API by adding priorities so that type transformation occurs before synthetic attributes
	 * are added.
	 */
	static IEventSinkFactory wrapSinkFactory(final IEventSinkFactory subFactory) {
		return new IEventSinkFactory() {

			@Override
			public IEventSink create(
				String identifier, String label, String[] category, String description,
				List<ValueField> dataStructure) {
				boolean needsTransform = JdkTypeIDsPreJdk11.needTransform(identifier);
				if (JdkTypeIDsPreJdk11.RECORDING_SETTING.equals(identifier) ||
						(needsTransform &&
								JdkTypeIDs.RECORDING_SETTING.equals(JdkTypeIDsPreJdk11.translate(identifier)))) {
					SettingsTransformer st = new SettingsTransformer(subFactory, label, category, description,
							dataStructure);
					if (st.isValid() || (needsTransform && st.isValidV1())) {
						return st;
					} else {
						// FIXME: Avoid System.err.println
						System.err
								.println("Cannot create SettingsTransformer from fields: " + dataStructure.toString()); //$NON-NLS-1$
					}
				} else if (JdkTypeIDsPreJdk11.RECORDINGS.equals(identifier)) {
					/*
					 * NOTE: Renaming 'duration' and 'startTime' attributes for JDK 8 'Recording'
					 * events so that they won't conflict with general attributes with the same
					 * names in JDK 9+ recordings.
					 */
					ValueField[] struct = new ValueField[dataStructure.size()];
					for (int i = 0; i < struct.length; i++) {
						ValueField vf = dataStructure.get(i);
						if (vf.matches(JfrAttributes.START_TIME)) {
							vf = new ValueField(JdkAttributes.RECORDING_START);
						} else if (vf.matches(JfrAttributes.DURATION)) {
							vf = new ValueField(JdkAttributes.RECORDING_DURATION);
						}
						struct[i] = vf;
					}
					return subFactory.create(JdkTypeIDs.RECORDINGS, label, category, description,
							Arrays.asList(struct));
				} else if (JdkTypeIDsPreJdk11.CODE_CACHE_STATISTICS.equals(identifier)) {
					for (int i = 0; i < dataStructure.size(); i++) {
						if (UNALLOCATED_CAPACITY_FIELD_ID.equals(dataStructure.get(i).getIdentifier())) {
							return new FixCodeCacheSink(i, subFactory.create(JdkTypeIDsPreJdk11.translate(identifier),
									label, category, description, dataStructure));
						}
					}
				}
				return subFactory.create(JdkTypeIDsPreJdk11.translate(identifier), label, category, description,
						translate(identifier, dataStructure));
			}

			private List<ValueField> translate(String identifier, List<ValueField> dataStructure) {
				Map<String, String> attrMap = attributeRenameMap.get(identifier);
				if (attrMap == null) {
					return dataStructure;
				}
				List<ValueField> renamedDataStructure = new ArrayList<>();
				for (ValueField vf : dataStructure) {
					String renamedId = attrMap.get(vf.getIdentifier());
					if (renamedId == null) {
						renamedDataStructure.add(vf);
					} else {
						renamedDataStructure
								.add(new ValueField(renamedId, vf.getName(), vf.getDescription(), vf.getContentType()));
					}
				}
				return renamedDataStructure;
			}

			@Override
			public void flush() {
				subFactory.flush();
			}
		};
	}

}
