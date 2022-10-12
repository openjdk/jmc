/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.rules.report.html.internal;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.ResultToolkit;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.owasp.encoder.Encode;

public class RulesHtmlToolkit {

	public static final TypedResult<Boolean> FAILED = new TypedResult<>("failed", "", "", UnitLookup.FLAG); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	public static final TypedResult<Boolean> IN_PROGRESS = new TypedResult<>("inProgress", "", "", UnitLookup.FLAG); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	public static final TypedResult<Boolean> IGNORED = new TypedResult<>("ignored", "", "", UnitLookup.FLAG); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^]]*)\\]\\(([^\\s^\\)]*)[\\s\\)]"); //$NON-NLS-1$

	static {
		RULE_TEMPLATE = readFromFile("rule_result.html"); //$NON-NLS-1$
		TEMPLATE = readFromFile("rules_overview.html"); //$NON-NLS-1$
	}

	/**
	 * Gets the CSS type for the overview template. Due to how the Severity enum is defined it
	 * cannot be used here.
	 *
	 * @param value
	 *            result value
	 * @return CSS type
	 */
	private static String getType(IResult result) {
		if (result.getSeverity() == Severity.WARNING) {
			return "warning"; //$NON-NLS-1$
		} else if (result.getSeverity() == Severity.INFO) {
			return "info"; //$NON-NLS-1$
		} else if (result.getSeverity() == Severity.OK) {
			return "ok"; //$NON-NLS-1$
		} else if (result.getSeverity() == Severity.NA) {
			if (Boolean.TRUE.equals(result.getResult(FAILED))) {
				return "error"; //$NON-NLS-1$
			} else if (Boolean.TRUE.equals(result.getResult(IN_PROGRESS))) {
				return "progress"; //$NON-NLS-1$
			} else if (Boolean.TRUE.equals(result.getResult(IGNORED))) {
				return "ignore"; //$NON-NLS-1$
			}
			return "na"; //$NON-NLS-1$
		}
		return "error"; //$NON-NLS-1$
	}

	private static final String RULE_TEMPLATE;
	private static final String TEMPLATE;

	private static String readFromFile(String path) {
		String result = ""; //$NON-NLS-1$
		InputStream is = RulesHtmlToolkit.class.getResourceAsStream(path);
		try {
			List<String> loadFromFile = IOToolkit.loadFromStream(is);
			StringBuilder sb = new StringBuilder();
			for (String s : loadFromFile) {
				sb.append(s + System.getProperty("line.separator")); //$NON-NLS-1$
			}
			result = sb.toString();
		} catch (IOException e) {
			Logger.getLogger(RulesHtmlToolkit.class.getName()).log(Level.WARNING, "Couldn't read HTML template file.", //$NON-NLS-1$
					e);
		}
		return result;
	}

	private static final String START_DIV = "<div class=\"wrapper\" id=\"notAllOk\">"; //$NON-NLS-1$
	private static final String END_DIV = "</div>"; //$NON-NLS-1$
	private static final String CLOSE_HTML = "</body></html>"; //$NON-NLS-1$
	private static final String PUSH_DIV = "<div class=\"push\"></div>"; //$NON-NLS-1$

	private static String createShowOK() {
		String ok = "<div class=\"okbox\"><label class=\"showOkText\" for=\"showOk\">"; //$NON-NLS-1$
		ok += Messages.getString(Messages.RULESPAGE_SHOW_OK_RESULTS_ACTION);
		ok += "</label>&nbsp;<input type=\"checkbox\" onclick='overview.showOk(this.checked);' id=\"showOk\">&nbsp;&nbsp;&nbsp;</div>"; //$NON-NLS-1$
		ok += "<script>window.onload = function() {overview.showOk(false)}</script>"; //$NON-NLS-1$
		return ok;
	}

	private static String buildShowOkCheckBox() {
		String template = PUSH_DIV;
		template += END_DIV;
		template += createShowOK();
		template += END_DIV;
		return template;
	}

	private static String getHtmlTemplate() {
		return TEMPLATE;
	}

	private static String getAllOkTemplate() {
		return MessageFormat.format(
				"<div id=\"allgreen\" style=\"display: none;\"><p class=\"allOk\">{0}</p><p style=\"font-family: sans-serif\">{1}</p></div>", //$NON-NLS-1$
				Messages.getString(Messages.RULES_NO_PROBLEMS_FOUND),
				Messages.getString(Messages.RULES_NO_PROBLEMS_FOUND_DETAILS));
	}

	private static String getAllIgnoredTemplate() {
		return MessageFormat.format(
				"<div id=\"allignored\" style=\"display: none;\"><p class=\"allIgnored\">{0}</p></div>", //$NON-NLS-1$
				Messages.getString(Messages.RULES_ALL_IGNORED_MESSAGE));
	}

	private static String createRuleHtml(IResult result, boolean expanded, int margin) throws IOException {
		String description = getDescription(result);
		return createRuleHtml(result.getRule().getId(), result.getRule().getName(), description, expanded, margin,
				result.getRule().getTopic(), result);
	}

	/**
	 * Creates an html representation of a result for use in the result report ui.
	 *
	 * @param id
	 *            the rule id
	 * @param value
	 *            the value of the result
	 * @param title
	 *            the name of the rule
	 * @param description
	 *            the full description of the result
	 * @param expanded
	 *            whether or not the description should be expanded by default
	 * @param margin
	 *            the left margin of this result
	 * @param uuid
	 *            a uuid used to uniquely identify different instances of the same result
	 * @return an html string representing the given result parameters
	 */
	private static String createRuleHtml(
		String id, String title, String description, Boolean expanded, int margin, String uuid, IResult result) {
		IQuantity score = result.getResult(TypedResult.SCORE);
		double value = Math.round(score == null ? result.getSeverity().getLimit() : score.doubleValue());
		StringBuilder sb = new StringBuilder(RULE_TEMPLATE);
		String clazz, button;
		if (expanded) {
			clazz = "visible"; //$NON-NLS-1$
			button = "-"; //$NON-NLS-1$
		} else {
			clazz = "hidden"; //$NON-NLS-1$
			button = "+"; //$NON-NLS-1$
		}
		boolean isInProgress = Boolean.TRUE.equals(result.getResult(IN_PROGRESS));
		String displayScore = isInProgress ? "none" : "inline block"; //$NON-NLS-1$ //$NON-NLS-2$
		String displayProgress = isInProgress ? "inline block" : "none"; //$NON-NLS-1$ //$NON-NLS-2$
		return MessageFormat.format(sb.toString(), Encode.forHtml(id + uuid), (value == -1) ? "N/A" : value, //$NON-NLS-1$
				Encode.forHtml(title), description, getType(result), margin, clazz, button, displayScore,
				displayProgress, id);
	}

	private static final String HEADING_PATTERN = "<div id=\"{0}_group\"><div id=\"{0}_heading\" style=\"margin-left: {4}px;\" class=\"{2}\"><div onclick=\"overview.link(''{0}'');\"><img class=\"{2}_icon\" alt=\"{1}\" src=\"data:image/png;base64,{3}\"/>{1}</div></div>"; //$NON-NLS-1$

	private static String createSubHeading(String id, String name, String type, String image, int margin) {
		return MessageFormat.format(HEADING_PATTERN, Encode.forHtml(id), Encode.forHtml(name), Encode.forHtml(type),
				image, margin);
	}

	private static String createSubHeading(HtmlResultGroup page, String type, int margin) {
		return createSubHeading(page.getId(), page.getName(), type, page.getImage(), margin);
	}

	// FIXME: Make private and instead add a method for creating javascript updates
	public static String getDescription(IResult result) {
		String summary = result.getSummary() == null ? "" : Encode.forHtml(result.getSummary()); //$NON-NLS-1$
		String explanation = result.getExplanation() == null ? "" : Encode.forHtml(result.getExplanation()); //$NON-NLS-1$
		String solution = result.getSolution() == null ? "" : Encode.forHtml(result.getSolution()); //$NON-NLS-1$
		summary = ResultToolkit.populateMessage(result, summary, true);
		explanation = ResultToolkit.populateMessage(result, explanation, true);
		solution = ResultToolkit.populateMessage(result, solution, true);
		String description = "<div class=\"longDescription\">" + summary + "</div>"; //$NON-NLS-1$ //$NON-NLS-2$
		description += (explanation != null) ? "<div class=\"longDescription\">" + explanation + "</div>" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		description += (solution != null) ? "<div class=\"longDescription\">" + solution + "</div>" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		Matcher matcher = LINK_PATTERN.matcher(description);
		description.replace("\n", "<p>"); //$NON-NLS-1$//$NON-NLS-2$
		while (matcher.find()) {
			String title = matcher.group(1);
			String url = matcher.group(2);
			String encodedUrl = Encode.forUriComponent(url);
			String link = "<a class=\"anchorTag\" onclick=\"window.location.assign(decodeURIComponent('" + encodedUrl //$NON-NLS-1$
					+ "'))\">" + title + "</a>"; //$NON-NLS-1$//$NON-NLS-2$
			description = description.replace(matcher.group(), link);
		}
		return description;
	}

	private static final Comparator<IResult> RESULT_RULEID_COMPARATOR = new Comparator<IResult>() {

		@Override
		public int compare(IResult r1, IResult r2) {
			return r1.getRule().getId().compareTo(r2.getRule().getId());
		}
	};

	private static final Comparator<IResult> RESULT_SCORE_COMPARATOR = new Comparator<IResult>() {

		@Override
		public int compare(IResult r1, IResult r2) {
			return r1.getSeverity().compareTo(r2.getSeverity());
		}
	};

	public static String generateSinglePageHtml(Collection<IResult> results) throws IOException {
		if (results == null) {
			return ""; //$NON-NLS-1$
		}
		StringBuilder html = new StringBuilder(getHtmlTemplate());
		html.append(getAllIgnoredTemplate());
		html.append(START_DIV);
		html.append("<section id=\"sec\">"); //$NON-NLS-1$
		List<IResult> resultList = new ArrayList<>(results);
		resultList.sort(RESULT_SCORE_COMPARATOR);
		if (results.size() == 0) {
			html.append("<p style=\"font-size: 1.1em;\">"); //$NON-NLS-1$
			html.append(Messages.getString(Messages.ResultOverview_NO_RESULTS_FOR_PAGE));
			html.append("</p>"); //$NON-NLS-1$
		} else {
			for (IResult result : resultList) {
				boolean expand = result.getSeverity().compareTo(Severity.INFO) >= 0;
				html.append(createRuleHtml(result, expand, 0));
			}
		}
		html.append("</section>"); //$NON-NLS-1$
		html.append(END_DIV);
		html.append(CLOSE_HTML);
		return html.toString();
	}

	public static String generateStructuredHtml(
		HtmlResultProvider editor, Iterable<HtmlResultGroup> descriptors, HashMap<String, Boolean> resultExpandedStates,
		boolean addShowOkCheckBox) {
		StringBuilder div = new StringBuilder(getHtmlTemplate());
		if (addShowOkCheckBox) {
			div.append(buildShowOkCheckBox());
		}
		div.append(getAllOkTemplate());
		div.append(getAllIgnoredTemplate());
		div.append(START_DIV);
		Set<String> displayed = new HashSet<>();
		for (HtmlResultGroup dpd : descriptors) {
			Collection<IResult> headResults = editor.getResults(dpd.getTopics());
			if (!dpd.hasChildren() && headResults.size() == 0) {
				continue;
			}
			div.append("<section>"); //$NON-NLS-1$
			RulesHtmlToolkit.generateTitleAndResults(createSubHeading(dpd, "column_title", 0), dpd.getId(), headResults, //$NON-NLS-1$
					resultExpandedStates, div);
			for (HtmlResultGroup child : dpd.getChildren()) {
				div.append(RulesHtmlToolkit.generateSubPageHTML(editor, child, 10, displayed, resultExpandedStates));
			}
			div.append(END_DIV); // ends the <div> group tag opened by the .createSubHeading call
			div.append("</section>"); //$NON-NLS-1$
			for (IResult headResult : headResults) {
				displayed.add(headResult.getRule().getTopic());
			}
		}
		Collection<String> topics = RulesToolkit.getAllTopics();
		Collection<String> unusedTopics = new HashSet<>();
		for (IResult result : editor.getResults(topics)) {
			if (!displayed.contains(result.getRule().getTopic())) {
				unusedTopics.add(result.getRule().getTopic());
			}
		}
		div = RulesHtmlToolkit.addTopics(div, editor, unusedTopics, resultExpandedStates);
		div.append(CLOSE_HTML);
		return div.toString();
	}

	private static StringBuilder addTopics(
		StringBuilder div, HtmlResultProvider editor, Collection<String> topics,
		HashMap<String, Boolean> resultExpandedStates) {
		for (String topic : topics) {
			div.append("<section>"); //$NON-NLS-1$
			List<String> topicColl = new ArrayList<>(1);
			topicColl.add(topic);
			RulesHtmlToolkit.generateTitleAndResults(createSubHeading(topic, topic, "column_title", null, 0), //$NON-NLS-1$
					topic, editor.getResults(topicColl), resultExpandedStates, div);
			div.append("</section>"); //$NON-NLS-1$
		}
		return div;
	}

	private static String generateSubPageHTML(
		HtmlResultProvider editor, HtmlResultGroup parent, int margin, Set<String> displayed,
		HashMap<String, Boolean> resultExpandedStates) {
		StringBuilder html = new StringBuilder();
		List<HtmlResultGroup> children = parent.getChildren();
		StringBuilder results = new StringBuilder();
		List<IResult> sortResults = RulesHtmlToolkit.sortResults(editor.getResults(parent.getTopics()));
		for (IResult result : sortResults) {
			results.append(createRuleHtml(result.getRule().getId(), result.getRule().getName(), getDescription(result),
					RulesHtmlToolkit.isExpanded(resultExpandedStates, result), margin + 10, parent.getId(), result)); // $NON-NLS-1$
			displayed.add(result.getRule().getTopic());
		}
		for (HtmlResultGroup child : children) {
			results.append(generateSubPageHTML(editor, child, margin <= 60 ? margin + 20 : margin, displayed,
					resultExpandedStates));
		}
		if (results.length() > 0) {
			html.append(createSubHeading(parent, "page_heading", margin)); //$NON-NLS-1$
			html.append(results);
			html.append(END_DIV); // closes the <div> element opened by the createSubHeading call, so that rules are proper children of datapages
		}
		return html.toString();
	}

	private static void generateTitleAndResults(
		String subHeading, String uuid, Collection<IResult> results, HashMap<String, Boolean> resultExpandedStates,
		StringBuilder div) {
		List<IResult> headResults = RulesHtmlToolkit.sortResults(results);
		div.append(subHeading); // $NON-NLS-1$
		if (headResults != null) {
			for (IResult result : headResults) {
				div.append(createRuleHtml(result.getRule().getId(), result.getRule().getName(), getDescription(result),
						RulesHtmlToolkit.isExpanded(resultExpandedStates, result), 10, uuid, result));
			}
		}
	}

	private static Boolean isExpanded(HashMap<String, Boolean> resultExpandedStates, IResult result) {
		Boolean isExpanded = resultExpandedStates.get(result.getRule().getId());
		return isExpanded != null ? isExpanded : Boolean.FALSE;
	}

	private static List<IResult> sortResults(Collection<IResult> results) {
		List<IResult> sorted = new ArrayList<>(results);
		sorted.sort(RESULT_RULEID_COMPARATOR);
		return sorted;
	}

}
