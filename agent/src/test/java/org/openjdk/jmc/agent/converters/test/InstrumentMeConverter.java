package org.openjdk.jmc.agent.converters.test;

import java.io.IOException;

import org.openjdk.jmc.agent.test.Gurka;

public class InstrumentMeConverter {

	public static void main(String[] args) throws InterruptedException, IOException {
		Thread runner = new Thread(new Runner(), "InstrumentMeConverter Runner");
		runner.setDaemon(true);
		System.out.println("Press <enter> at any time to quit");
		System.out.println("Now starting looping through the instrumentation examples");
		runner.start();
		System.in.read();
	}

	private final static class Runner implements Runnable {
		public void run() {
			while (true) {
				try {
					printGurkaToString(Gurka.createGurka());
					printGurkaToInt(Gurka.createGurka());
					printGurkaToLong(Gurka.createGurka());
					printGurkaToFloat(Gurka.createGurka());
					printGurkaToDouble(Gurka.createGurka());
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public static void printGurkaToString(Gurka gurka) throws InterruptedException {
		System.out.println("String: " + gurka);
		Thread.sleep(1000);
	}

	public static void printGurkaToInt(Gurka gurka) throws InterruptedException {
		System.out.println("Int: " + gurka);
		Thread.sleep(1000);
	}
	
	public static void printGurkaToLong(Gurka gurka) throws InterruptedException {
		System.out.println("Long: " + gurka);
		Thread.sleep(1000);
	}

	public static void printGurkaToFloat(Gurka gurka) throws InterruptedException {
		System.out.println("Float: " + gurka);
		Thread.sleep(1000);
	}

	public static void printGurkaToDouble(Gurka gurka) throws InterruptedException {
		System.out.println("Double: " + gurka);
		Thread.sleep(1000);
	}
}
