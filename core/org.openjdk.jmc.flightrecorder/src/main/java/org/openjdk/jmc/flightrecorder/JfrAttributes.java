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
package org.openjdk.jmc.flightrecorder;

import static org.openjdk.jmc.common.item.Attribute.attr;
import static org.openjdk.jmc.common.unit.UnitLookup.MEMORY;
import static org.openjdk.jmc.common.unit.UnitLookup.PLAIN_TEXT;
import static org.openjdk.jmc.common.unit.UnitLookup.STACKTRACE;
import static org.openjdk.jmc.common.unit.UnitLookup.THREAD;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMERANGE;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESPAN;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESTAMP;
import static org.openjdk.jmc.common.unit.UnitLookup.TYPE;

import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.MemberAccessorToolkit;
import org.openjdk.jmc.flightrecorder.messages.internal.Messages;

/**
 * Attributes that are common to most flight recorder event types.
 */
public interface JfrAttributes {

	IAttribute<IType<?>> EVENT_TYPE = Attribute.canonicalize(new Attribute<IType<?>>("(eventType)", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_EVENT_TYPE), Messages.getString(Messages.ATTR_EVENT_TYPE_DESC), TYPE) {
		@Override
		public <U> IMemberAccessor<IType<?>, U> customAccessor(final IType<U> type) {
			return MemberAccessorToolkit.constant(type);
		}
	});
	
	IAttribute<String> EVENT_TYPE_ID = Attribute.canonicalize(new Attribute<String>("(eventTypeId)", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_EVENT_TYPE_ID), Messages.getString(Messages.ATTR_EVENT_TYPE_ID_DESC), PLAIN_TEXT) {
		@Override
		public <U> IMemberAccessor<String, U> customAccessor(final IType<U> type) {
			return MemberAccessorToolkit.constant(type.getIdentifier());
		}
	});
	
	IAttribute<IQuantity> END_TIME = Attribute.canonicalize(
			new Attribute<IQuantity>("(endTime)", Messages.getString(Messages.ATTR_END_TIME), null, TIMESTAMP) { //$NON-NLS-1$
				@Override
				public <U> IMemberAccessor<IQuantity, U> customAccessor(IType<U> type) {
					IMemberAccessor<IQuantity, U> stAccessor = type.getAccessor(START_TIME.getKey());
					IMemberAccessor<IQuantity, U> durationAccessor = type.getAccessor(DURATION.getKey());
					if (stAccessor != null && durationAccessor != null) {
						return MemberAccessorToolkit.sum(stAccessor, durationAccessor);
					} else {
						return stAccessor;
					}
				}
			});

	IAttribute<IQuantity> START_TIME = Attribute.canonicalize(
			new Attribute<IQuantity>("startTime", Messages.getString(Messages.ATTR_START_TIME), null, TIMESTAMP) { //$NON-NLS-1$
				@Override
				public <U> IMemberAccessor<IQuantity, U> customAccessor(IType<U> type) {
					IMemberAccessor<IQuantity, U> etAccessor = type.getAccessor(END_TIME.getKey());
					IMemberAccessor<IQuantity, U> durationAccessor = type.getAccessor(DURATION.getKey());
					if (etAccessor != null && durationAccessor != null) {
						return MemberAccessorToolkit.difference(etAccessor, durationAccessor);
					} else {
						return etAccessor;
					}
				}
			});

	IAttribute<IQuantity> DURATION = Attribute.canonicalize(
			new Attribute<IQuantity>("duration", Messages.getString(Messages.ATTR_DURATION), null, TIMESPAN) { //$NON-NLS-1$
				@Override
				public <U> IMemberAccessor<IQuantity, U> customAccessor(IType<U> type) {
					IMemberAccessor<IQuantity, U> etAccessor = type.getAccessor(END_TIME.getKey());
					IMemberAccessor<IQuantity, U> stAccessor = type.getAccessor(START_TIME.getKey());
					if (etAccessor == null || stAccessor == null || etAccessor == stAccessor) {
						return MemberAccessorToolkit.constant(UnitLookup.SECOND.quantity(0));
					} else {
						return MemberAccessorToolkit.difference(etAccessor, stAccessor);
					}
				}
			});

	IAttribute<IQuantity> CENTER_TIME = Attribute.canonicalize(
			new Attribute<IQuantity>("(centerTime)", Messages.getString(Messages.ATTR_CENTER_TIME), null, TIMESTAMP) { //$NON-NLS-1$
				@Override
				public <U> IMemberAccessor<IQuantity, U> customAccessor(IType<U> type) {
					IMemberAccessor<IQuantity, U> stAccessor = type.getAccessor(START_TIME.getKey());
					IMemberAccessor<IQuantity, U> etAccessor = type.getAccessor(END_TIME.getKey());
					IMemberAccessor<IQuantity, U> durationAccessor = type.getAccessor(DURATION.getKey());
					if (stAccessor != null) {
						if (durationAccessor != null) {
							return MemberAccessorToolkit.addHalfDelta(stAccessor, durationAccessor);
						} else if (etAccessor != null) {
							return MemberAccessorToolkit.avg(stAccessor, etAccessor);
						}
						return stAccessor;
					} else if (etAccessor != null) {
						if (durationAccessor != null) {
							return MemberAccessorToolkit.subtractHalfDelta(etAccessor, durationAccessor);
						}
						return etAccessor;
					}
					return null;
				}
			});

	IAttribute<IRange<IQuantity>> LIFETIME = Attribute.canonicalize(new Attribute<IRange<IQuantity>>("(lifetime)", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_LIFETIME), null, TIMERANGE) {
		@Override
		public <U> IMemberAccessor<IRange<IQuantity>, U> customAccessor(IType<U> type) {
			IMemberAccessor<IQuantity, U> stAccessor = type.getAccessor(START_TIME.getKey());
			IMemberAccessor<IQuantity, U> etAccessor = type.getAccessor(END_TIME.getKey());
			IMemberAccessor<IQuantity, U> durationAccessor = type.getAccessor(DURATION.getKey());
			if (stAccessor != null) {
				if (durationAccessor != null) {
					return MemberAccessorToolkit.rangeWithExtent(stAccessor, durationAccessor);
				} else if (etAccessor != null) {
					return MemberAccessorToolkit.rangeWithEnd(stAccessor, etAccessor);
				}
				return MemberAccessorToolkit.pointRange(stAccessor);
			} else if (etAccessor != null) {
				if (durationAccessor != null) {
					return MemberAccessorToolkit.rangeWithExtentEnd(durationAccessor, etAccessor);
				}
				return MemberAccessorToolkit.pointRange(etAccessor);
			}
			return null;
		}
	});

	IAttribute<IMCStackTrace> EVENT_STACKTRACE = attr("stackTrace", Messages.getString(Messages.ATTR_EVENT_STACKTRACE), //$NON-NLS-1$
			STACKTRACE);
	IAttribute<IMCThread> EVENT_THREAD = attr("eventThread", Messages.getString(Messages.ATTR_EVENT_THREAD), //$NON-NLS-1$
			Messages.getString(Messages.ATTR_EVENT_THREAD_DESC), THREAD);
	IAttribute<IQuantity> EVENT_TIMESTAMP = attr(END_TIME.getIdentifier(),
			Messages.getString(Messages.ATTR_EVENT_TIMESTAMP), Messages.getString(Messages.ATTR_EVENT_TIMESTAMP_DESC),
			END_TIME.getContentType());
	IAttribute<IQuantity> FLR_DATA_LOST = attr("amount", Messages.getString(Messages.ATTR_FLR_DATA_LOST), //$NON-NLS-1$
			Messages.getString(Messages.ATTR_FLR_DATA_LOST_DESC), MEMORY);
}
