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
package org.openjdk.jmc.ui.rate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmc.ui.misc.IRefreshable;

/**
 * Coordinating different refresh rates, delivering refreshes in the SWT display thread. Currently
 * driven externally, by adding it to a {@link RefreshController}.
 */
public class RateCoordinator implements IRefreshable {
	private final DelayQueue<Task> taskQueue = new DelayQueue<>();
	private long currentReferenceTime;

	/**
	 * Note: this class has a natural ordering that is inconsistent with equals.
	 */
	private class Task implements Delayed {
		public final long scheduledTime;
		public final Runnable runnable;

		public Task(long scheduledTime, Runnable runnable) {
			this.scheduledTime = scheduledTime;
			this.runnable = runnable;
		}

		@Override
		public int compareTo(Delayed other) {
			return (int) (scheduledTime - ((Task) other).scheduledTime);
		}

		// Attempt to silence SpotBugs.
		@Override
		public boolean equals(Object other) {
			return this == other;
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return scheduledTime - currentReferenceTime;
		}
	}

	public RateCoordinator() {
	}

	public void schedule(Runnable runnable, int minDelayMillis) {
		Task task = new Task(System.currentTimeMillis() + minDelayMillis, runnable);
		taskQueue.add(task);
	}

	@Override
	public boolean refresh() {
		currentReferenceTime = System.currentTimeMillis();
		if (!taskQueue.isEmpty()) {
			List<Task> tasks = new ArrayList<>(taskQueue.size());
			taskQueue.drainTo(tasks);
			for (Task task : tasks) {
				task.runnable.run();
			}

			return true;
		}
		return false;
	}
}
