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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration;

import java.util.Objects;

/**
 * Class responsible for holding a segment of a path. E.g. the path "one/two/three" could be
 * represented as:
 *
 * <pre>
 * new PathElement(new PathElement(new PathElement(null, &quot;one&quot;), &quot;two&quot;), &quot;three&quot;);
 * </pre>
 */
public abstract class PathElement {
	public enum PathElementKind {
		UNKNOWN(0), IN_CONFIGURATION(1), IN_SERVER(2), IN_BOTH(3);

		private PathElementKind(int flags) {
			assert flags == ordinal();
		}

		public boolean contains(PathElementKind otherKind) {
			int otherFlags = otherKind.ordinal();
			return (ordinal() & otherFlags) == otherFlags;
		}

		public PathElementKind add(PathElementKind otherKind) {
			return values()[ordinal() | otherKind.ordinal()];
		}
	}

	private final String label;
	private final PropertyContainer m_parent;
	private PathElementKind kind;

	protected PathElement(PropertyContainer parent, String label, PathElementKind kind) {
		this.label = label;
		m_parent = parent;
		this.kind = kind;
	}

	public final PropertyContainer getParent() {
		return m_parent;
	}

	public final String getName() {
		return label;
	}

	protected final void setKind(PathElementKind kind) {
		this.kind = kind;
	}

	protected boolean addKind(PathElementKind kind) {
		PathElementKind newKind = this.kind.add(kind);
		if (newKind != this.kind) {
			this.kind = newKind;
			// NOTE: We rely on the dummy root having a kind IN_BOTH to avoid having to check if parent is null here.
			m_parent.addKind(kind);
			return true;
		}
		return false;
	}

	public PathElementKind getKind() {
		return kind;
	}

	@Override
	public int hashCode() {
		return Objects.hash(label, m_parent);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PathElement) {
			return Objects.equals(label, ((PathElement) obj).label)
					&& Objects.equals(m_parent, ((PathElement) obj).m_parent);
		}
		return false;
	}

	@Override
	public String toString() {
		return this.getClass().getName() + "@" + getName(); //$NON-NLS-1$
	}
}
