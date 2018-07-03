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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.IPredicate;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.PersistableItemFilter.Kind;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.RangeContentType;
import org.openjdk.jmc.common.util.PredicateToolkit;

/**
 * Methods for creating item filters.
 */
public class ItemFilters {

	public static class Not extends PersistableItemFilter {
		private final IItemFilter filter;

		Not(IItemFilter filter) {
			super(Kind.NOT);
			this.filter = filter;
		}

		public IItemFilter getFilter() {
			return filter;
		}

		@Override
		protected void saveArgs(IWritableState memento) {
			if (filter instanceof PersistableItemFilter) {
				PersistableItemFilter persistable = (PersistableItemFilter) filter;
				persistable.saveTo(memento.createChild(KEY_FILTER));
			}
			// FIXME: Else warn?
		}

		@Override
		public IPredicate<IItem> getPredicate(IType<IItem> type) {
			return PredicateToolkit.not(filter.getPredicate(type));
		}
	}

	public static class Composite extends PersistableItemFilter {
		private final IItemFilter[] filters;

		Composite(Kind kind, IItemFilter[] filters) {
			super(kind);
			this.filters = filters;
		}

		public boolean isUnion() {
			return kind == Kind.OR;
		}

		@Override
		protected void saveArgs(IWritableState memento) {
			for (IItemFilter filter : filters) {
				if (filter instanceof PersistableItemFilter) {
					PersistableItemFilter persistable = (PersistableItemFilter) filter;
					persistable.saveTo(memento.createChild(KEY_FILTER));
				}
				// FIXME: Else warn?
			}
		}

		public IItemFilter[] getFilters() {
			return filters;
		}

		@Override
		public IPredicate<IItem> getPredicate(IType<IItem> type) {
			List<IPredicate<IItem>> predicates = new ArrayList<>(filters.length);
			for (IItemFilter f : filters) {
				predicates.add(f.getPredicate(type));
			}
			return kind == Kind.OR ? PredicateToolkit.or(predicates) : PredicateToolkit.and(predicates);
		}
	}

	private static class MemberOf<M> extends Composite {
		private final IAccessorFactory<M> attribute;
		private final Set<M> values;

		MemberOf(IItemFilter[] filters, IAccessorFactory<M> attribute, Set<M> values) {
			super(Kind.OR, filters);
			this.attribute = attribute;
			this.values = values;
		}

		@Override
		public IPredicate<IItem> getPredicate(IType<IItem> type) {
			IMemberAccessor<? extends M, IItem> accessor = attribute.getAccessor(type);
			if (accessor != null) {
				return PredicateToolkit.memberOf(accessor, values);
			}
			return PredicateToolkit.falsePredicate();
		}
	}

	public static class Types extends Composite {
		private final Set<String> types;

		Types(IItemFilter[] filters, Set<String> types) {
			super(Kind.OR, filters);
			this.types = types;
		}

		@Override
		public IPredicate<IItem> getPredicate(IType<IItem> type) {
			if (types.contains(type.getIdentifier())) {
				return PredicateToolkit.truePredicate();
			}
			return PredicateToolkit.falsePredicate();
		}

		public Set<String> getTypes() {
			return types;
		}
	}

	public static class Type extends PersistableItemFilter {
		private final String typeId;

		Type(String typeId) {
			super(Kind.TYPE);
			this.typeId = typeId;
		}

		public String getTypeId() {
			return typeId;
		}

		@Override
		protected void saveArgs(IWritableState memento) {
			memento.putString(KEY_TYPE, typeId);
		}

		@Override
		public IPredicate<IItem> getPredicate(IType<IItem> type) {
			if (typeId.equals(type.getIdentifier())) {
				return PredicateToolkit.truePredicate();
			} else {
				return PredicateToolkit.falsePredicate();
			}
		}
	}

	public static class TypeMatches extends PersistableItemFilter {
		private String typeMatchString;

		// FIXME: Should we require type matches to be a complete regexp, or should we look for any internal matches, using find?
		TypeMatches(String typeMatchString) {
			super(Kind.TYPE_MATCHES);
			this.typeMatchString = typeMatchString;
		}

		public String getTypeMatch() {
			return typeMatchString;
		}

		@Override
		protected void saveArgs(IWritableState memento) {
			memento.putString(KEY_TYPE_MATCHES, typeMatchString);
		}

