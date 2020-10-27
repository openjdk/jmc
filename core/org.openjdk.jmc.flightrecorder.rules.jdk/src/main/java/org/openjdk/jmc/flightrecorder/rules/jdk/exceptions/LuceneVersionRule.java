package org.openjdk.jmc.flightrecorder.rules.jdk.exceptions;

import java.util.Collection;
import java.util.Collections;
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
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;

public class LuceneVersionRule implements IRule {

	private static final String RESULT_ID = "LuceneVersion"; //$NON-NLS-1$

	private enum LuceneConsumer {
		LUCENE, SOLR, ELASTIC_SEARCH;
	}

	@Override
	public RunnableFuture<Result> evaluate(final IItemCollection items, IPreferenceValueProvider valueProvider) {
		return new FutureTask<>(new Callable<Result>() {
			@Override
			public Result call() throws Exception {
				return getResult(items);
			}
		});
	}

	private static final String LOOKAHEAD_SUCCESS_NAME = "org.apache.lucene.queryparser.classic.QueryParser$LookaheadSuccess"; //$NON-NLS-1$

	private Result getResult(IItemCollection items) {
		IItemCollection throwables = items.apply(JdkFilters.THROWABLES)
				.apply(ItemFilters.equals(JdkAttributes.EXCEPTION_THROWNCLASS_NAME, LOOKAHEAD_SUCCESS_NAME));
		IQuantity lookaheadSuccessErrors = throwables.getAggregate(Aggregators.count());
		LuceneConsumer consumerType = isElasticSearch(throwables);
		// Lucene post 7.1.0 still creates a LookaheadSuccess error, but only on class load
		if (lookaheadSuccessErrors.longValue() > 1) {
			double score = RulesToolkit.mapExp100(lookaheadSuccessErrors.longValue(), 2, 20);
			switch (consumerType) {
			case ELASTIC_SEARCH:
				return new Result(this, score, Messages.getString(Messages.LuceneVersionRule_SHORT_DESCRIPTION_ES),
						Messages.getString(Messages.LuceneVersionRule_LONG_DESCRIPTION_ES));
			case SOLR:
				return new Result(this, score, Messages.getString(Messages.LuceneVersionRule_SHORT_DESCRIPTION_SOLR),
						Messages.getString(Messages.LuceneVersionRule_LONG_DESCRIPTION_SOLR));
			default:
				return new Result(this, score, Messages.getString(Messages.LuceneVersionRule_SHORT_DESCRIPTION_LUCENE),
						Messages.getString(Messages.LuceneVersionRule_LONG_DESCRIPTION_LUCENE));
			}
		} else if (lookaheadSuccessErrors.longValue() == 1) {
			switch (consumerType) {
			case ELASTIC_SEARCH:
				return new Result(this, 0, Messages.getString(Messages.LuceneVersionRule_OK_TEXT_ES));
			case SOLR:
				return new Result(this, 0, Messages.getString(Messages.LuceneVersionRule_OK_TEXT_SOLR));
			default:
				return new Result(this, 0, Messages.getString(Messages.LuceneVersionRule_OK_TEXT_LUCENE));
			}
		}
		return RulesToolkit.getNotApplicableResult(this, Messages.getString(Messages.LuceneVersionRule_NA_TEXT));
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
		return JfrRuleTopics.EXCEPTIONS;
	}

}
