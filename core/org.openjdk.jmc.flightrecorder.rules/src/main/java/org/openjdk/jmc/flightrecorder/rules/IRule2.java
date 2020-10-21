package org.openjdk.jmc.flightrecorder.rules;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

/**
 * Rules are used for analyzing flight recordings and creating results that can inform a user about
 * problems.
 * <p>
 * The key method to implement is
 * {@link IRule#createEvaluation(IItemCollection, IPreferenceValueProvider, IResultValueProvider)}.
 * {@link IRule#getId()} must return an id that is unique for each implementation and
 * {@link IRule#getName()} should return a human readable name.
 * <p>
 * Rule instances may be reused for multiple evaluations with different input data so it is strongly
 * recommended that implementations be stateless.
 */
public interface IRule2 {

	/**
	 * @return a unique id for this rule implementation
	 */
	String getId();

	/**
	 * @return the topic for this rule, may be {@code null}
	 */
	String getTopic();

	/**
	 * @return a human readable name for this rule
	 */
	String getName();

	/**
	 * @return a map of all event type IDs and the minimum {@link EventAvailability} this rule
	 *         requires for that event type
	 */
	Map<String, EventAvailability> getRequiredEvents();

	/**
	 * Gets a future representing the result of the evaluation of this rule. Running the
	 * RunnableFuture is the responsibility of the caller of this method, not the implementation.
	 *
	 * @param items
	 *            items to evaluate
	 * @param valueProvider
	 *            Provider of configuration values used for evaluation. The attributes that will be
	 *            asked for from the provider should be provided by
	 *            {@link IRule#getConfigurationAttributes()}.
	 * @param dependencyResults
	 *            Provider of results from rules evaluated prior to this rule and which this rule
	 *            explicitly depends on via a {@link DependsOn} annotation. The attributes that will
	 *            be asked for from the provider will be provided by each dependant rule, e.g. via
	 *            public static constants.
	 * @return a RunnableFuture that when run will return the evaluation result
	 */
	RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider preferenceValueProvider,
		final IResultValueProvider dependencyResults);

	/**
	 * Gets information about which attributes may be configured during rule evaluation.
	 *
	 * @return a list of configuration attributes
	 */
	Collection<TypedPreference<?>> getConfigurationAttributes();

	/**
	 * Gets information about which results may be part of an {@link IResult} instance.
	 *
	 * @return a list of result attributes
	 */
	Collection<TypedResult<?>> getResults();
}
