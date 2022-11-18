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

import java.util.List;
import java.util.NoSuchElementException;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCStackTrace;

/**
 * Base class for stack traces.
 */
// FIXME: Move MC* classes and related toolkits to a separate package?
public class MCStackTrace implements IMCStackTrace {
	private final List<IMCFrame> frames;
	private final TruncationState truncationState;
	private final int hashCode;

	/**
	 * Create a new stack trace instance.
	 *
	 * @param frames
	 *            the frames of the stack trace, see {@link IMCStackTrace#getFrames()}
	 * @param truncationState
	 *            the stack trace truncation state
	 */
	public MCStackTrace(List<IMCFrame> frames, TruncationState truncationState) {
		this(frames, truncationState, calcMethodHash(frames, truncationState));
	}

	private MCStackTrace(List<IMCFrame> frames, TruncationState truncationState, int hashCode) {
		this.frames = frames;
		this.truncationState = truncationState;
		this.hashCode = hashCode;
	}

	@Override
	public final List<IMCFrame> getFrames() {
		return frames;
	}

	@Override
	public TruncationState getTruncationState() {
		return truncationState;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof MCStackTrace) {
			MCStackTrace other = ((MCStackTrace) obj);
			return other.truncationState == truncationState && other.frames.equals(frames);
		}
		return false;
	}

	@Override
	public IMCStackTrace tail() {
		if (frames.isEmpty()) {
			throw new NoSuchElementException("stack trace is empty");
		}
		// polynomial hashcodes allow removal of additive terms,
		// which saves recomputing the hash of the rest of the stacktrace
		int constant = getHashCoefficient(frames.size() + 1);
		int power = getHashCoefficient(frames.size());
		int hash = hashCode - frames.get(0).hashCode() * power - constant + power;
		return new MCStackTrace(frames.subList(1, frames.size()), truncationState, hash);
	}

	@Override
	public IMCStackTrace tail(int framesToRemove) {
		if (frames.size() < framesToRemove) {
			throw new NoSuchElementException("stack trace has less than " + framesToRemove + " frames");
		}

		int hash = hashCode;
		int constant = getHashCoefficient(frames.size() + 1);
		for (int i = 0; i < framesToRemove; i++) {
			int power = getHashCoefficient(frames.size() - i);
			hash = hash - frames.get(i).hashCode() * power - constant + power;
			constant = power;
		}

		return new MCStackTrace(frames.subList(framesToRemove, frames.size()), truncationState, hash);
	}

	private static int calcMethodHash(List<IMCFrame> frames, TruncationState truncationState) {
		return truncationState.hashCode() + 31 * frames.hashCode();
	}

	private static int getHashCoefficient(int listSize) {
		if (listSize >= HASH_POWERS.length) {
			int power = HASH_POWERS[HASH_POWERS.length - 1];
			for (int i = HASH_POWERS.length - 1; i < listSize; i++) {
				power *= 31;
			}
			return power;
		}
		return HASH_POWERS[listSize];
	}

	private static final int[] HASH_POWERS = new int[257];
	static {
		for (int hash = 1, power = 0; power < HASH_POWERS.length; power++) {
			HASH_POWERS[power] = hash;
			hash *= 31;
		}
	}
}