		@Override
		public IPredicate<IItem> getPredicate(IType<IItem> type) {
			Pattern typeMatch = PredicateToolkit.getValidPattern(typeMatchString);
			if (typeMatch.matcher(type.getIdentifier()).matches()) {
				return PredicateToolkit.truePredicate();
			} else {
				return PredicateToolkit.falsePredicate();
			}
		}

		@Override
		public String toString() {
			return super.toString() + toString("typeMatchString", typeMatchString); //$NON-NLS-1$
		}
	}

	public static abstract class AttributeFilter<M> extends PersistableItemFilter {

		protected final ICanonicalAccessorFactory<M> attribute;

		protected AttributeFilter(Kind kind, ICanonicalAccessorFactory<M> attribute) {
			super(kind);
			this.attribute = attribute;
		}

		@Override
		protected void saveArgs(IWritableState memento) {
			memento.putString(KEY_FIELD, attribute.getIdentifier());
			putValueType(memento, attribute.getContentType());
		}

		public ICanonicalAccessorFactory<M> getAttribute() {
			return attribute;
		}

		public Kind getKind() {
			return kind;
		}

		@Override
		public String toString() {
			return super.toString() + toString("attribute", attribute); //$NON-NLS-1$
		}
	}

	public static abstract class AttributeValue<M> extends AttributeFilter<M> {
		protected final M value;

		private AttributeValue(Kind kind, ICanonicalAccessorFactory<M> attribute, M value) {
			super(kind, attribute);
			this.value = value;
			// FIXME: We need to decide if we should allow content types that are not persistable
//			if (attribute.getContentType().getPersister() == null) {
//				throw new IllegalArgumentException(attribute.getContentType() + " is not persistable");
//			}
		}

		public M getValue() {
			return value;
		}

		@Override
		protected void saveArgs(IWritableState memento) {
			super.saveArgs(memento);
			writeValue(value, attribute.getContentType().getPersister(), memento);
		}

		@Override
		public IPredicate<IItem> getPredicate(IType<IItem> type) {
			IMemberAccessor<? extends M, IItem> accessor = attribute.getAccessor(type);
			if (accessor != null) {
				return getPredicate(accessor, value);
			}
			return PredicateToolkit.falsePredicate();
		}

		protected abstract IPredicate<IItem> getPredicate(IMemberAccessor<? extends M, IItem> accessor, M value);

		@Override
		public String toString() {
			return super.toString() + toString("value", String.valueOf(value)); //$NON-NLS-1$
		}
	}

	public static class HasAttribute<M> extends AttributeFilter<M> {
		public HasAttribute(ICanonicalAccessorFactory<M> attribute) {
			super(Kind.EXISTS, attribute);
		}

		@Override
		public IPredicate<IItem> getPredicate(IType<IItem> type) {
			if (attribute.getAccessor(type) != null) {
				return PredicateToolkit.truePredicate();
			}
			return PredicateToolkit.falsePredicate();
		}
	}

	public static class NotHasAttribute<M> extends AttributeFilter<M> {
		public NotHasAttribute(ICanonicalAccessorFactory<M> attribute) {
			super(Kind.NOT_EXISTS, attribute);
		}

		@Override
		public IPredicate<IItem> getPredicate(IType<IItem> type) {
			if (attribute.getAccessor(type) == null) {
				return PredicateToolkit.truePredicate();
			}
			return PredicateToolkit.falsePredicate();
		}
	}

	private static class Equals<M> extends AttributeValue<M> {
		private Equals(ICanonicalAccessorFactory<M> attribute, M value) {
			super(Kind.EQUALS, attribute, value);
		}

		@Override
		protected IPredicate<IItem> getPredicate(IMemberAccessor<? extends M, IItem> accessor, M value) {
			return PredicateToolkit.equals(accessor, value);
		}
	}

	private static class NotEquals<M> extends AttributeValue<M> {
		private NotEquals(ICanonicalAccessorFactory<M> attribute, M value) {
			super(Kind.NOT_EQUALS, attribute, value);
		}

		@Override
		protected IPredicate<IItem> getPredicate(IMemberAccessor<? extends M, IItem> accessor, M value) {
			return PredicateToolkit.notEquals(accessor, value);
		}
	}

	private static class IsNull<M> extends AttributeFilter<M> {
		private IsNull(ICanonicalAccessorFactory<M> attribute) {
			super(Kind.IS_NULL, attribute);
		}

