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

import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.management.MBeanServerConnection;
import javax.management.openmbean.CompositeData;

import org.openjdk.jmc.console.ui.ConsolePlugin;
import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.console.ui.preferences.ConsoleConstants;
import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.ui.polling.PollManager;

public class ThreadsModel implements IThreadsModel {
	private static class CPUSample {
		public long time;
		public long value;
	}

	private final static String THREAD_GET_THREAD_INFO = "getThreadInfo"; //$NON-NLS-1$

	private final PollManager m_pollManager = new PollManager(ConsoleConstants.DEFAULT_THREAD_DUMP_INTERVAL,
			ConsoleConstants.PROPERTY_THREAD_DUMP_INTERVAL);
	private final IConnectionHandle m_connectionHandle;

	private volatile boolean m_cpuTimeEnabled;
	private volatile boolean m_findDeadlocked;
	private boolean m_useMonitoredDeadlockedThreads = false;
	private volatile ThreadInfoCompositeSupport[] m_threads = new ThreadInfoCompositeSupport[0];

	private int m_numberOfCPUs = -1;
	private final Map<Long, CPUSample> m_cpuSampleTimes = new HashMap<>();
	private boolean m_allocationEnabled;

	public ThreadsModel(IConnectionHandle connectionHandle) {
		ConsolePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(m_pollManager);
		m_connectionHandle = connectionHandle;
	}

	@Override
	public void dispose() {
		ConsolePlugin.getDefault().getPreferenceStore().removePropertyChangeListener(m_pollManager);
		m_pollManager.stop();
	}

	@Override
	public void setDeadlockDetectionEnabled(boolean findDeadlocked) {
		if (findDeadlocked != m_findDeadlocked) {
			m_findDeadlocked = findDeadlocked;
			getPollManager().poll();
		}
	}

	@Override
	public boolean isDeadlockeDetectionEnabled() {
		return m_findDeadlocked;
	}

	@Override
	public PollManager getPollManager() {
		return m_pollManager;
	}

	private ThreadMXBean getThreadMxBean() throws Exception {
		return ConnectionToolkit.getThreadBean(m_connectionHandle.getServiceOrThrow(MBeanServerConnection.class));
	}

	@Override
	public int getNumberOfCPUs() {
		if (m_numberOfCPUs == -1) {
			try {
				MBeanServerConnection server = m_connectionHandle.getServiceOrThrow(MBeanServerConnection.class);
				m_numberOfCPUs = ConnectionToolkit.getOperatingSystemBean(server).getAvailableProcessors();
			} catch (Exception e) {
				ConsolePlugin.getDefault().getLogger().log(Level.SEVERE, "Error getting number of cpus", e); //$NON-NLS-1$
			}
		}
		return m_numberOfCPUs;
	}

	@Override
	public boolean isEmpty() {
		return m_threads.length == 0;
	}

	@Override
	public ThreadInfoCompositeSupport[] elements() {
		return m_threads;
	}

	/**
	 * Get the thread list with CPU consumption and the ThreadInfo for each thread sorted by the CPU
	 * time.
	 *
	 * @throws ThreadModelException
	 */
	private void addCPUInformation(ThreadInfoCompositeSupport[] tips) throws ThreadModelException {

		if (isCPUTimeEnabled()) {
			// int MAX_NUMBER_OF_THREADS =
			// ConsolePlugin.getDefault().getPreferenceStore().getInt(ConsoleConstants.PROPERTY_MAX_THREADS_TO_ALLOW_FOR_THREAD_PROFILING);
			int cpuCores = getNumberOfCPUs();
			long[] threadIDs = toIDArray(tips);
			try {
				long[] cpuTimes = (long[]) invoke("getThreadCpuTime", threadIDs); //$NON-NLS-1$
				if (cpuTimes != null) {
					for (int n = 0; n < tips.length; n++) {
						double partOfTimeRunning = calculateCPUTime(tips[n].getThreadId(), cpuTimes[n]);
						if (cpuCores > 0) {
							tips[n].setCPUTime(partOfTimeRunning, partOfTimeRunning / cpuCores);
						} else {
							tips[n].setCPUTime(partOfTimeRunning, ThreadInfoCompositeSupport.UNKNOWN_NUMBER_OF_CORES);
						}
					}
				}
			} catch (Exception e) {
				setCPUTimeEnabled(false);
				// TODO: This is expected on JVMs other than Hotspot. Add user feedback.
			}

		}
	}

	@Override
	public void setAllocationEnabled(boolean enabled) {
		m_allocationEnabled = enabled;
		getPollManager().poll();
	}

	@Override
	public boolean isAllocationEnabled() {
		return m_allocationEnabled;
	}

	@Override
	public void setCPUTimeEnabled(boolean enabled) {
		m_cpuSampleTimes.clear();
		m_cpuTimeEnabled = enabled;
		getPollManager().poll();
	}

	@Override
	public boolean isCPUTimeEnabled() {
		return m_cpuTimeEnabled;
	}

	private double calculateCPUTime(Long threadId, long cpuTime) {
		long wallClockTime = System.currentTimeMillis() * 1000 * 1000;
		if (cpuTime != -1) {
			CPUSample last = m_cpuSampleTimes.get(threadId);
			if (last == null) {
				CPUSample sample = new CPUSample();
				sample.time = wallClockTime;
				sample.value = cpuTime;
				m_cpuSampleTimes.put(threadId, sample);
				return Double.NEGATIVE_INFINITY;
			}
			double cpuTimeDiff = cpuTime - last.value;
			long wallClockDiff = wallClockTime - last.time;
			last.value = cpuTime;
			last.time = wallClockTime;
			return wallClockDiff > 0 ? cpuTimeDiff / wallClockDiff : Double.NEGATIVE_INFINITY;
		}
		return Double.NaN;
	}

