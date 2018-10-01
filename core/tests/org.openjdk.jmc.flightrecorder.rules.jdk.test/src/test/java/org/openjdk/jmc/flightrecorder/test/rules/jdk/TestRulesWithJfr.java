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
package org.openjdk.jmc.flightrecorder.test.rules.jdk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IItemQuery;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.test.SlowTests;
import org.openjdk.jmc.common.test.TestToolkit;
import org.openjdk.jmc.common.test.io.IOResource;
import org.openjdk.jmc.common.test.io.IOResourceSet;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class for testing jfr rule report consistency
 */
@SuppressWarnings("nls")
public class TestRulesWithJfr {
	private static final String JFR_RULE_BASELINE_JFR = "JfrRuleBaseline.xml";
	private static final String BASELINE_DIR = "baseline";
	static final String RECORDINGS_DIR = "jfr";
	static final String RECORDINGS_INDEXFILE = "index.txt";

	private TimeZone defaultTimeZone;
	
	@Before
	public void before() {
		// empty the log before each test
		DetailsTracker.clear();
		// force UTC time zone during test
		defaultTimeZone = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}
	
	@After
	public void after() {
		// restore previous default time zone
		TimeZone.setDefault(defaultTimeZone);
	}

	@Test
	public void verifyOneResult() throws IOException {
		verifyRuleResults(true);
	}

	@Category(value = {SlowTests.class})
	@Test
	public void verifyAllResults() throws IOException {
		verifyRuleResults(false);
	}

	private void verifyRuleResults(boolean onlyOneRecording) throws IOException {
		IOResourceSet jfrs = TestToolkit.getResourcesInDirectory(TestRulesWithJfr.class, RECORDINGS_DIR, RECORDINGS_INDEXFILE);
		String reportName = null;
		if (onlyOneRecording) {
			IOResource firstJfr = jfrs.iterator().next();
			jfrs = new IOResourceSet(firstJfr);
			reportName = firstJfr.getName();
		}
		// Run all the .jfr files in the directory through the rule engine
		ReportCollection rulesReport = generateRulesReport(jfrs);

		// Parse the baseline XML file
		ReportCollection baselineReport = parseRulesReportXml(BASELINE_DIR, JFR_RULE_BASELINE_JFR, reportName);

		// Compare the baseline with the current rule results
		boolean resultsEqual = rulesReport.compareAndLog(baselineReport);

		// Save file for later inspection and/or updating the baseline with
		if (!resultsEqual) {
			// Save the generated file to XML
			saveToFile(rulesReport.toXml(), BASELINE_DIR, JFR_RULE_BASELINE_JFR, onlyOneRecording);
		}

		// Assert that the comparison returned true
		Assert.assertTrue(DetailsTracker.getEntries(), resultsEqual);
	}

