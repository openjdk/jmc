/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.agent.test;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;

import jdk.jfr.Recording;

/**
 * A small demo application that slowly leaks entries into several different kinds of collections,
 * to exercise (and let you eyeball) the collection resize tracking capability.
 * <p>
 * For each collection, one in every {@code interval} puts is "leaked" (retained); the rest are
 * added and immediately removed. The retained entries make each collection grow without bound,
 * which periodically resizes its backing array and produces {@code jdk.jmc.CollectionResize}
 * events.
 * <p>
 * Run it with the agent attached and collection tracking enabled; see the agent README for the
 * command line.
 * <p>
 * Configuration (command line argument or system property):
 * <ul>
 * <li>{@code interval} (arg 0, or {@code -Dgurka.leak.interval}, default 100): leak 1 in every N
 * puts.</li>
 * <li>{@code -Dgurka.leak.sleepMillis} (default 1): pause between iterations, to keep the leak
 * observably slow.</li>
 * <li>{@code -Dgurka.record} (optional): if set, records {@code jdk.jmc.CollectionResize} to this
 * file and dumps it on exit.</li>
 * </ul>
 */
public class CollectionLeakDemo {

	public static void main(String[] args) throws Exception {
		int interval = args.length > 0 ? Integer.parseInt(args[0]) : Integer.getInteger("gurka.leak.interval", 100); //$NON-NLS-1$
		long sleepMillis = Long.getLong("gurka.leak.sleepMillis", 1L); //$NON-NLS-1$
		long durationMillis = Long.getLong("gurka.leak.durationMillis", 0L); //$NON-NLS-1$
		String recordPath = System.getProperty("gurka.record"); //$NON-NLS-1$

		List<Leaker> leakers = createLeakers();

		Recording recording = null;
		if (recordPath != null) {
			recording = new Recording();
			recording.enable("jdk.jmc.CollectionResize"); //$NON-NLS-1$
			recording.start();
			System.out.println("Recording jdk.jmc.CollectionResize to " + recordPath); //$NON-NLS-1$
		}

		System.out.println("Gurka collection leak demo: leaking 1 in every " + interval //$NON-NLS-1$
				+ " entries across " + leakers.size() + " collections."); //$NON-NLS-1$ //$NON-NLS-2$

		Thread runner = new Thread(() -> runLeak(leakers, interval, sleepMillis), "Gurka Leaker"); //$NON-NLS-1$
		runner.setDaemon(true);
		runner.start();

		if (durationMillis > 0) {
			System.out.println("Running for " + durationMillis + " ms."); //$NON-NLS-1$ //$NON-NLS-2$
			Thread.sleep(durationMillis);
		} else {
			System.out.println("Press <enter> to stop."); //$NON-NLS-1$
			System.in.read();
		}

		if (recording != null) {
			recording.stop();
			recording.dump(Path.of(recordPath));
			recording.close();
			System.out.println("Dumped recording to " + recordPath); //$NON-NLS-1$
		}
	}

	private static List<Leaker> createLeakers() {
		List<Leaker> leakers = new ArrayList<>();
		leakers.add(new MapLeaker("HashMap", new HashMap<>())); //$NON-NLS-1$
		leakers.add(new MapLeaker("LinkedHashMap", new LinkedHashMap<>())); //$NON-NLS-1$
		leakers.add(new MapLeaker("Hashtable", new Hashtable<>())); //$NON-NLS-1$
		leakers.add(new SetLeaker("HashSet", new HashSet<>())); //$NON-NLS-1$
		leakers.add(new ListLeaker("ArrayList", new ArrayList<>())); //$NON-NLS-1$
		leakers.add(new ListLeaker("Vector", new Vector<>())); //$NON-NLS-1$
		leakers.add(new QueueLeaker("PriorityQueue", new PriorityQueue<>(Comparator.comparingInt(Gurka::getID)))); //$NON-NLS-1$
		leakers.add(new DequeLeaker("ArrayDeque", new ArrayDeque<>())); //$NON-NLS-1$
		return leakers;
	}

	private static void runLeak(List<Leaker> leakers, int interval, long sleepMillis) {
		long iteration = 0;
		long statusEvery = interval * 100L;
		long nextStatus = statusEvery;
		try {
			while (true) {
				iteration++;
				for (Leaker leaker : leakers) {
					leaker.step(iteration, interval);
				}
				if (iteration >= nextStatus) {
					printStatus(leakers, iteration);
					nextStatus += statusEvery;
				}
				if (sleepMillis > 0) {
					Thread.sleep(sleepMillis);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static void printStatus(List<Leaker> leakers, long iteration) {
		StringBuilder builder = new StringBuilder("iteration=").append(iteration); //$NON-NLS-1$
		for (Leaker leaker : leakers) {
			builder.append("  ").append(leaker.name()).append('=').append(leaker.size()); //$NON-NLS-1$
		}
		System.out.println(builder);
	}

	private interface Leaker {
		void step(long iteration, int interval);

		String name();

		int size();
	}

	private static final class MapLeaker implements Leaker {
		private static final Integer CHURN_KEY = Integer.valueOf(-1);
		private final String name;
		private final Map<Integer, Gurka> map;
		private int leakKey;

		MapLeaker(String name, Map<Integer, Gurka> map) {
			this.name = name;
			this.map = map;
		}

		@Override
		public void step(long iteration, int interval) {
			if (iteration % interval == 0) {
				map.put(leakKey++, Gurka.createGurka());
			} else {
				map.put(CHURN_KEY, Gurka.createGurka());
				map.remove(CHURN_KEY);
			}
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public int size() {
			return map.size();
		}
	}

	private static final class SetLeaker implements Leaker {
		private final String name;
		private final Set<Gurka> set;

		SetLeaker(String name, Set<Gurka> set) {
			this.name = name;
			this.set = set;
		}

		@Override
		public void step(long iteration, int interval) {
			Gurka gurka = Gurka.createGurka();
			set.add(gurka);
			if (iteration % interval != 0) {
				set.remove(gurka);
			}
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public int size() {
			return set.size();
		}
	}

	private static final class ListLeaker implements Leaker {
		private final String name;
		private final List<Gurka> list;

		ListLeaker(String name, List<Gurka> list) {
			this.name = name;
			this.list = list;
		}

		@Override
		public void step(long iteration, int interval) {
			list.add(Gurka.createGurka());
			if (iteration % interval != 0) {
				list.remove(list.size() - 1);
			}
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public int size() {
			return list.size();
		}
	}

	private static final class QueueLeaker implements Leaker {
		private final String name;
		private final Queue<Gurka> queue;

		QueueLeaker(String name, Queue<Gurka> queue) {
			this.name = name;
			this.queue = queue;
		}

		@Override
		public void step(long iteration, int interval) {
			Gurka gurka = Gurka.createGurka();
			queue.add(gurka);
			if (iteration % interval != 0) {
				queue.remove(gurka);
			}
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public int size() {
			return queue.size();
		}
	}

	private static final class DequeLeaker implements Leaker {
		private final String name;
		private final Deque<Gurka> deque;

		DequeLeaker(String name, Deque<Gurka> deque) {
			this.name = name;
			this.deque = deque;
		}

		@Override
		public void step(long iteration, int interval) {
			deque.addLast(Gurka.createGurka());
			if (iteration % interval != 0) {
				deque.removeLast();
			}
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public int size() {
			return deque.size();
		}
	}
}
