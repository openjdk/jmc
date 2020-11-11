/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, 2020, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.stacktrace.tree;

import java.util.Objects;

/**
 * A node in the graph of aggregated stack traces.
 */
public final class Node {
	/**
	 * Integer uniquely identifying this node within our data structure.
	 */
	private final Integer nodeId;

	/**
	 * The frame associated with this node.
	 */
	private final AggregatableFrame frame;

	/**
	 * The weight when being the top frame.
	 */
	double weight;

	/**
	 * The cumulative weight for all contributions.
	 */
	double cumulativeWeight;

	public Node(Integer nodeId, AggregatableFrame frame) {
		this.nodeId = nodeId;
		this.frame = frame;
		if (frame == null) {
			throw new NullPointerException("Frame cannot be null!");
		}
	}

	/**
	 * @return the weight of this node.
	 */
	public double getWeight() {
		return weight;
	}


	public AggregatableFrame getFrame() {
		return frame;
	}

	@Override
	public int hashCode() {
		// This will get a few extra collisions.
		return frame.getMethod().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;

		return Objects.equals(nodeId, other.nodeId) &&
				Objects.equals(frame, other.frame) &&
				weight == other.weight &&
				cumulativeWeight == other.cumulativeWeight;
	}

	public Integer getNodeId() {
		return nodeId;
	}

	@Override
	public String toString() {
		return String.format("%s %.2f (%.2f)",
				frame.getHumanReadableShortString(), weight, cumulativeWeight);
	}
}
