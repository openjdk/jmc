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
package org.openjdk.jmc.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.openjdk.jmc.common.IPredicate;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.RangeMatchPolicy;
import org.openjdk.jmc.common.unit.IRange;

/**
 * Toolkit used to create instances of {@link IPredicate} matching various criteria.
 */
public class PredicateToolkit {

	private static final IPredicate<Object> FALSE = new IPredicate<Object>() {
		@Override
		public boolean evaluate(Object o) {
			return false;
		}
	};
	private static final IPredicate<Object> TRUE = new IPredicate<Object>() {
		@Override
		public boolean evaluate(Object o) {
			return true;
		}
	};

	/**
	 * @return a predicate that always will evaluate to {@code true}
	 */
	@SuppressWarnings("unchecked")
	public static <T> IPredicate<T> truePredicate() {
		return (IPredicate<T>) TRUE;
	}

	/**
	 * @return a predicate that always will evaluate to {@code false}
	 */
	@SuppressWarnings("unchecked")
	public static <T> IPredicate<T> falsePredicate() {
		return (IPredicate<T>) FALSE;
	}

	/**
	 * Test if a predicate is guaranteed to always evaluate to {@code true}. Note that if this
	 * method returns {@code false}, then it only means that it is unknown what the predicate will
	 * return.
	 * 
	 * @param p
	 *            a predicate to test
	 * @return {@code true} if the predicate is guaranteed to evaluate to {@code true}
	 */
	public static boolean isTrueGuaranteed(IPredicate<?> p) {
		return p == TRUE;
	}

	/**
	 * Test if a predicate is guaranteed to always evaluate to {@code false}. Note that if this
	 * method returns {@code false}, then it only means that it is unknown what the predicate will
	 * return.
	 * 
	 * @param p
	 *            a predicate to test
	 * @return {@code true} if the predicate is guaranteed to evaluate to {@code false}
	 */
	public static boolean isFalseGuaranteed(IPredicate<?> p) {
		return p == FALSE;
	}

	/**
	 * Combine a collection of predicates using an AND operation.
	 * 
	 * @param predicates
	 *            input predicates
	 * @return a predicate that evaluates to {@code true} if all input predicates evaluate to
	 *         {@code true}
	 */
	public static <T> IPredicate<T> and(Collection<IPredicate<T>> predicates) {
		switch (predicates.size()) {
		case 0:
			return truePredicate();
		case 1:
			return predicates.iterator().next();
		default:
			final List<IPredicate<T>> nonTrivialPredicates = new ArrayList<>(predicates.size());
			for (IPredicate<T> p : predicates) {
				if (isFalseGuaranteed(p)) {
					return p;
				} else if (!isTrueGuaranteed(p)) {
					nonTrivialPredicates.add(p);
				}
			}
			if (nonTrivialPredicates.size() == 0) {
				return truePredicate(); // All predicates are TRUE
			} else if (nonTrivialPredicates.size() == 1) {
				return nonTrivialPredicates.get(0); // A single predicate is not TRUE or FALSE
			} else {
				return new IPredicate<T>() {

					@Override
					public boolean evaluate(T o) {
						for (IPredicate<T> ex : nonTrivialPredicates) {
							if (!ex.evaluate(o)) {
								return false;
							}
						}
						return true;
					}
				};
			}
		}
	}

	/**
	 * Combine a collection of predicates using an OR operation.
	 * 
	 * @param predicates
	 *            input predicates
	 * @return a predicate that evaluates to {@code true} if at least one of the input predicates
	 *         evaluate to {@code true}
	 */
	public static <T> IPredicate<T> or(Collection<IPredicate<T>> predicates) {
		switch (predicates.size()) {
		case 0:
			return falsePredicate();
		case 1:
			return predicates.iterator().next();
		default:
			final List<IPredicate<T>> nonTrivialPredicates = new ArrayList<>(predicates.size());
			for (IPredicate<T> p : predicates) {
				if (isTrueGuaranteed(p)) {
					return p;
				} else if (!isFalseGuaranteed(p)) {
					nonTrivialPredicates.add(p);
				}
			}
			if (nonTrivialPredicates.size() == 0) {
				return falsePredicate(); // All predicates are FALSE
			} else if (nonTrivialPredicates.size() == 1) {
				return nonTrivialPredicates.get(0); // A single predicate is not TRUE or FALSE
			} else {
				return new IPredicate<T>() {

					@Override
					public boolean evaluate(T o) {
						for (IPredicate<T> ex : nonTrivialPredicates) {
							if (ex.evaluate(o)) {
								return true;
							}
						}
						return false;
					}
				};
			}
		}
	}

