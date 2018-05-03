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
package org.openjdk.jmc.joverflow.stats;

/**
 * Detects all the problems that can occur with primitive arrays: length 0, length 1, empty
 * (contains only zeros), long zero-tail (more than half consecutive elements, from the back, equal
 * to zero), and unused high bytes (high bytes in each element are unused, e.g. all elements in an
 * int[] array would fit into shorts).
 */
public class PrimitiveArrayHandler {
	private final byte[] data;
	private final int elSize;
	private final boolean isFloatOrDouble;
	private boolean length0, length1, empty;
	private int lztOvhd = -1, unusedHighBytesOvhd = -1;

	static PrimitiveArrayHandler createInstance(byte[] data, int elSize, boolean isFloatOrDouble) {
		return new PrimitiveArrayHandler(data, elSize, isFloatOrDouble);
	}

	/** Returns true if the underlying array has length 0 */
	public boolean isLength0() {
		return length0;
	}

	/** Returns true if the underlying array has length 1 */
	public boolean isLength1() {
		return length1;
	}

	/** Returns true if the underlying array is empty, i.e. all its elements are 0 */
	public boolean isEmpty() {
		return empty;
	}

	/**
	 * Returns a positive overhead value if the underlying array is "long zero-tail", i.e. half of
	 * more of its elements from the back are all zeros.
	 */
	public int getLztOverhead() {
		return lztOvhd;
	}

	/**
	 * Returns a positive overhead value if in the underlying array (that should not be a byte[],
	 * float[] or double[]), all elements don't utilize the upper byte (for short[] and char[]), the
	 * upper two or three bytes (for int[]), or the upper four, six or seven bytes (for long[]).
	 */
	public int getUnusedHighBytesOvhd() {
		return unusedHighBytesOvhd;
	}

	private PrimitiveArrayHandler(byte[] data, int elSize, boolean isFloatOrDouble) {
		this.data = data;
		this.elSize = elSize;
		this.isFloatOrDouble = isFloatOrDouble;
		checkForProblems();
	}

	private void checkForProblems() {
		int numElements = data.length / elSize;

		if (data.length == 0) {
			length0 = true;
			return; // Cannot have any other problems
		}

		if (numElements == 1) {
			length1 = true;
		}

		switch (elSize) {
		case 1:
			checkForElSize1();
			break;
		case 2:
			checkForElSize2();
			break;
		case 4:
			checkForElSize4();
			break;
		case 8:
			checkForElSize8();
			break;
		default:
			throw new IllegalArgumentException("Unsupported elSize: " + elSize);
		}
	}

	private void checkForElSize1() {
		int i;
		for (i = data.length - 1; i >= 0; i--) {
			if (data[i] != 0) {
				break;
			}
		}
		if (i < 0) {
			empty = true;
		} else if (i < data.length / 2) {
			lztOvhd = data.length - i + 1;
		}
	}

	private void checkForElSize2() {
		int zeroElementMinIndex = data.length;
		boolean highByteUnusedForAll = true, zeroTailSoFar = true;
		for (int i = data.length - 2; i >= 0; i -= 2) {
			int b1 = (data[i] & 0xff);
			int b2 = (data[i + 1] & 0xff);
			short value = (short) ((b1 << 8) + b2);

			if (value == 0 && zeroTailSoFar) {
				zeroElementMinIndex = i;
			} else {
				zeroTailSoFar = false;
				highByteUnusedForAll = (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE);
				// Stop as early as possible
				if (!highByteUnusedForAll) {
					break;
				}
			}
		}

		if (zeroElementMinIndex == 0) {
			empty = true;
		} else {
			if (zeroElementMinIndex < data.length / 2) {
				lztOvhd = data.length - zeroElementMinIndex;
			}
			if (highByteUnusedForAll) {
				unusedHighBytesOvhd = data.length / 2;
			}
		}
	}

	private void checkForElSize4() {
		int zeroElementMinIndex = data.length;
		// For float[] and double[] arrays, checking for unused high bytes doesn't work
		boolean threeBytesUnusedForAll = !isFloatOrDouble, twoBytesUnusedForAll = !isFloatOrDouble;
		boolean zeroTailSoFar = true;
		for (int i = data.length - 4; i >= 0; i -= 4) {
			int b1 = (data[i] & 0xFF);
			int b2 = (data[i + 1] & 0xFF);
			int b3 = (data[i + 2] & 0xFF);
			int b4 = (data[i + 3] & 0xFF);
			int value = (b1 << 24) + (b2 << 16) + (b3 << 8) + b4;

			if (value == 0 && zeroTailSoFar) {
				zeroElementMinIndex = i;
			} else {
				zeroTailSoFar = false;
				threeBytesUnusedForAll &= (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE);
				twoBytesUnusedForAll &= (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE);
				// Stop as early as possible
				if (!threeBytesUnusedForAll && !twoBytesUnusedForAll) {
					break;
				}
			}
		}

		if (zeroElementMinIndex == 0) {
			empty = true;
		} else {
			if (zeroElementMinIndex < data.length / 2) {
				lztOvhd = data.length - zeroElementMinIndex;
			}
			if (threeBytesUnusedForAll) {
				unusedHighBytesOvhd = data.length / 4 * 3;
			} else if (twoBytesUnusedForAll) {
				unusedHighBytesOvhd = data.length / 2;
			}
		}
	}

	private void checkForElSize8() {
		int zeroElementMinIndex = data.length;
		// For float[] and double[] arrays checking for unused high bytes doesn't work
		boolean sevenBytesUnusedForAll = !isFloatOrDouble, sixBytesUnusedForAll = !isFloatOrDouble;
		boolean fourBytesUnusedForAll = !isFloatOrDouble, zeroTailSoFar = true;
		for (int i = data.length - 8; i >= 0; i -= 8) {
			long value = 0;
			for (int j = i; j < i + 8; j++) {
				value <<= 8;
				int b = (data[j]) & 0xFF;
				value |= b;
			}

			if (value == 0 && zeroTailSoFar) {
				zeroElementMinIndex = i;
			} else {
				zeroTailSoFar = false;
				sevenBytesUnusedForAll &= (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE);
				sixBytesUnusedForAll &= (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE);
				fourBytesUnusedForAll &= (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE);
				// Stop as early as possible
				if (!sevenBytesUnusedForAll && !sixBytesUnusedForAll && !fourBytesUnusedForAll) {
					break;
				}
			}
		}

		if (zeroElementMinIndex == 0) {
			empty = true;
		} else {
			if (zeroElementMinIndex < data.length / 2) {
				lztOvhd = data.length - zeroElementMinIndex;
			}
			if (sevenBytesUnusedForAll) {
				unusedHighBytesOvhd = data.length / 8 * 7;
			} else if (sixBytesUnusedForAll) {
				unusedHighBytesOvhd = data.length / 8 * 6;
			} else if (fourBytesUnusedForAll) {
				unusedHighBytesOvhd = data.length / 2;
			}
		}
	}

}
