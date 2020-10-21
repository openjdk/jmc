package org.openjdk.jmc.flightrecorder.rules;

import org.openjdk.jmc.common.unit.ContentType;

public class TypedCollectionResult<T> extends TypedResult<T> {

	public TypedCollectionResult(String identifier, String name, String description, ContentType<T> persister,
			Class<T> clazz) {
		super(identifier, name, description, persister, clazz);
	}

}
