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
package org.openjdk.jmc.test.jemmy;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

/**
 * A JUnit TestRule to be instantiated (with select overridden methods) to implement the different
 * operations provided by the "@Before", "@After", "@BeforeClass" and "@AfterClass" annotated
 * methods. Use the skeleton below in each test class (and/or base class) to implement the different
 * methods of choice.
 *
 * <pre>
 * &#064;Rule
 * public McUITestRule testRule = new McUITestRule(verboseRuleOutput) {
 * 	&#064;Override
 * 	public void before() {
 *
 * 	}
 *
 * 	&#064;Override
 * 	public void after() {
 *
 * 	}
 * };
 *
 * &#064;ClassRule
 * public static McUITestRule classTestRule = new McUITestRule(verboseRuleOutput) {
 * 	&#064;Override
 * 	public void before() {
 *
 * 	}
 *
 * 	&#064;Override
 * 	public void after() {
 *
 * 	}
 * };
 * </pre>
 */
@SuppressWarnings("restriction")
public class MCUITestRule implements TestRule {
	private static final String REPLACEMENT_STRING = "_";
	private static final String REPLACE_STRING = "[\\s<>]";
	private static final String JEMMY_PACKAGE_NAME = "org.openjdk.jmc.test.jemmy";
	private static final String DEFAULT_METHOD_NAME = "METHOD_NOT_FOUND";
	private static String failedMethodClassName = "";
	private static Boolean doScreenDumpOnFailure = !Boolean.getBoolean("mc.test.noscreendumponfailure");
	private final boolean verbose;

	/**
	 * Creates a TestRule (with no informational printing to System.out)
	 */
	public MCUITestRule() {
		this(false);
	}

	/**
	 * Creates a TestRule
	 *
	 * @param printVerbose
	 *            {@code true} if informational printing to System.out for each method call is
	 *            desired (useful for debugging).
	 */
	public MCUITestRule(boolean printVerbose) {
		verbose = printVerbose;
	}

	@Override
	public Statement apply(final Statement base, final Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				List<Throwable> errors = new ArrayList<>();
				try {
					starting(description);
					base.evaluate();
					succeeded(description);
				} catch (AssumptionViolatedException e) {
					errors.add(e);
					skippedQuietly(e, description, errors);
				} catch (Throwable t) {
					errors.add(t);
					failedQuietly(t, description, errors);
				} finally {
					finishedQuietly(description, errors);
				}
				MultipleFailureException.assertEmpty(errors);
			}
		};
	}

	final private void failedQuietly(Throwable t, Description description, List<Throwable> errors) {
		try {
			failed(t, description);
		} catch (Throwable t1) {
			errors.add(t1);
		}
	}

	final private void skippedQuietly(AssumptionViolatedException e, Description description, List<Throwable> errors) {
		try {
			skipped(e, description);
		} catch (Throwable t) {
			errors.add(t);
		}
	}

	final private void finishedQuietly(Description description, List<Throwable> errors) {
		try {
			finished(description);
		} catch (Throwable t) {
			errors.add(t);
		}
	}

	final private void succeeded(Description description) {
		if (verbose) {
			System.out.println("Successfully ran " + description.getDisplayName());
		}
	}

	final private void skipped(AssumptionViolatedException e, Description description) {
		if (verbose) {
			System.out.println("Skipping " + description.getDisplayName() + " due to: " + e.getMessage());
		}
	}

	final private void starting(Description description) {
		if (verbose) {
			System.out.println("Starting " + description.getDisplayName());
		}
		before();
	}

	final private void finished(Description description) {
		if (verbose) {
			System.out.println("Ending " + description.getDisplayName());
		}
		after();
	}

	final private void failed(Throwable e, Description description) {
		if (doScreenDumpOnFailure) {
			String methodClassName = getFailingMethodAndClass(e, description);
			if (!methodClassName.equals(failedMethodClassName)) {
				if (verbose) {
					System.out.println("Test failed. Dumping screen to file prefixed with " + methodClassName);
				}
				TestHelper.dumpScreen(methodClassName);
			} else {
				if (verbose) {
					System.out.println("Test failed. Screen for test " + methodClassName + " already dumped.");
				}
			}
			failedMethodClassName = methodClassName;
		}
	}

	/**
	 * Override this method to implement actions that are supposed to happen before starting test(s)
	 */
	public void before() {
	}

	/**
	 * Override this method to implement actions that are supposed to happen after test(s) have been
	 * run
	 */
	public void after() {
	}

	private static String getFailingMethodAndClass(Throwable e, Description description) {
		String result;
		if (description.getMethodName() == null || description.getMethodName().equals("null")) {
			// Not a failure in a test method. Get the failing method name from the stack trace
			result = getNameFromTrace(getTrace(e), description.getClassName()) + "(" + description.getClassName() + ")";
		} else {
			result = description.getDisplayName();
		}
		// Make sure that we have no whitespace in the resulting name since this is used in the file name
		return result.replaceAll(REPLACE_STRING, REPLACEMENT_STRING);
	}

	private static String getNameFromTrace(String stack, String className) {
		// Get the first stack trace line containing the test class
		String line = getFirstMatchingLine(stack, className);

		if (line == null) {
			// Fallback looking for the first Jemmy class related line instead
			line = getFirstMatchingLine(stack, JEMMY_PACKAGE_NAME);
			if (line != null) {
				// Extract the class and method where failure happened (in Jemmy). Prepend with the test class name
				int endIndex = line.indexOf("(");
				String beforeEnd = line.substring(0, endIndex);
				int beginIndex = beforeEnd.substring(0, beforeEnd.lastIndexOf(".")).lastIndexOf(".") + 1;
				line = className.substring(className.lastIndexOf(".") + 1) + "_" + line.substring(beginIndex, endIndex);
			} else {
				// Could not find a Jemmy class in the stack trace. Giving up
				line = DEFAULT_METHOD_NAME;
			}
		} else {
			// Extract the method name (initializer or something like that)
			int endIndex = line.indexOf("(");
			int beginIndex = line.substring(0, endIndex).lastIndexOf(".") + 1;
			line = line.substring(beginIndex, endIndex);
		}
		return line;
	}

	private static String getFirstMatchingLine(String stack, String className) {
		StringReader sr = new StringReader(stack);
		BufferedReader br = new BufferedReader(sr);

		String line;
		try {
			while ((line = br.readLine()) != null) {
				if (line.indexOf(className) > 0) {
					return line;
				}
			}
		} catch (Exception IOException) {
			// do nothing
		}
		return null;
	}

	private static String getTrace(Throwable e) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		e.printStackTrace(writer);
		StringBuffer buffer = stringWriter.getBuffer();
		return buffer.toString();
	}
}
