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

import java.util.Observable;
import java.util.Observer;

/**
 * An {@link Observer} that limits the rate which with it forwards the notifications. In addition,
 * notifications will typically, subject to the {@link RateCoordinator}, be delivered in the user
 * interface thread.
 */
public abstract class RateLimitedObserver extends RateLimitedRefresher implements Observer {
	private Object lastArg;

	public RateLimitedObserver(RateCoordinator rateLimiter, int minPeriodMillis) {
		super(rateLimiter, minPeriodMillis);
	}

	@Override
	public final void update(Observable observable, Object arg) {
		synchronized (this) {
			lastArg = updateArg(lastArg, arg);
		}

		setNeedsRefresh();
	}

	/**
	 * Determine which argument from {@link Observer#update(Observable, Object)} to pass along to
	 * the next {@link #doRefresh(Object)}. By default, the last non-null argument will be
	 * propagated. Subclasses can override.
	 *
	 * @param oldArg
	 *            previously determined argument, or null right after a refresh.
	 * @param newArg
	 *            newly recieved argument, might be null.
	 * @return
	 */
	protected Object updateArg(Object oldArg, Object newArg) {
		return (newArg != null) ? newArg : oldArg;
	}

	@Override
	protected final void doRefresh() {
		Object arg;
		synchronized (this) {
			arg = lastArg;
			lastArg = null;
		}
		doRefresh(arg);
	}

	protected abstract void doRefresh(Object arg);
}
