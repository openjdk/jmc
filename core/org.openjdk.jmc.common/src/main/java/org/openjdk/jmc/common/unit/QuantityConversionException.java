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
package org.openjdk.jmc.common.unit;

import java.text.MessageFormat;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.messages.internal.Messages;

/**
 * Exception denoting problem converting between representations of physical quantities ("numbers
 * with units"). Has a problem enum designating the kind of problem and is able to return a
 * prototype value that could be used to circumvent that problem. Has different representations for
 * persisting/logging (locale independent) and presenting to a human (using the default locale). Be
 * sure to use {@link #getLocalizedMessage()} for error dialogs.
 */
// FIXME: Can we subclass IllegalArgumentException, like NumberFormatException?
public abstract class QuantityConversionException extends Exception {
	private static final long serialVersionUID = 1L;

	protected final Problem problem;
	protected final String badString;

	public static enum Problem {
		UNPARSEABLE("Unparsable {0}, expected {1}", Messages.QuantityConversionException_UNPARSEABLE_MSG), //$NON-NLS-1$
		NO_UNIT("No unit in {0}, expected {1}", Messages.QuantityConversionException_NO_UNIT_MSG), //$NON-NLS-1$
		UNKNOWN_UNIT("Unknown unit in {0}, expected {1}", Messages.QuantityConversionException_UNKNOWN_UNIT_MSG), //$NON-NLS-1$
		TOO_LOW("Value {0} too low, min is {1}", Messages.QuantityConversionException_TOO_LOW_MSG), //$NON-NLS-1$
		TOO_HIGH("Value {0} too high, max is {1}", Messages.QuantityConversionException_TOO_HIGH_MSG), //$NON-NLS-1$
		TOO_SMALL_MAGNITUDE("Value {0} below precision, smallest unit is {1}", Messages.QuantityConversionException_TOO_SMALL_MAGNITUDE_MSG), //$NON-NLS-1$
		// FIXME: Move this to another Exception class that is a sibling to this class.
		CONFLICTING_CONSTRAINTS("Constraints for value {1} and key {0} do not match.", Messages.QuantityConversionException_CONSTRAINTS_DO_NOT_MATCH); //$NON-NLS-1$

		public final String logMsg;
		public final String localizedMsg;

		private Problem(String logMsg, String localizedMsgKey) {
			this.logMsg = logMsg;
			localizedMsg = Messages.getString(localizedMsgKey);
		}
	};

	public static class Persisted extends QuantityConversionException {
		private static final long serialVersionUID = 1L;

		protected final Object badValue;
		protected final Object prototype;
		// NOTE: Only transient to silence SpotBugs. This exception should never be serialized.
		protected final transient IPersister<?> persister;

		private Persisted(Problem problem, String badString, Object prototype, IPersister<?> persister) {
			super(problem, badString);
			badValue = null;
			this.prototype = prototype;
			this.persister = persister;
		}

		private Persisted(Problem problem, Object badValue, Object prototype, IPersister<?> persister) {
			super(problem, null);
			this.badValue = badValue;
			this.prototype = prototype;
			this.persister = persister;
		}

		@SuppressWarnings("unchecked")
		private <T> String persistable(T value) {
			return ((IPersister<T>) persister).persistableString(value);
		}

		@SuppressWarnings("unchecked")
		private <T> String interactive(T value) {
			return ((IPersister<T>) persister).interactiveFormat(value);
		}

		@Override
		public String getMessage() {
			if (badValue != null) {
				return MessageFormat.format(problem.logMsg, persistable(badValue), getPersistablePrototype());
			}
			return super.getMessage();
		}

		@Override
		public String getLocalizedMessage() {
			if (badValue != null) {
				return MessageFormat.format(problem.localizedMsg, interactive(badValue), getInteractivePrototype());
			}
			return super.getLocalizedMessage();
		}

		@Override
		public String getPersistablePrototype() {
			return persistable(prototype);
		}

		@Override
		public String getInteractivePrototype() {
			return interactive(prototype);
		}
	}

