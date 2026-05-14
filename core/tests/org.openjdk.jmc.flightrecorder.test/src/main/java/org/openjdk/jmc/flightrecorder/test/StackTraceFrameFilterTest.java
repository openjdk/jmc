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
package org.openjdk.jmc.flightrecorder.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.stacktrace.StackTraceFrameFilter;
import org.openjdk.jmc.flightrecorder.test.util.RecordingToolkit;
import org.openjdk.jmc.flightrecorder.test.util.StacktraceTestToolkit;
import org.openjdk.jmc.test.io.IOResourceSet;

/**
 * Tests for {@link StackTraceFrameFilter} that exercise the filter end-to-end against a real JFR
 * recording, plus a few targeted edge-case checks.
 */
@SuppressWarnings("nls")
public class StackTraceFrameFilterTest {

	private static IItemCollection recording;

	@BeforeClass
	public static void loadRecording() throws IOException, CouldNotLoadRecordingException {
		IOResourceSet resourceSet = StacktraceTestToolkit.getTestResourceByRecordingName("7u40.jfr");
		recording = RecordingToolkit.getFlightRecording(resourceSet, true);
	}

	@Test
	public void containsClassFindsExactlyTheEventsHavingThatClassInStack() {
		String knownType = findFirstFrameTypeName(recording);
		assertNotNull("Recording should contain at least one event with a stack trace", knownType);

		IItemCollection matches = recording.apply(StackTraceFrameFilter.containsClass(knownType));
		long matchCount = countItems(matches);
		assertTrue("containsClass(\"" + knownType + "\") should match at least one event", matchCount > 0);

		// Every matched event must actually have that class in its stack trace.
		for (IItemIterable iter : matches) {
			IMemberAccessor<IMCStackTrace, IItem> stAcc = JfrAttributes.EVENT_STACKTRACE.getAccessor(iter.getType());
			assertNotNull(stAcc);
			for (IItem item : iter) {
				assertTrue("Matched event should contain frame for type " + knownType,
						stackContainsClass(stAcc.getMember(item), knownType));
			}
		}
	}

	@Test
	public void containsClassReturnsNoneForUnknownClass() {
		IItemCollection result = recording.apply(StackTraceFrameFilter
				.containsClass("com.example.does.not.exist.NoSuchType__StackTraceFrameFilterTest"));
		assertEquals(0, countItems(result));
	}

	@Test
	public void excludesClassIsComplementaryAmongStacktracedEvents() {
		String knownType = findFirstFrameTypeName(recording);
		assertNotNull(knownType);

		// Restrict to events that have stack trace information so the partition is well-defined.
		IItemCollection withStacks = recording.apply(ItemFilters.hasAttribute(JfrAttributes.EVENT_STACKTRACE));
		long total = countItems(withStacks);
		long contains = countItems(withStacks.apply(StackTraceFrameFilter.containsClass(knownType)));
		long excludes = countItems(withStacks.apply(StackTraceFrameFilter.excludesClass(knownType)));
		assertEquals("contains + excludes should partition stacktraced events", total, contains + excludes);
		assertTrue(contains > 0);
	}

	@Test
	public void containsPackageMatchesSubPackages() {
		String pkg = findFirstFramePackageName(recording);
		assertNotNull("Recording should contain at least one stack frame with a package", pkg);

		IItemCollection inPackage = recording.apply(StackTraceFrameFilter.containsPackage(pkg));
		assertTrue("containsPackage(\"" + pkg + "\") should match some events", countItems(inPackage) > 0);

		// Every matched event must carry a frame in that package or a sub-package.
		for (IItemIterable iter : inPackage) {
			IMemberAccessor<IMCStackTrace, IItem> stAcc = JfrAttributes.EVENT_STACKTRACE.getAccessor(iter.getType());
			assertNotNull(stAcc);
			for (IItem item : iter) {
				assertTrue("Matched event should contain frame in package " + pkg + " or sub-package",
						stackContainsPackage(stAcc.getMember(item), pkg));
			}
		}
	}

	@Test
	public void containsPackageRespectsNameBoundary() {
		// Appending characters to a real package name yields a string that is neither a real
		// package nor a name-boundary prefix of one, so containsPackage must match zero events.
		// This pins down the boundary check (packageName.equals(name) || name.startsWith(name + "."))
		// against a regression where the trailing "." gets dropped.
		String pkg = findFirstFramePackageName(recording);
		assertNotNull(pkg);
		String notAPackage = pkg + "X_NotARealPackageSuffix";
		IItemCollection matches = recording.apply(StackTraceFrameFilter.containsPackage(notAPackage));
		assertEquals("containsPackage must enforce name-boundary matching", 0, countItems(matches));
	}

	@Test
	public void containsPackageReturnsNoneForUnknownPackage() {
		IItemCollection result = recording.apply(StackTraceFrameFilter
				.containsPackage("zzz.no.such.package.in.any.recording.StackTraceFrameFilterTest"));
		assertEquals(0, countItems(result));
	}

	@Test
	public void containsMethodMatchesEventsWithThatMethod() {
		FrameKey known = findFirstFrameKey(recording);
		assertNotNull(known);

		IItemCollection matches = recording
				.apply(StackTraceFrameFilter.containsMethod(known.typeName, known.methodName));
		assertTrue("containsMethod should match at least one event", countItems(matches) > 0);

		for (IItemIterable iter : matches) {
			IMemberAccessor<IMCStackTrace, IItem> stAcc = JfrAttributes.EVENT_STACKTRACE.getAccessor(iter.getType());
			assertNotNull(stAcc);
			for (IItem item : iter) {
				assertTrue("Matched event should contain frame for " + known.typeName + "#" + known.methodName,
						stackContainsMethod(stAcc.getMember(item), known.typeName, known.methodName));
			}
		}
	}

