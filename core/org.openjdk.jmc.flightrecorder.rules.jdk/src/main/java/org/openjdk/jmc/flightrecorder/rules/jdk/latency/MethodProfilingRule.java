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
package org.openjdk.jmc.flightrecorder.rules.jdk.latency;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.Aggregators.CountConsumer;
import org.openjdk.jmc.common.item.GroupingAggregator;
import org.openjdk.jmc.common.item.GroupingAggregator.GroupEntry;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders.MethodProfilingDataProvider;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.SlidingWindowToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.SlidingWindowToolkit.IUnorderedWindowVisitor;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel;

/**
 * Rule that calculates the top method balance in a sliding window throughout the recording with a
 * relevance calculated by the ratio of samples to maximum samples for that period.
 */
public class MethodProfilingRule implements IRule {

	/**
	 * Constant value of the maximum number of samples the JVM attempts per sampling period.
	 */
	private static final double SAMPLES_PER_PERIOD = 5;

	/**
	 * Constant value of the maximum number of stack frames to display for the hottest path.
	 */
	private static final int MAX_STACK_DEPTH = 10;

	/**
	 * A simple class for storing execution sample period settings, allowing the sliding window to
	 * get the correct samples for each time slice.
	 */
	private static class PeriodRangeMap {
		private List<Pair<IQuantity, IQuantity>> settingPairs = new ArrayList<>();

		void addSetting(IQuantity settingTime, IQuantity setting) {
			settingPairs.add(new Pair<>(settingTime, setting));
		}

		/**
		 * Gets the execution sample period that is in effect for the given timestamp.
		 *
		 * @param timestamp
		 *            the timestamp for which to find the given period setting
		 * @return an IQuantity representing the period setting for the period given
		 */
		IQuantity getSetting(IQuantity timestamp) {
			for (Pair<IQuantity, IQuantity> settingPair : settingPairs) {
				boolean isAfterOrAtSettingTime = settingPair.left.compareTo(timestamp) <= 0;
				if (isAfterOrAtSettingTime) {
					return settingPair.right;
				}
			}
			return null; // before first period setting event in recording, i.e. we should ignore any profiling events that get this result
		}

		void sort() {
			Collections.sort(settingPairs, new Comparator<Pair<IQuantity, IQuantity>>() {
				@Override
				public int compare(Pair<IQuantity, IQuantity> p1, Pair<IQuantity, IQuantity> p2) {
					return p1.left.compareTo(p2.left); // sorting according to time of setting event
				}
			});
		}
	}

	private static class MethodProfilingWindowResult {
		IMCMethod method;
		IMCStackTrace path;
		IQuantity ratioOfAllPossibleSamples;
		IRange<IQuantity> window;

		public MethodProfilingWindowResult(IMCMethod method, IMCStackTrace path, IQuantity ratio, IRange<IQuantity> window) {
			this.method = method;
			this.path = path;
			this.ratioOfAllPossibleSamples = ratio;
			this.window = window;
		}

		@Override
		public String toString() {
			return FormatToolkit.getHumanReadable(method, false, false, true, true, true, false) + " (" //$NON-NLS-1$
					+ ratioOfAllPossibleSamples.displayUsing(IDisplayable.AUTO) + ") " //$NON-NLS-1$
					+ window.displayUsing(IDisplayable.AUTO);
		}
	}

	private static final String RESULT_ID = "MethodProfiling"; //$NON-NLS-1$
	public static final TypedPreference<IQuantity> WINDOW_SIZE = new TypedPreference<>(
			"method.profiling.evaluation.window.size", //$NON-NLS-1$
			Messages.getString(Messages.MethodProfilingRule_WINDOW_SIZE),
			Messages.getString(Messages.MethodProfilingRule_WINDOW_SIZE_DESC), UnitLookup.TIMESPAN,
			UnitLookup.SECOND.quantity(30));
	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays.<TypedPreference<?>> asList(WINDOW_SIZE);