	/**
	 * Invert a predicate.
	 * 
	 * @param predicate
	 *            predicate to invert
	 * @return a predicate that evaluates to {@code true} if the input predicate evaluates to
	 *         {@code false} and vice versa
	 */
	public static <T> IPredicate<T> not(final IPredicate<T> predicate) {
		if (isTrueGuaranteed(predicate)) {
			return falsePredicate();
		} else if (isFalseGuaranteed(predicate)) {
			return truePredicate();
		} else {
			return new IPredicate<T>() {

				@Override
				public boolean evaluate(T o) {
					return !predicate.evaluate(o);
				}
			};
		}
	}

	/**
	 * Create a predicate that compares values to a limit.
	 * <p>
	 * The predicate takes an input object as argument but the value that is checked is extracted
	 * from the input object using a member accessor.
	 * 
	 * @param valueAccessor
	 *            accessor used to get the value to check from the input type
	 * @param limit
	 *            value to compare against
	 * @param orEqual
	 *            if {@code true}, evaluate values that are equal to the limit to {@code true}
	 * @param <T>
	 *            type of objects passed into the predicate
	 * @param <M>
	 *            type of the value that is compared
	 * @return a predicate that evaluates to {@code true} if the value to check is less than, or
	 *         optionally equal to, the limit value
	 */
	public static <T, M> IPredicate<T> less(
		IMemberAccessor<? extends M, T> valueAccessor, Comparable<? super M> limit, boolean orEqual) {
		// NOTE: Compiler could do constant propagation to achieve the same from more condensed code, but this is more readable.
		return orEqual ? lessOrEqual(valueAccessor, limit) : less(valueAccessor, limit);
	}

	/**
	 * Create a predicate that compares values to a limit.
	 * <p>
	 * The predicate takes an input object as argument but the value that is checked is extracted
	 * from the input object using a member accessor.
	 * 
	 * @param valueAccessor
	 *            accessor used to get the value to check from the input type
	 * @param limit
	 *            value to compare against
	 * @param <T>
	 *            type of objects passed into the predicate
	 * @param <M>
	 *            type of the value that is compared
	 * @return a predicate that evaluates to {@code true} if the value to check is strictly less
	 *         than the limit value
	 */
	public static <T, M> IPredicate<T> less(
		final IMemberAccessor<? extends M, T> valueAccessor, final Comparable<? super M> limit) {
		return new IPredicate<T>() {
			@Override
			public boolean evaluate(T o) {
				M value = valueAccessor.getMember(o);
				return (value != null) && (limit.compareTo(value) > 0);
			}
		};
	}

	/**
	 * Create a predicate that compares values to a limit.
	 * <p>
	 * The predicate takes an input object as argument but the value that is checked is extracted
	 * from the input object using a member accessor.
	 * 
	 * @param valueAccessor
	 *            accessor used to get the value to check from the input type
	 * @param limit
	 *            value to compare against
	 * @param <T>
	 *            type of objects passed into the predicate
	 * @param <M>
	 *            type of the value that is compared
	 * @return a predicate that evaluates to {@code true} if the value to check is less than or
	 *         equal to the limit value
	 */
	public static <T, M> IPredicate<T> lessOrEqual(
		final IMemberAccessor<? extends M, T> valueAccessor, final Comparable<? super M> limit) {
		return new IPredicate<T>() {
			@Override
			public boolean evaluate(T o) {
				M value = valueAccessor.getMember(o);
				return (value != null) && (limit.compareTo(value) >= 0);
			}
		};
	}

