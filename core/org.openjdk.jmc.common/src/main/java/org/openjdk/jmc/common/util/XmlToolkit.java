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
package org.openjdk.jmc.common.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openjdk.jmc.common.io.IOToolkit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * Class containing helper methods for XML processing.
 */
public final class XmlToolkit {
	private static final String XML_PARSER_DISALLOW_DOCTYPE_ATTRIBUTE = "http://apache.org/xml/features/disallow-doctype-decl"; //$NON-NLS-1$
	private static final Logger LOGGER = Logger.getLogger("org.openjdk.jmc.common.xml"); //$NON-NLS-1$
	private static final Pattern AMP = Pattern.compile("&"); //$NON-NLS-1$
	private static final Pattern LT = Pattern.compile("<"); //$NON-NLS-1$
	private static final Pattern GT = Pattern.compile(">"); //$NON-NLS-1$
	private static final Pattern QUOT = Pattern.compile("\""); //$NON-NLS-1$
	private static final Pattern APOS = Pattern.compile("\'"); //$NON-NLS-1$
	private static final Pattern CR = Pattern.compile("\r"); //$NON-NLS-1$
	private static final Pattern NL = Pattern.compile("\n"); //$NON-NLS-1$
	private static final Pattern TAB = Pattern.compile("\u0009"); //$NON-NLS-1$

	/**
	 * The constructor is private since no instances should ever be created.
	 */
	private XmlToolkit() {
		// Not creating instances.
	}

	/**
	 * Returns the child element with the specified tag name of the specified parent node. If no
	 * such child element is found, a new element with the specified tag name is created and
	 * returned.
	 *
	 * @param parentNode
	 *            parent node for the wanted element
	 * @param tagName
	 *            name of the wanted element
	 * @return the child element
	 * @see #getChildElementOrNull(Element, String)
	 */
	public static Element getOrCreateElement(Element parentNode, String tagName) {
		NodeList list = parentNode.getElementsByTagName(tagName);
		if (list.getLength() == 0) {
			Element newElement = parentNode.getOwnerDocument().createElement(tagName);
			parentNode.appendChild(newElement);
			return newElement;
		}
		return (Element) list.item(0);
	}

	/**
	 * Adds a child element with the name tagName to the parent and returns the new child element.
	 *
	 * @param parentNode
	 *            parent node to add the new element to
	 * @param tagName
	 *            the name of the new child element
	 * @return the new child element
	 */
	public static Element createElement(Element parentNode, String tagName) {
		Element newElement = parentNode.getOwnerDocument().createElement(tagName);
		parentNode.appendChild(newElement);
		return newElement;
	}

	/**
	 * Sets the value of a a "setting" element. If it already exists it will be updated. If it does
	 * not exist it will be created. If the setting element already exists, then there must not be
	 * any child elements to it other than a text value.
	 *
	 * @param parentNode
	 *            parent node of the setting element
	 * @param settingName
	 *            tag name of the setting element
	 * @param settingValue
	 *            the value to set
	 */
	public static void setSetting(Element parentNode, String settingName, String settingValue) {
		Element settingElement = getOrCreateElement(parentNode, settingName);
		String sValue = (settingValue != null ? settingValue : ""); //$NON-NLS-1$

		if (settingElement == null) {
			settingElement = parentNode.getOwnerDocument().createElement(settingName);
			parentNode.appendChild(settingElement);
		}

		setStringValue(settingElement, sValue);
	}

	/**
	 * Creates a new empty XML document.
	 *
	 * @param rootElementName
	 *            the name of the root element
	 * @return an empty document
	 * @throws IOException
	 *             if there is a problem creating the XML document
	 */
	public static Document createNewDocument(String rootElementName) throws IOException {
		Document doc = null;
		try {
			// Ensure the encoding is UTF-8 (capable of representing all unicode chars)
			InputSource xml = new InputSource(
					new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?><" + rootElementName + "/>")); //$NON-NLS-1$ //$NON-NLS-2$
			DocumentBuilderFactory dbf = createDocumentBuildFactoryInstance();

			doc = dbf.newDocumentBuilder().parse(xml);
		} catch (IOException e) {
			// just rethrow
			throw e;
		} catch (SAXException e) {
			LOGGER.log(Level.WARNING, "Error in creating new XML document", e); //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
			LOGGER.log(Level.WARNING, "Error in creating new XML document", e); //$NON-NLS-1$
		} catch (FactoryConfigurationError e) {
			LOGGER.log(Level.WARNING, "Error in creating new XML document", e); //$NON-NLS-1$
		}
		return doc;
	}

	/**
	 * Create a SAX parser factory with safe settings.
	 * <p>
	 * See <a href=
	 * "https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#Java">OWASP
	 * XXE cheat sheet</a>.
	 * 
	 * @return a new SAX parser factory
	 */
	public static SAXParserFactory createSAXParserFactory()
			throws SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setFeature(XML_PARSER_DISALLOW_DOCTYPE_ATTRIBUTE, true);
		return factory;
	}

