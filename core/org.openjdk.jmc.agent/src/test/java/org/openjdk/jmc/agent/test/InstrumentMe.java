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
package org.openjdk.jmc.agent.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.openjdk.jmc.agent.test.util.TestToolkit;

public class InstrumentMe {
	public static void main(String[] args) throws InterruptedException {
		InstrumentMe instance = new InstrumentMe();
		while (true) {
			runStatic();
			runInstance(instance);
		}
	}

	private static void runInstance(InstrumentMe instance) throws InterruptedException {
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
		Thread.sleep(1000);
	}

	public static int printHelloWorld2(String str, long l) throws InterruptedException {
		int returnval = TestToolkit.RND.nextInt(45);
		System.out.println(String.format("#S2. Str:%s long:%d retval:%d", str, l, returnval)); //$NON-NLS-1$
		Thread.sleep(1000);
		return returnval;
	}

	public static void printHelloWorld3(Gurka gurka) throws InterruptedException {
		System.out.println(String.format("#S3. Got a gurka with id: %d", gurka.getID())); //$NON-NLS-1$
		Thread.sleep(1000);
	}

	public static void printHelloWorld4(Gurka[] gurkor) throws InterruptedException {
		System.out.println(String.format("#S4. Got gurkor: %s", Arrays.toString(gurkor))); //$NON-NLS-1$
		Thread.sleep(1000);
	}

	public static void printHelloWorld5(Collection<Gurka> gurkor) throws InterruptedException {
		System.out.println(String.format("#S5. Got gurkor: %s", String.valueOf(gurkor))); //$NON-NLS-1$
		Thread.sleep(1000);
	}

	public static void printHelloWorldJFR1() throws InterruptedException {
		System.out.println("#SJFR1. Hello World!"); //$NON-NLS-1$
		Thread.sleep(1000);
	}

	public static int printHelloWorldJFR2(String str, long l) throws InterruptedException {
		int returnval = TestToolkit.RND.nextInt(45);
		System.out.println(String.format("#SJFR2. Str:%s long:%d retval:%d", str, l, returnval)); //$NON-NLS-1$
		Thread.sleep(1000);
		return returnval;
	}

	public static void printHelloWorldJFR3(Gurka gurka) throws InterruptedException {
		System.out.println(String.format("#SJFR3. Got a gurka with id: %d", gurka.getID())); //$NON-NLS-1$
		Thread.sleep(1000);
	}

	public static void printHelloWorldJFR4(Gurka[] gurkor) throws InterruptedException {
		System.out.println(String.format("#SJFR4. Got gurkor: %s", Arrays.toString(gurkor))); //$NON-NLS-1$
		Thread.sleep(1000);
	}

	public static void printHelloWorldJFR5(Collection<Gurka> gurkor) throws InterruptedException {
		System.out.println(String.format("#SJFR5. Got gurkor: %s", String.valueOf(gurkor))); //$NON-NLS-1$
		Thread.sleep(1000);
	}

	public static double printHelloWorldJFR6() throws InterruptedException {
		double returnval = TestToolkit.RND.nextDouble() * 100;
		System.out.println(String.format("#SJFR6. retval:%3.3f", returnval)); //$NON-NLS-1$
		Thread.sleep(1000);
		return returnval;
	}

	public void printInstanceHelloWorld1() throws InterruptedException {
		System.out.println("#I1. Hello World!"); //$NON-NLS-1$
		Thread.sleep(1000);
	}

	public int printInstanceHelloWorld2(String str, long l) throws InterruptedException {
		int returnval = TestToolkit.RND.nextInt(45);
		System.out.println(String.format("#I2. Str:%s long:%d retval:%d", str, l, returnval)); //$NON-NLS-1$
		Thread.sleep(1000);
		return returnval;
	}

	public void printInstanceHelloWorld3(Gurka gurka) throws InterruptedException {
		System.out.println(String.format("#I3. Got a gurka with id: %d", gurka.getID())); //$NON-NLS-1$
		Thread.sleep(1000);
	}

	public void printInstanceHelloWorld4(Gurka[] gurkor) throws InterruptedException {
		System.out.println(String.format("#I4. Got gurkor: %s", Arrays.toString(gurkor))); //$NON-NLS-1$
		Thread.sleep(1000);
	}

	public void printInstanceHelloWorld5(Collection<Gurka> gurkor) throws InterruptedException {
		System.out.println(String.format("#I5. Got gurkor: %s", String.valueOf(gurkor))); //$NON-NLS-1$
		Thread.sleep(1000);
	}

	public void printInstanceHelloWorldJFR1() throws InterruptedException {
		System.out.println("#IJFR1. Hello World!"); //$NON-NLS-1$
		Thread.sleep(1000);
	}

	public int printInstanceHelloWorldJFR2(String str, long l) throws InterruptedException {
		int returnval = TestToolkit.RND.nextInt(45);
		System.out.println(String.format("#IJFR2. Str:%s long:%d retval:%d", str, l, returnval)); //$NON-NLS-1$
		Thread.sleep(1000);
		return returnval;
	}

	public void printInstanceHelloWorldJFR3(Gurka gurka) throws InterruptedException {
		System.out.println(String.format("#IJFR3. Got a gurka with id: %d", gurka.getID())); //$NON-NLS-1$
		Thread.sleep(1000);
	}

	public void printInstanceHelloWorldJFR4(Gurka[] gurkor) throws InterruptedException {
		System.out.println(String.format("#IJFR4. Got gurkor: %s", Arrays.toString(gurkor))); //$NON-NLS-1$
		Thread.sleep(1000);
	}

	public void printInstanceHelloWorldJFR5(Collection<Gurka> gurkor) throws InterruptedException {
		System.out.println(String.format("#IJFR5. Got gurkor: %s", String.valueOf(gurkor))); //$NON-NLS-1$
		Thread.sleep(1000);
	}

	public double printInstanceHelloWorldJFR6() throws InterruptedException {
		double returnval = TestToolkit.RND.nextDouble();
		System.out.println(String.format("#IJFR6. retval:%1.3f", returnval)); //$NON-NLS-1$
		Thread.sleep(1000);
		return returnval;
	}
}
