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
package org.openjdk.jmc.console.ui.tabs.memory;

import org.openjdk.jmc.common.unit.IQuantity;

public class MemoryPoolInformation {

	private volatile String m_poolName;
	private volatile String m_poolType;
	private volatile IQuantity m_curUsed;
	private volatile IQuantity m_curMax;
	private volatile IQuantity m_curUsage;
	private volatile IQuantity m_peakUsed;
	private volatile IQuantity m_peakMax;

	public void setPoolName(String name) {
		m_poolName = name;
	}

	public void setPoolType(String type) {
		m_poolType = type;
	}

	public void setCurUsed(IQuantity curUsed) {
		m_curUsed = curUsed;
	}

	public void setCurMax(IQuantity curMax) {
		m_curMax = curMax;
	}

	public void setCurUsage(IQuantity curUsage) {
		m_curUsage = curUsage;
	}

	public void setPeakUsed(IQuantity peakUsed) {
		m_peakUsed = peakUsed;
	}

	public void setPeakMax(IQuantity peakMax) {
		m_peakMax = peakMax;
	}

	public String getPoolName() {
		return m_poolName;
	}

	public String getPoolType() {
		return m_poolType;
	}

	public IQuantity getCurUsed() {
		return m_curUsed;
	}

	public IQuantity getCurMax() {
		return m_curMax;
	}

	public IQuantity getCurUsage() {
		return m_curUsage;
	}

	public IQuantity getPeakUsed() {
		return m_peakUsed;
	}

	public IQuantity getPeakMax() {
		return m_peakMax;
	}
}