	/**
	 * Create a predicate that compares values to a limit.
	 * <p>
	 * The predicate takes an input object as argument but the value that is checked is extracted
	 * from the input object using a member accessor.
	 * 
	 * @param valueAccessor
	 *            accessor used to get the value to check from the input type
	 * @param limit
	 *            value to compare against
	 * @param orEqual
	 *            if {@code true}, evaluate values that are equal to the limit to {@code true}
	 * @param <T>
	 *            type of objects passed into the predicate
	 * @param <M>
	 *            type of the value that is compared
	 * @return a predicate that evaluates to {@code true} if the value to check is greater than, or
	 *         optionally equal to, the limit value
	 */
	public static <T, M> IPredicate<T> more(
		IMemberAccessor<? extends M, T> valueAccessor, Comparable<? super M> limit, boolean orEqual) {
		// NOTE: Compiler could do constant propagation to achieve the same from more condensed code, but this is more readable.
		return orEqual ? moreOrEqual(valueAccessor, limit) : more(valueAccessor, limit);
	}

	/**
	 * Create a predicate that compares values to a limit.
	 * <p>
	 * The predicate takes an input object as argument but the value that is checked is extracted
	 * from the input object using a member accessor.
	 * 
	 * @param valueAccessor
	 *            accessor used to get the value to check from the input type
	 * @param limit
	 *            value to compare against
	 * @param <T>
	 *            type of objects passed into the predicate
	 * @param <M>
	 *            type of the value that is compared
	 * @return a predicate that evaluates to {@code true} if the value to check is strictly greater
	 *         than the limit value
	 */
	public static <T, M> IPredicate<T> more(
		final IMemberAccessor<? extends M, T> valueAccessor, final Comparable<? super M> limit) {
		return new IPredicate<T>() {
			@Override
			public boolean evaluate(T o) {
				M value = valueAccessor.getMember(o);
				return (value != null) && (limit.compareTo(value) < 0);
			}
		};
	}

	/**
	 * Create a predicate that compares values to a limit.
	 * <p>
	 * The predicate takes an input object as argument but the value that is checked is extracted
	 * from the input object using a member accessor.
	 * 
	 * @param valueAccessor
	 *            accessor used to get the value to check from the input type
	 * @param limit
	 *            value to compare against
	 * @param <T>
	 *            type of objects passed into the predicate
	 * @param <M>
	 *            type of the value that is compared
	 * @return a predicate that evaluates to {@code true} if the value to check is greater than or
	 *         equal to the limit value
	 */
	public static <T, M> IPredicate<T> moreOrEqual(
		final IMemberAccessor<? extends M, T> valueAccessor, final Comparable<? super M> limit) {
		return new IPredicate<T>() {
			@Override
			public boolean evaluate(T o) {
				M value = valueAccessor.getMember(o);
				return (value != null) && (limit.compareTo(value) <= 0);
			}
		};
	}

	/**
	 * Return a predicate based on {@code limit} according to
	 * {@link RangeMatchPolicy#CLOSED_INTERSECTS_WITH_CLOSED}.
	 * <p>
	 * The predicate takes an input object as argument but the range that is checked is extracted
	 * from the input object using a member accessor.
	 * 
	 * @param rangeAccessor
	 *            accessor used to get the range value to check from the input type
	 * @param limit
	 *            range value to compare against
	 * @param <T>
	 *            type of objects passed into the predicate
	 * @param <M>
	 *            type of the range value that is compared
	 * @return a predicate that evaluates to {@code true} if the range value to check intersects
	 *         with the limit range
	 */
	public static <T, M extends Comparable<? super M>> IPredicate<T> rangeIntersects(
		final IMemberAccessor<? extends IRange<M>, T> rangeAccessor, final IRange<M> limit) {
		return new IPredicate<T>() {
			@Override
			public boolean evaluate(T o) {
				IRange<M> value = rangeAccessor.getMember(o);
				if (value != null) {
					return (value.getStart().compareTo(limit.getEnd()) <= 0)
							&& (value.getEnd().compareTo(limit.getStart()) >= 0);
				}
				return false;
			}
		};
	}