	/**
	 * Create a document builder factory with safe settings.
	 * <p>
	 * See <a href=
	 * "https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#Java">OWASP
	 * XXE cheat sheet</a>.
	 * 
	 * @return a new document builder factory
	 */
	public static DocumentBuilderFactory createDocumentBuildFactoryInstance() throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setFeature(XML_PARSER_DISALLOW_DOCTYPE_ATTRIBUTE, true);
		return dbf;
	}

	/**
	 * Create a transformer factory with safe settings.
	 * <p>
	 * See <a href=
	 * "https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#Java">OWASP
	 * XXE cheat sheet</a>.
	 * 
	 * @return a new transformer factory
	 */
	public static TransformerFactory createTransformerFactory()
			throws TransformerFactoryConfigurationError, TransformerConfigurationException {
		TransformerFactory factory = TransformerFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		// Recommended by Fortify, should already be included in FEATURE_SECURE_PROCESSING but let's make it explicit
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); //$NON-NLS-1$
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, ""); //$NON-NLS-1$
		return factory;
	}

	/**
	 * Sets the text value as a text node child of a an element. The element must not have any other
	 * child elements.
	 *
	 * @param element
	 *            the element to set the text value for
	 * @param value
	 *            the new value to set
	 */
	public static void setStringValue(Element element, String value) {
		if (element == null) {
			return;
		}
		if (element.getFirstChild() == null) {
			element.appendChild(element.getOwnerDocument().createTextNode(value));
		} else {
			element.getFirstChild().setNodeValue(value);
		}
	}

	/**
	 * Returns the value of the setting with the specified name or a default value if the setting
	 * had no value.
	 * <p>
	 * Since everything should have a default value, no other version of get setting exists. This
	 * method implicitly builds the setting node with a default value if none is found.
	 *
	 * @param parent
	 *            the parent element whose children to search for the settings node.
	 * @param settingName
	 *            name of the setting
	 * @param defaultValue
	 *            default value to return if setting is empty
	 * @return see above.
	 */
	public static String getSetting(Element parent, String settingName, String defaultValue) {
		Element settingsNode = getOrCreateElement(parent, settingName);
		String value = getStringValue(settingsNode);
		if (value == null) {
			setSetting(parent, settingName, defaultValue);
			value = defaultValue;
		}
		return value;
	}

	/**
	 * Returns the content between the tags of the element, for example &lt;tag&gt;hello
	 * &lt;/tag&gt; where the value is "hello". If the element itself or its child is null, null
	 * will be returned. This method will only return a non-null String if the child node of the
	 * element is a text node.
	 *
	 * @param element
	 *            the element from which to extract the text node.
	 * @return the String value of the text node.
	 */
	public static String getStringValue(Element element) {
		if (element == null) {
			return null;
		}

		Node n = element.getFirstChild();

		if ((n == null) || (n.getNodeType() != Node.TEXT_NODE)) {
			return null;
		}

		return element.getFirstChild().getNodeValue();
	}

	/**
	 * Pretty prints an XML document to a string, starting from the specified element.
	 *
	 * @param node
	 *            node from which to start pretty printing
	 * @return a string containing the pretty printed document
	 */
	public static String prettyPrint(Element node) {
		StringWriter wrt = new StringWriter();
		prettyPrint(node, wrt);
		return wrt.toString();
	}

	/**
	 * Pretty prints an XML document to a writer, starting from the specified element.
	 *
	 * @param node
	 *            node from which to start pretty printing
	 * @param wrt
	 *            writer to write the document to
	 */
	public static void prettyPrint(Element node, Writer wrt) {
		try {
			TransformerFactory factory = createTransformerFactory();
			try {
				factory.setAttribute("indent-number", "4"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (IllegalArgumentException iae) {
				LOGGER.info("Could not set indent-number attribute on the transformer factory."); //$NON-NLS-1$
			}

			Transformer passThrough = factory.newTransformer();
			passThrough.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$

			passThrough.transform(new DOMSource(node), new StreamResult(wrt));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Couldn't serialize the document to string!", e); //$NON-NLS-1$
		}
	}

	/**
	 * Returns all the children from a node with a tag matching the tag argument.
	 *
	 * @param contextNode
	 *            node whose children to search
	 * @param tag
	 *            the tag to search for
	 * @return A list of elements with the found nodes. Will return an empty list if no matching
	 *         element could be found.
	 */

	public static List<Element> getChildElementsByTag(Node contextNode, String tag) {
		List<Element> resultList = new LinkedList<>();
		NodeList fullList = contextNode.getChildNodes();
		for (int i = 0; i < fullList.getLength(); i++) {
			Node n = fullList.item(i);
			if (fullList.item(i).getNodeName().equals(tag) && (n instanceof Element)) {
				resultList.add((Element) n);
			}
		}
		return resultList;
	}

	/**
	 * Returns a document builder for XML documents.
	 *
	 * @return a new document builder
	 */
	private static DocumentBuilder getDocumentBuilder() {
		DocumentBuilder docBuilder = null;
		try {
			DocumentBuilderFactory factory = createDocumentBuildFactoryInstance();
			docBuilder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// This shouldn't happen since all configuration is done within XmlToolkit
			LOGGER.log(Level.WARNING, "Parser configuration error", e); //$NON-NLS-1$
		}
		return docBuilder;
	}

	/**
	 * Loads an XML document from the specified file.
	 *
	 * @param file
	 *            the file from which to read the document
	 * @return the parsed XML document
	 * @throws SAXException
	 *             if the document could not be parsed
	 * @throws IOException
	 *             if the stream could not be read
	 */
	public static Document loadDocumentFromFile(File file) throws SAXException, IOException {
		FileInputStream is = null;
		try {
			is = new FileInputStream(file);
			return XmlToolkit.loadDocumentFromStream(new BufferedInputStream(is));
		} finally {
			if (is != null) {
				IOToolkit.closeSilently(is);
			}
		}
	}

	/**
	 * Loads an XML document from the specified stream.
	 *
	 * @param stream
	 *            the input stream from which to read the document
	 * @return the parsed XML document
	 * @throws SAXException
	 *             if the document could not be parsed
	 * @throws IOException
	 *             if the stream could not be read
	 */
	public static Document loadDocumentFromStream(InputStream stream) throws SAXException, IOException {
		return getDocumentBuilder().parse(stream);
	}

	/**
	 * Loads an XML document from its string representation.
	 *
	 * @param doc
	 *            the string to read from
	 * @return the parsed XML document
	 * @throws SAXException
	 *             if the document could not be parsed
	 * @throws NullPointerException
	 *             if the input string is null
	 */
	public static Document loadDocumentFromString(String doc) throws SAXException {
		try {
			if (doc == null) {
				throw new NullPointerException();
			}
			return getDocumentBuilder().parse(new InputSource(new StringReader(doc)));
		} catch (IOException e) {
			// Should not happen - reading from a String.
			throw new RuntimeException(e);
		}
	}

	/**
	 * Stores an XML document in a file.
	 *
	 * @param doc
	 *            the XML document to store
	 * @param file
	 *            the file to store it in
	 * @throws IOException
	 *             if the file could not written
	 */
	public static void storeDocumentToFile(Document doc, File file) throws IOException {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file, "UTF-8"); //$NON-NLS-1$
			prettyPrint(doc.getDocumentElement(), pw);
		} finally {
			if (pw != null) {
				IOToolkit.closeSilently(pw);
			}
		}
	}

	/**
	 * Stores an XML document as a string.
	 *
	 * @param doc
	 *            the XML document to store
	 * @return the XML document as a string
	 */
	public static String storeDocumentToString(Document doc) {
		return prettyPrint(doc.getDocumentElement());
	}

	/**
	 * Returns the child element with the specified tag name of the specified parent node. If no
	 * such child element is found, {@code null} is returned.
	 *
	 * @param parent
	 *            parent node for the wanted element
	 * @param name
	 *            name of the wanted element
	 * @return the child element, or {@code null} if no such element exists
	 * @see #getOrCreateElement(Element, String)
	 */
	public static Element getChildElementOrNull(Element parent, String name) {
		List<Element> nodes = XmlToolkit.getChildElementsByTag(parent, name);
		if (nodes.isEmpty()) {
			return null;
		} else {
			return nodes.get(0);
		}
	}

	// FIXME: Replace usage with OWASP encoder
	public static String escapeAll(String s) {
		s = escapeTagContent(s);
		s = QUOT.matcher(s).replaceAll("&quot;"); //$NON-NLS-1$
		s = APOS.matcher(s).replaceAll("&apos;"); //$NON-NLS-1$
		s = CR.matcher(s).replaceAll("&#x0D;"); //$NON-NLS-1$
		s = NL.matcher(s).replaceAll("&#x0A;"); //$NON-NLS-1$
		return TAB.matcher(s).replaceAll("&#x09;"); //$NON-NLS-1$
	}

	// FIXME: Replace usage with OWASP encoder
	public static String escapeTagContent(String s) {
		s = AMP.matcher(s).replaceAll("&amp;"); //$NON-NLS-1$
		s = LT.matcher(s).replaceAll("&lt;"); //$NON-NLS-1$
		return GT.matcher(s).replaceAll("&gt;"); //$NON-NLS-1$
	}

}