	public static class Quantity extends QuantityConversionException {
		private static final long serialVersionUID = 1L;

		protected final IQuantity badQuantity;
		protected final IQuantity prototype;

		public Quantity(Problem problem, String badString, IQuantity prototype) {
			super(problem, badString);
			badQuantity = null;
			this.prototype = prototype;
		}

		public Quantity(Problem problem, IQuantity badQuantity, IQuantity prototype) {
			super(problem, null);
			this.badQuantity = badQuantity;
			this.prototype = prototype;
		}

		@Override
		public String getMessage() {
			if (badQuantity != null) {
				return MessageFormat.format(problem.logMsg, badQuantity.persistableString(), getPersistablePrototype());
			}
			return super.getMessage();
		}

		@Override
		public String getLocalizedMessage() {
			if (badQuantity != null) {
				if (badQuantity.getUnit() instanceof LinearUnit) {
					@SuppressWarnings("unchecked")
					ITypedQuantity<LinearUnit> typedProto = (ITypedQuantity<LinearUnit>) prototype;
					String protoString = readableProto(typedProto, (LinearUnit) badQuantity.getUnit());
					return MessageFormat.format(problem.localizedMsg, badQuantity.displayUsing(IDisplayable.EXACT),
							protoString);
				}
				return MessageFormat.format(problem.localizedMsg, badQuantity.displayUsing(IDisplayable.EXACT),
						prototype.displayUsing(IDisplayable.EXACT));
			}
			return super.getLocalizedMessage();
		}

		/**
		 * Produce a string that presents a prototype value. "Exact value ~= value in user unit ~=
		 * value in auto unit".
		 *
		 * @param prototype
		 *            prototype value to present
		 * @param userUnit
		 *            alternative unit to present the prototype value in
		 * @return a readable string containing the value
		 */
		private static String readableProto(ITypedQuantity<LinearUnit> prototype, LinearUnit userUnit) {
			LinearUnit exactUnit = prototype.getUnit();

			StringBuilder out = new StringBuilder(prototype.displayUsing(IDisplayable.EXACT));
			if (!userUnit.equals(exactUnit)) {
				LinearKindOfQuantity kindOfQuantity = exactUnit.getContentType();
				LinearUnit autoUnit = kindOfQuantity.getPreferredUnit(prototype, 1, 1000);

				if (!userUnit.equals(autoUnit)) {
					out.append(" \u2248 "); //$NON-NLS-1$
					// Since we can, format with reduced precision.
					out.append(LinearKindOfQuantity.AutoFormatter.formatInUnit(prototype, userUnit, 3));
				}

				out.append(" \u2248 "); //$NON-NLS-1$
				// Since we can, format with reduced precision.
				out.append(LinearKindOfQuantity.AutoFormatter.formatInUnit(prototype, autoUnit, 3));
			}
			return out.toString();
		}

		@Override
		public String getPersistablePrototype() {
			return prototype.persistableString();
		}

		@Override
		public String getInteractivePrototype() {
			return prototype.interactiveFormat();
		}
	}

	/*
	 * NOTE: The type parameter T doesn't strictly need to extend Comparable<T>, particularly not
	 * for UNPARSEABLE. For the other cases, it seemed possible that they could be used where being
	 * Comparable would be beneficial.
	 */
	public static <T extends Comparable<T>> QuantityConversionException unparsable(
		String badString, T prototype, IPersister<T> persister) {
		return new Persisted(Problem.UNPARSEABLE, badString, prototype, persister);
	}

	public static <T extends Comparable<T>> QuantityConversionException noUnit(
		String badString, T prototype, IPersister<T> persister) {
		return new Persisted(Problem.NO_UNIT, badString, prototype, persister);
	}

	public static <T extends Comparable<T>> QuantityConversionException unknownUnit(
		String badString, T prototype, IPersister<T> persister) {
		return new Persisted(Problem.UNKNOWN_UNIT, badString, prototype, persister);
	}

