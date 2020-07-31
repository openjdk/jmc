package org.openjdk.jmc.flightrecorder.rules;

public interface IResultValueProvider {

	/**
	 * Get the value of a result.
	 *
	 * @param result
	 *            result to get the value for
	 * @return the result value
	 */
	<T> T getResultValue(TypedResult<T> result);
	
	<T> T getResultByIdentifier(String identifier);
}
