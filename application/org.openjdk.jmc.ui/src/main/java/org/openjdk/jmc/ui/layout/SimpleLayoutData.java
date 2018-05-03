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
package org.openjdk.jmc.ui.layout;

import org.eclipse.swt.widgets.Composite;

/**
 * Layout data for a {@link SimpleLayout} that sets restriction on how the component should be
 * size.It can have a:
 * <ul>
 * <li>a minimum size</li>
 * <li>a maximum size</li>
 * <li>a weight factor that determines how much excessive space the component should grab, in
 * comparison to the weight factor of other children with the same parent component</li>
 * <li>a minimum and maximum horizontal ratio that determines how it should be sized in relation to
 * the height of the component</li>
 * </ul>
 */
public class SimpleLayoutData {
	// Constraint for horizontal ratio
	private float m_minHorizontalRatio = 0.0f;
	private float m_maxHorizontalRatio = SimpleLayout.INFINITE_RATIO;

	// Weight factor for excessive space
	private float m_weight = SimpleLayout.INIFINITE_WEIGHT;

	// Minimum and maximum size of the component
	private int m_minSize = SimpleLayout.ZERO_SIZE;
	private int m_maxSize = Integer.MAX_VALUE;
	private boolean m_visible = true;

	public SimpleLayoutData() {

	}

	public SimpleLayoutData(float weight) {
		setWeight(weight);
	}

	/**
	 * Sets the maximum size of the controls. If the control does not have a maximum size you can
	 * set it to {@link SimpleLayout#INFINITE_SIZE}, which is the default value. If the maximum size
	 * is less than the minimum size or or a size calculated by the minimum horizontal ratio, the
	 * maximum size is ignored.
	 * <p>
	 * If the size is equal to {@link SimpleLayout#PREFERRED_SIZE} the maximum size will be the
	 * preferred size for the component. That is what a call to
	 * {@link Control#computeSize(SWT.DEFAULT, height, true)} or {@link Control#computeSize(width,
	 * SWT.DEFAUKT, true)} would return.
	 *
	 * @param size
	 *            the maximum size, must be positive
	 */
	public void setMaxSize(int size) {
		m_maxSize = size;
	}

	/**
	 * Sets the minimum size of the component. If the component does not have a minimum size you can
	 * set it to 0, which is also the default. If the minimum size should be larger than the max
	 * size or a size calculated by by the maximum aspect ratio, the minimum size is ignored.
	 * <p>
	 * If the size is equal to {@link SimpleLayout#PREFERRED_SIZE} the maximum size will be the
	 * preferred size for the component. That is what a call to
	 * {@link Control#computeSize(SWT.DEFAULT, height, true)} or {@link Control#computeSize(width,
	 * SWT.DEFAUKT, true)} would return.
	 *
	 * @param minSize
	 *            the minimum size of the component, must be a positive value
	 */
	public void setMinSize(int size) {
		m_minSize = size;
	}

	/**
	 * Sets a weight factor that determines the amount of space the control should grab when it
	 * competes with other controls for excessive space. Let's say control A and B are the only
	 * children of a {@link Composite} and that they have a weight factor of 3 and 5 then control A
	 * will get 63 pixel and control B 37 pixels, given that the total amount of excessive space is
	 * 100 pixels.
	 * <p>
	 * Excessive space is defined as aggregated space for all controls that exceeds their minimum
	 * size. The component with the largest weight will win if there is tie for a pixel. In the
	 * example above A should really get 3*100/12 = 37.5 and B get 5*100/12 = 62.5 but A has larger
	 * weight so it will get half a pixel from B. If the weights are the same and there is a draw
	 * the component that was added to the parent the first will win.
	 * <p>
	 * The default weight factor is MCLayout.INIFINITE_WEIGHT. If all controls have an infinitive
	 * weight they will all get the same amount of excessive space.
	 */
	public void setWeight(float weight) {
		m_weight = weight;
	}

	/**
	 * Sets the maximum horizontal ratio This is an advanced option and allows the control to be
	 * constrained horizontally by the height of the default height of the control.
	 * <p>
	 * That is, the width of the control can't exceed the ratio * height of the component. This
	 * option is typically used when you want a control to be completely square or circular. It can
	 * also be used if the control consists of a bitmap and you want it to scale, but without the
	 * image losing its proportions.
	 * <p>
	 * If you want the control to be complete square you would set the the maximum and minimum
	 * horizontal ratio to 1.0. If you have bitmap that has a width of 256 pixel and a height of 64
	 * pixel you would set the minimum and maximum horizontal ratio to 4.
	 *
	 * @param ratio
	 *            the ratio between the width and the height of the control that can't be exceeded.
	 *            If you want the control to disregard the maximum horizontal ratio set it to 0,
	 *            which is also the default.
	 */
	public void setMaxHorizontalRatio(float ratio) {
		m_maxHorizontalRatio = ratio;
	}

	/**
	 * Sets the minimal horizontal ratio This is an advanced option and allows the control to be
	 * constrained horizontally by the height of the component.
	 * <p>
	 * That is, the width of the component in pixels can't be below ratio * height of the control.
	 * This option is typically used when you want a control to be completely square or circular. It
	 * can also be used if the control consists of a bitmap and you want it to scale but without the
	 * image losing it's proportions.
	 * <p>
	 * If you want the control to be complete square you would set the the maximum and minimum
	 * horizontal ratio to 1. If you have bitmap that has a width of 256 pixel and a height of 64
	 * pixel you would set the minimum and maximum horizontal ratio to 4.
	 *
	 * @param ratio
	 *            the ratio between the width and the height of the component that control can't
	 *            fall below. If you want the control to disregard a minimum horizontal ratio set it
	 *            to 0, which is also the default.
	 */
	public void setMinHorizontalRatio(float ratio) {
		m_minHorizontalRatio = ratio;
	}

	/**
	 * Returns the maximum size. See {@link #setMaxSize(int)} for more information
	 *
	 * @return the maximum size
	 */
	public int getMaximumSize() {
		return m_maxSize;
	}

	/**
	 * Returns the aspect ratio. See {@link #setMinSize(int)}
	 *
	 * @return the minimum horizontal ratio
	 */
	public float getMinimumHorizontalRatio() {
		return m_minHorizontalRatio;
	}

	/**
	 * Returns the maximum horizontal ratio. See {@link #setMaxHorizontalRatio(double)} for more
	 * information.
	 *
	 * @return the maximum horizontal ratio
	 */
	public float getMaximumHorizontalRatio() {
		return m_maxHorizontalRatio;
	}

	/**
	 * Returns the minimum size. Default is 0. See {@link #setMinSize(int)} for more information.
	 *
	 * @return the minimum size
	 */
	public int getMinimumSize() {
		return m_minSize;
	}

	/**
	 * Returns the weight factor. The default is {@link SimpleLayout#INIFINITE_WEIGHT}.
	 * <p>
	 * See {@link #setWeight(float)} for more information.
	 *
	 * @return the weight.
	 */
	public float getWeight() {
		return m_weight;
	}

	/**
	 * Returns if the control should be visible or not
	 *
	 * @return true if the control should be visible
	 */
	public boolean isVisible() {
		return m_visible;
	}

	/**
	 * Sets if the control should be visible.
	 *
	 * @param visible
	 *            flag indicating if the control should be visible
	 */
	public void setVisible(boolean visible) {
		m_visible = visible;
	}
}