	public static <T extends Comparable<T>> QuantityConversionException tooLow(
		T badValue, T min, IPersister<T> persister) {
		return new Persisted(Problem.TOO_LOW, badValue, min, persister);
	}

	public static <T extends Comparable<T>> QuantityConversionException tooHigh(
		T badValue, T max, IPersister<T> persister) {
		return new Persisted(Problem.TOO_HIGH, badValue, max, persister);
	}

	/*
	 * FIXME: This currently reports that the value is "below precision". Replace precisionLimit
	 * with a closest valid quantity (and change the problem message)?
	 */
	public static <T extends Comparable<T>> QuantityConversionException belowPrecision(
		T badValue, T precisionLimit, IPersister<T> persister) {
		return new Persisted(Problem.TOO_SMALL_MAGNITUDE, badValue, precisionLimit, persister);
	}

	public static QuantityConversionException unparsable(String badString, IQuantity prototype) {
		return new Quantity(Problem.UNPARSEABLE, badString, prototype);
	}

	public static QuantityConversionException noUnit(String badString, IQuantity prototype) {
		return new Quantity(Problem.NO_UNIT, badString, prototype);
	}

	public static QuantityConversionException unknownUnit(String badString, IQuantity prototype) {
		return new Quantity(Problem.UNKNOWN_UNIT, badString, prototype);
	}

	public static <Q extends IQuantity> QuantityConversionException tooLow(Q badValue, Q min) {
		return new Quantity(Problem.TOO_LOW, badValue, min);
	}

	public static <Q extends IQuantity> QuantityConversionException tooHigh(Q badValue, Q max) {
		return new Quantity(Problem.TOO_HIGH, badValue, max);
	}

	/*
	 * FIXME: This currently reports that the value is "below precision". Replace precisionLimit
	 * with a closest valid quantity (and change the problem message)?
	 */
	public static <Q extends IQuantity> QuantityConversionException belowPrecision(Q badValue, Q precisionLimit) {
		return new Quantity(Problem.TOO_SMALL_MAGNITUDE, badValue, precisionLimit);
	}

	// FIXME: Move this to another Exception class that is a sibling to this class.
	public static QuantityConversionException conflictingConstraints(Object value, String key) {
		// Really fake placeholder. Will most likely fail.
		return new Quantity(Problem.CONFLICTING_CONSTRAINTS, key, (IQuantity) value);
	}

	protected QuantityConversionException(Problem problem, String badString) {
		this.problem = problem;
		this.badString = badString;
	}

	/**
	 * The kind of problem encountered.
	 *
	 * @return one of the {@link Problem} values, never null
	 */
	public Problem getProblem() {
		return problem;
	}

	/**
	 * The string where the problem was encountered, so that it might be highlighted. May be the
	 * entire string on which parsing was attempted. May be null if this problem was detected after
	 * parsing, such as out of range.
	 *
	 * @return a substring of the parser input, or null
	 */
	public String getBadString() {
		return badString;
	}

	/**
	 * Prototype value suitable to be persisted or logged.
	 *
	 * @return a non-localized (English) value that will prevent this particular problem when
	 *         parsed.
	 */
	public abstract String getPersistablePrototype();

	/**
	 * Prototype value suitable to be presented to a human. Must not be persisted or logged.
	 *
	 * @return a localized value that will prevent this particular problem when parsed using
	 *         interactive parsing.
	 */
	public abstract String getInteractivePrototype();

	@Override
	public String getMessage() {
		return MessageFormat.format(problem.logMsg, badString, getPersistablePrototype());
	}

	@Override
	public String getLocalizedMessage() {
		return MessageFormat.format(problem.localizedMsg, badString, getInteractivePrototype());
	}

	@Override
	public String toString() {
		String s = getClass().getName();
		// FIXME: toString() is used for logging and debugging. Get a non-localized string for this.
		String message = getMessage();
		return (message != null) ? (s + ": " + message) : s; //$NON-NLS-1$
	}
}