	/**
	 * Private Callable implementation specifically used to avoid storing the FutureTask as a field.
	 */
	private class MethodProfilingCallable implements Callable<Result> {
		private FutureTask<Result> evaluationTask = null;
		private IItemCollection items;
		private IPreferenceValueProvider valueProvider;

		private MethodProfilingCallable(IItemCollection items, IPreferenceValueProvider valueProvider) {
			this.items = items;
			this.valueProvider = valueProvider;
		}

		@Override
		public Result call() throws Exception {
			return getResult(items, valueProvider, evaluationTask);
		}

		void setTask(FutureTask<Result> task) {
			evaluationTask = task;
		}
	}

	@Override
	public RunnableFuture<Result> evaluate(final IItemCollection items, final IPreferenceValueProvider valueProvider) {
		MethodProfilingCallable callable = new MethodProfilingCallable(items, valueProvider);
		FutureTask<Result> evaluationTask = new FutureTask<>(callable);
		callable.setTask(evaluationTask);
		return evaluationTask;
	}

	private Result getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, FutureTask<Result> evaluationTask) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.EXECUTION_SAMPLE,
				JdkTypeIDs.RECORDING_SETTING);
		if (eventAvailability != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.EXECUTION_SAMPLE,
					JdkTypeIDs.RECORDING_SETTING);
		}

		PeriodRangeMap settings = new PeriodRangeMap();
		IItemFilter settingsFilter = RulesToolkit.getSettingsFilter(RulesToolkit.REC_SETTING_NAME_PERIOD,
				JdkTypeIDs.EXECUTION_SAMPLE);
		populateSettingsMap(items.apply(settingsFilter), settings);

		IQuantity windowSize = valueProvider.getPreferenceValue(WINDOW_SIZE);
		IQuantity slideSize = UnitLookup.SECOND.quantity(windowSize.ratioTo(UnitLookup.SECOND.quantity(2)));

		List<MethodProfilingWindowResult> windowResults = new ArrayList<>();
		IUnorderedWindowVisitor visitor = createWindowVisitor(settings, settingsFilter, windowSize, windowResults,
				evaluationTask);
		SlidingWindowToolkit.slidingWindowUnordered(visitor, items, windowSize, slideSize);
		// If a window visitor over a non empty quantity of events is guaranteed to always generate at minimum one raw score, this can be removed.
		if (windowResults.isEmpty()) {
			return RulesToolkit.getNotApplicableResult(this,
					Messages.getString(Messages.HotMethodsRuleFactory_NOT_ENOUGH_SAMPLES));
		}
		Pair<MethodProfilingWindowResult, Map<IMCStackTrace, MethodProfilingWindowResult>> interestingMethods = getInterestingMethods(
				windowResults);
		Map<IMCStackTrace, MethodProfilingWindowResult> percentByMethod = interestingMethods.right;
		MethodProfilingWindowResult mostInterestingResult = interestingMethods.left;
		if (mostInterestingResult == null) { // Couldn't find any interesting methods
			return new Result(this, 0, Messages.getString(Messages.HotMethodsRuleFactory_TEXT_OK));
		}
		double mappedScore = performSigmoidMap(
				mostInterestingResult.ratioOfAllPossibleSamples.doubleValueIn(UnitLookup.PERCENT_UNITY));

		Result result = null;
		if (mappedScore < 25) {
			result = new Result(this, mappedScore, Messages.getString(Messages.HotMethodsRuleFactory_TEXT_OK));
		} else {
			String shortDescription = MessageFormat.format(Messages.getString(Messages.HotMethodsRuleFactory_TEXT_INFO),
					FormatToolkit.getHumanReadable(mostInterestingResult.method, false, false, true, false, true,
							false),
					mostInterestingResult.ratioOfAllPossibleSamples.displayUsing(IDisplayable.AUTO),
					windowSize.displayUsing(IDisplayable.AUTO));
			String formattedPath = "<ul>" + //$NON-NLS-1$
					FormatToolkit.getHumanReadable(mostInterestingResult.path, false, false, true, true, true, false,
							MAX_STACK_DEPTH, null, "<li>", //$NON-NLS-1$
							"</li>" //$NON-NLS-1$
							) + "</ul>"; //$NON-NLS-1$
			String longDescription = MessageFormat.format(
					Messages.getString(Messages.HotMethodsRuleFactory_TEXT_INFO_LONG),
					buildResultList(percentByMethod),
					formattedPath
					);
			result = new Result(this, mappedScore, shortDescription, shortDescription + "<p>" + longDescription); //$NON-NLS-1$
		}
		return result;
	}

	private String buildResultList(Map<IMCStackTrace, MethodProfilingWindowResult> percentByMethod) {
		StringBuilder longList = new StringBuilder();
		longList.append("<ul>"); //$NON-NLS-1$
		for (Entry<IMCStackTrace, MethodProfilingWindowResult> entry : percentByMethod.entrySet()) {
			longList.append("<li>"); //$NON-NLS-1$
			longList.append(entry.getValue());
			longList.append("</li>"); //$NON-NLS-1$
		}
		longList.append("</ul>"); //$NON-NLS-1$
		return longList.toString();
	}

	private Pair<MethodProfilingWindowResult, Map<IMCStackTrace, MethodProfilingWindowResult>> getInterestingMethods(
		List<MethodProfilingWindowResult> windowResults) {
		Map<IMCStackTrace, MethodProfilingWindowResult> percentByMethod = new HashMap<>();
		IQuantity maxRawScore = UnitLookup.PERCENT_UNITY.quantity(0);
		MethodProfilingWindowResult mostInterestingResult = null;
		for (MethodProfilingWindowResult result : windowResults) {
			if (result != null) {
				if (result.ratioOfAllPossibleSamples.compareTo(maxRawScore) > 0) {
					mostInterestingResult = result;
					maxRawScore = result.ratioOfAllPossibleSamples;
				}
				if (result.path != null && performSigmoidMap(
						result.ratioOfAllPossibleSamples.doubleValueIn(UnitLookup.PERCENT_UNITY)) >= 25) {
					MethodProfilingWindowResult r = percentByMethod.get(result.path);
					if (r == null || result.ratioOfAllPossibleSamples.compareTo(r.ratioOfAllPossibleSamples) > 0) {
						percentByMethod.put(result.path, result);
					}
				}
			}
		}
		return new Pair<>(mostInterestingResult, percentByMethod);
	}

	private double performSigmoidMap(double input) {
		return RulesToolkit.mapSigmoid(input, 0, 100, 150, 0.03333, 7);
	}

	/**
	 * Creates an IUnorderedWindowVisitor that is called on each slice in the recording and
	 * generates the scores for each slice and places them in the rawScores list. The given
	 * parameters that are also given to the slidingWindowUnordered call must be the same as in this
	 * call.
	 *
	 * @param settings
	 *            the settings map with all the times the execution sample event has a change of
	 *            periodicity
	 * @param settingsFilter
	 *            the filter used to select the recording setting for the execution sample event
	 * @param windowSize
	 *            the size of the sliding window
	 * @param rawScores
	 *            the list of raw scores that will be populated by this visitor
	 * @return an IUnorderedWindowVisitor implementation that will populate the rawScores list with
	 *         raw score values
	 */
	private IUnorderedWindowVisitor createWindowVisitor(
		final PeriodRangeMap settings, final IItemFilter settingsFilter, final IQuantity windowSize,
		final List<MethodProfilingWindowResult> rawScores, final FutureTask<Result> evaluationTask) {
		return new IUnorderedWindowVisitor() {
			@Override
			public void visitWindow(IItemCollection items, IQuantity startTime, IQuantity endTime) {
				IRange<IQuantity> windowRange = QuantityRange.createWithEnd(startTime, endTime);
				if (RulesToolkit.getSettingMaxPeriod(items, JdkTypeIDs.EXECUTION_SAMPLE) == null) {
					Pair<IQuantity, IMCStackTrace> resultPair = performCalculation(items, settings.getSetting(startTime));
					if (resultPair != null) {
						rawScores.add(new MethodProfilingWindowResult(resultPair.right.getFrames().get(0).getMethod(), resultPair.right, resultPair.left, windowRange));
					}
				} else {
					Set<IQuantity> settingTimes = items.apply(settingsFilter)
							.getAggregate(Aggregators.distinct(JfrAttributes.START_TIME));
					IQuantity start = startTime;
					List<Pair<IQuantity, IMCStackTrace>> scores = new ArrayList<>(settingTimes.size());
					for (IQuantity settingTime : settingTimes) {
						IItemFilter window = ItemFilters.interval(JfrAttributes.END_TIME, start, true, settingTime,
								true);
						scores.add(performCalculation(items.apply(window), settings.getSetting(start)));
						start = settingTime;
					}
					Map<IMCStackTrace, IQuantity> scoresByMethod = new HashMap<>();
					for (Pair<IQuantity, IMCStackTrace> score : scores) {
						if (score != null) {
							if (scoresByMethod.get(score.right) == null) {
								scoresByMethod.put(score.right, score.left);
							} else {
								scoresByMethod.put(score.right, score.left.add(scoresByMethod.get(score.right)));
							}
						}
					}
					IQuantity sumScore = UnitLookup.PERCENT_UNITY.quantity(0);
					IMCStackTrace hottestPath = null;
					for (Entry<IMCStackTrace, IQuantity> entry : scoresByMethod.entrySet()) {
						if (entry.getValue().compareTo(sumScore) > 0) {
							hottestPath = entry.getKey();
							sumScore = sumScore.add(entry.getValue());
						}
					}
					IQuantity averageOfAllPossibleSamples = sumScore.multiply(1d / scores.size());
					IMCMethod hottestMethod = (hottestPath == null ? null : hottestPath.getFrames().get(0).getMethod());
					rawScores.add(new MethodProfilingWindowResult(hottestMethod, hottestPath, averageOfAllPossibleSamples, windowRange));
				}
			}

			@Override
			public boolean shouldContinue() {
				return evaluationTask != null && !evaluationTask.isCancelled();
			}

			/**
			 * Performs the actual calculation of the score for the given period of the recording.
			 *
			 * @param items
			 *            the items to base the score on
			 * @param period
			 *            the periodicity to base the relevancy calculation on
			 * @return a double value in the interval [0,1] with 1 being a system in completely
			 *         saturated load with only one method called
			 */
			private Pair<IQuantity, IMCStackTrace> performCalculation(IItemCollection items, IQuantity period) {
				IItemCollection filteredItems = items.apply(JdkFilters.EXECUTION_SAMPLE);
				final IMCMethod[] maxMethod = new IMCMethod[1];
				final IMCStackTrace[] maxPath = new IMCStackTrace[1];
				// Using this GroupingAggregator because it's the only way to extract the keys from the aggregation along with values
				IAggregator<IQuantity, ?> aggregator = GroupingAggregator.build("", "", //$NON-NLS-1$ //$NON_NLS_2$
						MethodProfilingDataProvider.PATH_ACCESSOR_FACTORY, Aggregators.count(),
						new GroupingAggregator.IGroupsFinisher<IQuantity, IMCStackTrace, CountConsumer>() {

							@Override
							public IType<IQuantity> getValueType() {
								return UnitLookup.NUMBER;
							}

							@Override
							public IQuantity getValue(Iterable<? extends GroupEntry<IMCStackTrace, CountConsumer>> groupEntries) {
								HashMap<IMCMethod, IQuantity> map = new HashMap<>();
								HashMap<IMCMethod, IMCStackTrace> pathMap = new HashMap<>();
								int total = 0;
								// When we group by stack trace we can run into situations where the top frames are otherwise the same
								// for our purposes (finding the hottest method), but they differ by BCI, throwing off the count.
								// so we should collect further on the method for the top frame.
								for (GroupEntry<IMCStackTrace, CountConsumer> group : groupEntries) {
									total += group.getConsumer().getCount();
									IMCMethod topFrameMethod = group.getKey().getFrames().get(0).getMethod();
									if (map.get(topFrameMethod) == null) {
										map.put(topFrameMethod, UnitLookup.NUMBER_UNITY.quantity(group.getConsumer().getCount()));
										pathMap.put(topFrameMethod, group.getKey());
									} else {
										IQuantity old = map.get(topFrameMethod);
										map.put(topFrameMethod, old.add(UnitLookup.NUMBER_UNITY.quantity(group.getConsumer().getCount())));
									}
								}
								if (!pathMap.isEmpty() && !map.isEmpty()) {
									Entry<IMCMethod, IQuantity> topEntry = Collections.max(map.entrySet(), new Comparator<Entry<IMCMethod, IQuantity>>() {
										@Override
										public int compare(Entry<IMCMethod, IQuantity> arg0,
												Entry<IMCMethod, IQuantity> arg1) {
											return arg0.getValue().compareTo(arg1.getValue());
										}
									});
									maxPath[0] = pathMap.get(topEntry.getKey());
									maxMethod[0] = topEntry.getKey();
									return topEntry.getValue().multiply(1d/total);
								}
								return UnitLookup.NUMBER_UNITY.quantity(0);
							}
				});

				IQuantity maxRatio = filteredItems.getAggregate(aggregator);
				Pair<IQuantity, IMCStackTrace> result = null;
				if (maxMethod[0] != null && maxRatio != null && period != null) { // ignoring if there are no samples or if we don't yet know the periodicity
					double periodsPerSecond = 1 / period.doubleValueIn(UnitLookup.SECOND);
					double maxSamplesPerSecond = SAMPLES_PER_PERIOD * periodsPerSecond;
					double samplesInPeriod = items
							.getAggregate(Aggregators.count(ItemFilters.type(JdkTypeIDs.EXECUTION_SAMPLE)))
							.doubleValueIn(UnitLookup.NUMBER_UNITY);
					double maxSamplesInPeriod = maxSamplesPerSecond * windowSize.doubleValueIn(UnitLookup.SECOND);
					double relevancy = samplesInPeriod / maxSamplesInPeriod;
					double highestRatioOfSamples = maxRatio.doubleValueIn(UnitLookup.NUMBER_UNITY);
					IQuantity percentOfAllPossibleSamples = UnitLookup.PERCENT_UNITY
							.quantity(highestRatioOfSamples * relevancy);
					result = new Pair<>(percentOfAllPossibleSamples, maxPath[0]);
				}
				return result;
			}
		};
	}

	/**
	 * Populates the settings map with all the period settings for the execution sample event found
	 * in this recording.
	 *
	 * @param items
	 *            the items to search for execution sample period events
	 * @param settings
	 *            the map to populate with the events
	 */
	private void populateSettingsMap(IItemCollection items, final PeriodRangeMap settings) {
		Iterator<IItemIterable> itemIterableIterator = items.iterator();
		while (itemIterableIterator.hasNext()) {
			IItemIterable itemIterable = itemIterableIterator.next();
			IMemberAccessor<IQuantity, IItem> startTimeAccessor = JfrAttributes.START_TIME
					.getAccessor(itemIterable.getType());
			IMemberAccessor<String, IItem> settingValueAccessor = JdkAttributes.REC_SETTING_VALUE
					.getAccessor(itemIterable.getType());

			Iterator<IItem> itemIterator = itemIterable.iterator();
			while (itemIterator.hasNext()) {
				IItem item = itemIterator.next();
				settings.addSetting(startTimeAccessor.getMember(item),
						getValueQuantity(settingValueAccessor.getMember(item)));
			}
		}
		settings.sort();
	}

	/**
	 * Used to parse the value of a Recording Setting Period attribute
	 *
	 * @param settingValue
	 *            the value to parse
	 * @return an IQuantity representation of the passed String object
	 */
	private IQuantity getValueQuantity(String settingValue) {
		try {
			if (RulesToolkit.REC_SETTING_PERIOD_EVERY_CHUNK.equals(settingValue)) {
				return null;
			}
			return RulesToolkit.parsePersistedJvmTimespan(settingValue);
		} catch (QuantityConversionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return CONFIG_ATTRIBUTES;
	}

	@Override
	public String getId() {
		return RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.MethodProfilingRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.METHOD_PROFILING_TOPIC;
	}

}