		@Override
		public IPredicate<IItem> getPredicate(IType<IItem> type) {
			IMemberAccessor<M, IItem> accessor = attribute.getAccessor(type);
			if (accessor != null) {
				return PredicateToolkit.equals(accessor, null);
			}
			return PredicateToolkit.truePredicate();
		}
	}

	private static class IsNotNull<M> extends AttributeFilter<M> {
		private IsNotNull(ICanonicalAccessorFactory<M> attribute) {
			super(Kind.IS_NOT_NULL, attribute);
		}

		@Override
		public IPredicate<IItem> getPredicate(IType<IItem> type) {
			IMemberAccessor<M, IItem> accessor = attribute.getAccessor(type);
			if (accessor != null) {
				return PredicateToolkit.notEquals(accessor, null);
			}
			return PredicateToolkit.falsePredicate();
		}
	}

	private static class Compare<M extends Comparable<? super M>> extends AttributeValue<M> {
		private Compare(Kind kind, ICanonicalAccessorFactory<M> attribute, M limit) {
			super(kind, attribute, limit);
		}

		@Override
		protected IPredicate<IItem> getPredicate(IMemberAccessor<? extends M, IItem> accessor, M limit) {
			switch (kind) {
			case MORE:
				return PredicateToolkit.more(accessor, limit);
			case MORE_OR_EQUAL:
				return PredicateToolkit.moreOrEqual(accessor, limit);
			case LESS:
				return PredicateToolkit.less(accessor, limit);
			case LESS_OR_EQUAL:
				return PredicateToolkit.lessOrEqual(accessor, limit);
			default:
				throw new RuntimeException("Unknown kind " + kind); //$NON-NLS-1$
			}
		}
	}

	private static class RangeMatches<M extends Comparable<? super M>> extends AttributeValue<IRange<M>> {
		private RangeMatches(Kind kind, ICanonicalAccessorFactory<IRange<M>> attribute, IRange<M> limit) {
			super(kind, attribute, limit);
		}

		@Override
		protected IPredicate<IItem> getPredicate(
			IMemberAccessor<? extends IRange<M>, IItem> accessor, IRange<M> limit) {
			switch (kind) {
			case RANGE_INTERSECTS:
				return PredicateToolkit.rangeIntersects(accessor, limit);
			case RANGE_CONTAINED:
				return PredicateToolkit.rangeContained(accessor, limit);
			case CENTER_CONTAINED:
				return PredicateToolkit.centerContained(accessor, limit);
			case RANGE_NOT_INTERSECTS:
				return PredicateToolkit.not(PredicateToolkit.rangeIntersects(accessor, limit));
			case RANGE_NOT_CONTAINED:
				return PredicateToolkit.not(PredicateToolkit.rangeContained(accessor, limit));
			case CENTER_NOT_CONTAINED:
				return PredicateToolkit.not(PredicateToolkit.centerContained(accessor, limit));
			default:
				throw new RuntimeException("Unknown kind " + kind); //$NON-NLS-1$
			}
		}

		@Override
		protected void saveArgs(IWritableState memento) {
			memento.putString(KEY_FIELD, attribute.getIdentifier());
			ContentType<M> valueType = (((RangeContentType<M>) attribute.getContentType()).getEndPointContentType());
			putValueType(memento, valueType);
			writeValue(value.getStart(), valueType.getPersister(), memento, KEY_START);
			writeValue(value.getEnd(), valueType.getPersister(), memento, KEY_END);
		}
	}

	public static class Matches extends AttributeValue<String> {
		Matches(String regexp, ICanonicalAccessorFactory<String> attribute) {
			super(Kind.MATCHES, attribute, regexp);
		}

		@Override
		protected IPredicate<IItem> getPredicate(IMemberAccessor<? extends String, IItem> accessor, String regexp) {
			return PredicateToolkit.matches(accessor, regexp);
		}
	}

	public static class NotMatches extends AttributeValue<String> {
		NotMatches(String regexp, ICanonicalAccessorFactory<String> attribute) {
			super(Kind.NOT_MATCHES, attribute, regexp);
		}

		@Override
		protected IPredicate<IItem> getPredicate(IMemberAccessor<? extends String, IItem> accessor, String regexp) {
			return PredicateToolkit.not(PredicateToolkit.matches(accessor, regexp));
		}
	}

	public static class Contains extends AttributeValue<String> {
		Contains(String substring, ICanonicalAccessorFactory<String> attribute) {
			super(Kind.CONTAINS, attribute, substring);
		}

