package org.openjdk.jmc.flightrecorder.rules;

import java.util.Collection;

public interface IResultValueProvider {

	/**
	 * Get the value of a result.
	 *
	 * @param result
	 *            result to get the value for
	 * @return the result value
	 */
	<T> T getResultValue(TypedResult<T> result);
	
	<T> Collection<T> getResultValue(TypedCollectionResult<T> result);

	TypedResult<?> getResultByIdentifier(String identifier);
}
