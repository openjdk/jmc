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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.util.XmlToolkit;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public final class XMLModel extends Observable {
	final private static class XMLModelBuilder extends DefaultHandler {
		private final Stack<XMLTagInstance> m_stack = new Stack<>();
		private StringBuilder characterBuilder;

		public XMLModelBuilder(XMLTagInstance dummyRoot) {
			m_stack.push(dummyRoot);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			XMLTagInstance current = m_stack.peek();
			XMLTagInstance child = current.create(qName, createMap(attributes));
			m_stack.push(child);
			characterBuilder = new StringBuilder();
		}

		@Override
		public void characters(char ch[], int start, int length) throws SAXException {
			characterBuilder.append(ch, start, length);
		}

		@Override
		public void endElement(String uri, String localName, String qName) {
			String content = characterBuilder.toString().trim();
			if (content.length() != 0) {
				if (m_stack.peek().hasContent()) {
					m_stack.peek().setContent(content);
				}
			}

			XMLTagInstance current = m_stack.peek();
			if (current.getTag().getName().equalsIgnoreCase(qName)) {
				m_stack.pop();
			} else {
				throw new IllegalStateException("Unexpected <" + qName + "/>"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		private Map<String, String> createMap(Attributes attributes) {
			LinkedHashMap<String, String> map = new LinkedHashMap<>();
			for (int i = 0; i < attributes.getLength(); i++) {
				map.put(attributes.getQName(i), attributes.getValue(i).trim());
			}
			return map;
		}
	}

	private final XMLTagInstance m_root;
	private final Map<Object, XMLValidationResult> m_resultLookup = new LinkedHashMap<>();
	private final IXMLValidator m_validator;
	private boolean m_dirty;

	/**
	 * Create a new XML model.
	 *
	 * @param root
	 *            the root element
	 * @param validator
	 *            a validator or null
	 */
	XMLModel(XMLTagInstance root, IXMLValidator validator) {
		m_root = root;
		m_validator = validator;
		checkErrors();
	}

	public static XMLModel createEmpty(IXMLValidator validator, Map<String, String> attributes) {
		XMLTagInstance dummyRoot = new XMLTagInstance(null, validator.getRootElementType());
		XMLTagInstance container = dummyRoot.create(dummyRoot.getTag().getTags().get(0).getName(), attributes);
		// Attempt to pass validation by setting all required attributes to their implicit defaults.
		for (XMLAttributeInstance attr : container.getAttributeInstances()) {
			if (attr.getAttribute().isRequired()) {
				attr.setValue(attr.getValue());
			}
		}
		return new XMLModel(container, validator);
	}

	public static XMLModel create(InputSource input, IXMLValidator validator) throws IOException, ParseException {
		try {
			XMLTagInstance dummyRoot = new XMLTagInstance(null, validator.getRootElementType());

			SAXParserFactory spf = XmlToolkit.createSAXParserFactory();

			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			XMLModelBuilder dataHandler = new XMLModelBuilder(dummyRoot);
			xr.setContentHandler(dataHandler);
			xr.parse(input);
			List<XMLTagInstance> instances = dummyRoot.getTagsInstances();
			if (instances.size() != 1) {
				throw new ParseException("There must be exactly one root element", 1); //$NON-NLS-1$
			}
			return new XMLModel(instances.get(0), validator);
		} catch (SAXParseException sp) {
			ParseException pe = new ParseException(sp.getMessage(), sp.getLineNumber());
			pe.initCause(sp);
			throw pe;
		} catch (SAXException s) {
			ParseException pe = new ParseException("Could not parse XML", -1); //$NON-NLS-1$
			pe.initCause(s);
			throw pe;
		} catch (ParserConfigurationException s) {
			ParseException pe = new ParseException("Could not parse XML", -1); //$NON-NLS-1$
			pe.initCause(s);
			throw pe;
		}

	}

	public static void validate(InputStream xmlStream, String streamName, Schema schema)
			throws ParseException, IOException {
		try {
			validateAgainstSchema(xmlStream, schema);
		} catch (SAXParseException spe) {
			throw new ParseException(spe.getMessage(), spe.getLineNumber());
		} catch (SAXException e) {
			throw new IOException("Could not validate " + streamName, e); //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
			throw new IOException("Could not validate " + streamName, e); //$NON-NLS-1$
		} catch (IOException e) {
			throw new IOException("Could not validate " + streamName, e); //$NON-NLS-1$
		}
	}

	private static void validateAgainstSchema(InputStream xmlStream, Schema schema)
			throws SAXException, ParserConfigurationException, IOException, ParseException {
		class SimpleErrorHandler implements ErrorHandler {
			private final List<SAXParseException> exceptions = new ArrayList<>();

			@Override
			public void warning(SAXParseException se) throws SAXException {
				exceptions.add(se);
			}

			@Override
			public void error(SAXParseException se) throws SAXException {
				exceptions.add(se);
			}

			@Override
			public void fatalError(SAXParseException se) throws SAXException {
				exceptions.add(se);
			}
		}

		try {
			SAXParserFactory factory = XmlToolkit.createSAXParserFactory();
			factory.setNamespaceAware(true);
			factory.setSchema(schema);

			SimpleErrorHandler seh = new SimpleErrorHandler();

			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			reader.setErrorHandler(seh);
			reader.parse(new InputSource(xmlStream));
			if (!seh.exceptions.isEmpty()) {
				throwParseException(seh.exceptions);
			}
		} finally {
			IOToolkit.closeSilently(xmlStream);
		}
	}

	private static void throwParseException(List<SAXParseException> exceptions) throws ParseException {
		StringBuilder sb = new StringBuilder();
		int firstError = -1;
		for (SAXParseException spe : exceptions) {
			if (firstError == -1) {
				firstError = spe.getLineNumber();
			}
			sb.append("Line " + spe.getLineNumber() + " " + spe.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("\r\n"); //$NON-NLS-1$
			sb.append("\r\n"); //$NON-NLS-1$
		}
		throw new ParseException(sb.toString(), firstError);
	}

	/**
	 * Saves the model to the given {@link File}. If successful, sets dirtyness to false, as
	 * returned by {@link #isModified()}.
	 *
	 * @param writer
	 * @param oneLineElements
	 *            XML tags to output on a single line
	 */
	public void saveToFile(File file) throws IOException {
		// NOTE: The pretty printer writes that the encoding is UTF-8, so we must make sure it is.
		// Ensure charset exists before opening file for writing.
		Charset charset = Charset.forName("UTF-8"); //$NON-NLS-1$
		Writer osw = new OutputStreamWriter(new FileOutputStream(file), charset);
		if (writeTo(osw)) {
			setDirty(false);
		}
	}

	/**
	 * Writes the model to the given {@link Writer}. Does not change dirtyness, as returned by
	 * {@link #isModified()}.
	 *
	 * @param writer
	 * @param oneLineElements
	 *            XML tags to output on a single line
	 * @return true iff the model was successfully written to the {@link Writer}.
	 */
	public boolean writeTo(Writer writer) {
		PrintWriter pw = new PrintWriter(writer);
		try {
			PrettyPrinter pp = new PrettyPrinter(pw, m_validator.getElementsTooKeepOnOneLine());
			pp.print(this);
			pw.flush();
			// PrintWriter never throws any exceptions, so this is how we find out if something went wrong.
			return !pw.checkError();
		} finally {
			IOToolkit.closeSilently(pw);
		}
	}

	public XMLTagInstance getRoot() {
		return m_root;
	}

	public void markDirty() {
		/*
		 * FIXME: Mixing up "dirty" as in not-saved-to-file, with notification of
		 * in-memory-model-change? Or is it that the observable state is the dirtyness? Still, only
		 * the transition from non-dirty to dirty is reported. And only if this method is used. This
		 * can be called if the underlying file has changed to some other reason, and result in the
		 * JFCEditor being marked dirty, when it shouldn't.
		 */
		if (!m_dirty) {
			m_dirty = true;
			setChanged();
			notifyObservers();
		}
	}

	public void setDirty(boolean dirty) {
		m_dirty = dirty;
	}

	public boolean isModified() {
		return m_dirty;
	}

	public void checkErrors() {
		m_resultLookup.clear();
		if (m_validator != null) {
			for (XMLValidationResult r : m_validator.validate(this)) {
				// NOTE: This will only keep one result per node, although many may have been found.
				m_resultLookup.put(r.getObject(), r);
				if (r.isError()) {
					// FIXME: Get a logger when this is in a better bundle.
					System.out.println(r.getObject() + ": " + r.getText()); //$NON-NLS-1$
				}
			}
		}
	}

	public Collection<XMLValidationResult> getResults() {
		return m_resultLookup.values();
	}

	public XMLValidationResult getResult(Object o) {
		return m_resultLookup.get(o);
	}

	public boolean hasErrors() {
		for (XMLValidationResult r : m_resultLookup.values()) {
			if (r.isError()) {
				return true;
			}
		}
		return false;
	}

	public XMLModel deepClone() {
		StringWriter sw = new StringWriter(2000); // Below 2048 to keep initial char array within 4kB, next within 8kB,
													// and so on.
		if (writeTo(sw)) {
			try {
				return XMLModel.create(new InputSource(new StringReader(sw.toString())), m_validator);
			} catch (Exception e) {
				// Shouldn't happen
				throw new IllegalStateException(e);
			}
		}
		throw new IllegalStateException("Could not write model to string"); //$NON-NLS-1$
	}

	@Override
	public String toString() {
		StringWriter sw = new StringWriter(2000); // Below 2048 to keep initial char array within 4kB, next within 8kB,
													// and so on.
		writeTo(sw);
		return sw.toString();
	}
}
