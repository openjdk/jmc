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
package org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper class used to share analysis of JVM related information, such as flags.
 */
@SuppressWarnings("nls")
public class JvmInternalsDataProvider {

	private static final String[] PREFIXES = new String[] {"Xmx", "Xms", "Xmn", "Xss", "Xmaxjitcodesize"};
	/**
	 * Options that are OK to use multiple times if different values are provided. Check for
	 * duplicates using the full argument.
	 */
	private static final String[] VERBATIM = new String[] {"verbose", "-add-exports"};
	private static final Map<String, String> EQUIVALENT = new HashMap<>();

	static {
		putBiMap("Xbatch", "BackgroundCompilation");
		putBiMap("Xmaxjitcodesize", "ReservedCodeCacheSize");
		putBiMap("Xmx", "MaxHeapSize");
		putBiMap("Xmn", "NewSize");
		putBiMap("Xss", "ThreadStackSize");
		putBiMap("Xusealtsigs", "UseAltSigs");
		putBiMap("cp", "classpath");
		putBiMap("esa", "enablesystemassertions");
		putBiMap("dsa", "disablesystemassertions");
		putBiMap("Xconcgc", "UseConcMarkSweepGC");
		putBiMap("Xnoconcgc", "UseConcMarkSweepGC");
		putBiMap("Xnoclassgc", "ClassUnloading");
		putBiMap("Xminf", "MinHeapFreeRatio");
		putBiMap("Xmaxf", "MaxHeapFreeRatio");
		putBiMap("Xrs", "ReduceSignalUsage");
		putBiMap("Dcom.sun.management", "ManagementServer");
		putBiMap("Xshare:dump", "DumpSharedSpaces");
		putBiMap("Xboundthreads", "UseBoundThreads");
		putBiMap("AlwaysTenure", "NeverTenure");
		putBiMap("ResizeTLE", "ResizeTLAB");
		putBiMap("PrintTLE", "PrintTLAB");
		putBiMap("TLESize", "TLABSize");
		putBiMap("UseTLE", "UseTLAB");
		putBiMap("UsePermISM", "UseISM");
		putBiMap("G1MarkStackSize", "CMSMarkStackSize");
		putBiMap("Xms", "InitialHeapSize");
		putBiMap("DisplayVMOutputToStderr", "DisplayVMOutputToStdout");
		putBiMap("Xverify", "BytecodeVerificationLocal");
		putBiMap("Xverify", "BytecodeVerificationRemote");
		putBiMap("DefaultMaxRAMFraction", "MaxRAMFraction");
		putBiMap("CMSMarkStackSizeMax", "MarkStackSizeMax");
		putBiMap("ParallelMarkingThreads", "ConcGCThreads");
		putBiMap("ParallelCMSThreads", "ConcGCThreads");
		putBiMap("CreateMinidumpOnCrash", "CreateCoredumpOnCrash");
	}

	private static void putBiMap(String one, String two) {
		EQUIVALENT.put(one, two);
		EQUIVALENT.put(two, one);
	}

	/**
	 * Checks a set of JVM flags for any possible duplicates, including synonymous flags.
	 * 
	 * @param arguments
	 *            the set of JVM flags to check
	 * @return a set of all duplicated JVM flags
	 */
	public static Set<String> checkDuplicates(String arguments) {
		HashSet<String> seenFlags = new HashSet<>();
		HashSet<String> dupes = new HashSet<>();
		String[] argumentArray = arguments.split(" -");
		if (argumentArray.length == 1 && argumentArray[0].equals("")) {
			return dupes;
		}
		argumentArray[0] = argumentArray[0].substring(1);
		for (String fullArgument : argumentArray) {
			boolean verbatim = false;
			for (int i = 0; i < VERBATIM.length; i++) {
				if (fullArgument.contains(VERBATIM[i])) {
					verbatim = true;
					break;
				}
			}
			String argument;
			if (verbatim) {
				argument = fullArgument;
			} else {
				String[] split = fullArgument.split("[=:]", 3);
				argument = split[0];
				if ("XX".equals(split[0])) {
					argument = split[1];
					if (argument.startsWith("+") || argument.startsWith("-")) {
						argument = argument.substring(1);
					}
				}
				for (int i = 0; i < PREFIXES.length; i++) {
					argument = scrubPrefix(argument, PREFIXES[i]);
				}
				String equivalentArgument = EQUIVALENT.get(argument);
				if (equivalentArgument != null && !seenFlags.contains(argument)
						&& seenFlags.contains(equivalentArgument)) {
					String longerArgument = equivalentArgument.length() > argument.length() ? equivalentArgument
							: argument;
					String shorterArgument = equivalentArgument.length() > argument.length() ? argument
							: equivalentArgument;
					String combinedArgument = String.format("%s (%s)", longerArgument, shorterArgument);
					dupes.add(combinedArgument);
				}
			}
			if (seenFlags.contains(argument)) {
				dupes.add(argument);
			}
			seenFlags.add(argument);
		}
		return dupes;
	}

	private static String scrubPrefix(String argument, String prefix) {
		return argument.startsWith(prefix) ? prefix : argument;
	}

}
