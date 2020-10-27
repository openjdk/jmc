package org.openjdk.jmc.flightrecorder.rules;

import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemQuery;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.rules.messages.internal.Messages;

public class TypedResult<T> {

	/**
	 * A constant to be used while transitioning the rules api from 1.0 to 2.0 to keep the old score
	 * values.
	 */
	public static final TypedResult<IQuantity> SCORE = new TypedResult<>("score", //$NON-NLS-1$
			Messages.getString(Messages.TypedResult_SCORE_NAME),
			Messages.getString(Messages.TypedResult_SCORE_DESCRIPTION), UnitLookup.NUMBER, IQuantity.class);

	public static final ContentType<IItemQuery> QUERY = UnitLookup.createSyntheticContentType("itemQuery"); //$NON-NLS-1$

	public static final TypedResult<IItemQuery> ITEM_QUERY = new TypedResult<>("itemQuery", "Item Query", //$NON-NLS-1$
			"Relevant items used to evaluate this rule.", QUERY, IItemQuery.class);

	private final String identifier;
	private final String name;
	private final String description;
	private final ContentType<T> contentType;
	private final Class<T> clazz;

	/**
	 * Creates an object describing a singular typed result value with all needed information.
	 * 
	 * @param identifier
	 *            result identifier
	 * @param name
	 *            result name
	 * @param description
	 *            a longer description of the result
	 * @param contentType
	 *            a contentType that can parse and format values
	 * @param clazz
	 *            the class of the typed result
	 */
	public TypedResult(String identifier, String name, String description, ContentType<T> contentType, Class<T> clazz) {
		this.identifier = identifier;
		this.name = name;
		this.description = description;
		this.contentType = contentType;
		this.clazz = clazz;
	}

	public TypedResult(String identifier, String name, String description, ContentType<T> contentType) {
		this.identifier = identifier;
		this.name = name;
		this.description = description;
		this.contentType = contentType;
		this.clazz = null;
	}

	public TypedResult(String identifier, IAggregator<T, ?> aggregator, ContentType<T> contentType, Class<T> clazz) {
		this.identifier = identifier;
		name = aggregator.getName();
		description = aggregator.getDescription();
		this.contentType = contentType;
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
	 * Get the {@link ContentType} for the result.
	 *
	 * @return value contentType
	 */
	public ContentType<T> getPersister() {
		return contentType;
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
