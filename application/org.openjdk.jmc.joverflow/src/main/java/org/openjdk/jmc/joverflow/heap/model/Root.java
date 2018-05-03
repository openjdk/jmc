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
package org.openjdk.jmc.joverflow.heap.model;

/**
 * Represents a member of the rootset, that is, one of the objects that the GC starts from when
 * marking reachable objects.
 * <p>
 * Note that this class implements compareTo() but has no implementation of equals(). In other
 * words, it's currently not guaranteed that compareTo() returns zero if and only if equals()
 * returns true. However, that only matters if instances of a class are used in classes like
 * PriorityQueue, which is highly unlikely for this class and its subclasses.
 */
public class Root implements Comparable<Root> {

	private long id; // ID of the JavaThing we refer to
	private long refererId; // Thread or Class responsible for this, or 0
	private int type;
	private JavaHeapObject referer = null;
	private StackTrace stackTrace = null;

	// Values for type.  Higher values are more interesting -- see getType().
	// See also getTypeName()
	public final static int INVALID_TYPE = 0;
	public final static int UNKNOWN = 1;
	public final static int SYSTEM_CLASS = 2;

	public final static int JNI_LOCAL = 3;
	public final static int JNI_GLOBAL = 4;
	public final static int THREAD_BLOCK = 5;
	public final static int BUSY_MONITOR = 6;
	public final static int JAVA_LOCAL = 7;
	public final static int NATIVE_STACK = 8;
	public final static int JAVA_STATIC = 9;

	private static final String UNKNOWN_ROOT_STR = "Unknown GC root";

	public static final Root UNKNOWN_ROOT = new Root(0, 0, UNKNOWN, "") {
		@Override
		public String getIdString() {
			return UNKNOWN_ROOT_STR;
		}
	};

	public Root(long id, long refererId, int type, String description) {
		this(id, refererId, type, description, null);
	}

	public Root(long id, long refererId, int type, String description, StackTrace stackTrace) {
		this.id = id;
		this.refererId = refererId;
		this.type = type;
		this.stackTrace = stackTrace;
	}

	public long getId() {
		return id;
	}

	public String getIdString() {
		return getTypeName() + '@' + getId();
	}

	public boolean isUnknownRoot() {
		return this == UNKNOWN_ROOT;
	}

	@Override
	public String toString() {
		return getIdString();
	}

	/**
	 * Return type. We guarantee that more interesting roots will have a type that is numerically
	 * higher.
	 */
	public int getType() {
		return type;
	}

	public String getTypeName() {
		switch (type) {
		case INVALID_TYPE:
			return "Invalid (?!?)";
		case UNKNOWN:
			return "Unknown";
		case SYSTEM_CLASS:
			return "System Class";
		case JNI_LOCAL:
			return "JNI Local";
		case JNI_GLOBAL:
			return "JNI Global";
		case THREAD_BLOCK:
			return "Thread Block";
		case BUSY_MONITOR:
			return "Busy Monitor";
		case JAVA_LOCAL:
			return "Java Local";
		case NATIVE_STACK:
			return "Native Stack (possibly Java local)";
		case JAVA_STATIC:
			return "Java Static";
		default:
			return "??";
		}
	}

	/**
	 * Get the object that's responsible for this root, if there is one. This will be null, a Thread
	 * object, or a Class object.
	 */
	public JavaHeapObject getReferer() {
		return referer;
	}

	/**
	 * @return the stack trace responsible for this root, or null if there is none.
	 */
	public StackTrace getStackTrace() {
		return stackTrace;
	}

	void resolve(Snapshot ss) {
		if (refererId != 0) {
			referer = ss.getObjectForId(refererId);
		}
		if (stackTrace != null) {
			stackTrace.resolve(ss);
		}
	}

	/**
	 * Helps to sort Roots in the order of more interesting to less interesting. More interesting
	 * roots have higher type values.
	 */
	@Override
	public int compareTo(Root other) {
		// A root with higher type value should come first. So if this Root
		// should come first, a negative value should be returned below.
		// That's what will happen if this.type > other.type.
		if (other.type != this.type) {
			return other.type - this.type;
		} else {
			// Later we may differentiate roots within the same type
			return 0;
		}
	}
}
