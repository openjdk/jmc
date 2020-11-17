package org.openjdk.jmc.flightrecorder.rules.jdk.memory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

public class GarbageCollectionInfoRule implements IRule {

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create().addEventType(JdkTypeIDs.GARBAGE_COLLECTION, EventAvailability.AVAILABLE).build();
	
	private static final ContentType<GarbageCollectionsInfo> GC_INFO_TYPE = UnitLookup.createSyntheticContentType("gcInfoType"); //$NON-NLS-1$
	
	public static final TypedResult<GarbageCollectionsInfo> GC_INFO = new TypedResult<>("gcInfo", GarbageCollectionsInfo.GC_INFO_AGGREGATOR, GC_INFO_TYPE, GarbageCollectionsInfo.class); //$NON-NLS-1$
	
	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(GC_INFO);
	
	@Override
	public String getId() {
		return "GarbageCollectionInfoRule";
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.GARBAGE_COLLECTION_TOPIC;
	}

	@Override
	public String getName() {
		return "Garbage Collection Info";
	}

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return REQUIRED_EVENTS;
	}

	@Override
	public RunnableFuture<IResult> createEvaluation(final IItemCollection items, final IPreferenceValueProvider preferenceValueProvider, final IResultValueProvider dependencyResults) {
		FutureTask<IResult> evaluationTask = new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				GarbageCollectionsInfo aggregate = items.getAggregate(GarbageCollectionsInfo.GC_INFO_AGGREGATOR);
				if (aggregate.foundNonRequestedSerialOldGc() || aggregate.getGcCount() > 0 || aggregate.getGcLockers() > 0 || aggregate.getObjectCountGCs() > 0 || aggregate.getSystemGcCount() > 0) {
					return ResultBuilder.createFor(GarbageCollectionInfoRule.this, preferenceValueProvider)
							.setSeverity(Severity.OK)
							.addResult(GC_INFO, aggregate)
							.build();
				}
				return ResultBuilder.createFor(GarbageCollectionInfoRule.this, preferenceValueProvider)
						.setSeverity(Severity.NA)
						.build();
			}
		});
		return evaluationTask;
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
