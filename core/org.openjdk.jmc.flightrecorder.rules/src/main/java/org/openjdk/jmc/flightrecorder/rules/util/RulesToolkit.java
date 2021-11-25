/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.rules.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.collection.EntryHashMap;
import org.openjdk.jmc.common.collection.IteratorToolkit;
import org.openjdk.jmc.common.collection.MapToolkit;
import org.openjdk.jmc.common.collection.MapToolkit.IntEntry;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAccessorFactory;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.common.unit.BinaryPrefix;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.LabeledIdentifier;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.common.util.PredicateToolkit;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.DependsOn;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.ResultProvider;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.tree.Range;
import org.openjdk.jmc.flightrecorder.rules.tree.TimeRangeFilter;
import org.openjdk.jmc.flightrecorder.rules.tree.TimeRangeThreadFilter;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFormatToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Branch;

/**
 * A collection of useful methods when evaluating rules.
 */
// FIXME: Should probably be broken apart into methods related to rules (kept here) and methods related to JDK events (which should be moved to wherever JfrAttributes etc. are)
public class RulesToolkit {

	// FIXME: Quick and dirty inlining of constants constants defined in SettingsTransformer. These should be handled in some other way.
	private static final String REC_SETTING_NAME_ENABLED = "enabled"; //$NON-NLS-1$
	private static final String REC_SETTING_NAME_THRESHOLD = "threshold"; //$NON-NLS-1$
	public static final String REC_SETTING_NAME_PERIOD = "period"; //$NON-NLS-1$
	public static final String REC_SETTING_PERIOD_EVERY_CHUNK = "everyChunk"; //$NON-NLS-1$

	/*
	 * Returns the type name, as available in the recording settings, or simply the type id, if not
	 * available.
	 */
	private static final IAccessorFactory<String> TYPE_NAME_ACCESSOR_FACTORY = new IAccessorFactory<String>() {
		@Override
		public <T> IMemberAccessor<String, T> getAccessor(final IType<T> type) {
			final IMemberAccessor<LabeledIdentifier, T> ta = JdkAttributes.REC_SETTING_FOR.getAccessor(type);
			return new IMemberAccessor<String, T>() {
				@Override
				public String getMember(T inObject) {
					LabeledIdentifier labeledIdentifier = ta.getMember(inObject);
					return labeledIdentifier == null ? type.getIdentifier() : labeledIdentifier.getName();
				}
			};
		}
	};
	private final static LinearUnit MEBIBYTES = UnitLookup.MEMORY.getUnit(BinaryPrefix.MEBI);

	/**
	 * Matches strings containing an identifiable version number as presented in a JVM info event.
	 * The minimal matching form is "JRE (" followed by 1 to 4 numbers on the format a.b.c_d or
	 * a.b.c.d. Examples are 1.7.0, 1.8.0_70, 9, 9.1, and 9.1.2.3. Match group 1 will contain the
	 * matched version string.
	 */
	private static final Pattern VERSION_PATTERN = Pattern
			.compile(".*?JRE \\((\\d+(?:\\.\\d+(?:\\.\\d+(?:[\\._]\\d+)?)?)?(?:-ea)?).*"); //$NON-NLS-1$

	/**
	 * Knowledge about the state of affairs of an event type in an IItemCollection.
	 */
	public enum EventAvailability {
				/**
				 * The type has events available in the collection.
				 */
				AVAILABLE(4),
				/**
				 * The type was actively enabled in the collection.
				 */
				ENABLED(3),
				/**
				 * The type was actively disabled in the collection.
				 */
				DISABLED(2),
				/**
				 * The type is known in the collection, but no events were found.
				 */
				NONE(1),
				/**
				 * The type is unknown in the collection.
				 */
				UNKNOWN(0);

		/*
		 * Used to determine the ordering of availabilities.
		 */
		private final int availabilityScore;

		EventAvailability(int availabilityScore) {
			this.availabilityScore = availabilityScore;
		}

		/**
		 * Returns true if this EventAvailability is less available than the provided one.
		 *
		 * @param availability
		 *            the {@link EventAvailability} to compare to.
		 * @return true if this EventAvailability is less available than the provided one, false
		 *         otherwise.
		 */
		public boolean isLessAvailableThan(EventAvailability availability) {
			return availabilityScore < availability.availabilityScore;
		}
	}

	public static class RequiredEventsBuilder {
		private Map<String, EventAvailability> requiredEvents;

		private RequiredEventsBuilder() {
			requiredEvents = new HashMap<>();
		}

		public static RequiredEventsBuilder create() {
			return new RequiredEventsBuilder();
		}

		public RequiredEventsBuilder addEventType(String typeId, EventAvailability availability) {
			if (requiredEvents.containsKey(typeId)) {
				throw new IllegalArgumentException("Already specified " + availability + " for " + typeId); //$NON-NLS-1$ //$NON-NLS-2$
			}
			requiredEvents.put(typeId, availability);
			return this;
		}

		public Map<String, EventAvailability> build() {
			return Collections.unmodifiableMap(requiredEvents);
		}
	}

	/**
	 * @return a least squares approximation of the increase in memory over the given time period,
	 *         in mebibytes/second
	 */
	public static double leastSquareMemory(
		Iterator<? extends IItem> items, IMemberAccessor<IQuantity, IItem> timeField,
		IMemberAccessor<IQuantity, IItem> memField) {
		double sumX = 0;
		double sumY = 0;
		double sumX2 = 0;
		double sumXY = 0;
		double num = 0;
		double startTime = 0;

		while (items.hasNext()) {
			IItem item = items.next();
			long time = timeField.getMember(item).clampedLongValueIn(UnitLookup.EPOCH_S);
			long mem = memField.getMember(item).clampedLongValueIn(MEBIBYTES);
			if (num == 0) {
				startTime = time;
			}
			time -= startTime;
			sumX += time;
			sumY += mem;
			sumX2 += time * time;
			sumXY += time * mem;
			num++;
		}
		double value = (num * sumXY - sumX * sumY) / (num * sumX2 - sumX * sumX);
		return Double.isNaN(value) ? 0 : value;
	}

	/**
	 * Finds items of a specific type where the given attribute has a value matching that of the
	 * provided match string.
	 *
	 * @param typeId
	 *            the event type to find matches in
	 * @param items
	 *            the set of items to search
	 * @param attribute
	 *            the attribute to match
	 * @param match
	 *            the pattern to find
	 * @param ignoreCase
	 *            whether or not to ignore case when matching
	 * @return a comma-delimited string with all matching attributes
	 */
	public static String findMatches(
		String typeId, IItemCollection items, IAttribute<String> attribute, String match, boolean ignoreCase) {
		String regexp = ".*(" + (ignoreCase ? "?i:" : "") + match + ").*"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		return items.getAggregate(
				(IAggregator<String, ?>) Aggregators.filter(Aggregators.distinctAsString(typeId, attribute),
						ItemFilters.and(ItemFilters.type(typeId), ItemFilters.matches(attribute, regexp))));
	}

