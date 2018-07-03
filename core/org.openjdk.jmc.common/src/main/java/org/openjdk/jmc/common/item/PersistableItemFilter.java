/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmc.common.item;

import static org.openjdk.jmc.common.item.Attribute.attr;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IStateful;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IPersister;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.RangeContentType;
import org.openjdk.jmc.common.unit.UnitLookup;

/**
 * Simple provisional persistence implementation for item filter. It has a few issues of various
 * importance:
 * <ul>
 * <li>Like the current filter variants, it is non-canonical: MEMBER_OF can be rewritten as an OR of
 * EQUALs, and similarly with the TYPE variants. INTERVAL can be expressed as an AND of LESS and
 * MORE. (Apart from the API omission that closedness cannot by expressed in the latter two.)
 * <li>The current XML structure isn't as simple as it could be, and not easily verifiable by a
 * simple schema.
 * <li>Delegation responsibilities to sub-filters and content types for persisting and parsing
 * aren't very clear.
 * <li>Behavior with unpersistable sub-filters and content types is somewhat unspecified.
 * <li>Content types for values use the internal legacy ad-hoc identifiers. It would be better to
 * use well specified (standardized) expressions where possible (units/kind-of-quantities, Java
 * classes, as in "JMX 2.0").
 * </ul>
 */
public abstract class PersistableItemFilter implements IItemFilter, IStateful {
	public enum Kind {
		AND,
		OR,
		NOT,
		EQUALS,
		NOT_EQUALS(EQUALS),
		MATCHES,
		NOT_MATCHES(MATCHES),
		CONTAINS,
		NOT_CONTAINS(CONTAINS),
		LESS,
		LESS_OR_EQUAL,
		MORE(LESS_OR_EQUAL),
		MORE_OR_EQUAL(LESS),
		RANGE_INTERSECTS,
		RANGE_NOT_INTERSECTS(RANGE_INTERSECTS),
		RANGE_CONTAINED,
		RANGE_NOT_CONTAINED(RANGE_CONTAINED),
		CENTER_CONTAINED,
		CENTER_NOT_CONTAINED(CENTER_CONTAINED),
		TYPE,
		TYPE_MATCHES,
		EXISTS,
		NOT_EXISTS(EXISTS),
		IS_NULL,
		IS_NOT_NULL(IS_NULL);

		private Kind negatedKind;

		private Kind() {
			negatedKind = null;
		}

		private Kind(Kind neg) {
			assert neg.negatedKind == null;
			negatedKind = neg;
			neg.negatedKind = this;
		}

		/**
		 * Return the exact negation of this kind of filter, if such a kind is defined. Otherwise,
		 * return {@code null}.
		 *
		 * @return the negation of this kind of filter or {@code null}
		 */
		public Kind negate() {
			return negatedKind;
		}
	};

	private static final String KEY_KIND = "kind"; //$NON-NLS-1$
	static final String KEY_FILTER = "filter"; //$NON-NLS-1$
	// FIXME: Rename "field" identifier which is now an attribute id
	static final String KEY_FIELD = "field"; //$NON-NLS-1$
	// FIXME: Differentiate between "value type", that needs to be looked up, and others.
	static final String KEY_TYPE = "type"; //$NON-NLS-1$
	static final String KEY_TYPE_MATCHES = "typeMatches"; //$NON-NLS-1$
	static final String KEY_VALUE = "value"; //$NON-NLS-1$
	static final String KEY_START = "start"; //$NON-NLS-1$
	static final String KEY_END = "end"; //$NON-NLS-1$

	protected final Kind kind;

	protected PersistableItemFilter(Kind kind) {
		this.kind = kind;
	}

	@Override
	public final void saveTo(IWritableState memento) {
		memento.putString(KEY_KIND, kind.name());
		saveArgs(memento);
	}

	protected abstract void saveArgs(IWritableState memento);

	protected static void putValueType(IWritableState memento, ContentType<?> contentType) {
		memento.putString(KEY_TYPE, contentType.getIdentifier());
	}

	public static IItemFilter readFrom(IState memento) {
		Kind kind = Kind.valueOf(memento.getAttribute(KEY_KIND));
		if (kind == null) {
			return null;
		}
		switch (kind) {
		case AND:
			return ItemFilters.and(readFrom(memento.getChildren(KEY_FILTER)));
		case OR:
			return ItemFilters.or(readFrom(memento.getChildren(KEY_FILTER)));
		case NOT:
			return ItemFilters.not(readFrom(memento.getChildren(KEY_FILTER)[0]));
		case MATCHES:
			return ItemFilters.matches(readStringAttribute(memento), memento.getAttribute(KEY_VALUE));
		case NOT_MATCHES:
			return ItemFilters.notMatches(readStringAttribute(memento), memento.getAttribute(KEY_VALUE));
		case CONTAINS:
			return ItemFilters.contains(readStringAttribute(memento), memento.getAttribute(KEY_VALUE));
		case NOT_CONTAINS:
			return ItemFilters.notContains(readStringAttribute(memento), memento.getAttribute(KEY_VALUE));
		case EQUALS:
			return readEquals(readAttribute(memento), memento);
		case NOT_EQUALS:
			return readNotEquals(readAttribute(memento), memento);
		case LESS:
		case LESS_OR_EQUAL:
		case MORE:
		case MORE_OR_EQUAL:
			return readComparableKindFrom(kind, memento);
		case RANGE_INTERSECTS:
		case RANGE_CONTAINED:
		case CENTER_CONTAINED:
		case RANGE_NOT_INTERSECTS:
		case RANGE_NOT_CONTAINED:
		case CENTER_NOT_CONTAINED:
			return readRangeMatchesFrom(kind, memento);
		case EXISTS:
			return ItemFilters.hasAttribute(readAttribute(memento));
		case NOT_EXISTS:
			return ItemFilters.notHasAttribute(readAttribute(memento));
		case TYPE:
			return ItemFilters.type(memento.getAttribute(KEY_TYPE));
		case TYPE_MATCHES:
			return ItemFilters.typeMatches(memento.getAttribute(KEY_TYPE_MATCHES));
		case IS_NULL:
			return ItemFilters.isNull(readAttribute(memento));
		case IS_NOT_NULL:
			return ItemFilters.isNotNull(readAttribute(memento));
		default:
			return null;
		}
	}