	@Test
	public void excludesMethodComplementsContainsMethod() {
		FrameKey known = findFirstFrameKey(recording);
		assertNotNull(known);
		IItemCollection withStacks = recording.apply(ItemFilters.hasAttribute(JfrAttributes.EVENT_STACKTRACE));
		long total = countItems(withStacks);
		long contains = countItems(
				withStacks.apply(StackTraceFrameFilter.containsMethod(known.typeName, known.methodName)));
		long excludes = countItems(
				withStacks.apply(StackTraceFrameFilter.excludesMethod(known.typeName, known.methodName)));
		assertEquals(total, contains + excludes);
	}

	@Test
	public void containsClassReturnsNoEventWithoutStackTrace() {
		// Events that have no stacktrace attribute on their type must not be matched by ANY-mode.
		IItemCollection noStacks = recording
				.apply(ItemFilters.not(ItemFilters.hasAttribute(JfrAttributes.EVENT_STACKTRACE)));
		long noStacksCount = countItems(noStacks);
		long matched = countItems(noStacks.apply(StackTraceFrameFilter.containsClass("java.lang.Thread")));
		assertEquals(0, matched);
		// And NONE-mode must include all of them (vacuously).
		long excluded = countItems(noStacks.apply(StackTraceFrameFilter.excludesClass("java.lang.Thread")));
		assertEquals(noStacksCount, excluded);
	}

	// ---------- helpers ----------

	private static long countItems(IItemCollection items) {
		long count = 0;
		for (IItemIterable iter : items) {
			for (@SuppressWarnings("unused")
			IItem ignored : iter) {
				count++;
			}
		}
		return count;
	}

	private static String findFirstFrameTypeName(IItemCollection items) {
		for (IItemIterable iter : items) {
			IMemberAccessor<IMCStackTrace, IItem> stAcc = JfrAttributes.EVENT_STACKTRACE.getAccessor(iter.getType());
			if (stAcc == null) {
				continue;
			}
			for (IItem item : iter) {
				IMCStackTrace st = stAcc.getMember(item);
				if (st == null) {
					continue;
				}
				for (IMCFrame frame : st.getFrames()) {
					IMCMethod method = frame == null ? null : frame.getMethod();
					IMCType type = method == null ? null : method.getType();
					if (type != null && type.getFullName() != null) {
						return type.getFullName();
					}
				}
			}
		}
		return null;
	}

	private static String findFirstFramePackageName(IItemCollection items) {
		for (IItemIterable iter : items) {
			IMemberAccessor<IMCStackTrace, IItem> stAcc = JfrAttributes.EVENT_STACKTRACE.getAccessor(iter.getType());
			if (stAcc == null) {
				continue;
			}
			for (IItem item : iter) {
				IMCStackTrace st = stAcc.getMember(item);
				if (st == null) {
					continue;
				}
				for (IMCFrame frame : st.getFrames()) {
					IMCMethod method = frame == null ? null : frame.getMethod();
					IMCType type = method == null ? null : method.getType();
					IMCPackage pkg = type == null ? null : type.getPackage();
					if (pkg != null && pkg.getName() != null && !pkg.getName().isEmpty()) {
						return pkg.getName();
					}
				}
			}
		}
		return null;
	}

	private static FrameKey findFirstFrameKey(IItemCollection items) {
		for (IItemIterable iter : items) {
			IMemberAccessor<IMCStackTrace, IItem> stAcc = JfrAttributes.EVENT_STACKTRACE.getAccessor(iter.getType());
			if (stAcc == null) {
				continue;
			}
			for (IItem item : iter) {
				IMCStackTrace st = stAcc.getMember(item);
				if (st == null) {
					continue;
				}
				for (IMCFrame frame : st.getFrames()) {
					IMCMethod method = frame == null ? null : frame.getMethod();
					IMCType type = method == null ? null : method.getType();
					if (type != null && type.getFullName() != null && method.getMethodName() != null) {
						return new FrameKey(type.getFullName(), method.getMethodName());
					}
				}
			}
		}
		return null;
	}

	private static boolean stackContainsClass(IMCStackTrace st, String typeName) {
		if (st == null) {
			return false;
		}
		for (IMCFrame frame : st.getFrames()) {
			IMCMethod method = frame == null ? null : frame.getMethod();
			IMCType type = method == null ? null : method.getType();
			if (type != null && typeName.equals(type.getFullName())) {
				return true;
			}
		}
		return false;
	}

	private static boolean stackContainsPackage(IMCStackTrace st, String packageName) {
		if (st == null) {
			return false;
		}
		String prefix = packageName + ".";
		for (IMCFrame frame : st.getFrames()) {
			IMCMethod method = frame == null ? null : frame.getMethod();
			IMCType type = method == null ? null : method.getType();
			IMCPackage pkg = type == null ? null : type.getPackage();
			if (pkg == null) {
				continue;
			}
			String name = pkg.getName();
			if (name != null && (packageName.equals(name) || name.startsWith(prefix))) {
				return true;
			}
		}
		return false;
	}

	private static boolean stackContainsMethod(IMCStackTrace st, String typeName, String methodName) {
		if (st == null) {
			return false;
		}
		for (IMCFrame frame : st.getFrames()) {
			IMCMethod method = frame == null ? null : frame.getMethod();
			IMCType type = method == null ? null : method.getType();
			if (type != null && typeName.equals(type.getFullName()) && methodName.equals(method.getMethodName())) {
				return true;
			}
		}
		return false;
	}

	private static final class FrameKey {
		final String typeName;
		final String methodName;

		FrameKey(String typeName, String methodName) {
			this.typeName = typeName;
			this.methodName = methodName;
		}
	}
}