	/**
	 * Return a predicate based on {@code limit} according to
	 * {@link RangeMatchPolicy#CONTAINED_IN_CLOSED}.
	 * <p>
	 * The predicate takes an input object as argument but the range that is checked is extracted
	 * from the input object using a member accessor.
	 * 
	 * @param rangeAccessor
	 *            accessor used to get the range value to check from the input type
	 * @param limit
	 *            range value to compare against
	 * @param <T>
	 *            type of objects passed into the predicate
	 * @param <M>
	 *            type of the range value that is compared
	 * @return a predicate that evaluates to {@code true} if the range value to check is contained
	 *         in the limit range
	 */
	public static <T, M extends Comparable<? super M>> IPredicate<T> rangeContained(
		final IMemberAccessor<? extends IRange<M>, T> rangeAccessor, final IRange<M> limit) {
		// Optimize the point limit case although not strictly needed when the limit range is treated as closed.
		if (limit.isPoint()) {
			final M point = limit.getStart();
			return new IPredicate<T>() {
				@Override
				public boolean evaluate(T o) {
					IRange<M> value = rangeAccessor.getMember(o);
					return (value != null) && value.isPoint() && (point.compareTo(value.getStart()) == 0);
				}
			};
		} else {
			return new IPredicate<T>() {
				@Override
				public boolean evaluate(T o) {
					IRange<M> value = rangeAccessor.getMember(o);
					if (value != null) {
						return (value.getStart().compareTo(limit.getStart()) >= 0)
								&& (value.getEnd().compareTo(limit.getEnd()) <= 0);
					}
					return false;
				}
			};
		}
	}

	/**
	 * Return a predicate based on {@code limit} according to
	 * {@link RangeMatchPolicy#CENTER_CONTAINED_IN_RIGHT_OPEN}.
	 * <p>
	 * The predicate takes an input object as argument but the range that is checked is extracted
	 * from the input object using a member accessor.
	 * 
	 * @param rangeAccessor
	 *            accessor used to get the range value to check from the input type
	 * @param limit
	 *            range value to compare against
	 * @param <T>
	 *            type of objects passed into the predicate
	 * @param <M>
	 *            type of the range value that is compared
	 * @return a predicate that evaluates to {@code true} if the center point of the range value to
	 *         check is contained in the limit range
	 */
	public static <T, M extends Comparable<? super M>> IPredicate<T> centerContained(
		final IMemberAccessor<? extends IRange<M>, T> rangeAccessor, final IRange<M> limit) {
		return new IPredicate<T>() {
			@Override
			public boolean evaluate(T o) {
				IRange<M> value = rangeAccessor.getMember(o);
				if (value != null) {
					M center = value.getCenter();
					return (center.compareTo(limit.getStart()) >= 0) && (center.compareTo(limit.getEnd()) < 0);
				}
				return false;
			}
		};
	}

	/**
	 * Create a predicate that checks if a value is equal to a specified object.
	 * <p>
	 * The predicate takes an input object as argument but the value that is checked is extracted
	 * from the input object using a member accessor.
	 * 
	 * @param valueAccessor
	 *            accessor used to get the value to check from the input type
	 * @param item
	 *            object to compare against
	 * @param <T>
	 *            type of objects passed into the predicate
	 * @return a predicate that evaluates to {@code true} if the value to check is equal to the
	 *         specified object
	 */
	public static <T> IPredicate<T> equals(final IMemberAccessor<?, T> valueAccessor, final Object item) {
		return new IPredicate<T>() {

			@Override
			public boolean evaluate(T o) {
				Object value = valueAccessor.getMember(o);
				return item == null ? value == null : item.equals(value);
			}
		};
	}

	/**
	 * Create a predicate that checks if a value is not equal to a specified object.
	 * <p>
	 * The predicate takes an input object as argument but the value that is checked is extracted
	 * from the input object using a member accessor.
	 * 
	 * @param valueAccessor
	 *            accessor used to get the value to check from the input type
	 * @param item
	 *            object to compare against
	 * @param <T>
	 *            type of objects passed into the predicate
	 * @return a predicate that evaluates to {@code true} if the value to check is not equal to the
	 *         specified object
	 */
	public static <T> IPredicate<T> notEquals(final IMemberAccessor<?, T> valueAccessor, final Object item) {
		return new IPredicate<T>() {

			@Override
			public boolean evaluate(T o) {
				Object value = valueAccessor.getMember(o);
				return item == null ? value != null : !item.equals(value);
			}
		};
	}