	private static <M> IItemFilter readEquals(ICanonicalAccessorFactory<M> attribute, IState memento) {
		return ItemFilters.equals(attribute, readValue(attribute.getContentType().getPersister(), memento));
	}

	private static <M> IItemFilter readNotEquals(ICanonicalAccessorFactory<M> attribute, IState memento) {
		return ItemFilters.notEquals(attribute, readValue(attribute.getContentType().getPersister(), memento));
	}

	/*
	 * Method extracted from readFrom() in order to bind M. In some cases the Eclipse compiler
	 * didn't manage to track the generic types when readComparableAttribute was called as an
	 * argument to the readComparableKindFrom call.
	 */
	private static <M extends Comparable<? super M>> IItemFilter readComparableKindFrom(Kind kind, IState memento) {
		ICanonicalAccessorFactory<M> attr = readComparableAttribute(memento);
		return readComparableKindFrom(attr, kind, memento);
	}

	private static <M extends Comparable<? super M>> IItemFilter readComparableKindFrom(
		ICanonicalAccessorFactory<M> attribute, Kind kind, IState memento) {
		M value = readValue(attribute.getContentType().getPersister(), memento);
		switch (kind) {
		case LESS:
			return ItemFilters.less(attribute, value);
		case LESS_OR_EQUAL:
			return ItemFilters.lessOrEqual(attribute, value);
		case MORE:
			return ItemFilters.more(attribute, value);
		case MORE_OR_EQUAL:
			return ItemFilters.moreOrEqual(attribute, value);
		default:
			throw new IllegalArgumentException(
					"Only to be called with LESS, LESS_OR_EQUAL, MORE or MORE_OR_EQUAL kind."); //$NON-NLS-1$
		}
	}

	private static <M extends Comparable<? super M>> IItemFilter readRangeMatchesFrom(Kind kind, IState memento) {
		ContentType<M> type = readComparableType(memento);
		RangeContentType<M> rangeType = UnitLookup.getRangeType(type);
		ICanonicalAccessorFactory<IRange<M>> attr = attr(memento.getAttribute(KEY_FIELD), rangeType);
		M start = readValue(type.getPersister(), memento, KEY_START);
		M end = readValue(type.getPersister(), memento, KEY_END);
		return ItemFilters.matchRange(kind, attr, rangeType.rangeWithEnd(start, end));
	}

	static <M> M readValue(IPersister<M> persister, IState from) {
		return readValue(persister, from, KEY_VALUE);
	}

	static <M> M readValue(IPersister<M> persister, IState from, String key) {
		String persistedValue = from.getAttribute(key);
		if (persistedValue != null) {
			try {
				return persister.parsePersisted(persistedValue);
			} catch (QuantityConversionException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	static <M> void writeValue(M value, IPersister<M> persister, IWritableState to) {
		writeValue(value, persister, to, KEY_VALUE);
	}

	static <M> void writeValue(M value, IPersister<M> persister, IWritableState to, String key) {
		if (value != null) {
			to.putString(key, persister.persistableString(value));
		}
	}

	private static IItemFilter[] readFrom(IState[] mementos) {
		IItemFilter[] filters = new IItemFilter[mementos.length];
		for (int i = 0; i < mementos.length; i++) {
			filters[i] = readFrom(mementos[i]);
		}
		return filters;
	}

	@SuppressWarnings("unchecked")
	private static ICanonicalAccessorFactory<String> readStringAttribute(IState memento) {
		return (ICanonicalAccessorFactory<String>) readAttribute(memento);
	}

	@SuppressWarnings("unchecked")
	private static <M extends Comparable<? super M>> ContentType<M> readComparableType(IState memento) {
		return (ContentType<M>) UnitLookup.getContentType(memento.getAttribute(KEY_TYPE));
	}

	private static ICanonicalAccessorFactory<?> readAttribute(IState memento) {
		return createAttribute(memento.getAttribute(KEY_FIELD),
				UnitLookup.getContentType(memento.getAttribute(KEY_TYPE)));
	}

	private static <M extends Comparable<? super M>> ICanonicalAccessorFactory<M> readComparableAttribute(
		IState memento) {
		ContentType<M> type = readComparableType(memento);
		return createAttribute(memento.getAttribute(KEY_FIELD), type);
	}

	private static <M> ICanonicalAccessorFactory<M> createAttribute(String id, ContentType<M> type) {
		return attr(id, type);
	}

	@Override
	public String toString() {
		return String.valueOf(kind);
	}

	public String toString(String argumentName, Object value) {
		return String.format(" %s=%s", argumentName, String.valueOf(value)); //$NON-NLS-1$
	}
}
