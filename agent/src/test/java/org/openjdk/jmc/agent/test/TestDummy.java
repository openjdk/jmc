package org.openjdk.jmc.agent.test;

public class TestDummy {
		
		public void testWithoutException() {
			System.out.println("I'm going to return now. bye!");
			return;
		}
		
		public void testWithException() throws Exception {
			System.out.println("I'm going to throw an exception now. bye!");
			throw new RuntimeException();
		}
}
