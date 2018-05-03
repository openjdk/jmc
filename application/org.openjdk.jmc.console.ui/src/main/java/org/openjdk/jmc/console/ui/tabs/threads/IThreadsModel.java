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

import org.openjdk.jmc.ui.common.tree.IArray;
import org.openjdk.jmc.ui.polling.PollManager;

/**
 * Interface that encapsulates the behavior of requesting thread data from the threading MX bean and
 * to provide it to the Console thread tab. If no platform thread MX bean is available a dummy model
 * ({@link DummyThreadsModel}) is used.
 */
public interface IThreadsModel extends IArray<ThreadInfoCompositeSupport> {

	void dispose();

	void setDeadlockDetectionEnabled(boolean findDeadlocked);

	boolean isDeadlockeDetectionEnabled();

	PollManager getPollManager();

	int getNumberOfCPUs();

	void setAllocationEnabled(boolean enabled);

	boolean isAllocationEnabled();

	void setCPUTimeEnabled(boolean enabled);

	boolean isCPUTimeEnabled();

	boolean isUsingMonitoredThreadlockedThreads();

	ThreadInfoCompositeSupport[] getThreadInfo(long[] threadIDArray, Integer depth) throws ThreadModelException;

	void update() throws ThreadModelException;

	boolean isConnected();
}
