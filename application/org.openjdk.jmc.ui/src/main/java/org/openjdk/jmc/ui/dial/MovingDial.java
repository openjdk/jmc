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
package org.openjdk.jmc.ui.dial;

import java.awt.Color;

import org.openjdk.jmc.ui.dial.MovingDial.RK2Solver.DifferentialEquation;

/**
 * {@link IDialProvider} that simulates a moving dial.
 */
public class MovingDial implements IDialProvider {
	/**
	 * Class for solving differential equations numerical using the Runge-Kutta method of the second
	 * degrees(RK2).
	 */
	static public class RK2Solver {
		public static interface DifferentialEquation {
			double evaluate(double t, double[] y);
		}

		public static double[] solve(double[] x, double t, double h, DifferentialEquation[] f) {
			double[] a = evaluate(f, t, x);
			double[] b = evaluate(f, t + h / 2, add(x, multiply(h / 2, a)));
			double[] c = evaluate(f, t + h / 2, add(x, multiply(h / 2, b)));
			double[] d = evaluate(f, t + h, add(x, multiply(h, c)));

			return add(x, multiply(h / 6, add(multiply(2, add(b, c)), add(a, d))));
		}

		private static double[] evaluate(DifferentialEquation f[], double t, double[] x) {
			double[] result = new double[f.length];
			for (int n = 0; n < result.length; n++) {
				result[n] = f[n].evaluate(t, x);
			}
			return result;
		}

		private static double[] multiply(double constant, double[] v) {
			double[] result = new double[v.length];
			for (int n = 0; n < result.length; n++) {
				result[n] = constant * v[n];
			}
			return result;
		}

		private static double[] add(double[] v1, double[] v2) {
			double[] result = new double[v1.length];
			for (int n = 0; n < result.length; n++) {
				result[n] = v1[n] + v2[n];
			}
			return result;
		}
	}

	private double m_damping = 0.9;
	private double m_mass = 0.05;
	private double m_stiffness = 2.0;
	private double m_target = 0.0;

	private double m_timeOffset = Double.NaN;
	private double m_lastTime = 0.0;
	private double m_position = 0.0;
	private double m_velocity = 0.0;
	private double m_maxIterationStep = 0.060;
	private double m_maximumSimulationLength = 2.0;

	private final String m_id;
	private Color m_color;

	public MovingDial(String id, Color color) {
		m_color = color;
		m_id = id;
	}

	public MovingDial(String id) {
		this(id, new Color(0, 0, 0));
	}

	/**
	 * Changes the velocity of the dial momentarily. Typically used to set up the initial velocity.
	 * By default zero.
	 *
	 * @param velocity
	 *            the velocity
	 */
	public void setCurrentVelocity(double velocity) {
		m_velocity = velocity;
	}

	/**
	 * Sets the position of the dial momentarily. Typically uised to set up the initial position.
	 * Zero by default
	 *
	 * @param position
	 *            the position
	 */
	public void setCurrentPosition(double position) {
		m_position = position;
	}

	/**
	 * Sets the damping of the dial. A value between 0 and 1.0 . A low value means the dial will
	 * swing back and forth a lot before it stabilizes. 0.9 by default.
	 *
	 * @param damping
	 *            the damping
	 */
	public void setDamping(double damping) {
		m_damping = damping;
	}

	/**
	 * Sets the stiffness of the dial. This control how fast the dial moves between to positions. A
	 * high value means it moves fast. 2.0 by default.
	 *
	 * @param stiffness
	 *            the stiffness
	 */
	public void setStiffness(double stiffness) {
		m_stiffness = stiffness;
	}

	/**
	 * Sets the value the dial should move to.
	 *
	 * @param target
	 *            the target the dial should move to.
	 */
	public void setTarget(double target) {
		m_target = target;
	}

	/**
	 * Sets the mass of the dial. A dial with a high mass accelerates and deceleration slower. 0.05
	 * by default
	 *
	 * @param mass
	 *            the mass
	 */
	public void setMass(double mass) {
		m_mass = mass;
	}

	/**
	 * The color of the dial.
	 */
	@Override
	public Color getColor(Object input) {
		return m_color;
	}

	public void setColor(Color color) {
		m_color = color;
	}

	/**
	 * The current value of the dial.
	 */
	@Override
	public double getValue(Object input) {
		if (input instanceof Number) {
			if (getLastCalculatedTime() == Double.NEGATIVE_INFINITY) {
				setCurrentPosition(((Number) input).doubleValue());
			}
			setTarget(((Number) input).doubleValue());
			runSimulation();
			return getLastCalculatedPosition();
		}
		return Double.NEGATIVE_INFINITY;
	}

