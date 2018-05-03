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
package org.openjdk.jmc.joverflow.batch;

import org.openjdk.jmc.joverflow.util.MemNumFormatter;

/**
 * An output buffer that accumulates formatted text. It also has some intelligence for checking
 * certain user-specified values (e.g. the overhead caused by some particular memory problem)
 * against the previously specified threshold. If such a "critical issue" is detected, information
 * about it is stored separately, and eventually is placed on top of the output.
 */
final class FormattedOutputBuffer {

	private final StringBuilder sb = new StringBuilder(32000);
	private final StringBuilder csb = new StringBuilder(1000);

	private final MemNumFormatter nf;
	private final long totalHeapSize;

	private String currentSection, currentIssue;
	private double currentCriticalLevelInPercent;
	private boolean criticalIssuesInCurrentSectionFound;

	FormattedOutputBuffer(long totalHeapSize) {
		this.totalHeapSize = totalHeapSize;
		nf = new MemNumFormatter(totalHeapSize);
	}

	void print(String s) {
		sb.append(s);
	}

	void println(String s) {
		sb.append(s);
		sb.append('\n');
	}

	void println() {
		sb.append('\n');
	}

	void format(String format, Object ... args) {
		String result = String.format(format, args);
		sb.append(result);
	}

	String k(long num) {
		return nf.getNumInKAndPercent(num);
	}

	void startSection(String sectionName, String issueName, double criticalLevelInPercent) {
		currentSection = sectionName;
		currentIssue = issueName;
		currentCriticalLevelInPercent = criticalLevelInPercent;
		sb.append('\n');
		sb.append(sectionName);
		sb.append("\n");
		criticalIssuesInCurrentSectionFound = false;
	}

	void startSection(String sectionName) {
		startSection(sectionName, "", 0.0);
	}

	void criticalCheck(long ovhd, String issueQualifier) {
		if (((double) ovhd) / totalHeapSize * 100 <= currentCriticalLevelInPercent) {
			return;
		}

		if (!criticalIssuesInCurrentSectionFound) {
			csb.append("\n Section ");
			csb.append(currentSection);
			csb.append(" (overhead > ");
			csb.append((int) currentCriticalLevelInPercent);
			csb.append("% reported):\n");
			criticalIssuesInCurrentSectionFound = true;
		}
		csb.append("  ");
		csb.append(currentIssue);
		csb.append(" ");
		csb.append(issueQualifier);
		csb.append(" ");
		csb.append(nf.getNumInKAndPercent(ovhd));
		csb.append('\n');
	}

	MemNumFormatter getMemNumFormatter() {
		return nf;
	}

	String getOutput() {
		if (csb.length() > 0) {
			csb.insert(0, "0. IMPORTANT ISSUES OVERVIEW:\n");
			csb.append('\n');
			sb.insert(0, csb);
		}

		return sb.toString();
	}
}
