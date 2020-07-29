package org.openjdk.jmc.flightrecorder.rules;

import java.util.Collection;

public interface IResult {

	Severity getSeverity();
	
	IRule2 getRule();
	
	String getSummary();
	
	String getExplanation();
	
	String getSolution();
	
	Collection<IRecordingSetting> suggestRecordingSettings();
	
	<T> T getResult(TypedResult<T> result);
}