	/**
	 * Sets the maximum length of an iteration when simulating the a moving dial, without breaking
	 * it up into two or more steps
	 *
	 * @param seconds
	 *            number seconds.
	 */
	public void setIterationLength(long seconds) {
		m_maxIterationStep = seconds;
	}

	/**
	 * Update the simulation to the current time.
	 */
	public void runSimulation() {
		final double time = currentTime();
		final double elapsed = time - getLastCalculatedTime();
		final double maxIterationStep = getMaximumIterationLength();
		if (elapsed != 0.0) {
			if (elapsed > getMaximumSimulationLength()) {
				setCurrentPosition(getTarget());
				setCurrentVelocity(0);
				setCurrentTime(time);
			} else {
				for (double t = getLastCalculatedTime() + maxIterationStep; t < time; t += maxIterationStep) {
					simulate(t, maxIterationStep);
				}
				simulate(time, time - getLastCalculatedTime());
			}
		}
	}

	/**
	 * Returns the maximum time the simulation should run without moving the dial to the target
	 * immediately.
	 *
	 * @return the maximum simulation time
	 */
	public double getMaximumSimulationLength() {
		return m_maximumSimulationLength;
	}

	/**
	 * Sets the maximum time the simulation should run without moving the dial to the target
	 * immediately.
	 */
	public void setMaximumSimulationLength(double seconds) {
		m_maximumSimulationLength = seconds;
	}

	/**
	 * Returns the maximum iteration length, in seconds, when simulation the dial movement
	 *
	 * @return
	 */
	public double getMaximumIterationLength() {
		return m_maxIterationStep;
	}

	/**
	 * Simulate from time t
	 *
	 * @param time
	 * @param e
	 */
	private void simulate(final double time, double e) {
		final double m = getMass();
		final double k = getStiffness();
		final double b = getDamping();
		final double p = getTarget();

		double[] y = new double[] {getLastCalculatedPosition(), getLastCalculatedVelocity()};
		y = RK2Solver.solve(y, time, e, new DifferentialEquation[] {new DifferentialEquation() {
			@Override
			public double evaluate(double t, double[] x) {
				return x[1];
			}
		}, new DifferentialEquation() {
			@Override
			public double evaluate(double t, double[] x) {
				return (-k * (x[0] - p) - b * x[1]) / m;
			}
		}});
		setCurrentPosition(y[0]);
		setCurrentVelocity(y[1]);
		setCurrentTime(time);
	}

	/**
	 * Returns the current target for the dial.
	 *
	 * @return the target the dial is moving to
	 */
	public double getTarget() {
		return m_target;
	}

	/**
	 * Returns the damping that is used.
	 *
	 * @return the damping
	 */
	public double getDamping() {
		return m_damping;
	}

	/**
	 * Returns the stiffness that is used.
	 *
	 * @return the stiffness
	 */
	public double getStiffness() {
		return m_stiffness;
	}

	/**
	 * Returns the mass that is used.
	 *
	 * @return
	 */
	public double getMass() {
		return m_mass;
	}

	/**
	 * Returns the last time value the dial position was calculated for. Time is in seconds from the
	 * first call to {@link IDialProvider#getValue(Object)}
	 *
	 * @return the last time the position was calculated.
	 */
	public double getLastCalculatedTime() {
		return m_lastTime;
	}

	/**
	 * Returns the last calculated velocity for the dial.
	 *
	 * @return the last calculated velocity.
	 */
	public double getLastCalculatedVelocity() {
		return m_velocity;
	}

	/**
	 * Returns the last calculated position for the dial.
	 *
	 * @return the last calculated position.
	 */
	public double getLastCalculatedPosition() {
		return m_position;
	}

	/**
	 * Sets the last time the dial position was calculated
	 *
	 * @param time
	 */
	private void setCurrentTime(double time) {
		m_lastTime = time;
	}

	/**
	 * Returns the current time, with offset from the first call.
	 *
	 * @return
	 */
	private double currentTime() {
		if (Double.isNaN(m_timeOffset)) {
			m_timeOffset = System.currentTimeMillis() / 1000.0;
			m_lastTime = 0;
		}
		return System.currentTimeMillis() / (1000.0) - m_timeOffset;
	}

	@Override
	public String getId() {
		return m_id;
	}
}