	/**
	 * Gets the value of a certain attribute for a given item
	 *
	 * @param item
	 *            the item to get the attribute from
	 * @param attribute
	 *            the attribute to get
	 * @return the value of the specified attribute for the given item
	 */
	public static <T> T getValue(IItem item, IAccessorFactory<T> attribute) {
		IType<IItem> itemType = ItemToolkit.getItemType(item);
		IMemberAccessor<? extends T, IItem> accessor = attribute.getAccessor(itemType);
		if (accessor == null) {
			throw new IllegalArgumentException("The accessor factory could not build accessor for type " //$NON-NLS-1$
					+ itemType.getIdentifier()
					+ ". This is likely due to an old unsupported recording where an attribute has changed name."); //$NON-NLS-1$
		}
		return accessor.getMember(item);
	}

	/**
	 * Gets a filter for a specific setting for the provided types.
	 *
	 * @param settingsName
	 *            the specific setting to find
	 * @param typeIds
	 *            the ids of the types to find the setting for
	 * @return a filter for a specified setting for the provided type ids
	 */
	public static IItemFilter getSettingsFilter(String settingsName, String ... typeIds) {
		final Set<String> types = new HashSet<>(Arrays.asList(typeIds));
		IItemFilter typeFilter = new IItemFilter() {
			@Override
			public Predicate<IItem> getPredicate(IType<IItem> type) {
				final IMemberAccessor<LabeledIdentifier, IItem> ma = JdkAttributes.REC_SETTING_FOR.getAccessor(type);
				if (ma != null) {
					return new Predicate<IItem>() {

						@Override
						public boolean test(IItem o) {
							LabeledIdentifier eventType = ma.getMember(o);
							return eventType != null && types.contains(eventType.getInterfaceId());
						}

					};
				}
				return PredicateToolkit.falsePredicate();
			}
		};
		return ItemFilters.and(JdkFilters.RECORDING_SETTING, typeFilter,
				ItemFilters.equals(JdkAttributes.REC_SETTING_NAME, settingsName));
	}

	/**
	 * Gets the maximum period setting for the specified event types in the given item collection.
	 *
	 * @param items
	 *            the items to find the period setting in
	 * @param typeIds
	 *            the event type ids to find settings for
	 * @return the maximum period setting for the specified event types
	 */
	public static IQuantity getSettingMaxPeriod(IItemCollection items, String ... typeIds) {
		Set<String> values = getPeriodSettings(items, typeIds);
		return values == null || values.isEmpty() ? null : getSettingMaxPeriod(values);
	}

	/**
	 * If possible, gets the longest period setting that is longer than the specified minimum period
	 * for the given event types.
	 *
	 * @param items
	 *            the item collection to search through
	 * @param minPeriod
	 *            the minimum period setting
	 * @param typeIds
	 *            the event type ids to find the period setting for
	 * @return a string representation of the longest period longer than the {@code minPeriod}, or
	 *         {@code null} if all periods are shorter than {@code minPeriod}
	 */
	public static String getPeriodIfGreaterThan(IItemCollection items, IQuantity minPeriod, String ... typeIds) {
		Set<String> values = getPeriodSettings(items, typeIds);
		if (values != null && !values.isEmpty()) {
			IQuantity max = getSettingMaxPeriod(values);
			if (max == null) {
				return Messages.getString(Messages.RulesToolkit_EVERY_CHUNK);
			} else if (max.compareTo(minPeriod) > 0) {
				return max.displayUsing(IDisplayable.AUTO);
			}
		}
		return null;
	}

	/**
	 * Converts a value persisted as a string by the JVM into an {@link IQuantity}.
	 *
	 * @param persistedValue
	 *            the persisted value to convert
	 * @return the resulting {@link IQuantity}
	 */
	public static IQuantity parsePersistedJvmTimespan(String persistedValue) throws QuantityConversionException {
		// FIXME: Copied from CommonConstraints.TimePersisterBrokenSI. Use that when it is exposed.
		if (persistedValue.endsWith("m")) { //$NON-NLS-1$
			persistedValue += "in"; //$NON-NLS-1$
		}
		return UnitLookup.TIMESPAN.parsePersisted(persistedValue);
	}

	/**
	 * Returns a string describing the subset of event types given which have no duration threshold
	 * set.
	 *
	 * @param items
	 *            the item collection to search
	 * @param typeIds
	 *            the event type ids to find thresholds for
	 * @return a comma-delimited string describing the event types with no threshold
	 */
	public static String getTypesWithZeroThreshold(IItemCollection items, String ... typeIds) {
		IItemFilter f = new IItemFilter() {

			@Override
			public Predicate<IItem> getPredicate(IType<IItem> type) {
				final IMemberAccessor<String, IItem> accessor = JdkAttributes.REC_SETTING_VALUE.getAccessor(type);
				return new Predicate<IItem>() {

					@Override
					public boolean test(IItem o) {
						try {
							String thresholdValue = accessor.getMember(o);
							return parsePersistedJvmTimespan(thresholdValue).longValue() == 0L;
						} catch (QuantityConversionException e) {
							throw new RuntimeException(e);
						}
					}
				};
			}
		};
		IItemFilter filter = ItemFilters.and(getSettingsFilter(REC_SETTING_NAME_THRESHOLD, typeIds), f);
		return getEventTypeNames(items.apply(filter));
	}

	/**
	 * This method checks if the provided event types were explicitly enabled by checking the
	 * recording setting events.
	 *
	 * @param items
	 *            the collection to check.
	 * @param typeIds
	 *            the identifiers for the event types to check.
	 * @return true if all of the required event types were known to be explicitly enabled.
	 */
	public static boolean isEventsEnabled(IItemCollection items, String ... typeIds) {
		IQuantity aggregate = items.apply(createEnablementFilter(true, typeIds)).getAggregate(Aggregators.count());
		return aggregate != null && aggregate.longValue() == typeIds.length;
	}

	/**
	 * This method returns false if any {@link EventAvailability} is disabled or unavailable.
	 * Otherwise true.
	 *
	 * @param eventAvailabilities
	 *            the {@link EventAvailability} to check
	 * @return false if any {@link EventAvailability} is disabled or unavailable. Otherwise true.
	 */
	public static boolean isEventsEnabled(EventAvailability ... eventAvailabilities) {
		for (EventAvailability availability : eventAvailabilities) {
			if (availability == EventAvailability.DISABLED || availability == EventAvailability.UNKNOWN) {
				return false;
			}
		}
		return true;
	}

