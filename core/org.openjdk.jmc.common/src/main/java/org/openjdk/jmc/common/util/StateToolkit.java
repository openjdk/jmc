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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Deque;
import java.util.LinkedList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IStateful;
import org.openjdk.jmc.common.IWritableState;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Toolkit for working with the state interfaces {@link IState}, {@link IStateful}, and
 * {@link IWritableState}.
 */
public class StateToolkit {

	/**
	 * An SAX event handler that saves elements to a {@link IWritableState writable state}.
	 */
	private static class StateHandler extends DefaultHandler {

		private final IWritableState documentElement;
		private final Deque<IWritableState> stateStack = new LinkedList<>();

		public StateHandler(IWritableState state) {
			documentElement = state;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			IWritableState state = stateStack.isEmpty() ? documentElement : stateStack.peek().createChild(qName);
			for (int i = 0; i < attributes.getLength(); i++) {
				state.putString(attributes.getQName(i), attributes.getValue(i));
			}
			stateStack.push(state);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			stateStack.pop();
		}

	}

	/**
	 * Read an XML document from a reader and write its structure to a {@link IWritableState
	 * writable state}.
	 *
	 * @param reader
	 *            reader to read XML from
	 * @param state
	 *            writable state to write data to
	 */
	public static void saveXMLDocumentTo(Reader reader, IWritableState state)
			throws SAXException, IOException, ParserConfigurationException {
		SAXParser parser = XmlToolkit.createSAXParserFactory().newSAXParser();
		parser.parse(new InputSource(reader), new StateHandler(state));
	}

	/**
	 * Read an XML document from a string and return its structure as a {@link IState state}.
	 *
	 * @param document
	 *            string to read XML from
	 * @return state reflecting the XML document
	 */
	public static IState fromXMLString(String document) throws SAXException {
		return new StateElement(XmlToolkit.loadDocumentFromString(document).getDocumentElement());
	}

	/**
	 * Read an XML document from a string and return its structure as a {@link IStateful stateful
	 * object}.
	 *
	 * @param document
	 *            string to read XML from
	 * @return stateful object reflecting the XML document
	 */
	public static IStateful statefulFromXMLString(final String document) {
		return new IStateful() {
			@Override
			public void saveTo(IWritableState state) {
				try {
					StateToolkit.saveXMLDocumentTo(new StringReader(document), state);
				} catch (Exception e) {
					throw new RuntimeException("Could not read state from XML string", e); //$NON-NLS-1$
				}
			}
		};
	}

	/**
	 * Read an XML document from a file and return its structure as a {@link IStateful stateful
	 * object}.
	 *
	 * @param document
	 *            file to read XML from
	 * @return stateful object reflecting the XML document
	 */
	public static IStateful statefulFromXMLFile(final File document, final Charset charset) {
		return new IStateful() {
			@Override
			public void saveTo(IWritableState state) {
				try (FileInputStream fis = new FileInputStream(document)) {
					StateToolkit.saveXMLDocumentTo(new InputStreamReader(fis, charset), state);
				} catch (Exception e) {
					throw new RuntimeException("Could not read state from XML string", e); //$NON-NLS-1$
				}
			}
		};
	}

	/**
	 * Read an XML document from a file and return its structure as a {@link IState state}.
	 *
	 * @param document
	 *            file to read XML from
	 * @return state reflecting the XML document
	 */
	public static IState fromXMLFile(File document) throws SAXException, IOException {
		return new StateElement(XmlToolkit.loadDocumentFromFile(document).getDocumentElement());
	}

	/**
	 * Create a new writable state.
	 *
	 * @param rootName
	 *            root name of the writable state
	 * @return a new writable state
	 */
	public static IWritableState createWriter(String rootName) throws IOException {
		return new StateElementWriter(rootName);
	}

