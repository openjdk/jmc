package org.openjdk.jmc.flightrecorder.writer.api;

import java.util.List;

public interface TypedValue {
	/** @return the type */
	Type getType();

	/** @return the wrapped value */
	Object getValue();

	/** @return {@literal true} if this holds {@literal null} value */
	boolean isNull();

	/** @return the field values structure */
	List<? extends TypedFieldValue> getFieldValues();
}