		@Override
		protected IPredicate<IItem> getPredicate(IMemberAccessor<? extends String, IItem> accessor, String substring) {
			return PredicateToolkit.contains(accessor, substring);
		}
	}

	public static class NotContains extends AttributeValue<String> {
		NotContains(String substring, ICanonicalAccessorFactory<String> attribute) {
			super(Kind.NOT_CONTAINS, attribute, substring);
		}

		@Override
		protected IPredicate<IItem> getPredicate(IMemberAccessor<? extends String, IItem> accessor, String substring) {
			return PredicateToolkit.not(PredicateToolkit.contains(accessor, substring));
		}
	}

	// FIXME: Perhaps make persistable (implement PersistableItemFilter)
	public static class BooleanFilter implements IItemFilter {
		public boolean value;

		protected BooleanFilter(boolean value) {
			this.value = value;
		}

		// Instance creation optimization
		private final static IPredicate<IItem> ALWAYSTRUE = new IPredicate<IItem>() {
			@Override
			public boolean evaluate(IItem o) {
				return true;
			}
		};

		private final static IPredicate<IItem> ALWAYSFALSE = new IPredicate<IItem>() {
			@Override
			public boolean evaluate(IItem o) {
				return false;
			}
		};

		@Override
		public IPredicate<IItem> getPredicate(IType<IItem> type) {
			return value ? ALWAYSTRUE : ALWAYSFALSE;
		}
	}

	public static IItemFilter type(String typeId) {
		return new Type(typeId);
	}

	public static IItemFilter type(String ... typeIds) {
		return type(new HashSet<>(Arrays.asList(typeIds)));
	}

	public static <V> IItemFilter hasAttribute(final ICanonicalAccessorFactory<V> attribute) {
		return new HasAttribute<>(attribute);
	}

	public static <V> IItemFilter notHasAttribute(final ICanonicalAccessorFactory<V> attribute) {
		return new NotHasAttribute<>(attribute);
	}

	public static IItemFilter type(Set<String> typeIds) {
		IItemFilter[] filters = new IItemFilter[typeIds.size()];
		int i = 0;
		for (String type : typeIds) {
			filters[i++] = type(type);
		}
		return new Types(filters, typeIds);
	}

	public static IItemFilter typeMatches(String typeMatches) {
		return new TypeMatches(typeMatches);
	}

	public static <M> IItemFilter equals(ICanonicalAccessorFactory<M> attribute, M value) {
		return new Equals<>(attribute, value);
	}

	public static <M> IItemFilter notEquals(ICanonicalAccessorFactory<M> attribute, M value) {
		return new NotEquals<>(attribute, value);
	}

	/**
	 * Creates an item filter that will return true if attribute value is null, or if the attribute
	 * doesn't exist, and false otherwise. The negated version of {@link #isNotNull}. Analogous to
	 * how java.util.Map.get(key) will return null both if the key is mapped to null, and if the key
	 * doesn't exist.
	 *
	 * @param <M>
	 *            attribute value type
	 * @param attribute
	 *            attribute to filter on
	 * @return a filter
	 */
	public static <M> IItemFilter isNull(ICanonicalAccessorFactory<M> attribute) {
		return new IsNull<>(attribute);
	}

	/**
	 * Creates an item filter that will return true if attribute value exists, and is not null. The
	 * negated version of {@link #isNull}
	 *
	 * @param <M>
	 *            attribute value type
	 * @param attribute
	 *            attribute to filter on
	 * @return a filter
	 */
	public static <M> IItemFilter isNotNull(ICanonicalAccessorFactory<M> attribute) {
		return new IsNotNull<>(attribute);
	}

	/**
	 * @return returns a filter matching everything.
	 */
	public static IItemFilter all() {
		return new BooleanFilter(true);
	}

	/**
	 * @return returns a filter matching nothing.
	 */
	public static IItemFilter none() {
		return new BooleanFilter(false);
	}

	public static IItemFilter matches(ICanonicalAccessorFactory<String> attribute, String regexp) {
		return new Matches(regexp, attribute);
	}

	public static IItemFilter contains(ICanonicalAccessorFactory<String> attribute, String substring) {
		return new Contains(substring, attribute);
	}

	public static IItemFilter notMatches(ICanonicalAccessorFactory<String> attribute, String regexp) {
		return new NotMatches(regexp, attribute);
	}

	public static IItemFilter notContains(ICanonicalAccessorFactory<String> attribute, String regexp) {
		return new NotContains(regexp, attribute);
	}