	private Object invoke(String operation, Object ... params) throws Exception {
		return ConnectionToolkit.invokeOperation(m_connectionHandle.getServiceOrThrow(MBeanServerConnection.class),
				ConnectionToolkit.THREAD_BEAN_NAME, operation, params);
	}

	private void addDeadlockInformation(ThreadInfoCompositeSupport[] tips) throws ThreadModelException {
		if (isDeadlockeDetectionEnabled()) {
			long[] deadlocked = findDeadlockedThreads();
			for (ThreadInfoCompositeSupport tip : tips) {
				if (tip != null) {
					tip.setDeadlocked(Boolean.FALSE);
					for (long element : deadlocked) {
						Long l = tip.getThreadId();
						if (l != null && l.longValue() == element) {
							tip.setDeadlocked(Boolean.TRUE);
							break;
						}
					}
				}
			}
		}
	}

	private long[] findDeadlockedThreads() throws ThreadModelException {
		try {
			long[] deadlocked = tryFindDeadlockedThreads();
			// Note! deadLockedIds == null means no deadlocked threads
			return deadlocked == null ? new long[0] : deadlocked;
		} catch (Exception e) {
			setDeadlockDetectionEnabled(false);
			throw new ThreadModelException(Messages.ThreadsModel_EXCEPTION_NO_DEADLOCK_DETECTION_AVAILABLE_MESSAGE, e);
		}
	}

	private long[] tryFindDeadlockedThreads() throws Exception {
		ThreadMXBean threadMxBean = getThreadMxBean();
		try {
			if (!m_useMonitoredDeadlockedThreads) {
				return threadMxBean.findDeadlockedThreads();
			}
		} catch (UnsupportedOperationException e) {
			// the JVM does not support monitoring of ownable synchronizers
			m_useMonitoredDeadlockedThreads = true;
		}
		return threadMxBean.findMonitorDeadlockedThreads();
	}

	@Override
	public boolean isUsingMonitoredThreadlockedThreads() {
		return m_useMonitoredDeadlockedThreads;
	}

	private void addAllocationInformation(ThreadInfoCompositeSupport[] tips) throws ThreadModelException {
		if (isAllocationEnabled()) {
			long[] ids = toIDArray(tips);
			try {
				long[] allocs = (long[]) invoke("getThreadAllocatedBytes", ids); //$NON-NLS-1$
				if (allocs != null) {
					for (int n = 0; n < tips.length; n++) {
						if (tips[n] != null) {
							tips[n].setAllocatedBytes(allocs[n]);
						}
					}
				}
			} catch (Exception e) {
				setAllocationEnabled(false);
				// TODO: This is expected on JVMs other than Hotspot. Add user feedback.
			}
		}
	}

	private static long[] toIDArray(ThreadInfoCompositeSupport[] tips) {
		long ids[] = new long[tips.length];
		for (int i = 0; i < tips.length; i++) {
			if (tips[i] != null) {
				ids[i] = tips[i].getThreadId().longValue();
			}
		}
		return ids;
	}

	@Override
	public ThreadInfoCompositeSupport[] getThreadInfo(long[] threadIDArray, Integer depth) throws ThreadModelException {
		CompositeData[] cdArray = getThreadInfos(threadIDArray, depth);
		ThreadInfoCompositeSupport[] threadInfoWithStackTrace = new ThreadInfoCompositeSupport[cdArray.length];
		ArrayList<ThreadInfoCompositeSupport> result = new ArrayList<>();
		for (int n = 0; n < threadInfoWithStackTrace.length; n++) {
			if (cdArray[n] != null) {
				result.add(new ThreadInfoCompositeSupport(cdArray[n]));
			}
		}
		ThreadInfoCompositeSupport[] threadInfoCompositeSupport = new ThreadInfoCompositeSupport[result.size()];
		result.toArray(threadInfoCompositeSupport);
		addDeadlockInformation(threadInfoCompositeSupport);
		addCPUInformation(threadInfoCompositeSupport);
		addAllocationInformation(threadInfoCompositeSupport);
		return threadInfoCompositeSupport;
	}

	@Override
	public void update() throws ThreadModelException {
		CompositeData[] objectInfos = getThreadInfos();
		if (objectInfos != null) {
			ArrayList<ThreadInfoCompositeSupport> tList = new ArrayList<>(objectInfos.length);
			for (CompositeData objectInfo : objectInfos) {
				if (objectInfo != null) {
					tList.add(new ThreadInfoCompositeSupport(objectInfo));
				}
			}
			ThreadInfoCompositeSupport[] tips = new ThreadInfoCompositeSupport[tList.size()];
			tList.toArray(tips);
			m_threads = tips;
			addDeadlockInformation(tips);
			addCPUInformation(tips);
			addAllocationInformation(tips);
		}
	}

	private CompositeData[] getThreadInfos(Object ... params) throws ThreadModelException {
		try {
			if (params.length == 0) {
				params = new Object[] {getThreadMxBean().getAllThreadIds()};
			}
			// FIXME: We could use ThreadMXBean.getThreadInfo(long[]) instead
			return (CompositeData[]) invoke(THREAD_GET_THREAD_INFO, params);
		} catch (Exception e) {
			ConsolePlugin.getDefault().getLogger().log(Level.SEVERE,
					"Error when getting information from ThreadMxBean.", e); //$NON-NLS-1$
			throw new ThreadModelException(Messages.ThreadsModel_EXCEPTION_NO_THREAD_INFO_MESSAGE, e);
		}
	}

	@Override
	public boolean isConnected() {
		return m_connectionHandle.isConnected();
	}

}
