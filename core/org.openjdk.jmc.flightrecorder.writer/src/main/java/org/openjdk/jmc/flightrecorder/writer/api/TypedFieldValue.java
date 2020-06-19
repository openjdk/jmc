package org.openjdk.jmc.flightrecorder.writer.api;

import org.openjdk.jmc.flightrecorder.writer.TypedFieldImpl;

/**
 * The composite of {@linkplain TypedFieldImpl} and corresponding {@link TypedValue TypedValue(s)}
 */
public interface TypedFieldValue {
	/** @return the corresponding {@linkplain TypedFieldImpl} */
	TypedFieldImpl getField();

	/**
	 * @return the associated value
	 * @throws IllegalArgumentException
	 *             if the field is an array
	 */
	TypedValue getValue();

	/**
	 * @return the associated values
	 * @throws IllegalArgumentException
	 *             if the field is not an array
	 */
	TypedValue[] getValues();
}