	/**
	 * This method checks if the provided event types were explicitly disabled by checking the
	 * recording setting events.
	 *
	 * @param items
	 *            the collection to check.
	 * @param typeIds
	 *            the identifiers for the event types to check.
	 * @return true if all of the required event types were known to be explicitly disabled.
	 */
	private static boolean isEventsDisabled(IItemCollection items, String ... typeIds) {
		IQuantity aggregate = items.apply(createEnablementFilter(false, typeIds)).getAggregate(Aggregators.count());
		return aggregate != null && aggregate.longValue() == typeIds.length;
	}

	/**
	 * Checks the event availability for the event types.
	 * <p>
	 * Care should be taken when used with multiple typeIds. Use it when all of the provided typeIds
	 * are expected to have the same availability; if mixed, the lowest common availability for all
	 * types will be returned.
	 *
	 * @param items
	 *            the collection to check
	 * @param typeIds
	 *            the type identifiers to check
	 * @return the availability for the event types
	 */
	public static EventAvailability getEventAvailability(IItemCollection items, final String ... typeIds) {
		// Only AVAILABLE if exactly all types have events
		if (hasEvents(items, typeIds)) {
			return EventAvailability.AVAILABLE;
		}
		// If enabled at any point, it was indeed enabled, and events could have been recorded
		if (isEventsEnabled(items, typeIds)) {
			return EventAvailability.ENABLED;
		}
		if (isEventsDisabled(items, typeIds)) {
			return EventAvailability.DISABLED;
		}
		if (isEventsKnown(items, typeIds)) {
			return EventAvailability.NONE;
		}
		return EventAvailability.UNKNOWN;
	}

