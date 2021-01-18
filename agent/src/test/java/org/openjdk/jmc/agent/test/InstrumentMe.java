/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.agent.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.openjdk.jmc.agent.test.util.TestToolkit;

public class InstrumentMe {
	public static final String STATIC_STRING_FIELD = "org.openjdk.jmc.agent.test.InstrumentMe.STATIC_STRING_FIELD";
	public static final MyPojo STATIC_OBJECT_FIELD = new MyPojo();
	public static final MyPojo STATIC_NULL_FIELD = null;

	public final String instanceStringField = "org.openjdk.jmc.agent.test.InstrumentMe.instanceStringField";

	private static final int SLEEP_TIME = 500;

	public static class MyPojo {
		public String instanceStringField = "org.openjdk.jmc.agent.test.InstrumentMe.MyPojo.instanceStringField";
		public static String STATIC_STRING_FIELD = "org.openjdk.jmc.agent.test.InstrumentMe.MyPojo.STATIC_STRING_FIELD";
	}

	public class MyInnerClass extends InstrumentMe {
		@SuppressWarnings("unused")
		private final String innerClassField = "org.openjdk.jmc.agent.test.InstrumentMe.MyInnerClass.innerClassField";

		public void instrumentationPoint() {
			// no op
		}
	}

	public static void main(String[] args) throws InterruptedException, IOException {
		Thread runner = new Thread(new Runner(), "InstrumentMe Runner");
		runner.setDaemon(true);
		System.out.println("Press <enter> at any time to quit");
		System.out.println("Now starting looping through the instrumentation examples");
		runner.start();
		System.in.read();
	}

