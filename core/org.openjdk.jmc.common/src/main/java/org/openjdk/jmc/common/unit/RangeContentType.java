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

import static org.openjdk.jmc.common.IDisplayable.AUTO;
import static org.openjdk.jmc.common.IDisplayable.EXACT;
import static org.openjdk.jmc.common.IDisplayable.VERBOSE;

import java.text.MessageFormat;

import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.messages.internal.Messages;

// FIXME: Make package private if possible to restructure packages.
public abstract class RangeContentType<T extends Comparable<? super T>> extends StructContentType<IRange<T>> {
	private final ContentType<T> endPointType;

	static RangeContentType<IQuantity> create(KindOfQuantity<?> endPointType) {
		RangeContentType<IQuantity> rangeType = new RangeContentType<IQuantity>(endPointType,
				endPointType.getDeltaKind()) {
			@Override
			public IRange<IQuantity> rangeWithEnd(IQuantity start, IQuantity end) {
				return QuantityRange.createWithEnd(start, end);
			}
		};

		for (String hint : new String[] {AUTO, EXACT, VERBOSE}) {
			final IFormatter<IRange<IQuantity>> formatter = endPointType.getRangeFormatter(hint);
			// FIXME: Drop DisplayFormatter, empower hints with IDescribable. Or something.
			if (formatter != null) {
				rangeType.addFormatter(new DisplayFormatter<IRange<IQuantity>>(rangeType, hint, hint) {
					@Override
					public String format(IRange<IQuantity> range) {
						return formatter.format(range);
					}
				});
			}
		}
		return rangeType;
	}

	static <T extends Comparable<? super T>> RangeContentType<T> create(ContentType<T> endPointType) {
		assert !(endPointType instanceof KindOfQuantity);
		RangeContentType<T> rangeType = new RangeContentType<T>(endPointType, endPointType) {
			@Override
			public IRange<T> rangeWithEnd(T start, T end) {
				throw new UnsupportedOperationException("Range creation not implemented"); //$NON-NLS-1$
			}
		};

		// A little backwards to do it this way, and could cause infinite recursion, but it'll do for now.
		rangeType.addFormatter(new DisplayFormatter<IRange<T>>(rangeType, AUTO, "Value") { //$NON-NLS-1$
			@Override
			public String format(IRange<T> o) {
				return o.displayUsing(AUTO);
			}
		});
		return rangeType;
	}

	private RangeContentType(ContentType<T> endPointType, ContentType<T> deltaType) {
		// This content type should typically not be visible anywhere, so the name in particular shouldn't matter.
		super(endPointType.getIdentifier() + "-range", //$NON-NLS-1$
				MessageFormat.format(Messages.getString(Messages.RangeContentType_NAME), endPointType.getName()));
		this.endPointType = endPointType;

		addField("start", endPointType, Messages.getString(Messages.RangeContentType_FIELD_START), null, //$NON-NLS-1$
				new IMemberAccessor<T, IRange<T>>() {
					@Override
					public T getMember(IRange<T> range) {
						return range.getStart();
					}
				});

		addField("end", endPointType, Messages.getString(Messages.RangeContentType_FIELD_END), null, //$NON-NLS-1$
				new IMemberAccessor<T, IRange<T>>() {
					@Override
					public T getMember(IRange<T> range) {
						return range.getEnd();
					}
				});

		addField("center", endPointType, Messages.getString(Messages.RangeContentType_FIELD_CENTER), null, //$NON-NLS-1$
				new IMemberAccessor<T, IRange<T>>() {
					@Override
					public T getMember(IRange<T> range) {
						return range.getCenter();
					}
				});

		addField("extent", deltaType, Messages.getString(Messages.RangeContentType_FIELD_EXTENT), null, //$NON-NLS-1$
				new IMemberAccessor<T, IRange<T>>() {
					@Override
					public T getMember(IRange<T> range) {
						return range.getExtent();
					}
				});
	}

	public abstract IRange<T> rangeWithEnd(T start, T end);

	public ContentType<T> getEndPointContentType() {
		return endPointType;
	}
}