	public static boolean matchesEventAvailabilityMap(
		IItemCollection items, Map<String, EventAvailability> availabilityMap) {
		for (Entry<String, EventAvailability> entry : availabilityMap.entrySet()) {
			EventAvailability eventAvailability = getEventAvailability(items, entry.getKey());
			if (eventAvailability.isLessAvailableThan(entry.getValue())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the least available EventAvailability from the ones provided. See
	 * {@link EventAvailability}.
	 *
	 * @return the least available EventAvailability from the ones provided.
	 */
	public static EventAvailability getLeastAvailable(EventAvailability ... availabilites) {
		EventAvailability lowest = EventAvailability.AVAILABLE;

		for (EventAvailability availability : availabilites) {
			if (availability.isLessAvailableThan(lowest)) {
				lowest = availability;
			}
		}
		return lowest;
	}

	/**
	 * Checks if the event types are known in the collection. Note that it does not necessarily mean
	 * that there are events of the event type.
	 *
	 * @param items
	 *            the collection to check
	 * @param typeIds
	 *            the event types to check
	 * @return true if all the event types exists in the collection.
	 */
	private static boolean isEventsKnown(IItemCollection items, String ... typeIds) {
		Set<String> availableTypes = getAvailableTypeIds(items);
		if (availableTypes.containsAll(Arrays.asList(typeIds))) {
			return true;
		}
		return false;
	}

	/**
	 * Returns true if precisely all of the event types have events.
	 *
	 * @param items
	 *            the events.
	 * @param typeIds
	 *            the identifiers of the event types to check.
	 * @return true if all of the types have events.
	 */
	private static boolean hasEvents(IItemCollection items, String ... typeIds) {
		for (String typeId : typeIds) {
			if (!internalHasEvents(items, typeId)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Creates a {@link IResult} object for the given {@link IRule} object representing a result
	 * where there are too few events to properly evaluate a rule.
	 *
	 * @param rule
	 *            the rule to create a {@link IResult} object for
	 * @return an object describing that the rule could not be evaluated due to there not being
	 *         enough data
	 */
	public static IResult getTooFewEventsResult(IRule rule, IPreferenceValueProvider vp) {
		return getNotApplicableResult(rule, vp, Messages.getString(Messages.RulesToolkit_TOO_FEW_EVENTS));
	}

	/**
	 * Creates a {@link IResult} object with a generic not applicable (N/A) result for a given rule
	 * with a specified message.
	 *
	 * @param rule
	 *            the rule to create a {@link IResult} object for
	 * @param message
	 *            the description of the result
	 * @return an object representing a generic not applicable result for the provided rule
	 */
	public static IResult getNotApplicableResult(IRule rule, IPreferenceValueProvider vp, String message) {
		return getNotApplicableResult(rule, vp, message, null);
	}

	/**
	 * Creates a {@link IResult} object with a generic not applicable (N/A) result for a given rule
	 * with a specified message.
	 *
	 * @param rule
	 *            the rule to create a {@link IResult} object for
	 * @param shortMessage
	 *            the description of the result, as a short description
	 * @param longMessage
	 *            a longer version of the description, used to explain in more detail why the rule
	 *            could not be evaluated
	 * @return an object representing a generic not applicable result for the provided rule
	 */
	private static IResult getNotApplicableResult(
		IRule rule, IPreferenceValueProvider vp, String shortMessage, String longMessage) {
		return ResultBuilder.createFor(rule, vp).setSeverity(Severity.NA).setSummary(shortMessage)
				.setExplanation(longMessage).build();
	}

	/**
	 * Creates a text message informing that event types are recommended
	 *
	 * @param items
	 *            the events.
	 * @param typeIds
	 *            the identifiers of the event types to check.
	 * @return a text message informing that event types are recommended
	 */
	public static String getEnabledEventTypesRecommendation(IItemCollection items, String ... typeIds) {
		return MessageFormat.format(Messages.getString(Messages.RulesToolkit_RULE_RECOMMENDS_EVENTS),
				getDisabledEventTypeNames(items, typeIds));
	}

	/**
	 * Gets the Java version for the recording the provided {@link IItemCollection} represents.
	 *
	 * @param items
	 *            the recording to find the version of
	 * @return an object representing the Java version of the VM the items originate from
	 */
	public static JavaVersion getJavaSpecVersion(IItemCollection items) {
		IItemCollection versionProperties = items.apply(ItemFilters.and(JdkFilters.SYSTEM_PROPERTIES,
				ItemFilters.equals(JdkAttributes.ENVIRONMENT_KEY, "java.vm.specification.version"))); //$NON-NLS-1$
		Set<String> vmSpecificationVersions = versionProperties
				.getAggregate((IAggregator<Set<String>, ?>) Aggregators.distinct(JdkAttributes.ENVIRONMENT_VALUE));
		if (vmSpecificationVersions != null && vmSpecificationVersions.size() >= 1) {
			return new JavaVersion(vmSpecificationVersions.iterator().next());
		}
		// Fall back to using the major version number of the JVM version as Java specification version
		JavaVersion jvmVersion = getJavaVersion(items);
		if (jvmVersion != null) {
			return new JavaVersion(jvmVersion.getMajorVersion());
		}
		return null;
	}

	/**
	 * @param items
	 *            the items to look for the JVM version in.
	 * @return the parsed JavaVersion, or null if no VM information event with the JVM version could
	 *         be found.
	 */
	public static JavaVersion getJavaVersion(IItemCollection items) {
		String jvmVersion = items.getAggregate(JdkAggregators.JVM_VERSION);
		return getJavaVersion(jvmVersion);
	}

	/**
	 * An exponential mapping from 0/infinity to 0/74 passing through 25 at limit. This approaches
	 * 74 at about 300-400% of the limit.
	 *
	 * @param value
	 *            Input value. Negative values will be treated as zero.
	 * @param x1
	 *            Return 25 if value is equal to this. Must be more than zero.
	 * @return A value between 0 and 74.
	 */
	public static double mapExp74(double value, double x1) {
		return mapExp(value, 74, x1, 25);
	}

	/**
	 * An exponential mapping from 0/infinity to 0/100 passing through 75 at limit. This approaches
	 * 100 at about 300-400% of the limit.
	 *
	 * @param value
	 *            Input value. Negative values will be treated as zero.
	 * @param x1
	 *            Return 75 if value is equal to this. Must be more than zero.
	 * @return A value between 0 and 100.
	 */
	public static double mapExp100(double value, double x1) {
		return mapExp(value, 100, x1, 75);
	}

	/**
	 * An exponential mapping from 0/infinity to 0/100 passing through y1 at x1.
	 *
	 * @param value
	 *            Input value. Negative values will be treated as zero.
	 * @param x1
	 *            Return y1 if value is equal to this. Must be more than zero.
	 * @param y1
	 *            Return value at x1. Must be more than zero and less than 100.
	 * @return A value between 0 and 100.
	 */
	public static double mapExp100Y(double value, double x1, double y1) {
		return mapExp(value, 100, x1, y1);
	}

	/**
	 * An exponential mapping from 0/infinity to 0/100 passing through 25 and 75 at limits. This
	 * approaches 100 at about 300-400% of the 75 limit.
	 *
	 * @param value
	 *            Input value. Negative values will be treated as zero.
	 * @param x1
	 *            Return 25 if value is equal to this. Must be more than zero.
	 * @param x2
	 *            Return 75 if value is equal to this. Must be more than x1.
	 * @return A value between 0 and 100.
	 */
	public static double mapExp100(double value, double x1, double x2) {
		return mapExp(value, 100, x1, 25, x2, 75);
	}

	/**
	 * @param value
	 *            Input value. Negative values will be treated as zero.
	 * @param ceiling
	 *            Max return value. Must be more than zero.
	 * @param x1
	 *            Return y1 if value is equal to this. Must be more than zero.
	 * @param y1
	 *            Return value at x1. Must be more than zero and less than ceiling.
	 * @return A value between 0 and ceiling.
	 */
	// FIXME: We might want to have a wider input range that produces discernible output values.
	public static double mapExp(double value, double ceiling, double x1, double y1) {
		if (value < 0) {
			return 0;
		}
		double k = Math.log(1 - y1 / ceiling) / x1;
		return ceiling * (1 - Math.exp(k * value));
	}

	/**
	 * @param value
	 *            Input value. Negative values will be treated as zero.
	 * @param ceiling
	 *            Max return value. Must be more than zero.
	 * @param x1
	 *            Return y1 if value is equal to this. Must be more than zero.
	 * @param y1
	 *            Return y1 at x1. Must be more than zero and less than y2.
	 * @param x2
	 *            Return y2 if value is equal to this. Must be more than x1.
	 * @param y2
	 *            Return y2 at x2. Must be more than y1 and less than ceiling.
	 * @return A value between 0 and ceiling.
	 */
	private static double mapExp(double value, double ceiling, double x1, double y1, double x2, double y2) {
		if (value < 0) {
			return 0;
		}
		if (value < x1) {
			return y1 / x1 * value;
		}
		return y1 + mapExp(value - x1, ceiling - y1, x2 - x1, y2 - y1);
	}

	/**
	 * An multi-linear mapping from 0/1 to 0/100 passing through 25 and 75 at limits.
	 *
	 * @param value
	 *            Input value. Negative values will be treated as zero.
	 * @param x1
	 *            Return 25 if value is equal to this. Must be more than zero.
	 * @param x2
	 *            Return 75 if value is equal to this. Must be more than x1.
	 * @return A value between 0 and 100.
	 */
	public static double mapLin100(double value, double x1, double x2) {
		if (value <= 0) {
			return 0;
		}
		if (value >= 1) {
			return 1;
		}
		if (value <= x1) {
			return value * 25 / x1;
		}
		if (value <= x2) {
			return 25 + (value - x1) * (75 - 25) / (x2 - x1);
		}
		return 75 + (value - x2) * (100 - 75) / (1 - x2);
	}

	/**
	 * Each group is represented by the number of elements that belong in that group, elements are
	 * grouped by accessor value.
	 * <p>
	 * For example, the items {A, B, C, A, B, A, A} will become {1, 2, 4}
	 *
	 * @param items
	 *            input items
	 * @param accessorFactory
	 *            a factory that provides accessors for the input item types
	 * @return A sorted list of counts, one for each unique value that the accessor computes from
	 *         the input items, that tells how many input items gave that accessor value.
	 */
	public static <T> List<IntEntry<T>> calculateGroupingScore(
		IItemCollection items, IAccessorFactory<T> accessorFactory) {
		EntryHashMap<T, IntEntry<T>> map = MapToolkit.createIntMap(1000, 0.5f);
		for (IItemIterable ii : items) {
			IMemberAccessor<? extends T, IItem> accessor = accessorFactory.getAccessor(ii.getType());
			if (accessor == null) {
				continue;
			}
			for (IItem item : ii) {
				T member = accessor.getMember(item);
				if (member != null) {
					IntEntry<T> entry = map.get(member, true);
					entry.setValue(entry.getValue() + 1);
				}
			}
		}
		List<IntEntry<T>> array = IteratorToolkit.toList(map.iterator(), map.size());
		array.sort(null);
		return array;
	}

	/**
	 * Calculates a balance for entries, where later elements get a higher relevance than earlier
	 * elements.
	 * <p>
	 * For example the values 1, 1, 2, 5 will get the total score 5/9/1 + 2/9/2 + 1/9/3 + 1/9/4
	 *
	 * @param array
	 *            input values
	 * @return the balance score
	 */
	public static <T> double calculateBalanceScore(List<IntEntry<T>> array) {
		int totalCount = 0;
		for (IntEntry<T> e : array) {
			totalCount += e.getValue();
		}
		double score = 0;
		for (int i = array.size() - 1; i >= 0; i--) {
			int index = array.size() - i;
			score += ((double) array.get(i).getValue()) / totalCount / index;
		}
		return score;
	}

	/**
	 * Get the duration for item within the specified window
	 *
	 * @param windowStart
	 *            window start
	 * @param windowEnd
	 *            window end
	 * @param item
	 *            item to get duration for
	 * @return duration within window
	 */
	public static IQuantity getDurationInWindow(IQuantity windowStart, IQuantity windowEnd, IItem item) {
		IQuantity start = getStartTime(item);
		IQuantity end = getEndTime(item);
		IQuantity startCapped = start.compareTo(windowStart) > 0 ? start : windowStart;
		IQuantity endCapped = end.compareTo(windowEnd) > 0 ? windowEnd : end;
		IQuantity durationCapped = endCapped.subtract(startCapped);
		return durationCapped;
	}

	/**
	 * Maps the input value into a value between the minimum and maximum values (exclusive) using a
	 * sigmoidal curve with the given parameters. Minimum and maximum values are asymptotes, so will
	 * never be mapped to. If you want to map from [0,1] to (0,100) using this you should set a low
	 * inflection point and a single digit high curve fit with a low curve fit around 150. This will
	 * lead to exponential growth until the midway point where it will start growing
	 * logarithmically.
	 *
	 * @param input
	 *            the value to map
	 * @param minimum
	 *            the maximum value to map to (exclusive)
	 * @param maximum
	 *            the minimum value to map to (exclusive)
	 * @param lowCurveFit
	 *            fitting parameter for the lower end of the curve
	 * @param inflectionPoint
	 *            the inflection point of the curve (where input leads to 1/3 between min and max)
	 * @param highCurveFit
	 *            fitting parameter for the higher end of the curve
	 * @return a mapped value in the range (minimum,maximum)
	 */
	public static double mapSigmoid(
		double input, double minimum, double maximum, double lowCurveFit, double inflectionPoint, double highCurveFit) {
		// https://www.desmos.com/calculator/aylt9wv1x0 to view the curve
		double g = Math.exp(lowCurveFit * (inflectionPoint - input));
		double h = Math.exp(highCurveFit * (inflectionPoint - input));
		return minimum + (maximum / (1 + g + h));
	}

	private static IQuantity getSettingMaxPeriod(Iterable<String> settingsValues) {
		IQuantity maxPeriod = null;
		for (String s : settingsValues) {
			try {
				if (REC_SETTING_PERIOD_EVERY_CHUNK.equals(s)) {
					return null;
				}
				IQuantity p = parsePersistedJvmTimespan(s);
				if (maxPeriod == null || maxPeriod.compareTo(p) < 0) {
					maxPeriod = p;
				}
			} catch (QuantityConversionException e) {
				throw new RuntimeException(e);
			}
		}
		return maxPeriod;
	}

	private static Set<String> getPeriodSettings(IItemCollection items, String ... typeIds) {
		IItemFilter filter = getSettingsFilter(REC_SETTING_NAME_PERIOD, typeIds);
		return items.apply(filter)
				.getAggregate((IAggregator<Set<String>, ?>) Aggregators.distinct(JdkAttributes.REC_SETTING_VALUE));
	}

	/*
	 * Returns the names of the _explicitly_ disabled event types.
	 */
	private static String getDisabledEventTypeNames(IItemCollection items, String ... typeIds) {
		return getEventTypeNames(items.apply(createEnablementFilter(false, typeIds)));
	}

	private static String getEventTypeNames(IItemCollection items) {
		Set<String> names = items
				.getAggregate((IAggregator<Set<String>, ?>) Aggregators.distinct("", TYPE_NAME_ACCESSOR_FACTORY)); //$NON-NLS-1$
		if (names == null) {
			return null;
		}
		List<String> quotedNames = new ArrayList<>();
		for (String name : names) {
			quotedNames.add("'" + name + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		quotedNames.sort(null);
		return StringToolkit.join(quotedNames, ", "); //$NON-NLS-1$
	}

	private static IItemFilter createEnablementFilter(boolean enabled, String ... typeIds) {
		IItemFilter settingsFilter = getSettingsFilter(REC_SETTING_NAME_ENABLED, typeIds);
		IItemFilter enabledFilter = ItemFilters.equals(JdkAttributes.REC_SETTING_VALUE,
				enabled ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
		IItemFilter enablementFilter = ItemFilters.and(settingsFilter, enabledFilter);
		return enablementFilter;
	}

	private static boolean internalHasEvents(IItemCollection items, String typeId) {
		return items.apply(ItemFilters.type(typeId)).hasItems();
	}

	private static Set<String> getAvailableTypeIds(IItemCollection items) {
		Set<String> ids = new HashSet<>();
		for (IItemIterable iterable : items) {
			ids.add(iterable.getType().getIdentifier());
		}
		return ids;
	}

	/**
	 * @param vmInfoVersionString
	 *            the JVM version information as presented in the VM information events, containing
	 *            both the JVM and JDK version numbers.
	 * @return the parsed JavaVersion.
	 */
	public static JavaVersion getJavaVersion(String vmInfoVersionString) {
		if (vmInfoVersionString != null) {
			Matcher versionMatcher = VERSION_PATTERN.matcher(vmInfoVersionString);
			if (versionMatcher.matches()) {
				String versionString = versionMatcher.group(1);
				return new JavaVersion(versionString);
			}
		}
		return null;
	}

	static String getJavaCommandHelpLink(JavaVersion javaVersion) {
		if (javaVersion != null) {
			int major = javaVersion.getMajorVersion();
			switch (major) {
			case 8:
				return "https://docs.oracle.com/javase/8/docs/technotes/tools/unix/java.html"; //$NON-NLS-1$
			case 9:
			case 10:
				return "https://docs.oracle.com/javase/" + major + "/tools/java.htm#JSWOR624";
			case 11:
			case 12:
				return "https://docs.oracle.com/en/java/javase/" + major
						+ "/tools/java.html#GUID-3B1CE181-CD30-4178-9602-230B800D4FAE";
			case 13:
			case 14:
			case 15:
				return "https://docs.oracle.com/en/java/javase/" + major + "/docs/specs/man/java.html";
			}
		}
		// by default use version 8
		return "https://docs.oracle.com/javase/8/docs/technotes/tools/unix/java.html"; //$NON-NLS-1$
	}

	/**
	 * Gets the {@link IType} representation of a specific event type in an {@link IItemCollection}.
	 *
	 * @param items
	 *            the items to find the type in
	 * @param typeId
	 *            the event type id to find the type object of
	 * @return an object representing the type with the specified id in the provided item collection
	 */
	public static IType<IItem> getType(IItemCollection items, String typeId) {
		for (IItemIterable iter : items) {
			if (iter.getType().getIdentifier().equals(typeId)) {
				return iter.getType();
			}
		}
		return null;
	}

	/**
	 * Gets a {@link IResult} object representing a not applicable result due to a missing
	 * attribute.
	 *
	 * @param rule
	 *            the rule which could not be evaluated
	 * @param type
	 *            the type of the item which is missing a required attribute
	 * @param attribute
	 *            the attribute that is missing
	 * @return an object that represents the not applicable result due to the missing attribute
	 */
	public static IResult getMissingAttributeResult(
		IRule rule, IType<IItem> type, IAttribute<?> attribute, IPreferenceValueProvider vp) {
		return getNotApplicableResult(rule, vp,
				MessageFormat.format(Messages.getString(Messages.RulesToolkit_ATTRIBUTE_NOT_FOUND),
						attribute.getIdentifier(), type.getIdentifier()),
				MessageFormat.format(Messages.getString(Messages.RulesToolkit_ATTRIBUTE_NOT_FOUND_LONG),
						attribute.getIdentifier(), type.getIdentifier()));
	}

	/**
	 * Creates a thread and range filter for a set of related events. It assumes all the events
	 * provided are related and spans one contiguous time range per thread. The resulting filter
	 * will include all the original events plus any other occurred in the same thread during the
	 * time period (per thread) spanned by the events in the collection. Note that this is an
	 * expensive operation. Use with care.
	 *
	 * @param items
	 *            a collection of related items.
	 * @return a filter for the thread and time range.
	 */
	public static TimeRangeThreadFilter createThreadsAndRangesFilter(IItemCollection items) {
		Map<IMCThread, Range> rangeMap = new HashMap<>();

		for (IItemIterable iter : items) {
			for (IItem item : iter) {
				IMCThread thread = getThread(item);
				if (thread == null) {
					continue;
				}
				if (!rangeMap.containsKey(thread)) {
					rangeMap.put(thread, new Range(getStartTime(item), getEndTime(item)));
				} else {
					Range r = rangeMap.get(thread);
					IQuantity startTime = getStartTime(item);
					IQuantity endTime = getEndTime(item);
					if (startTime.compareTo(r.startTime) < 0 || endTime.compareTo(r.endTime) > 0) {
						IQuantity newStartTime = startTime.compareTo(r.startTime) < 0 ? startTime : r.startTime;
						IQuantity newEndTime = endTime.compareTo(r.endTime) > 0 ? endTime : r.endTime;
						rangeMap.put(thread, new Range(newStartTime, newEndTime));
					}
				}
			}
		}
		return new TimeRangeThreadFilter(rangeMap);
	}

	/**
	 * Creates a range filter for an event. The range will span the same time as the event.
	 *
	 * @param item
	 *            the event for which to create the range filter
	 * @return a filter for the time range of the event.
	 */
	public static IItemFilter createRangeFilter(IItem item) {
		return new TimeRangeFilter(new Range(getStartTime(item), getEndTime(item)));
	}

	/**
	 * Convenience method for getting the start time value from a specific event.
	 *
	 * @param item
	 *            the event to get the start time from
	 * @return the start time of the provided event
	 */
	public static IQuantity getStartTime(IItem item) {
		return RulesToolkit.getValue(item, JfrAttributes.START_TIME);
	}

	/**
	 * Convenience method to get the end time value from a specific event.
	 *
	 * @param item
	 *            the event to get the end time from
	 * @return the end time of the provided event
	 */
	public static IQuantity getEndTime(IItem item) {
		return RulesToolkit.getValue(item, JfrAttributes.END_TIME);
	}

	/**
	 * Convenience method to get the duration value from a specific event.
	 *
	 * @param item
	 *            the event to get the duration from
	 * @return the duration of the provided event
	 */
	public static IQuantity getDuration(IItem item) {
		return RulesToolkit.getValue(item, JfrAttributes.DURATION);
	}

	/**
	 * Convenience method to get the event thread value from a specific event.
	 *
	 * @param item
	 *            the event to get the thread value from
	 * @return the thread the provided event occurred in
	 */
	public static IMCThread getThread(IItem item) {
		return getOptionalValue(item, JfrAttributes.EVENT_THREAD);
	}

	/**
	 * Returns the value, or null if no accessor is available.
	 */
	private static <T> T getOptionalValue(IItem item, IAccessorFactory<T> attribute) {
		IType<IItem> itemType = ItemToolkit.getItemType(item);
		IMemberAccessor<? extends T, IItem> accessor = attribute.getAccessor(itemType);
		if (accessor == null) {
			return null;
		}
		return accessor.getMember(item);
	}

	/**
	 * Calculates the ratio between two {@link IQuantity} values of compatible, linear kind and
	 * returns it represented as a percentage.
	 *
	 * @param antecedent
	 *            the antecedent (numerator) value
	 * @param consequent
	 *            the consequent (denominator) value
	 * @return the ratio between the two values as a percentage
	 */
	public static IQuantity toRatioPercent(IQuantity antecedent, IQuantity consequent) {
		return UnitLookup.PERCENT.quantity(antecedent.ratioTo(consequent) * 100f);
	}

	/**
	 * Same calculation as {@link RulesToolkit#toRatioPercent(IQuantity, IQuantity)} but it returns
	 * the percentage as a string instead.
	 *
	 * @param antecedent
	 *            the antecedent (numerator) value
	 * @param consequent
	 *            the consequent (denominator) value
	 * @return the ratio between the two values as a percentage, as a string
	 */
	public static String toRatioPercentString(IQuantity antecedent, IQuantity consequent) {
		return toRatioPercent(antecedent, consequent).displayUsing(IDisplayable.AUTO);
	}

	/**
	 * Retrieves all topics that have rules associated with them.
	 *
	 * @return all topics associated with any rule
	 */
	public static Collection<String> getAllTopics() {
		Set<String> topics = new HashSet<>();
		for (IRule r : RuleRegistry.getRules()) {
			topics.add(r.getTopic());
		}
		return topics;
	}

	/**
	 * Evaluates a collection of rules in parallel threads. The method returns a map of rules and
	 * {@link Future future} results that are scheduled to run using the specified number of
	 * threads.
	 * <p>
	 * You can use a single threaded loop over the returned futures to {@link Future#get() get} the
	 * results.
	 * <p>
	 * If evaluation of a rule fails, then the get method of the corresponding future will throw an
	 * {@link ExecutionException}.
	 *
	 * @param rules
	 *            rules to run
	 * @param items
	 *            items to evaluate
	 * @param preferences
	 *            See
	 *            {@link IRule#createEvaluation(IItemCollection, IPreferenceValueProvider, IResultValueProvider)}.
	 *            If {@code null}, then default values will be used.
	 * @param nThreads
	 *            The number or parallel threads to use when evaluating. If 0, then the number of
	 *            available processors will be used.
	 * @return a map from rules to result futures
	 */
	public static Map<IRule, Future<IResult>> evaluateParallel(
		Collection<IRule> rules, IItemCollection items, IPreferenceValueProvider preferences, int nThreads) {
		if (preferences == null) {
			preferences = IPreferenceValueProvider.DEFAULT_VALUES;
		}
		if (nThreads < 1) {
			nThreads = Runtime.getRuntime().availableProcessors();
		}
		ResultProvider resultProvider = new ResultProvider();
		Map<IRule, Future<IResult>> resultFutures = new HashMap<>();
		Queue<RunnableFuture<IResult>> futureQueue = new ConcurrentLinkedQueue<>();
		// Map using the rule name as a key, and a Pair containing the rule (left) and it's dependency (right)
		Map<String, Pair<IRule, IRule>> rulesWithDependencies = new HashMap<>();
		Map<IRule, IResult> computedResults = new HashMap<>();
		for (IRule rule : rules) {
			if (matchesEventAvailabilityMap(items, rule.getRequiredEvents())) {
				if (hasDependency(rule)) {
					IRule depRule = rules.stream().filter(r -> r.getId().equals(getRuleDependencyName(rule)))
							.findFirst().orElse(null);
					rulesWithDependencies.put(rule.getId(), new Pair<>(rule, depRule));
				} else {
					RunnableFuture<IResult> resultFuture = rule.createEvaluation(items, preferences, resultProvider);
					resultFutures.put(rule, resultFuture);
					futureQueue.add(resultFuture);
				}
			} else {
				resultFutures.put(rule, CompletableFuture.completedFuture(getNotApplicableResult(rule, preferences,
						Messages.getString(Messages.RulesToolkit_RULE_IGNORED))));
			}
		}
		for (String ruleName : rulesWithDependencies.keySet()) {
			IRule rule = rulesWithDependencies.get(ruleName).left;
			IRule depRule = rulesWithDependencies.get(ruleName).right;
			Future<IResult> depResultFuture = resultFutures.get(depRule);
			if (depResultFuture == null) {
				resultFutures.put(rule, CompletableFuture.completedFuture(getNotApplicableResult(rule, preferences,
						Messages.getString(Messages.RulesToolkit_EVALUATION_ERROR_DESCRIPTION))));
			} else {
				IResult depResult = null;
				if (!depResultFuture.isDone()) {
					((Runnable) depResultFuture).run();
					try {
						depResult = depResultFuture.get();
						resultProvider.addResults(depResult);
						computedResults.put(depRule, depResult);
					} catch (InterruptedException | ExecutionException e) {
						Logger.getLogger(RulesToolkit.class.getName()).log(Level.WARNING, MessageFormat
								.format(Messages.getString(Messages.RulesToolkit_RULE_RESULT_RETRIEVAL_ERROR), e));
					}
				} else {
					depResult = computedResults.get(depRule);
				}
				if (depResult != null && shouldEvaluate(rule, depResult)) {
					RunnableFuture<IResult> resultFuture = rule.createEvaluation(items, preferences, resultProvider);
					resultFutures.put(rule, resultFuture);
					futureQueue.add(resultFuture);
				} else {
					resultFutures.put(rule, CompletableFuture.completedFuture(getNotApplicableResult(rule, preferences,
							Messages.getString(Messages.RulesToolkit_RULE_IGNORED))));
				}
			}
		}
		for (int i = 0; i < nThreads; i++) {
			RuleEvaluator re = new RuleEvaluator(futureQueue);
			Thread t = new Thread(re);
			t.start();
		}
		return resultFutures;
	}

	private static boolean hasDependency(IRule rule) {
		DependsOn dependency = rule.getClass().getAnnotation(DependsOn.class);
		return dependency != null;
	}

	private static String getRuleDependencyName(IRule rule) {
		DependsOn dependency = rule.getClass().getAnnotation(DependsOn.class);
		Class<? extends IRule> dependencyType = dependency.value();
		return dependencyType.getSimpleName();
	}

	/**
	 * Checks to see if a rule should be evaluated based on the severity value of its dependency's
	 * result severity value.
	 * 
	 * @param rule
	 *            rule to check severity value against its dependency's severity
	 * @param depResult
	 *            result from the rule's dependency
	 * @return true if the dependency rule result satisfies the severity requirement for the passed
	 *         rule
	 */
	private static boolean shouldEvaluate(IRule rule, IResult depResult) {
		DependsOn dependency = rule.getClass().getAnnotation(DependsOn.class);
		if (dependency != null) {
			if (depResult.getSeverity().compareTo(dependency.severity()) < 0) {
				return false;
			}
		}
		return true;
	}

	private static class RuleEvaluator implements Runnable {
		private Queue<RunnableFuture<IResult>> futureQueue;

		public RuleEvaluator(Queue<RunnableFuture<IResult>> futureQueue) {
			this.futureQueue = futureQueue;
		}

		@Override
		public void run() {
			RunnableFuture<IResult> resultFuture;
			while ((resultFuture = futureQueue.poll()) != null) {
				resultFuture.run();
			}
		}
	}

	/**
	 * Gets the second frame in the most common stack trace. Useful when showing what called a
	 * interesting method, like for example java.lang.Integer.valueOf (aka autoboxing)
	 *
	 * @param items
	 *            the item collection to build the aggregated stack trace on
	 * @return a stack trace frame
	 */
	// FIXME: Generalize this a bit, get the top N frames
	public static String getSecondFrameInMostCommonTrace(IItemCollection items) {
		FrameSeparator sep = new FrameSeparator(FrameSeparator.FrameCategorization.LINE, false);
		StacktraceModel stacktraceModel = new StacktraceModel(false, sep, items);
		Branch firstBranch = stacktraceModel.getRootFork().getBranch(0);
		StacktraceFrame secondFrame = null;
		if (firstBranch.getTailFrames().length > 0) {
			secondFrame = firstBranch.getTailFrames()[0];
		} else if (firstBranch.getEndFork().getBranchCount() > 0) {
			secondFrame = firstBranch.getEndFork().getBranch(0).getFirstFrame();
		} else {
			return null;
		}
		/*
		 * FIXME: Consider defining the method formatting based on preferences.
		 *
		 * Currently it's a compromise between keeping the length short, but still being able to
		 * identify the actual method, even if the line number is a bit incorrect.
		 */
		return StacktraceFormatToolkit.formatFrame(secondFrame.getFrame(), sep, false, false, true, true, true, false);
	}

	/**
	 * Convenience method for parsing the -XX:FlightRecorderOptions JVM flag. Since this is one flag
	 * that contains all flight recorder settings in a comma separated list as a single string it is
	 * useful to have one place to get the actual setting/value pairs from an
	 * {@link IItemCollection}.
	 *
	 * @param items
	 *            an item collection containing at least one {@link JdkTypeIDs#STRING_FLAG} event
	 *            with the value "FlightRecorderOptions"
	 * @return a setting/value map for all FlightRecorderOptions
	 */
	public static Map<String, String> getFlightRecorderOptions(IItemCollection items) {
		Map<String, String> options = new HashMap<>();
		IItemFilter stringFlagsFilter = ItemFilters.type(JdkTypeIDs.STRING_FLAG);
		IItemFilter optionsFilter = ItemFilters.matches(JdkAttributes.FLAG_NAME, "FlightRecorderOptions"); //$NON-NLS-1$
		IItemCollection optionsFlag = items.apply(ItemFilters.and(stringFlagsFilter, optionsFilter));
		Set<String> optionsValues = optionsFlag
				.getAggregate((IAggregator<Set<String>, ?>) Aggregators.distinct(JdkAttributes.FLAG_VALUE_TEXT));
		if (optionsValues != null && optionsValues.size() > 0) {
			String optionsValue = optionsValues.iterator().next();
			String[] allOptions = optionsValue.split(","); //$NON-NLS-1$
			for (String optionAndValue : allOptions) {
				String[] optionAndValueSplit = optionAndValue.split("="); //$NON-NLS-1$
				if (optionAndValueSplit.length >= 2) {
					options.put(optionAndValueSplit[0], optionAndValueSplit[1]);
				} else {
					options.put(optionAndValue, ""); //$NON-NLS-1$
				}
			}
		}
		return options;
	}

	/**
	 * Checks if the timerange spanned by the items is shorter than the limit, and returns a
	 * informative text message if that is the case.
	 *
	 * @param items
	 *            the item collection to get recording range from
	 * @param shortRecordingLimit
	 *            limit for a short recording
	 * @return a text message informing that this is a short recording, or null if recording is not
	 *         short
	 */
	public static String getShortRecordingInfo(IItemCollection items, IQuantity shortRecordingLimit) {
		IQuantity recordingDuration = getItemRange(items);
		boolean shortRecording = recordingDuration.compareTo(shortRecordingLimit) < 0;
		if (shortRecording) {
			return MessageFormat.format(Messages.getString(Messages.Result_SHORT_RECORDING),
					recordingDuration.displayUsing(IDisplayable.AUTO),
					shortRecordingLimit.displayUsing(IDisplayable.AUTO));
		}
		return null;
	}

	private static IQuantity getItemRange(IItemCollection items) {
		IQuantity first = getEarliestStartTime(items);
		IQuantity last = getLatestEndTime(items);

		return last.subtract(first);
	}

	/**
	 * Sorts map according to values.
	 *
	 * @param map
	 *            the map to sort
	 * @param sortAscending
	 *            true if the sorting should be from lower to higher, false for higher to lower
	 * @return sorted map
	 */
	public static Map<String, Integer> sortMap(final Map<String, Integer> map, final boolean sortAscending) {
		List<Map.Entry<String, Integer>> entries = new ArrayList<>(map.entrySet());
		entries.sort(new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				if (sortAscending) {
					return o1.getValue().compareTo(o2.getValue());
				} else {
					return o2.getValue().compareTo(o1.getValue());
				}
			}
		});
		final Map<String, Integer> sortedMap = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> entry : entries) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

	private static final IAggregator<IQuantity, ?> EARLIEST_START_TIME = Aggregators.min(JfrAttributes.START_TIME);

	/**
	 * Returns the earliest start time in the provided item collection. This method is based on the
	 * assumption that item collection lanes are sorted by timestamp.
	 * 
	 * @param items
	 *            the item collection to find the earliest start time in
	 * @return the earliest start time in the provided collection
	 */
	public static IQuantity getEarliestStartTime(IItemCollection items) {
		// JMC-7088: We use this check to disable the optimisation for IItemCollection implementations that don't contain sorted event lanes.
		if (items.getClass().getName().equals("EventCollection")) { //$NON-NLS-1$
			IQuantity earliestStartTime = null;
			for (IItemIterable iItemIterable : items) {
				IMemberAccessor<IQuantity, IItem> startTimeAccessor = JfrAttributes.START_TIME
						.getAccessor(iItemIterable.getType());
				if (iItemIterable.iterator().hasNext()) {
					IItem next = iItemIterable.iterator().next();
					if (next != null && startTimeAccessor != null) {
						IQuantity startTime = startTimeAccessor.getMember(next);
						if (earliestStartTime == null) {
							earliestStartTime = startTime;
						} else {
							if (earliestStartTime.compareTo(startTime) >= 0) {
								earliestStartTime = startTime;
							}
						}
					}
				}
			}
			return earliestStartTime;
		} else {
			return items.getAggregate(EARLIEST_START_TIME);
		}
	}

	private static final IAggregator<IQuantity, ?> EARLIEST_END_TIME = Aggregators.min(JfrAttributes.END_TIME);

	/**
	 * Returns the earliest end time in the provided item collection. This method is based on the
	 * assumption that item collection lanes are sorted by timestamp and are not overlapping.
	 * 
	 * @param items
	 *            the item collection to find the earliest end time in
	 * @return the earliest end time in the provided collection
	 */
	public static IQuantity getEarliestEndTime(IItemCollection items) {
		// JMC-7088: We use this check to disable the optimisation for IItemCollection implementations that don't contain sorted event lanes.
		if (items.getClass().getName().equals("EventCollection")) { //$NON-NLS-1$
			IQuantity earliestEndTime = null;
			for (IItemIterable iItemIterable : items) {
				IMemberAccessor<IQuantity, IItem> endTimeAccessor = JfrAttributes.END_TIME
						.getAccessor(iItemIterable.getType());
				if (iItemIterable.iterator().hasNext()) {
					IItem next = iItemIterable.iterator().next();
					if (next != null && endTimeAccessor != null) {
						IQuantity endTime = endTimeAccessor.getMember(next);
						if (earliestEndTime == null) {
							earliestEndTime = endTime;
						} else {
							if (earliestEndTime.compareTo(endTime) >= 0) {
								earliestEndTime = endTime;
							}
						}
					}
				}
			}
			return earliestEndTime;
		} else {
			return items.getAggregate(EARLIEST_END_TIME);
		}
	}

	private static final IAggregator<IQuantity, ?> LATEST_END_TIME = Aggregators.max(JfrAttributes.END_TIME);

	/**
	 * Returns the latest end time in the provided item collection. This method is based on the
	 * assumption that item collection lanes are sorted by timestamp and are not overlapping.
	 * 
	 * @param items
	 *            the item collection to find the latest end time in
	 * @return the latest end time in the provided collection
	 */
	public static IQuantity getLatestEndTime(IItemCollection items) {
		// JMC-7088: We use this check to disable the optimisation for IItemCollection implementations that don't contain sorted event lanes.
		if (items.getClass().getName().equals("EventCollection")) { //$NON-NLS-1$
			IQuantity latestEndTime = null;
			for (IItemIterable iItemIterable : items) {
				IMemberAccessor<IQuantity, IItem> endTimeAccessor = JfrAttributes.END_TIME
						.getAccessor(iItemIterable.getType());
				Iterator<IItem> iterator = iItemIterable.iterator();
				IItem next = null;
				while (iterator.hasNext()) {
					next = iterator.next();
				}
				if (next != null && endTimeAccessor != null) {
					IQuantity endTime = endTimeAccessor.getMember(next);
					if (latestEndTime == null) {
						latestEndTime = endTime;
					} else {
						if (latestEndTime.compareTo(endTime) <= 0) {
							latestEndTime = endTime;
						}
					}
				}
			}
			return latestEndTime;
		} else {
			return items.getAggregate(LATEST_END_TIME);
		}

	}
}
