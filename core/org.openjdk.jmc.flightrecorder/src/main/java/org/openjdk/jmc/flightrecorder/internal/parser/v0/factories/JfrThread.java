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
package org.openjdk.jmc.flightrecorder.internal.parser.v0.factories;

import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IMCThreadGroup;
import org.openjdk.jmc.flightrecorder.messages.internal.Messages;

/**
 * A thread.
 */
final class JfrThread implements IMCThread {
	private final int m_platformId;
	private volatile String m_name;
	private volatile ThreadGroup m_threadGroup;
	private volatile Long m_javaId;

	public JfrThread(int platformId) {
		m_platformId = platformId;
	}

	public Long getJavaId() {
		return m_javaId;
	}

	public synchronized void addJavaId(long javaId) {
		m_javaId = getAny(m_javaId, javaId);
	}

	public String getName() {
		return m_name;
	}

	public synchronized void addName(String name) {
		m_name = getAny(m_name, name);
	}

	public int getPlatformId() {
		return m_platformId;
	}

	@Override
	public String toString() {
		return getThreadName();
	}

	@Override
	public Long getThreadId() {
		return getJavaId();
	}

	@Override
	public String getThreadName() {
		return m_name == null ? Messages.getString(Messages.JfrThread_UNKNOWN_THREAD_NAME) : m_name;
	}

	@Override
	public IMCThreadGroup getThreadGroup() {
		return m_threadGroup;
	}

	public synchronized void addThreadGroup(ThreadGroup group) {
		m_threadGroup = getAny(m_threadGroup, group);
	}

	@Override
	public int hashCode() {
		return m_platformId;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || obj instanceof JfrThread && m_platformId == ((JfrThread) obj).m_platformId;
	}

	private static <T extends Comparable<T>> T getAny(T a, T b) {
		// As we can't handle that a thread has multiple names, groups, javaId etc, for now just make sure we deterministically get the
		// same regardless of the parse order. E.g. the 'biggest' ('main' is bigger than 'DestroyJavaVM')
		if (a == null || b != null && a.compareTo(b) < 0) {
			return b;
		}
		return a;
	}

}
