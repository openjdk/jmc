package org.openjdk.jmc.flightrecorder.rules;

import org.openjdk.jmc.common.unit.IPersister;

public class TypedResult<T> {
	
	private final String identifier;
	private final String name;
	private final String description;
	private final IPersister<T> persister;
	private final Class<T> clazz;
	
	/**
	 * @param identifier
	 *            result identifier
	 * @param name
	 *            result name
	 * @param description
	 *            a longer description of the result
	 * @param persister
	 *            a persister that can parse and format values
	 */
	public TypedResult(String identifier, String name, String description, IPersister<T> persister, Class<T> clazz) {
		this.identifier = identifier;
		this.name = name;
		this.description = description;
		this.persister = persister;
		this.clazz = clazz;
	}
	
	public Class<T> getResultClass() {
		return clazz;
	}
	
	/**
	 * @return result identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * @return result name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return result description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get a persister that can be used to convert between the preference value type and strings.
	 *
	 * @return value persister
	 */
	public IPersister<T> getPersister() {
		return persister;
	}

	@Override
	public int hashCode() {
		return identifier.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof TypedResult<?>) {
			return ((TypedResult<?>) o).identifier.equals(this.identifier);
		}
		return false;
	}

	@Override
	public String toString() {
		return "[" + identifier + ", " + description + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
