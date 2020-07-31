package org.openjdk.jmc.flightrecorder.rules;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public interface IRule2 {

	String getId();
	
	String getTopic();
	
	String getName();
	
	Map<String, EventAvailability> getRequiredEvents();
	
	RunnableFuture<IResult> createEvaluation(final IItemCollection items, final IPreferenceValueProvider preferenceValueProvider, final IResultValueProvider dependencyResults);
	
	Collection<TypedPreference<?>> getConfigurationAttributes();
	
	Collection<TypedResult<?>> getResults();
}
