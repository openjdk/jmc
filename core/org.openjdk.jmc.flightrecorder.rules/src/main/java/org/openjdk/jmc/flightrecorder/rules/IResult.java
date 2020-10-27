package org.openjdk.jmc.flightrecorder.rules;

import java.util.Collection;

import org.openjdk.jmc.common.util.TypedPreference;

public interface IResult {

	Severity getSeverity();

	IRule getRule();

	String getSummary();

	String getExplanation();

	String getSolution();

	Collection<IRecordingSetting> suggestRecordingSettings();

	<T> Collection<T> getResult(TypedCollectionResult<T> result);

	<T> T getPreference(TypedPreference<T> preference);

	<T> T getResult(TypedResult<T> result);
}