	private static final class Runner implements Runnable {
		public void run() {
			InstrumentMe instance = new InstrumentMe();
			while (true) {
				try {
					InstrumentMe.runStatic();
					InstrumentMe.runInstance(instance);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	private static void runInstance(InstrumentMe instance) throws InterruptedException {
		System.out.println("Running instance versions..."); //$NON-NLS-1$
		instance.printInstanceHelloWorld1();
		instance.printInstanceHelloWorld2(TestToolkit.randomString(), TestToolkit.randomLong());
		instance.printInstanceHelloWorld3(Gurka.createGurka());
		instance.printInstanceHelloWorld4(new Gurka[] {Gurka.createGurka(), Gurka.createGurka()});
		instance.printInstanceHelloWorld5(createGurkList());
		instance.printInstanceHelloWorldJFR1();
		instance.printInstanceHelloWorldJFR2(TestToolkit.randomString(), TestToolkit.randomLong());
		instance.printInstanceHelloWorldJFR3(Gurka.createGurka());
		instance.printInstanceHelloWorldJFR4(new Gurka[] {Gurka.createGurka(), Gurka.createGurka()});
		instance.printInstanceHelloWorldJFR5(createGurkList());
		instance.printInstanceHelloWorldJFR6();
		instance.printInstanceHelloWorldJFR7();
		try {
			instance.printInstanceHelloWorldJFR8();
		} catch (RuntimeException e) {
			System.out.println("#IJFR8. Caught a RuntimeException: " + e.getMessage());
		}
		try {
			instance.printInstanceHelloWorldJFR9();
		} catch (RuntimeException e) {
			System.out.println("#IJFR9. Caught a RuntimeException: " + e.getMessage());
		}
		try {
			instance.printInstanceHelloWorldJFR10();
		} catch (RuntimeException e) {
			System.out.println("#IJFR10. Caught a RuntimeException: " + e.getMessage());
		}
		instance.printInstanceHelloWorldJFR11();
		instance.printInstanceHelloWorldJFR12();
	}

	private static void runStatic() throws InterruptedException {
		System.out.println("Running static versions..."); //$NON-NLS-1$
		printHelloWorld1();
		printHelloWorld2(TestToolkit.randomString(), TestToolkit.randomLong());
		printHelloWorld3(Gurka.createGurka());
		printHelloWorld4(new Gurka[] {Gurka.createGurka(), Gurka.createGurka()});
		printHelloWorld5(createGurkList());
		printHelloWorldJFR1();
		printHelloWorldJFR2(TestToolkit.randomString(), TestToolkit.randomLong());
		printHelloWorldJFR3(Gurka.createGurka());
		printHelloWorldJFR4(new Gurka[] {Gurka.createGurka(), Gurka.createGurka()});
		printHelloWorldJFR5(createGurkList());
		printHelloWorldJFR6();
		printHelloWorldJFR7();
		try {
			printHelloWorldJFR8();
		} catch (RuntimeException e) {
			System.out.println("#SJFR8. Caught a RuntimeException: " + e.getMessage());
		}
		try {
			printHelloWorldJFR9();
		} catch (RuntimeException e) {
			System.out.println("#SJFR9. Caught a RuntimeException: " + e.getMessage());
		}
		try {
			printHelloWorldJFR10();
		} catch (RuntimeException e) {
			System.out.println("#SJFR10. Caught a RuntimeException: " + e.getMessage());
		}
		printHelloWorldJFR11();
		printHelloWorldJFR12();
		printHelloWorldJFR13();
		DoLittleContainer container = DoLittleContainer.createAndStart();
		printHelloWorldJFR14(container.getThread());
		container.shutdown();
		printHelloWorldJFR15(InstrumentMe.class);
	}

	private static Collection<Gurka> createGurkList() {
		List<Gurka> gurkList = new ArrayList<>();
		for (int i = 0; i < TestToolkit.RND.nextInt(4) + 1; i++) {
			gurkList.add(Gurka.createGurka());
		}
		return gurkList;
	}

	public static void printHelloWorld1() throws InterruptedException {
		System.out.println("#S1. Hello World!"); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public static int printHelloWorld2(String str, long l) throws InterruptedException {
		int returnval = TestToolkit.RND.nextInt(45);
		System.out.println(String.format("#S2. Str:%s long:%d retval:%d", str, l, returnval)); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
		return returnval;
	}

	public static void printHelloWorld3(Gurka gurka) throws InterruptedException {
		System.out.println(String.format("#S3. Got a gurka with id: %d", gurka.getID())); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public static void printHelloWorld4(Gurka[] gurkor) throws InterruptedException {
		System.out.println(String.format("#S4. Got gurkor: %s", Arrays.toString(gurkor))); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public static void printHelloWorld5(Collection<Gurka> gurkor) throws InterruptedException {
		System.out.println(String.format("#S5. Got gurkor: %s", String.valueOf(gurkor))); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public static void printHelloWorldJFR1() throws InterruptedException {
		System.out.println("#SJFR1. Hello World!"); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public static int printHelloWorldJFR2(String str, long l) throws InterruptedException {
		int returnval = TestToolkit.RND.nextInt(45);
		System.out.println(String.format("#SJFR2. Str:%s long:%d retval:%d", str, l, returnval)); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
		return returnval;
	}

	public static void printHelloWorldJFR3(Gurka gurka) throws InterruptedException {
		System.out.println(String.format("#SJFR3. Got a gurka with id: %d", gurka.getID())); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public static void printHelloWorldJFR4(Gurka[] gurkor) throws InterruptedException {
		System.out.println(String.format("#SJFR4. Got gurkor: %s", Arrays.toString(gurkor))); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public static void printHelloWorldJFR5(Collection<Gurka> gurkor) throws InterruptedException {
		System.out.println(String.format("#SJFR5. Got gurkor: %s", String.valueOf(gurkor))); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public static double printHelloWorldJFR6() throws InterruptedException {
		double returnval = TestToolkit.RND.nextDouble() * 100;
		System.out.println(String.format("#SJFR6. retval:%3.3f", returnval)); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
		return returnval;
	}

	public static void printHelloWorldJFR7() throws InterruptedException {
		try {
			System.out.println("#SJFR7. Hello World!"); //$NON-NLS-1$
			Thread.sleep(SLEEP_TIME);
		} catch (Exception e) {
			// intentionally empty
		}
	}

	public static void printHelloWorldJFR8() throws InterruptedException {
		System.out.println("#SJFR8. About to throw a RuntimeException"); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
		(new ArrayList<>()).get(1);
	}

	public static void printHelloWorldJFR9() throws InterruptedException {
		System.out.println("#SJFR9. About to throw a RuntimeException"); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
		(new ArrayList<>()).get(1);
	}

	public static void printHelloWorldJFR10() throws InterruptedException {
		System.out.println("#SJFR10. About to throw a RuntimeException"); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);

		try {
			(new ArrayList<>()).get(1);
		} catch (RuntimeException e) {
			System.out.println("#SJFR10. Caught a RuntimeException: " + e.getMessage()); //$NON-NLS-1$
			throw e;
		}
	}

	public static void printHelloWorldJFR11() throws InterruptedException {
		System.out.println("#SJFR11. Capturing static field 'STATIC_STRING_FIELD'"); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public static void printHelloWorldJFR12() throws InterruptedException {
		System.out.println(
				"#SJFR12. Capturing 'STATIC_OBJECT_FIELD.STATIC_STRING_FIELD' and 'STATIC_OBJECT_FIELD.instanceStringField'"); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public static void printHelloWorldJFR13() throws InterruptedException {
		System.out.println(
				"#SJFR13. Capturing 'STATIC_NULL_FIELD.STATIC_STRING_FIELD' and 'STATIC_NULL_FIELD.instanceStringField'"); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public static void printHelloWorldJFR14(Thread thread) throws InterruptedException {
		System.out.println("#SJFR14. Capturing thread parameter " + thread.getId() + ":" + thread.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		Thread.sleep(SLEEP_TIME);
	}

	public static void printHelloWorldJFR15(Class<?> clazz) throws InterruptedException {
		System.out.println("#SJFR15. Capturing class parameter " + clazz.getName()); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public void printInstanceHelloWorld1() throws InterruptedException {
		System.out.println("#I1. Hello World!"); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public int printInstanceHelloWorld2(String str, long l) throws InterruptedException {
		int returnval = TestToolkit.RND.nextInt(45);
		System.out.println(String.format("#I2. Str:%s long:%d retval:%d", str, l, returnval)); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
		return returnval;
	}

	public void printInstanceHelloWorld3(Gurka gurka) throws InterruptedException {
		System.out.println(String.format("#I3. Got a gurka with id: %d", gurka.getID())); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public void printInstanceHelloWorld4(Gurka[] gurkor) throws InterruptedException {
		System.out.println(String.format("#I4. Got gurkor: %s", Arrays.toString(gurkor))); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public void printInstanceHelloWorld5(Collection<Gurka> gurkor) throws InterruptedException {
		System.out.println(String.format("#I5. Got gurkor: %s", String.valueOf(gurkor))); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public void printInstanceHelloWorldJFR1() throws InterruptedException {
		System.out.println("#IJFR1. Hello World!"); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public int printInstanceHelloWorldJFR2(String str, long l) throws InterruptedException {
		int returnval = TestToolkit.RND.nextInt(45);
		System.out.println(String.format("#IJFR2. Str:%s long:%d retval:%d", str, l, returnval)); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
		return returnval;
	}

	public void printInstanceHelloWorldJFR3(Gurka gurka) throws InterruptedException {
		System.out.println(String.format("#IJFR3. Got a gurka with id: %d", gurka.getID())); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public void printInstanceHelloWorldJFR4(Gurka[] gurkor) throws InterruptedException {
		System.out.println(String.format("#IJFR4. Got gurkor: %s", Arrays.toString(gurkor))); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public void printInstanceHelloWorldJFR5(Collection<Gurka> gurkor) throws InterruptedException {
		System.out.println(String.format("#IJFR5. Got gurkor: %s", String.valueOf(gurkor))); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public double printInstanceHelloWorldJFR6() throws InterruptedException {
		double returnval = TestToolkit.RND.nextDouble();
		System.out.println(String.format("#IJFR6. retval:%1.3f", returnval)); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
		return returnval;
	}

	public void printInstanceHelloWorldJFR7() throws InterruptedException {
		try {
			System.out.println("#IJFR7. Hello World!"); //$NON-NLS-1$
			Thread.sleep(SLEEP_TIME);
		} catch (Exception e) {
			// intentionally empty
		}
	}

	public void printInstanceHelloWorldJFR8() throws InterruptedException {
		System.out.println("#IJFR8. About to throw a RuntimeException"); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
		(new ArrayList<>()).get(1);
	}

	public void printInstanceHelloWorldJFR9() throws InterruptedException {
		System.out.println("#IJFR9. About to throw a RuntimeException"); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
		(new ArrayList<>()).get(1);
	}

	public void printInstanceHelloWorldJFR10() throws InterruptedException {
		System.out.println("#IJFR10. About to throw a RuntimeException"); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);

		try {
			(new ArrayList<>()).get(1);
		} catch (RuntimeException e) {
			System.out.println("#IJFR10. Caught a RuntimeException: " + e.getMessage()); //$NON-NLS-1$
			throw e;
		}
	}

	public void printInstanceHelloWorldJFR11() throws InterruptedException {
		System.out.println("#IJFR11. Capturing instance field 'instanceStringField'"); //$NON-NLS-1$
		Thread.sleep(SLEEP_TIME);
	}

	public void printInstanceHelloWorldJFR12() throws InterruptedException {
		System.out.println("#IJFR12. Capturing fields from nested class 'InstrumentMe.MyInnerClass'"); //$NON-NLS-1$
		new MyInnerClass().instrumentationPoint();
	}
}
