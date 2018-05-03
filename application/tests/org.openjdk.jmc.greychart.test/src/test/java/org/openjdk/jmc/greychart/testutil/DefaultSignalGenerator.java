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
package org.openjdk.jmc.greychart.testutil;

import java.util.Random;

/**
 * Silly little configurable signal generator.
 */
public class DefaultSignalGenerator implements SignalGenerator {
	/**
	 * Level signal that stays on the init value.
	 */
	public static final int LEVEL = 0;

	/**
	 * Random noise.
	 */
	public static final int RANDOM = 1;

	/**
	 * Saw shaped signal.
	 */
	public static final int SAW = 2;

	/**
	 * Sinus shaped signal.
	 */
	public static final int SINUS = 3;

	private final double m_min;
	private final double m_max;
	private final double m_startValue;
	// Stored as double to avoid cast in calculations
	private final double m_period;
	private final long m_startTime;
	private final int m_type;
	private final SignalGenerator m_internalGenerator;

	/**
	 * Constructor.
	 *
	 * @param type
	 *            the type of signal generator. LEVEL | RANDOM | SAW | SINUS
	 * @param period
	 *            the period in ms. Only interesting if type is SAW or SINUS.
	 * @param min
	 *            the minimum level
	 * @param max
	 *            the maximum level
	 * @param startValue
	 *            the startvalue
	 * @param startTime
	 *            the start time. currentTimeMilis is usually a good one.
	 */
	public DefaultSignalGenerator(int type, long period, double min, double max, double startValue, long startTime) {
		m_type = type;
		m_period = period;
		m_min = min;
		m_max = max;
		m_startValue = startValue;
		m_startTime = startTime;
		switch (m_type) {
		case LEVEL:
			m_internalGenerator = new LevelGenerator();
			break;
		case RANDOM:
			m_internalGenerator = new RandomGenerator();
			break;
		case SAW:
			m_internalGenerator = new SawGenerator();
			break;
		case SINUS:
		default:
			m_internalGenerator = new SinusGenerator();
		}
	}

	private class LevelGenerator implements SignalGenerator {
		/**
		 * @see org.openjdk.jmc.greychart.testutil.SignalGenerator#getValue()
		 */
		@Override
		public double getValue() {
			return m_startValue;
		}

		/**
		 * @see org.openjdk.jmc.greychart.testutil.SignalGenerator#getValue(long)
		 */
		@Override
		public double getValue(long time) {
			return getValue();
		}
	}

	private class RandomGenerator implements SignalGenerator {
		Random m_random = new Random();

		/**
		 * @see org.openjdk.jmc.greychart.testutil.SignalGenerator#getValue()
		 */
		@Override
		public double getValue() {
			return m_random.nextDouble() * (m_max - m_min) + m_min;
		}

		/**
		 * @see org.openjdk.jmc.greychart.testutil.SignalGenerator#getValue(long)
		 */
		@Override
		public double getValue(long time) {
			return getValue();
		}
	}

	private class SawGenerator implements SignalGenerator {
		/**
		 * @see org.openjdk.jmc.greychart.testutil.SignalGenerator#getValue()
		 */
		@Override
		public double getValue() {
			return getValue(System.currentTimeMillis());
		}

		/**
		 * @see org.openjdk.jmc.greychart.testutil.SignalGenerator#getValue(long)
		 */
		@Override
		public double getValue(long time) {
			long diff = time - m_startTime;
			return (diff % m_period) / m_period * (m_max - m_min) + m_min;
		}
	}

	private class SinusGenerator implements SignalGenerator {
		double m_modifierAngle;

		/**
		 * Constructor.
		 */
		public SinusGenerator() {
			m_modifierAngle = Math.asin((m_startValue - m_min) * 2 / (m_max - m_min) - 1);
		}

		/**
		 * @see org.openjdk.jmc.greychart.testutil.SignalGenerator#getValue()
		 */
		@Override
		public double getValue() {
			return getValue(System.currentTimeMillis());
		}

		/**
		 * @see org.openjdk.jmc.greychart.testutil.SignalGenerator#getValue(long)
		 */
		@Override
		public double getValue(long time) {
			long diff = time - m_startTime;
			return (Math.sin((diff % m_period) / m_period * 2 * Math.PI + m_modifierAngle) + 1) / 2 * (m_max - m_min)
					+ m_min;
		}
	}

	/**
	 * @see org.openjdk.jmc.greychart.testutil.SignalGenerator#getValue()
	 */
	@Override
	public double getValue() {
		return m_internalGenerator.getValue();
	}

	/**
	 * Little test.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		DefaultSignalGenerator signal = new DefaultSignalGenerator(SINUS, 20000, -5, 5, 0, System.currentTimeMillis());
		while (System.currentTimeMillis() - startTime <= 20100) {
			System.out.println(signal.getValue());
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @see org.openjdk.jmc.greychart.testutil.SignalGenerator#getValue(long)
	 */
	@Override
	public double getValue(long time) {
		return m_internalGenerator.getValue(time);
	}
}
