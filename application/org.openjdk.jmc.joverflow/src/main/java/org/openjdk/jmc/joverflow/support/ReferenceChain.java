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
package org.openjdk.jmc.joverflow.support;

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmc.joverflow.heap.model.JavaClass;

/**
 * Convenience static methods for manipulations with a reference chain going up from a
 * RefChainElement instance.
 */
public class ReferenceChain {
	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	/**
	 * Returns the full reference chain in straight order for the given RefChainElement.
	 */
	public static List<RefChainElement> getChain(RefChainElement referer) {
		ArrayList<RefChainElement> reverseResult = new ArrayList<>();
		do {
			reverseResult.add(referer);
			referer = referer.getReferer();
		} while (referer != null);
		if (reverseResult.size() == 1) {
			return reverseResult;
		}
		ArrayList<RefChainElement> result = new ArrayList<>(reverseResult.size());
		for (int i = reverseResult.size() - 1; i >= 0; i--) {
			result.add(reverseResult.get(i));
		}
		return result;
	}

	public static RefChainElement getRootElement(RefChainElement referer) {
		RefChainElement parent = referer.getReferer();
		while (parent != null) {
			referer = parent;
			parent = referer.getReferer();
		}
		return referer;
	}

	public static String toStringInReverseOrder(RefChainElement referer, int maxChainDepth) {
		return toStringInReverseOrder(referer, maxChainDepth, EMPTY_STRING_ARRAY);
	}

	public static String toStringInReverseOrder(
		RefChainElement referer, int maxChainDepth, String[] stopperClassPrefixes) {
		StringBuilder sb = new StringBuilder(200);

		// First, check if we have one of the "stopper" classes in the chain
		int endIdx;
		RefChainElement curElement = referer;
		for (endIdx = 0; curElement != null; endIdx++) {
			JavaClass clazz = curElement.getJavaClass();
			if (clazz != null) { // clazz is null for root
				if (startsWithOneOf(clazz.getName(), stopperClassPrefixes)) {
					break;
				}
			}
			curElement = curElement.getReferer();
		}

		if (curElement == null) {
			// No stopper classes found
			endIdx = maxChainDepth;
		} else {
			// So that the stopper is actually included
			endIdx++;
		}

		curElement = referer;
		for (int i = 0; curElement != null && i < endIdx; i++) {
			if (curElement.getReferer() != null) {
				sb.append("<--");
			} else {
				sb.append("<<-");
			}
			sb.append(curElement.toString());
			curElement = curElement.getReferer();
		}

		if (curElement != null) {
			// We haven't reached the root
			sb.append("<--...");
			RefChainElement parent = curElement.getReferer();
			while (parent != null) {
				curElement = parent;
				parent = curElement.getReferer();
			}
			sb.append("<<-");
			sb.append(((RefChainElementImpl.GCRoot) curElement).getRoot().getIdString());
		}

		return sb.toString();
	}

	public static String toStringInStraightOrder(RefChainElement referer) {
		StringBuilder sb = new StringBuilder(80);
		List<RefChainElement> chain = getChain(referer);

		int startIdx = 0;
		if (chain.get(startIdx) instanceof RefChainElementImpl.GCRoot) {
			sb.append(((RefChainElementImpl.GCRoot) chain.get(startIdx)).getRoot().getIdString());
			sb.append("->>");
			startIdx++;
		}

		for (int i = startIdx; i < chain.size() - 1; i++) {
			sb.append(chain.get(i).toString());
			sb.append("-->");
		}
		sb.append(chain.get(chain.size() - 1).toString());

		return sb.toString();
	}

	private static boolean startsWithOneOf(String str, String[] prefixes) {
		if (prefixes.length == 0) {
			return false;
		}

		for (String prefix : prefixes) {
			if (str.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}
}
