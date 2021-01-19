/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2021, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.agent.converters.test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.openjdk.jmc.agent.test.DoLittleContainer;
import org.openjdk.jmc.agent.test.Gurka;
import org.openjdk.jmc.agent.test.util.TestToolkit;

public class InstrumentMeConverter {
	private static final int SLEEP_TIME = 500;

	public static void main(String[] args) throws InterruptedException, IOException {
		Thread runner = new Thread(new Runner(), "InstrumentMeConverter Runner");
		runner.setDaemon(true);
		System.out.println("Press <enter> at any time to quit");
		System.out.println("Now starting looping through the instrumentation examples");
		runner.start();
		System.in.read();
	}

	private static final class Runner implements Runnable {
		InnerClass innerClass = new InnerClass();

		public void run() {
			while (true) {
				try {
					printGurkaToString(Gurka.createGurka());
					printGurkaToInt(Gurka.createGurka());
					printGurkaToLong(Gurka.createGurka());
					printGurkaToFloat(Gurka.createGurka());
					printGurkaToDouble(Gurka.createGurka());
					printFileToString(new File(TestToolkit.randomString().toLowerCase() + ".tmp"));
					printUriToString(new URI("http://localhost:7777/" + TestToolkit.randomString()));
					printNumber(TestToolkit.randomLong());
					printIntMultiCustom(Gurka.createGurka());
					printDoubleMultiCustom(Gurka.createGurka());
					printLongMultiDefault(TestToolkit.randomLong());
					printDoubleMultiDefault(TestToolkit.randomLong() / 2.0d);
					printReturnGurka();
					printThread(DoLittleContainer.createAndStart());
					printClassGurka(Gurka.createGurka());
					innerClass.switchGurka();
					innerClass.instrumentationPoint();
				} catch (InterruptedException e) {
				} catch (URISyntaxException e) {
				}
			}
		}
	}

	public static class InnerClass {
		private volatile Gurka currentGurka = Gurka.createGurka();

		public void instrumentationPoint() {
			System.out.println("InnerClass: currentGurka field is " + currentGurka);
		}

		public void switchGurka() {
			currentGurka = Gurka.createGurka();
		}
	}

	public static void printGurkaToString(Gurka gurka) throws InterruptedException {
		System.out.println("C String: " + gurka);
		Thread.sleep(SLEEP_TIME);
	}

	public static void printGurkaToInt(Gurka gurka) throws InterruptedException {
		System.out.println("C Int: " + gurka);
		Thread.sleep(SLEEP_TIME);
	}

	public static void printGurkaToLong(Gurka gurka) throws InterruptedException {
		System.out.println("C Long: " + gurka);
		Thread.sleep(SLEEP_TIME);
	}

	public static void printGurkaToFloat(Gurka gurka) throws InterruptedException {
		System.out.println("C Float: " + gurka);
		Thread.sleep(SLEEP_TIME);
	}

	public static void printGurkaToDouble(Gurka gurka) throws InterruptedException {
		System.out.println("C Double: " + gurka);
		Thread.sleep(SLEEP_TIME);
	}

	public static void printFileToString(File file) throws InterruptedException {
		System.out.println("C File: " + file);
		Thread.sleep(SLEEP_TIME);
	}

	public static void printUriToString(URI someUri) throws InterruptedException {
		System.out.println("C URI: " + someUri);
		Thread.sleep(SLEEP_TIME);
	}

	public static void printNumber(Number number) throws InterruptedException {
		System.out.println("C Number: " + number.doubleValue());
		Thread.sleep(SLEEP_TIME);
	}

	private static void printIntMultiCustom(Gurka gurka) throws InterruptedException {
		System.out.println("C int MC: " + gurka);
		Thread.sleep(SLEEP_TIME);
	}

	public static void printDoubleMultiCustom(Gurka gurka) throws InterruptedException {
		System.out.println("C Long MC: " + gurka);
		Thread.sleep(SLEEP_TIME);
	}

	public static void printLongMultiDefault(Long value) throws InterruptedException {
		System.out.println("C Long MD: " + value.longValue());
		Thread.sleep(SLEEP_TIME);
	}

	public static void printDoubleMultiDefault(Double value) throws InterruptedException {
		System.out.println("C Long MD: " + value.doubleValue());
		Thread.sleep(SLEEP_TIME);
	}

	public static Gurka printReturnGurka() throws InterruptedException {
		Gurka gurka = Gurka.createGurka();
		System.out.println("C Gurka returned: " + gurka.toString());
		return gurka;
	}

	public static void printThread(DoLittleContainer doLittleContainer) throws InterruptedException {
		System.out.println("C Thread ID: " + doLittleContainer.getThread().getId() + " Name: "
				+ doLittleContainer.getThread().getName());
		Thread.sleep(SLEEP_TIME);
		doLittleContainer.shutdown();
	}

	public static void printClassGurka(Gurka createGurka) throws InterruptedException {
		System.out.println("C The class of Gurka is: " + createGurka.getClass().getName());
		Thread.sleep(SLEEP_TIME);
	}
}
