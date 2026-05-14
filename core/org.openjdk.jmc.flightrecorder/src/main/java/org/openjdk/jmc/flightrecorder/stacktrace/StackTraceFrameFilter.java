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
package org.openjdk.jmc.flightrecorder.stacktrace;

import java.util.Objects;
import java.util.function.Predicate;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.util.PredicateToolkit;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

/**
 * An {@link IItemFilter} that selects events based on the contents of their stack traces. A
 * {@link FrameFilter} is applied frame-by-frame; the {@link MatchMode} controls whether the event
 * is kept when at least one frame matches ({@link MatchMode#ANY}) or when no frame matches
 * ({@link MatchMode#NONE}).
 * <p>
 * Static factories are provided for the most common cases (matching by package, class, or
 * fully-qualified method). For anything else, supply a custom {@link FrameFilter} via the public
 * constructor.
 * <p>
 * Events that do not have an {@link JfrAttributes#EVENT_STACKTRACE} attribute, or whose stack trace
 * is {@code null}, are treated as having no matching frames: they are excluded by
 * {@link MatchMode#ANY} and included by {@link MatchMode#NONE}.
 */
public final class StackTraceFrameFilter implements IItemFilter {

	/**
	 * Determines how an event-level decision is derived from per-frame matches.
	 */
	public enum MatchMode {
	/**
	 * The event is kept when at least one frame satisfies the predicate.
	 */
	ANY,
	/**
	 * The event is kept when no frame satisfies the predicate.
	 */
	NONE
	}

	private final MatchMode mode;
	private final FrameFilter framePredicate;

	/**
	 * Creates a stack trace filter.
	 *
	 * @param mode
	 *            how to combine per-frame matches into an event-level decision
	 * @param framePredicate
	 *            the predicate evaluated against each frame
	 */
	public StackTraceFrameFilter(MatchMode mode, FrameFilter framePredicate) {
		this.mode = Objects.requireNonNull(mode, "mode"); //$NON-NLS-1$
		this.framePredicate = Objects.requireNonNull(framePredicate, "framePredicate"); //$NON-NLS-1$
	}

	@Override
	public Predicate<IItem> getPredicate(IType<IItem> type) {
		final IMemberAccessor<?, IItem> accessor = JfrAttributes.EVENT_STACKTRACE.getAccessor(type);
		if (accessor == null) {
			// No stack trace attribute on this type: ANY -> never; NONE -> always.
			return mode == MatchMode.NONE ? PredicateToolkit.truePredicate() : PredicateToolkit.falsePredicate();
		}
		return item -> {
			IMCStackTrace st = (IMCStackTrace) accessor.getMember(item);
			boolean anyMatch = false;
			if (st != null) {
				for (IMCFrame frame : st.getFrames()) {
					if (framePredicate.shouldInclude(frame)) {
						anyMatch = true;
						break;
					}
				}
			}
			return mode == MatchMode.NONE ? !anyMatch : anyMatch;
		};
	}

	/**
	 * Returns a filter that keeps events whose stack trace contains a frame for the given fully
	 * qualified type and method name.
	 */
	public static IItemFilter containsMethod(String typeName, String methodName) {
		return new StackTraceFrameFilter(MatchMode.ANY, methodPredicate(typeName, methodName));
	}

	/**
	 * Returns a filter that keeps events whose stack trace does <em>not</em> contain a frame for
	 * the given fully qualified type and method name.
	 */
	public static IItemFilter excludesMethod(String typeName, String methodName) {
		return new StackTraceFrameFilter(MatchMode.NONE, methodPredicate(typeName, methodName));
	}

	/**
	 * Returns a filter that keeps events whose stack trace contains a frame for the given fully
	 * qualified class name.
	 */
	public static IItemFilter containsClass(String typeName) {
		return new StackTraceFrameFilter(MatchMode.ANY, classPredicate(typeName));
	}

	/**
	 * Returns a filter that keeps events whose stack trace does <em>not</em> contain a frame for
	 * the given fully qualified class name.
	 */
	public static IItemFilter excludesClass(String typeName) {
		return new StackTraceFrameFilter(MatchMode.NONE, classPredicate(typeName));
	}

	/**
	 * Returns a filter that keeps events whose stack trace contains a frame in the given package or
	 * any sub-package. For example, {@code containsPackage("com.acme")} matches frames in
	 * {@code com.acme.Foo} and {@code com.acme.sub.Bar}, but not {@code com.acmeX.Baz}.
	 */
	public static IItemFilter containsPackage(String packageName) {
		return new StackTraceFrameFilter(MatchMode.ANY, packagePredicate(packageName));
	}

	/**
	 * Returns a filter that keeps events whose stack trace contains <em>no</em> frame in the given
	 * package or any sub-package.
	 */
	public static IItemFilter excludesPackage(String packageName) {
		return new StackTraceFrameFilter(MatchMode.NONE, packagePredicate(packageName));
	}

	private static FrameFilter methodPredicate(String typeName, String methodName) {
		Objects.requireNonNull(typeName, "typeName"); //$NON-NLS-1$
		Objects.requireNonNull(methodName, "methodName"); //$NON-NLS-1$
		return frame -> {
			IMCMethod method = method(frame);
			if (method == null || method.getType() == null) {
				return false;
			}
			return typeName.equals(method.getType().getFullName()) && methodName.equals(method.getMethodName());
		};
	}

	private static FrameFilter classPredicate(String typeName) {
		Objects.requireNonNull(typeName, "typeName"); //$NON-NLS-1$
		return frame -> {
			IMCMethod method = method(frame);
			if (method == null) {
				return false;
			}
			IMCType type = method.getType();
			return type != null && typeName.equals(type.getFullName());
		};
	}

	private static FrameFilter packagePredicate(String packageName) {
		Objects.requireNonNull(packageName, "packageName"); //$NON-NLS-1$
		String prefix = packageName + "."; //$NON-NLS-1$
		return frame -> {
			IMCMethod method = method(frame);
			if (method == null || method.getType() == null) {
				return false;
			}
			IMCPackage pkg = method.getType().getPackage();
			if (pkg == null) {
				return false;
			}
			String name = pkg.getName();
			if (name == null) {
				return false;
			}
			return packageName.equals(name) || name.startsWith(prefix);
		};
	}

	private static IMCMethod method(IMCFrame frame) {
		return frame == null ? null : frame.getMethod();
	}
}