	private static void saveToFile(Document doc, String directory, String fileName, boolean onlyOneRecording) {
		String filePath = getResultDir().getAbsolutePath() + File.separator
				+ ((directory != null) ? (directory + File.separator) : "")
				+ (onlyOneRecording ? "Generated_One_" : "Generated_") + fileName;
		File resultFile = new File(filePath);
		prepareFile(resultFile);
		try {
			writeDomToStream(doc, new FileOutputStream(resultFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static void prepareFile(File file) {
		if (file.exists()) {
			file.delete();
		}
		File parent = file.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("Error creating file \"" + file.getAbsolutePath() + "\". Error:\n" + e.getMessage());
		}
	}

	private static void writeDomToStream(Document doc, OutputStream os) {
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);
			StreamResult console = new StreamResult(os);
			transformer.transform(source, console);
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}

	private static ReportCollection parseRulesReportXml(String directory, String fileName, String reportName) {
		ReportCollection collection = new ReportCollection();
		try {
			// FIXME: No need to go via temp file. Just get the input stream directly from the resource.
			File dir = TestToolkit.materialize(TestRulesWithJfr.class, directory, fileName);
			File file = new File(dir, fileName);
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document baselineDoc = docBuilder.parse(file);
			collection = ReportCollection.fromXml(baselineDoc, reportName);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
		return collection;
	}

	private static ReportCollection generateRulesReport(IOResourceSet jfrs) {
		ReportCollection collection = new ReportCollection();
		for (IOResource jfr : jfrs) {
			Report report = generateReport(jfr, false, null);
			collection.put(report.getName(), report);
		}
		return collection;
	}

	private static File getResultDir() {
		if (System.getProperty("results.dir") != null) {
			return new File(System.getProperty("results.dir"));
		} else {
			return new File(System.getProperty("user.dir"));
		}
	}

	private static Report generateReport(IOResource jfr, boolean verbose, Severity minSeverity) {
		Report report = new Report(jfr.getName());
		try {
			IItemCollection events = JfrLoaderToolkit.loadEvents(jfr.open());

			for (IRule rule : RuleRegistry.getRules()) {
				try {
					RunnableFuture<Result> future = rule.evaluate(events,
							IPreferenceValueProvider.DEFAULT_VALUES);
					future.run();
					Result result = future.get();
//					for (Result result : results) {
					if (minSeverity == null || Severity.get(result.getScore()).compareTo(minSeverity) >= 0) {
						ItemSet itemSet = null;
						IItemQuery itemQuery = result.getItemQuery();
						if (verbose && itemQuery != null && !itemQuery.getAttributes().isEmpty()) {
							itemSet = new ItemSet();
							IItemCollection resultEvents = events.apply(itemQuery.getFilter());
							Collection<? extends IAttribute<?>> attributes = itemQuery.getAttributes();
							for (IAttribute<?> attribute : attributes) {
								itemSet.addField(attribute.getName());
							}
							Iterator<? extends IItemIterable> iterables = resultEvents.iterator();
							while (iterables.hasNext()) {
								IItemIterable ii = iterables.next();
								IType<IItem> type = ii.getType();
								List<IMemberAccessor<?, IItem>> accessors = new ArrayList<>(attributes.size());
								for (IAttribute<?> a : attributes) {
									accessors.add(a.getAccessor(type));
								}
								Iterator<? extends IItem> items = ii.iterator();
								while (items.hasNext()) {
									ItemList itemList = new ItemList();
									IItem item = items.next();
									for (IMemberAccessor<?, IItem> a : accessors) {
										itemList.add(String.valueOf(a.getMember(item)));
									}
									itemSet.addItem(itemList);
								}
							}
						}
						RuleResult ruleResult = new RuleResult(String.valueOf(result.getRule().getId()),
								Severity.get(result.getScore()).getLocalizedName(), String.valueOf(result.getScore()),
								result.getShortDescription(), result.getLongDescription(), itemSet);
						report.put(String.valueOf(result.getRule().getId()), ruleResult);
//						}
					}
				} catch (RuntimeException | InterruptedException | ExecutionException e) {
					System.out.println("Problem while evaluating rules for \"" + jfr.getName() + "\". Message: "
							+ e.getLocalizedMessage());
				}
			}
		} catch (IOException | CouldNotLoadRecordingException e) {
			e.printStackTrace();
		}
		return report;
	}

	private static Element createValueNode(Document doc, String name, String value) {
		Element node = doc.createElement(name);
		node.appendChild(doc.createTextNode(value != null ? value : ""));
		return node;
	}

	private static List<String> getNodeValues(String xpathExpr, Node node) {
		List<String> values = new ArrayList<>();
		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
			XPathExpression expression = xpath.compile(xpathExpr);
			NodeList nodes = ((NodeList) expression.evaluate(node, XPathConstants.NODESET));
			for (int i = 0; i < nodes.getLength(); i++) {
				Node thisNodeOnly = nodes.item(i);
				thisNodeOnly.getParentNode().removeChild(thisNodeOnly);
				Node child = thisNodeOnly.getFirstChild();
				if (child != null) {
					values.add(child.getNodeValue());
				} else {
					values.add("");
				}
			}
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		return values;
	}

	private static NodeList getNodeSet(String expr, Node node) {
		NodeList result = null;
		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
			XPathExpression xPath = xpath.compile(expr);
			result = (NodeList) xPath.evaluate(node, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		return result;
	}

	private static class ReportCollection {
		private SortedMap<String, Report> reports;

		public ReportCollection() {
			reports = new TreeMap<>();
		}

		public void put(String filename, Report report) {
			reports.put(filename, report);
		}

		public Report get(String filename) {
			return reports.get(filename);
		}

		public boolean compareAndLog(Object other) {
			ReportCollection otherReportCollection = (ReportCollection) other;
			boolean equals = reports.size() == otherReportCollection.reports.size();
			if (!equals) {
				if (reports.size() > otherReportCollection.reports.size()) {
					for (String reportname : reports.keySet()) {
						if (otherReportCollection.get(reportname) == null) {
							DetailsTracker.log("Report for " + reportname
									+ " could not be found in the other report collection. ");
						}
					}
				} else {
					for (String reportname : otherReportCollection.reports.keySet()) {
						if (reports.get(reportname) == null) {
							DetailsTracker.log(
									"Report for " + reportname + " could not be found in this report collection. ");
						}
					}
				}
				DetailsTracker.log("\n");
			}
			for (String reportname : reports.keySet()) {
				Report otherReport = otherReportCollection.get(reportname);
				if (otherReport != null) {
					equals = reports.get(reportname).compareAndLog(otherReport) && equals;
				} else {
					DetailsTracker
							.log("\nReport for " + reportname + " could not be found in the other report collection. ");
					equals = false;
				}
			}
			return equals;
		}

		public Document toXml() {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder;
			Document doc = null;
			try {
				docBuilder = docFactory.newDocumentBuilder();
				doc = docBuilder.newDocument();
				Element rootElement = doc.createElement("reportcollection");
				doc.appendChild(rootElement);
				for (Report report : reports.values()) {
					report.toXml(rootElement);
				}
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			}
			return doc;
		}

		public static ReportCollection fromXml(Document doc, String reportName) {
			ReportCollection collection = new ReportCollection();
			NodeList reports = getNodeSet("//report", doc);
			for (int i = 0; i < reports.getLength(); i++) {
				Node thisReportOnly = reports.item(i);
				thisReportOnly.getParentNode().removeChild(thisReportOnly);
				Report report = Report.fromXml(thisReportOnly);
				if (reportName == null || report.getName().equals(reportName)) {
					collection.put(report.getName(), report);
				}
			}
			return collection;
		}
	}

	private static class Report {
		private String filename;
		private SortedMap<String, RuleResult> rules;

		public Report(String filename) {
			this.filename = filename;
			rules = new TreeMap<>();
		}

		public void put(String id, RuleResult rule) {
			rules.put(id, rule);
		}

		public RuleResult get(String id) {
			return rules.get(id);
		}

		public String getName() {
			return filename;
		}

		public boolean compareAndLog(Object other) {
			Report otherReport = (Report) other;
			boolean equals = rules.size() == otherReport.rules.size();
			boolean fileNamePrinted = false;
			if (equals) {
				for (String rulename : rules.keySet()) {
					RuleResult otherRule = otherReport.get(rulename);
					if (otherRule != null) {
						equals = rules.get(rulename).compareAndLog(otherRule) && equals;
						if (!equals && !fileNamePrinted) {
							DetailsTracker.log("\n\nReport: \"" + filename + "\", ");
							fileNamePrinted = true;
						}
					} else {
						DetailsTracker.log("\n\nReport: \"" + filename + "\". Rule result for " + rulename
								+ " could not be found in the other report. ");
						equals = false;
					}
				}
			} else {
				if (rules.size() > otherReport.rules.size()) {
					for (String ruleId : rules.keySet()) {
						RuleResult otherRule = otherReport.get(ruleId);
						if (otherRule != null) {
							equals = rules.get(ruleId).compareAndLog(otherRule) && equals;
						} else {
							DetailsTracker.log("\nReport for file \"" + filename + "\", rule result for \"" + ruleId
									+ "\" could not be found in the other report. ");
						}
					}
				} else {
					for (String ruleId : otherReport.rules.keySet()) {
						RuleResult rule = rules.get(ruleId);
						if (rule != null) {
							equals = rule.compareAndLog(otherReport.rules.get(ruleId)) && equals;
						} else {
							DetailsTracker.log("\nReport for file \"" + filename + "\", rule result for \"" + ruleId
									+ "\" could not be found in this report. ");
						}
					}
				}
				DetailsTracker.log("\n");
			}
			return equals;
		}

		public void toXml(Element parent) {
			Element reportNode = parent.getOwnerDocument().createElement("report");
			parent.appendChild(reportNode);
			reportNode.appendChild(createValueNode(parent.getOwnerDocument(), "file", filename));
			for (RuleResult rule : rules.values()) {
				rule.toXml(reportNode);
			}
		}

		public static Report fromXml(Node node) {
			Report report = new Report(getNodeValues("./file", node).get(0));
			NodeList rules = getNodeSet("./rule", node);
			for (int i = 0; i < rules.getLength(); i++) {
				Node thisRuleOnly = rules.item(i);
				thisRuleOnly.getParentNode().removeChild(thisRuleOnly);
				RuleResult rule = RuleResult.fromXml(thisRuleOnly);
				report.put(rule.getId(), rule);
			}
			return report;
		}
	}

	private static class RuleResult {
		private String id;
		private String severity;
		private String score;
		private String shortDescription;
		private String longDescription;
		private ItemSet itemset;

		public RuleResult(String id, String severity, String score, String shortDescription, String longDescription,
				ItemSet itemset) {
			this.id = id;
			this.severity = severity;
			this.score = score;
			this.shortDescription = shortDescription;
			this.longDescription = longDescription;
			this.itemset = itemset;
		}

		public String getId() {
			return id;
		}

		public boolean compareAndLog(Object other) {
			RuleResult otherRule = (RuleResult) other;
			boolean scoreEquals = Objects.equals(score, otherRule.score);
			if (!scoreEquals) {
				// determine if this is just a rounding error
				scoreEquals = (Math.abs(Float.valueOf(score) - Float.valueOf(otherRule.score)) < 0.0000000000001f) ? true
						: false;
				if (scoreEquals) {
					// apparently a rounding issue. Print it out for informational purposes
					System.out
							.println("Rule \"" + id + "\": Encountered rounding issue for score when comparing values "
									+ score + " and " + otherRule.score);
				}
			}
			boolean itemSetEquality = compareAndLogItemSets(other);
			boolean ruleEquality = Objects.equals(severity, otherRule.severity) && scoreEquals
					&& Objects.equals(shortDescription, otherRule.shortDescription)
					&& Objects.equals(longDescription, otherRule.longDescription);
			if (!ruleEquality) {
				if (!Objects.equals(severity, otherRule.severity)) {
					DetailsTracker.log("\n    Severity mismatch: \"" + severity + "\" was not equal to \""
							+ otherRule.severity + "\". ");
				}
				if (!scoreEquals) {
					DetailsTracker.log(
							"\n    Score mismatch: \"" + score + "\" was not equal to \"" + otherRule.score + "\". ");
				}
				if (!Objects.equals(shortDescription, otherRule.shortDescription)) {
					DetailsTracker.log("\n    Message mismatch: \"" + shortDescription + "\" was not equal to \""
							+ otherRule.shortDescription + "\". ");
				}
				if (!Objects.equals(longDescription, otherRule.longDescription)) {
					DetailsTracker.log("\n    Description mismatch: \"" + longDescription + "\" was not equal to \""
							+ otherRule.longDescription + "\". ");
				}
			}
			if (!(itemSetEquality && ruleEquality)) {
				DetailsTracker.log("\n  Rule: \"" + id + "\". ");
			}
			return itemSetEquality && ruleEquality;
		}

		private boolean compareAndLogItemSets(Object other) {
			RuleResult otherRule = (RuleResult) other;
			if (itemset != null && otherRule.itemset != null) {
				// both rules have items, compare these
				return itemset.compareAndLog(otherRule.itemset);
			} else if (itemset == null && otherRule.itemset == null) {
				// no items in any of the rules (both null)
				return true;
			} else {
				if (itemset == null) {
					DetailsTracker.log("\n    This item set was null while the other wasn't. The other: "
							+ otherRule.itemset + ". ");
				} else {
					DetailsTracker.log("\n    The other item set was null while this wasn't. This: " + itemset + ". ");
				}
				return false;
			}
		}

		public void toXml(Element parent) {
			Element ruleNode = parent.getOwnerDocument().createElement("rule");
			parent.appendChild(ruleNode);
			ruleNode.appendChild(createValueNode(parent.getOwnerDocument(), "id", id));
			ruleNode.appendChild(createValueNode(parent.getOwnerDocument(), "severity", severity));
			ruleNode.appendChild(createValueNode(parent.getOwnerDocument(), "score", score));
			ruleNode.appendChild(createValueNode(parent.getOwnerDocument(), "shortDescription", shortDescription));
			if (longDescription != null) {
				ruleNode.appendChild(createValueNode(parent.getOwnerDocument(), "longDescription", longDescription));
			}
			if (itemset != null) {
				itemset.toXml(ruleNode);
			}
		}

		public static RuleResult fromXml(Node node) {
			RuleResult rule = null;
			List<String> longDescriptions = getNodeValues("./longDescription", node);
			String longDescription = null;
			if (longDescriptions != null && longDescriptions.size() == 1) {
				longDescription = longDescriptions.get(0);
			}
			NodeList items = getNodeSet("./itemset", node);
			ItemSet itemset = null;
			if (items != null && items.getLength() == 1) {
				itemset = ItemSet.fromXml(items.item(0));
			}
			rule = new RuleResult(getNodeValues("./id", node).get(0), getNodeValues("./severity", node).get(0),
					getNodeValues("./score", node).get(0), getNodeValues("./shortDescription", node).get(0),
					longDescription, itemset);
			return rule;
		}
	}

	private static class ItemSet {
		private List<String> fields;
		private List<ItemList> items;

		public ItemSet() {
			fields = new ArrayList<>();
			items = new ArrayList<>();
		}

		private ItemSet(List<String> fields, List<ItemList> items) {
			this.fields = fields;
			this.items = items;
		}

		public void addField(String field) {
			fields.add(field);
		}

		public void addItem(ItemList itemList) {
			items.add(itemList);
		}

		@Override
		public String toString() {
			return "Fields: " + fields + "\n      Items: " + items;
		}

		public boolean compareAndLog(Object other) {
			ItemSet otherItemSet = (ItemSet) other;
			boolean fieldEquality = fields.equals(otherItemSet.fields);
			if (!fieldEquality) {
				DetailsTracker.log("Item fields differ: " + fields + " was not equal to " + otherItemSet.fields + ". ");
			}
			boolean itemEquality = items.equals(otherItemSet.items);
			return itemEquality && fieldEquality;
		}

		public void toXml(Element parent) {
			Element itemSetNode = parent.getOwnerDocument().createElement("itemset");
			parent.appendChild(itemSetNode);
			Element fieldsNode = parent.getOwnerDocument().createElement("fields");
			itemSetNode.appendChild(fieldsNode);
			for (String field : fields) {
				Element fieldNode = parent.getOwnerDocument().createElement("field");
				fieldsNode.appendChild(fieldNode);
				fieldNode.appendChild(createValueNode(parent.getOwnerDocument(), "name", field));
			}
			Element itemsNode = parent.getOwnerDocument().createElement("items");
			itemSetNode.appendChild(itemsNode);
			for (ItemList list : items) {
				list.toXml(itemsNode);
			}
		}

		public static ItemSet fromXml(Node node) {
			ItemSet set = null;
			List<ItemList> itemList = new ArrayList<>();
			NodeList items = getNodeSet("./items/item", node);
			for (int i = 0; i < items.getLength(); i++) {
				Node thisItemOnly = items.item(i);
				thisItemOnly.getParentNode().removeChild(thisItemOnly);
				itemList.add(ItemList.fromXml(thisItemOnly));
			}
			List<String> fields = getNodeValues("./fields/field/name", node);
			set = new ItemSet(fields, itemList);
			return set;
		}

	}

	private static class ItemList {
		private List<String> items;

		public ItemList() {
			items = new ArrayList<>();
		}

		private ItemList(List<String> list) {
			items = list;
		}

		public void add(String item) {
			items.add(item);
		}

		@Override
		public String toString() {
			return items.toString();
		}

		@Override
		public boolean equals(Object other) {
			ItemList otherItemList = (ItemList) other;
			boolean equals = items.equals(otherItemList.items);
			if (!equals) {
				DetailsTracker.log("Item lists differ: " + items + " was not equal to " + otherItemList.items + ". ");
			}
			return equals;
		}

		public void toXml(Element parent) {
			Element itemNode = parent.getOwnerDocument().createElement("item");
			parent.appendChild(itemNode);
			for (String item : items) {
				itemNode.appendChild(createValueNode(parent.getOwnerDocument(), "value", item));
			}
		}

		public static ItemList fromXml(Node node) {
			return new ItemList(getNodeValues("./value", node));
		}
	}

	// FIXME: This class is not thread safe. Make non-static!
	private static class DetailsTracker {
		private static Deque<String> entries = new ArrayDeque<>();

		private DetailsTracker() {
		}

		public static void log(String entry) {
			entries.addFirst(entry);
		}

		public static String getEntries() {
			StringBuilder sb = new StringBuilder();
			for (String entry : entries) {
				sb.append(entry);
			}
			return sb.toString();
		}

		public static void clear() {
			entries.clear();
		}
	}

}
