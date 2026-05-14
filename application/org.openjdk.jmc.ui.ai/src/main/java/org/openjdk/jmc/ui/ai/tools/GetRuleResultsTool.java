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
package org.openjdk.jmc.ui.ai.tools;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.ui.RuleManager;
import org.openjdk.jmc.ui.ai.IAITool;

public class GetRuleResultsTool implements IAITool {

	private static final Pattern SEVERITY_PATTERN = Pattern
			.compile("\"minSeverity\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$

	@Override
	public String getName() {
		return "get_rule_results"; //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return "Retrieves the automated analysis rule results for the open flight recording." //$NON-NLS-1$
				+ " Results are computed when the recording is opened and cover common performance issues" //$NON-NLS-1$
				+ " such as GC pressure, lock contention, and I/O bottlenecks." //$NON-NLS-1$
				+ " Returns rule name, severity, summary, explanation, and suggested solution for each result."; //$NON-NLS-1$
	}

	@Override
	public String getParameterSchema() {
		return "{\"type\":\"object\",\"properties\":{" //$NON-NLS-1$
				+ "\"minSeverity\":{\"type\":\"string\"," //$NON-NLS-1$
				+ "\"description\":\"Minimum severity to include: OK, INFO, WARNING, or NA (default INFO)\"," //$NON-NLS-1$
				+ "\"enum\":[\"OK\",\"INFO\",\"WARNING\",\"NA\"]}" //$NON-NLS-1$
				+ "}}"; //$NON-NLS-1$
	}

	@Override
	public String execute(String parametersJson) {
		RuleManager ruleManager = JfrContext.getActiveRuleManager();
		if (ruleManager == null) {
			return "No flight recording is currently open."; //$NON-NLS-1$
		}

		Severity minSeverity = parseMinSeverity(parametersJson);
		Collection<IResult> results = ruleManager.getAllResults();

		StringBuilder sb = new StringBuilder();
		sb.append("Automated Analysis Results:\n\n"); //$NON-NLS-1$
		int count = 0;
		for (IResult result : results) {
			if (result.getSeverity().compareTo(minSeverity) < 0) {
				continue;
			}
			sb.append("Rule: ").append(result.getRule().getName()).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("  Severity: ").append(result.getSeverity()).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			String summary = result.getSummary();
			if (summary != null && !summary.isEmpty()) {
				sb.append("  Summary: ").append(summary).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			String explanation = result.getExplanation();
			if (explanation != null && !explanation.isEmpty()) {
				sb.append("  Explanation: ").append(explanation).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			String solution = result.getSolution();
			if (solution != null && !solution.isEmpty()) {
				sb.append("  Solution: ").append(solution).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			sb.append("\n"); //$NON-NLS-1$
			count++;
		}
		if (count == 0) {
			sb.append("No issues found at or above severity ").append(minSeverity).append(".\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return sb.toString();
	}

	private Severity parseMinSeverity(String json) {
		Matcher m = SEVERITY_PATTERN.matcher(json);
		if (m.find()) {
			String val = m.group(1).toUpperCase();
			switch (val) {
			case "OK": //$NON-NLS-1$
				return Severity.OK;
			case "INFO": //$NON-NLS-1$
				return Severity.get(25);
			case "WARNING": //$NON-NLS-1$
				return Severity.get(50);
			case "NA": //$NON-NLS-1$
				return Severity.NA;
			default:
				break;
			}
		}
		return Severity.get(25); // INFO default
	}
}
