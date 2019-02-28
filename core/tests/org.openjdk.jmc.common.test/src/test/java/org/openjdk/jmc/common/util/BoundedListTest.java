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
package org.openjdk.jmc.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openjdk.jmc.common.collection.BoundedList;
import org.openjdk.jmc.common.test.SlowTests;

public class BoundedListTest {

	private static class ProducerThread implements Runnable {
		private final BoundedList<Long> list;
		private volatile boolean shouldStop = false;
		private long counter;

		public ProducerThread(BoundedList<Long> list) {
			this.list = list;
		}

		@Override
		public void run() {
			while (!shouldStop) {
				for (int i = 0; i < 100000; i++) {
					list.add(counter++);
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Thread.yield();
			}
		}

		public void stop() {
			shouldStop = true;
		}
	}

	private static class ValidationThread implements Runnable {
		private final BoundedList<Long> list;
		private volatile boolean shouldStop = false;
		private volatile long countError = -1;
		private volatile long sequenceError = -1;
		private volatile long maxNum;

		public ValidationThread(BoundedList<Long> list) {
			this.list = list;
		}

		@Override
		public void run() {
			while (!shouldStop) {
				validate();
			}
		}

		private void validate() {
			int count = 0;
			long lastNumber = -1;
			for (Long l : list) {
				if (lastNumber != -1 && (l - lastNumber != 1)) {
					sequenceError = l - lastNumber;
				}
				count++;
				maxNum = Math.max(l, maxNum);
			}
			if (count > list.getMaxSize()) {
				countError = count;
			}
		}

		public void stop() {
			shouldStop = true;
		}
	}

	@Test
	public void testAdd() {
		BoundedList<Integer> bl = new BoundedList<>(10);
		bl.add(1);
		bl.add(2);
		assertEquals(2, bl.getSize());
		assertEquals(10, bl.getMaxSize());
	}

	@Test
	public void testBasicIterator() {
		BoundedList<Integer> bl = new BoundedList<>(10);
		bl.add(1);
		bl.add(2);
		bl.add(3);

		int val = 1;
		for (Integer iterVal : bl) {
			assertEquals(val++, iterVal.intValue());
		}
		assertEquals(4, val);
	}

	@Test
	public void testWrappingIterator() {
		BoundedList<Integer> bl = new BoundedList<>(10);
		for (int i = 1; i <= 20; i++) {
			bl.add(i);
		}

		int val = 11;
		for (Integer iterVal : bl) {
			assertEquals(val++, iterVal.intValue());
		}
	}

	@Test
	public void testEmptyIterator() {
		BoundedList<Integer> bl = new BoundedList<>(10);
		assertFalse("Empty list should have no elements!", bl.iterator().hasNext());
		try {
			bl.iterator().next();
			fail("next should have generated an exception!");
		} catch (NoSuchElementException el) {
			// Fall through...
		}
	}

	@Test
	public void testMultipleIterators() {
		// Shows that we can leak memory if we add faster than we can consume...
		BoundedList<Integer> bl = new BoundedList<>(10);
		for (int i = 1; i <= 10; i++) {
			bl.add(i);
		}
		Iterator<Integer> iter1 = bl.iterator();
		for (int i = 11; i <= 20; i++) {
			bl.add(i);
		}
		Iterator<Integer> iter2 = bl.iterator();

		int val1 = 1;
		while (iter1.hasNext()) {
			assertEquals(val1++, iter1.next().intValue());
		}

		int val2 = 11;
		while (iter2.hasNext()) {
			assertEquals(val2++, iter2.next().intValue());
		}
		// Iterated through all values, even though we've wrapped.
		assertEquals(11, val1);
		assertEquals(21, val2);
	}

	@Category(value = SlowTests.class)
	@Test
	public void testMultiThreadedConsumption() throws InterruptedException {
		final BoundedList<Long> bl = new BoundedList<>(20);
		ProducerThread t = new ProducerThread(bl);
		ValidationThread[] validators = new ValidationThread[10];
		new Thread(t, "Producer").start();
		for (int i = 0; i < validators.length; i++) {
			validators[i] = new ValidationThread(bl);
			new Thread(validators[i], "Validator " + i).start();
		}
		Thread.sleep(30000);
		for (ValidationThread validator : validators) {
			validator.stop();
		}
		t.stop();
		long maxNo = 0;
		for (ValidationThread validator : validators) {
			assertEquals("Failed count validation!", -1, validator.countError);
			assertEquals("Failed sequence validation!", -1, validator.sequenceError);
			maxNo = Math.max(maxNo, validator.maxNum);
		}
		System.out.println("Allocated up to " + t.counter);
		System.out.println("Max no was " + maxNo);
	}

	// FIXME: This test has been commented out for a long time. Check if it is still relevant and either remove or reintroduce it.
//	@Category(value = SlowTests.class)
//	@Test
//	public void testNoLeak() {
//		BoundedList<Long> bl = new BoundedList<Long>(10);
//		// Adding a few billion numbers, just to show that we do not leak under normal circumstances...
//		for (long i = 1; i <= Integer.MAX_VALUE; i++) {
//			if (i % (Integer.MAX_VALUE / 20) == 0) {
//				System.out.println(String.format("Passed %.0f%%", (i * 100f) / Integer.MAX_VALUE));
//			}
//			bl.add(i);
//		}
//		// On a 32-bit platform we should either crash or pass...
//	}

}