	public static <M> IItemFilter memberOf(ICanonicalAccessorFactory<M> attribute, Set<M> values) {
		IItemFilter[] filters = new IItemFilter[values.size()];
		int i = 0;
		for (M m : values) {
			filters[i++] = equals(attribute, m);
		}
		return new MemberOf<>(filters, attribute, values);
	}

	public static <M extends Comparable<? super M>> IItemFilter less(
		ICanonicalAccessorFactory<M> attribute, M upperLimit) {
		return new Compare<>(Kind.LESS, attribute, upperLimit);
	}

	public static <M extends Comparable<? super M>> IItemFilter lessOrEqual(
		ICanonicalAccessorFactory<M> attribute, M upperLimit) {
		return new Compare<>(Kind.LESS_OR_EQUAL, attribute, upperLimit);
	}

	public static <M extends Comparable<? super M>> IItemFilter more(
		ICanonicalAccessorFactory<M> attribute, M lowerLimit) {
		return new Compare<>(Kind.MORE, attribute, lowerLimit);
	}

	public static <M extends Comparable<? super M>> IItemFilter moreOrEqual(
		ICanonicalAccessorFactory<M> attribute, M lowerLimit) {
		return new Compare<>(Kind.MORE_OR_EQUAL, attribute, lowerLimit);
	}

	/**
	 * Match a range according to {@link RangeMatchPolicy#CLOSED_INTERSECTS_WITH_CLOSED}.
	 *
	 * @param <M>
	 *            the type of end points of the ranges
	 * @param rangeAttribute
	 *            the range attribute that should be filtered on (treated as closed)
	 * @param rangeLimit
	 *            the limiting range (treated as closed)
	 * @return a filter that accepts items with attribute values that intersects the limiting range
	 */
	public static <M extends Comparable<? super M>> IItemFilter rangeIntersects(
		ICanonicalAccessorFactory<IRange<M>> rangeAttribute, IRange<M> rangeLimit) {
		return matchRange(RangeMatchPolicy.CLOSED_INTERSECTS_WITH_CLOSED, rangeAttribute, rangeLimit);
	}

	/**
	 * Match a range according to {@link RangeMatchPolicy#CONTAINED_IN_CLOSED}.
	 *
	 * @param <M>
	 *            the type of end points of the ranges
	 * @param rangeAttribute
	 *            the range attribute that should be filtered on (treated as right open, unless
	 *            single point)
	 * @param rangeLimit
	 *            the limiting range (treated as closed)
	 * @return a filter that accepts items with attribute values that are contained in the limiting
	 *         range
	 */
	public static <M extends Comparable<? super M>> IItemFilter rangeContainedIn(
		ICanonicalAccessorFactory<IRange<M>> rangeAttribute, IRange<M> rangeLimit) {
		return matchRange(RangeMatchPolicy.CONTAINED_IN_CLOSED, rangeAttribute, rangeLimit);
	}

	/**
	 * Match a range according to {@link RangeMatchPolicy#CENTER_CONTAINED_IN_RIGHT_OPEN}.
	 *
	 * @param <M>
	 *            the type of end points of the ranges
	 * @param rangeAttribute
	 *            the range attribute whose center should be filtered on
	 * @param rangeLimit
	 *            the limiting range (treated as right open)
	 * @return a filter that accepts items with attribute values whose centers are contained in the
	 *         limiting range
	 */
	public static <M extends Comparable<? super M>> IItemFilter centerContainedIn(
		ICanonicalAccessorFactory<IRange<M>> rangeAttribute, IRange<M> rangeLimit) {
		return matchRange(RangeMatchPolicy.CENTER_CONTAINED_IN_RIGHT_OPEN, rangeAttribute, rangeLimit);
	}

	public static <M extends Comparable<? super M>> IItemFilter matchRange(
		RangeMatchPolicy policy, ICanonicalAccessorFactory<IRange<M>> rangeAttribute, IRange<M> rangeLimit) {
		return new RangeMatches<>(policy.kind, rangeAttribute, rangeLimit);
	}

	public static <M extends Comparable<? super M>> IItemFilter matchRange(
		Kind kind, ICanonicalAccessorFactory<IRange<M>> rangeAttribute, IRange<M> rangeLimit) {
		return new RangeMatches<>(kind, rangeAttribute, rangeLimit);
	}

