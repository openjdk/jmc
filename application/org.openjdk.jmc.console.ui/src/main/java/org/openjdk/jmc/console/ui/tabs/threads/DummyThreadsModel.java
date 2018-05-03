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
package org.openjdk.jmc.console.ui.tabs.threads;

import org.openjdk.jmc.console.ui.preferences.ConsoleConstants;
import org.openjdk.jmc.ui.polling.PollManager;

/**
 * A null-implementation of the threads model to be used if platform threading MBean is unavailable.
 */
public class DummyThreadsModel implements IThreadsModel {

	private final PollManager m_pollManager = new PollManager(ConsoleConstants.DEFAULT_THREAD_DUMP_INTERVAL,
			ConsoleConstants.PROPERTY_THREAD_DUMP_INTERVAL);

	@Override
	public void dispose() {
		m_pollManager.stop();
	}

	@Override
	public void setDeadlockDetectionEnabled(boolean findDeadlocked) {
	}

	@Override
	public boolean isDeadlockeDetectionEnabled() {
		return false;
	}

	@Override
	public PollManager getPollManager() {
		return m_pollManager;
	}

	@Override
	public int getNumberOfCPUs() {
		return 0;
	}

	@Override
	public void setAllocationEnabled(boolean enabled) {
	}

	@Override
	public boolean isAllocationEnabled() {
		return false;
	}

	@Override
	public void setCPUTimeEnabled(boolean enabled) {
	}

	@Override
	public boolean isCPUTimeEnabled() {
		return false;
	}

	@Override
	public boolean isUsingMonitoredThreadlockedThreads() {
		return false;
	}

	@Override
	public ThreadInfoCompositeSupport[] getThreadInfo(long[] threadIDArray, Integer depth) throws ThreadModelException {
		return new ThreadInfoCompositeSupport[0];
	}

	@Override
	public void update() throws ThreadModelException {
	}

	@Override
	public boolean isConnected() {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public ThreadInfoCompositeSupport[] elements() {
		return new ThreadInfoCompositeSupport[0];
	}
}