	/**
	 * Create a predicate that checks if a value is a specified object. This check is performed
	 * using object identity.
	 * <p>
	 * The predicate takes an input object as argument but the value that is checked is extracted
	 * from the input object using a member accessor.
	 * 
	 * @param item
	 *            object to compare against
	 * @param <T>
	 *            type of objects passed into the predicate
	 * @return a predicate that evaluates to {@code true} if the value to check is the specified
	 *         object
	 */
	public static <T> IPredicate<T> is(final T item) {
		return new IPredicate<T>() {

			@Override
			public boolean evaluate(T o) {
				return o == item;
			}
		};
	}

	/**
	 * Create a predicate that checks if a value is included in a specified set.
	 * <p>
	 * The predicate takes an input object as argument but the value that is checked is extracted
	 * from the input object using a member accessor.
	 * 
	 * @param valueAccessor
	 *            accessor used to get the value to check from the input type
	 * @param items
	 *            set of objects to compare against
	 * @param <T>
	 *            type of objects passed into the predicate
	 * @param <M>
	 *            type of the range value that is compared
	 * @return a predicate that evaluates to {@code true} if the object to check is included in the
	 *         specified set
	 */
	public static <T, M> IPredicate<T> memberOf(
		final IMemberAccessor<? extends M, T> valueAccessor, final Set<? extends M> items) {
		return new IPredicate<T>() {

			@Override
			public boolean evaluate(T o) {
				M value = valueAccessor.getMember(o);
				return items.contains(value);
			}
		};
	}

	/**
	 * Create a predicate that checks if a string value matches a regular expression.
	 * <p>
	 * The predicate takes an input object as argument but the value that is checked is extracted
	 * from the input object using a member accessor.
	 *
	 * @param valueAccessor
	 *            string accessor used to get the value to check from the input type
	 * @param regexp
	 *            the regular expression to match
	 * @param <T>
	 *            type of objects passed into the predicate
	 * @return a predicate that evaluates to {@code true} if the string value matches the regular
	 *         expression
	 */
	public static <T> IPredicate<T> matches(final IMemberAccessor<? extends String, T> valueAccessor, String regexp) {
		final Pattern pattern = getValidPattern(regexp);
		return new IPredicate<T>() {
			@Override
			public boolean evaluate(T o) {
				String value = valueAccessor.getMember(o);
				return value == null ? false : pattern.matcher(value).matches();
			}
		};
	}

	/**
	 * Create a predicate that checks if a string value contains a specified substring.
	 * <p>
	 * The predicate takes an input object as argument but the value that is checked is extracted
	 * from the input object using a member accessor.
	 * 
	 * @param valueAccessor
	 *            string accessor used to get the value to check from the input type
	 * @param substring
	 *            the substring to look for
	 * @return a predicate that evaluates to {@code true} if the string value contains the substring
	 */
	public static <T> IPredicate<T> contains(
		final IMemberAccessor<? extends String, T> valueAccessor, final String substring) {
		return new IPredicate<T>() {
			@Override
			public boolean evaluate(T o) {
				String value = valueAccessor.getMember(o);
				return value == null ? false : value.contains(substring);
			}
		};
	}

	/**
	 * Compile a regular expression into a pattern if possible. If the expression can't be compiled,
	 * return a valid pattern that will give 0 matches (at least for single lines).
	 *
	 * @param regexp
	 *            regular expression to compile
	 * @return a valid regular expression pattern instance
	 */
	// TODO: Possibly allow matching all (instead of none) if the regexp is invalid, by adding a boolean parameter to control the behavior
	public static Pattern getValidPattern(String regexp) {
		try {
			return Pattern.compile(regexp, Pattern.DOTALL);
		} catch (PatternSyntaxException pse) {
			Logger.getLogger("org.openjdk.jmc.common.util").log(Level.FINE, //$NON-NLS-1$
					"Got exception when compiling regular expression: " + pse.getMessage(), pse); //$NON-NLS-1$
			return Pattern.compile("$."); //$NON-NLS-1$
		}
	}
}