	public static <M extends Comparable<? super M>> IItemFilter interval(
		ICanonicalAccessorFactory<M> attribute, M lowerLimit, boolean lowerClosed, M upperLimit, boolean upperClosed) {
		IItemFilter lower = lowerClosed ? moreOrEqual(attribute, lowerLimit) : more(attribute, lowerLimit);
		IItemFilter upper = upperClosed ? lessOrEqual(attribute, upperLimit) : less(attribute, upperLimit);
		return and(lower, upper);
	}

	public static IItemFilter and(IItemFilter ... filters) {
		return new Composite(Kind.AND, filters);
	}

	public static IItemFilter or(IItemFilter ... filters) {
		if (filters.length > 0) { // Try to use set
			if (filters[0] instanceof Type || filters[0] instanceof Types) {
				Set<String> types = new HashSet<>();
				for (IItemFilter f : filters) {
					if (f instanceof Type) {
						types.add(((Type) f).typeId);
					} else if (f instanceof Types) {
						types.addAll(((Types) f).types);
					} else {
						return new Composite(Kind.OR, filters);
					}
				}
				return new Types(filters, types);
			} else if (filters[0] instanceof Equals) {
				return optimizeOr(((Equals<?>) filters[0]).getAttribute(), filters);
			}
		}
		return new Composite(Kind.OR, filters);
	}

	@SuppressWarnings({"unchecked"})
	private static <M> Composite optimizeOr(ICanonicalAccessorFactory<M> attribute, IItemFilter ... filters) {
		Set<M> values = new HashSet<>();
		for (IItemFilter f : filters) {
			if (f instanceof Equals) {
				Equals<?> ef = (Equals<?>) f;
				if (ef.getAttribute().equals(attribute)) {
					values.add((M) ef.getValue());
					continue;
				}
			}
			return new Composite(Kind.OR, filters);
		}
		return new MemberOf<>(filters, attribute, values);
	}

	public static IItemFilter not(final IItemFilter filter) {
		return new Not(filter);
	}

	public static IItemFilter convertToTypes(IItemFilter filter, List<IType<IItem>> types) {
		Set<String> typesForFilter = new HashSet<>();
		for (IType<IItem> t : types) {
			if (filter.getPredicate(t).equals(PredicateToolkit.truePredicate())) {
				typesForFilter.add(t.getIdentifier());
			}
		}
		return type(typesForFilter);
	}

	public static <V> IItemFilter buildEqualityFilter(
		PersistableItemFilter.Kind comparisonKind, ICanonicalAccessorFactory<V> attribute, V value) {
		switch (comparisonKind) {
		case EQUALS:
			return equals(attribute, value);
		case NOT_EQUALS:
			return notEquals(attribute, value);
		default:
			return ItemFilters.buildExistenceFilter(comparisonKind, attribute, value);
		}
	}

	public static <V> IItemFilter buildExistenceFilter(
		PersistableItemFilter.Kind comparisonKind, ICanonicalAccessorFactory<V> attribute, V value) {
		switch (comparisonKind) {
		case EXISTS:
			return hasAttribute(attribute);
		case NOT_EXISTS:
			return notHasAttribute(attribute);
		case IS_NULL:
			return isNull(attribute);
		case IS_NOT_NULL:
			return isNotNull(attribute);
		default:
			throw new RuntimeException("Unknown comparison kind"); //$NON-NLS-1$
		}
	}

	public static IItemFilter buildStringFilter(
		PersistableItemFilter.Kind comparisonKind, ICanonicalAccessorFactory<String> attribute, String value) {
		switch (comparisonKind) {
		case MATCHES:
			return matches(attribute, value);
		case NOT_MATCHES:
			return notMatches(attribute, value);
		case CONTAINS:
			return contains(attribute, value);
		case NOT_CONTAINS:
			return notContains(attribute, value);
		default:
			return ItemFilters.buildComparisonFilter(comparisonKind, attribute, value);
		}
	}

	// FIXME: We would like to merge this with PersistableItemFilter.readFrom(), but it is not a trivial task
	public static <V extends Comparable<V>> IItemFilter buildComparisonFilter(
		PersistableItemFilter.Kind comparisonKind, ICanonicalAccessorFactory<V> attribute, V value) {
		switch (comparisonKind) {
		case LESS:
			return less(attribute, value);
		case LESS_OR_EQUAL:
			return lessOrEqual(attribute, value);
		case MORE:
			return more(attribute, value);
		case MORE_OR_EQUAL:
			return moreOrEqual(attribute, value);
		default:
			return ItemFilters.buildEqualityFilter(comparisonKind, attribute, value);
		}
	}
}
