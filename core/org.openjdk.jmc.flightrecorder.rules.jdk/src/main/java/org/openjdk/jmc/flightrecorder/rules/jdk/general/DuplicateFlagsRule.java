package org.openjdk.jmc.flightrecorder.rules.jdk.general;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders.JvmInternalsDataProvider;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class DuplicateFlagsRule implements IRule {

	public static class DuplicateFlags implements IDisplayable {
		private final Collection<List<String>> duplicates;

		private DuplicateFlags() {
			duplicates = new ArrayList<>();
		}

		private void addDuplicates(List<String> duplicates) {
			this.duplicates.add(duplicates);
		}

		@Override
		public String displayUsing(String formatHint) {
			StringBuilder sb = new StringBuilder();
			sb.append('[');
			for (List<String> dupes : duplicates) {
				sb.append('[');
				for (String d : dupes) {
					sb.append(d);
					sb.append(',');
				}
				sb.deleteCharAt(sb.length() - 1);
				sb.append(']');
				sb.append(',');
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append(']');
			return sb.toString();
		}
	}

	private static final String RESULT_ID = "DuplicateFlags"; //$NON-NLS-1$

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = new HashMap<>();

	private static final ContentType<DuplicateFlags> DUPLICATE_FLAGS = UnitLookup
			.createSyntheticContentType("duplicateFlags"); //$NON-NLS-1$

	public static final TypedResult<DuplicateFlags> DUPLICATED_FLAGS = new TypedResult<>("duplicatedFlags", //$NON-NLS-1$
			Messages.getString(Messages.DuplicateFlagsRule_RESULT_DUPLICATED_FLAGS_NAME),
			Messages.getString(Messages.DuplicateFlagsRule_RESULT_DUPLICATED_FLAGS_DESCRIPTION), DUPLICATE_FLAGS,
			DuplicateFlags.class);
	public static final TypedResult<IQuantity> TOTAL_DUPLICATED_FLAGS = new TypedResult<>("totalDuplicatedFlags", //$NON-NLS-1$
			Messages.getString(Messages.DuplicateFlagsRule_RESULT_DUPLICATED_FLAGS_COUNT_NAME),
			Messages.getString(Messages.DuplicateFlagsRule_RESULT_DUPLICATED_FLAGS_COUNT_DESCRIPTION),
			UnitLookup.NUMBER, IQuantity.class);

	private static final List<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(DUPLICATED_FLAGS,
			TOTAL_DUPLICATED_FLAGS);

	static {
		REQUIRED_EVENTS.put(JdkTypeIDs.VM_INFO, EventAvailability.AVAILABLE);
	}

	@Override
	public String getId() {
		return RESULT_ID;
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.JVM_INFORMATION_TOPIC;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.DuplicateFlagsRuleFactory_RULE_NAME);
	}

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return REQUIRED_EVENTS;
	}

	@Override
	public RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider preferenceValueProvider,
		final IResultValueProvider dependencyResults) {
		FutureTask<IResult> evaluationTask = new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				return getResult(items, preferenceValueProvider, dependencyResults);
			}
		});
		return evaluationTask;
	}

	private IResult getResult(IItemCollection items, IPreferenceValueProvider vp, IResultValueProvider rp) {
		IItemCollection jvmInfoItems = items.apply(JdkFilters.VM_INFO);

		// FIXME: Should we check if there are different jvm args in different chunks?
		Set<String> args = jvmInfoItems.getAggregate(Aggregators.distinct(JdkAttributes.JVM_ARGUMENTS));
		if (args != null && !args.isEmpty()) {
			DuplicateFlags duplicateFlags = new DuplicateFlags();
			for (ArrayList<String> dupe : JvmInternalsDataProvider.checkDuplicates(args.iterator().next())) {
				duplicateFlags.addDuplicates(dupe);
			}
			if (!JvmInternalsDataProvider.checkDuplicates(args.iterator().next()).isEmpty()) {
				return ResultBuilder.createFor(this, vp).addResult(DUPLICATED_FLAGS, duplicateFlags)
						.setSeverity(Severity.INFO)
						.setSummary(Messages.getString(Messages.DuplicateFlagsRule_RESULT_SUMMARY))
						.setExplanation(Messages.getString(Messages.DuplicateFlagsRule_RESULT_EXPLANATION))
						.setSolution(Messages.getString(Messages.DuplicateFlagsRule_RESULT_SOLUTION)).build();
			}
		}
		return ResultBuilder.createFor(this, vp).setSeverity(Severity.OK)
				.setSummary(Messages.getString(Messages.DuplicateFlagsRule_RESULT_SUMMARY_OK)).build();
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Collections.emptyList();
	}

	@Override
	public Collection<TypedResult<?>> getResults() {
		return RESULT_ATTRIBUTES;
	}

}