	/**
	 * Write a stateful state as an XML string.
	 *
	 * @param state
	 *            state to write as XML
	 * @return an XML string reflecting the state
	 */
	public static String toXMLString(IStateful state) {
		try {
			StringWriter wrt = new StringWriter();
			writeAsXml(state, wrt);
			return wrt.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Write a stateful state to a reader as an XML string.
	 *
	 * @param state
	 *            state to write as XML
	 * @param writer
	 *            write to write the XML to
	 */
	public static void writeAsXml(IStateful state, Writer writer) throws IOException {
		StateElementWriter stateWriter = new StateElementWriter("state"); //$NON-NLS-1$
		state.saveTo(stateWriter);
		stateWriter.write(writer);
	}

	/**
	 * Write a boolean value to a state.
	 *
	 * @param state
	 *            state to write to
	 * @param attribute
	 *            attribute name
	 * @param value
	 *            attribute value
	 * @see #readBoolean(IState, String, Boolean)
	 */
	public static void writeBoolean(IWritableState state, String attribute, Boolean value) {
		if (value != null) {
			state.putString(attribute, Boolean.toString(value));
		}
	}

	/**
	 * Read a boolean value from a state.
	 *
	 * @param state
	 *            state to read from
	 * @param attribute
	 *            attribute name
	 * @param defaultValue
	 *            default attribute value
	 * @return attribute value if it is set in the state, {@code defaultValue} if not
	 * @see #writeBoolean(IWritableState, String, Boolean)
	 */
	public static Boolean readBoolean(IState state, String attribute, Boolean defaultValue) {
		if (state == null) {
			return defaultValue;
		} else {
			String v = state.getAttribute(attribute);
			// Boolean.valueOf("anything") == false, so we don't use it
			if (v == null) {
				return defaultValue;
			} else if ("true".equalsIgnoreCase(v)) { //$NON-NLS-1$
				return true;
			} else if ("false".equalsIgnoreCase(v)) { //$NON-NLS-1$
				return false;
			} else {
				return defaultValue;
			}
		}
	}

	/**
	 * Write a float value to a state.
	 *
	 * @param state
	 *            state to write to
	 * @param attribute
	 *            attribute name
	 * @param value
	 *            attribute value
	 * @see #readFloat(IState, String, Float)
	 */
	public static void writeFloat(IWritableState state, String attribute, Float value) {
		if (value != null) {
			state.putString(attribute, Float.toString(value));
		}
	}

	/**
	 * Read a float value from a state.
	 *
	 * @param state
	 *            state to read from
	 * @param attribute
	 *            attribute name
	 * @param defaultValue
	 *            default attribute value
	 * @return attribute value if it is set in the state, {@code defaultValue} if not
	 * @see #writeFloat(IWritableState, String, Float)
	 */
	public static Float readFloat(IState state, String attribute, Float defaultValue) {
		if (state == null) {
			return defaultValue;
		} else {
			String v = state.getAttribute(attribute);
			if (v != null) {
				try {
					return Float.parseFloat(v);
				} catch (NumberFormatException e) {
				}
			}
			return defaultValue;
		}
	}

	/**
	 * Write an integer value to a state.
	 *
	 * @param state
	 *            state to write to
	 * @param attribute
	 *            attribute name
	 * @param value
	 *            attribute value
	 * @see #readInt(IState, String, Integer)
	 */
	public static void writeInt(IWritableState state, String attribute, Integer value) {
		if (value != null) {
			state.putString(attribute, Integer.toString(value));
		}
	}

	/**
	 * Read an integer value from a state.
	 *
	 * @param state
	 *            state to read from
	 * @param attribute
	 *            attribute name
	 * @param defaultValue
	 *            default attribute value
	 * @return attribute value if it is set in the state, {@code defaultValue} if not
	 * @see #writeInt(IWritableState, String, Integer)
	 */
	public static Integer readInt(IState state, String attribute, Integer defaultValue) {
		if (state == null) {
			return defaultValue;
		} else {
			String v = state.getAttribute(attribute);
			if (v != null) {
				try {
					return Integer.parseInt(v);
				} catch (NumberFormatException e) {
				}
			}
			return defaultValue;
		}
	}

	/**
	 * Write an enum value to a state.
	 *
	 * @param state
	 *            state to write to
	 * @param attribute
	 *            attribute name
	 * @param value
	 *            attribute value
	 * @see #readEnum(IState, String, Enum, Class)
	 */
	public static <T extends Enum<T>> void writeEnum(IWritableState state, String attribute, T value) {
		if (value != null) {
			state.putString(attribute, value.name());
		}
	}

	/**
	 * Read an enum value from a state.
	 *
	 * @param state
	 *            state to read from
	 * @param attribute
	 *            attribute name
	 * @param defaultValue
	 *            default attribute value
	 * @param klass
	 *            enum class of the attribute value
	 * @return attribute value if it is set in the state, {@code defaultValue} if not
	 * @see #writeEnum(IWritableState, String, Enum)
	 */
	public static <T extends Enum<T>> T readEnum(IState state, String attribute, T defaultValue, Class<T> klass) {
		if (state == null) {
			return defaultValue;
		} else {
			String v = state.getAttribute(attribute);
			try {
				return v == null ? defaultValue : Enum.valueOf(klass, v);
			} catch (IllegalArgumentException e) {
				return defaultValue;
			}
		}
	}

}
