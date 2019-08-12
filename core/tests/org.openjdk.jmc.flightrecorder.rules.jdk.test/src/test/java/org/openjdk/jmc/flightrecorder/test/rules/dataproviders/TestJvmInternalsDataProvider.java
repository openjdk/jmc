package org.openjdk.jmc.flightrecorder.test.rules.dataproviders;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders.JvmInternalsDataProvider;

public class TestJvmInternalsDataProvider {

	@Test
	public void testJavaAgentDuplicateFlags() {
		assertEquals("same jar, no option", 1, JvmInternalsDataProvider.checkDuplicates(
				"-javaagent:myjar.jar -javaagent:myjar.jar").toArray().length);
		assertEquals("different jar, no option", 0, JvmInternalsDataProvider.checkDuplicates(
				"-javaagent:myjar.jar -javaagent:anotherjar.jar").toArray().length);

		assertEquals("same jar, same option", 1, JvmInternalsDataProvider.checkDuplicates(
				"-javaagent:myjar.jar=option -javaagent:myjar.jar=option").toArray().length);
		assertEquals("different jar, same option", 0, JvmInternalsDataProvider.checkDuplicates(
				"-javaagent:myjar.jar=option -javaagent:anotherjar.jar=option").toArray().length);

		assertEquals("same jar, different option", 1, JvmInternalsDataProvider.checkDuplicates(
				"-javaagent:myjar.jar=option -javaagent:myjar.jar=anotheroption").toArray().length);
		assertEquals("different jar, different option", 0, JvmInternalsDataProvider.checkDuplicates(
				"-javaagent:myjar.jar=option -javaagent:anotherjar.jar=anotheroption").toArray().length);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testJavaAgentPathIsParsedCorrectly() {
		String arguments = "-javagent:c:/path/to/archive/myjar.jar "
				+ "-javagent:c:/path/to/archive/myjar.jar";
		String expectedResult = "-javagent:c:/path/to/archive/myjar.jar";

		Collection<ArrayList<String>> result = JvmInternalsDataProvider.checkDuplicates(arguments);
		String actualResult = ((ArrayList<String>) result.toArray()[0]).get(0);
		assertEquals(expectedResult, actualResult);
	}
}
