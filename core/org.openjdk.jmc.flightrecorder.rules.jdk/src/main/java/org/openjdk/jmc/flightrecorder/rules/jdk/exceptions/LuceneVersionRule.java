package org.openjdk.jmc.flightrecorder.rules.jdk.exceptions;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule2;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

public class LuceneVersionRule implements IRule2 {

	private static final String RESULT_ID = "LuceneVersion"; //$NON-NLS-1$

	private enum LuceneConsumer {
		LUCENE, SOLR, ELASTIC_SEARCH;
	}

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.ERRORS_THROWN, EventAvailability.ENABLED)
			.addEventType(JdkTypeIDs.EXCEPTIONS_THROWN, EventAvailability.ENABLED)
			.build();
	
	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(TypedResult.SCORE);
	
	@Override
	public RunnableFuture<IResult> createEvaluation(final IItemCollection items, final IPreferenceValueProvider valueProvider, final IResultValueProvider resultProvider) {
		return new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				return getResult(items, valueProvider, resultProvider);
			}
		});
	}

	private static final String LOOKAHEAD_SUCCESS_NAME = "org.apache.lucene.queryparser.classic.QueryParser$LookaheadSuccess"; //$NON-NLS-1$

	private IResult getResult(IItemCollection items, IPreferenceValueProvider vp, IResultValueProvider rp) {
		IItemCollection throwables = items.apply(JdkFilters.THROWABLES)
				.apply(ItemFilters.equals(JdkAttributes.EXCEPTION_THROWNCLASS_NAME, LOOKAHEAD_SUCCESS_NAME));
		IQuantity lookaheadSuccessErrors = throwables.getAggregate(Aggregators.count());
		LuceneConsumer consumerType = isElasticSearch(throwables);
		// Lucene post 7.1.0 still creates a LookaheadSuccess error, but only on class load
		if (lookaheadSuccessErrors.longValue() > 1) {
			double score = RulesToolkit.mapExp100(lookaheadSuccessErrors.longValue(), 2, 20);
			ResultBuilder builder = ResultBuilder.createFor(this, vp)
					.setSeverity(Severity.get(score))
					.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score));
			switch (consumerType) {
			case ELASTIC_SEARCH:
				return builder.setSummary(Messages.getString(Messages.LuceneVersionRule_SHORT_DESCRIPTION_ES))
						.setExplanation(Messages.getString(Messages.LuceneVersionRule_LONG_DESCRIPTION_ES))
						.build();
			case SOLR:
				return builder.setSummary(Messages.getString(Messages.LuceneVersionRule_SHORT_DESCRIPTION_SOLR))
						.setExplanation(Messages.getString(Messages.LuceneVersionRule_LONG_DESCRIPTION_SOLR))
						.build();
			default:
				return builder.setSummary(Messages.getString(Messages.LuceneVersionRule_SHORT_DESCRIPTION_LUCENE))
						.setExplanation(Messages.getString(Messages.LuceneVersionRule_LONG_DESCRIPTION_LUCENE))
						.build();
			}
		} else if (lookaheadSuccessErrors.longValue() == 1) {
			ResultBuilder builder = ResultBuilder.createFor(this, vp)
					.setSeverity(Severity.OK);
			switch (consumerType) {
			case ELASTIC_SEARCH:
				return builder.setSummary(Messages.getString(Messages.LuceneVersionRule_OK_TEXT_ES)).build();
			case SOLR:
				return builder.setSummary(Messages.getString(Messages.LuceneVersionRule_OK_TEXT_SOLR)).build();
			default:
				return builder.setSummary(Messages.getString(Messages.LuceneVersionRule_OK_TEXT_LUCENE)).build();
			}
		}
		return ResultBuilder.createFor(this, vp)
				.setSeverity(Severity.NA)
				.setSummary(Messages.getString(Messages.LuceneVersionRule_NA_TEXT))
				.build();
	}

	private LuceneConsumer isElasticSearch(IItemCollection items) {
		for (IItemIterable itemIterable : items) {
			IMemberAccessor<IMCStackTrace, IItem> stacktraceAccessor = JfrAttributes.EVENT_STACKTRACE
					.getAccessor(itemIterable.getType());
			for (IItem item : itemIterable) {
				IMCStackTrace member = stacktraceAccessor.getMember(item);
				for (IMCFrame frame : member.getFrames()) {
					IMCPackage aPackage = frame.getMethod().getType().getPackage();
					if (aPackage != null) {
						if (aPackage.getName().startsWith("org.elasticsearch")) { //$NON-NLS-1$
							return LuceneConsumer.ELASTIC_SEARCH;
						}
						if (aPackage.getName().startsWith("org.apache.solr")) { //$NON-NLS-1$
							return LuceneConsumer.SOLR;
						}
					}
				}

			}
		}
		return LuceneConsumer.LUCENE;
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.LuceneVersionRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.EXCEPTIONS_TOPIC;
	}

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return REQUIRED_EVENTS;
	}

	@Override
	public Collection<TypedResult<?>> getResults() {
		return RESULT_ATTRIBUTES;
	}

}
