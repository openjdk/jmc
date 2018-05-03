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
package org.openjdk.jmc.flightrecorder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.parser.IParserExtension;
import org.openjdk.jmc.flightrecorder.parser.ParserExtensionRegistry;
import org.openjdk.jmc.flightrecorder.parser.filter.FilterExtension;
import org.openjdk.jmc.flightrecorder.parser.filter.IOnLoadFilter;
import org.openjdk.jmc.flightrecorder.parser.filter.OnLoadFilters;

/**
 * Prints a flight recording to a {@link PrintWriter}.
 */
public final class RecordingPrinter {

	/**
	 * Verbosity level to use when printing events.
	 */
	public enum Verbosity {
		/**
		 * Writes maximum amount of information
		 */
		HIGH,

		/**
		 * Write event and event vales, but not detailed information
		 */
		MEDIUM,

		/**
		 * Only write one line per event
		 */
		LOW,
	}

	private final Verbosity verbosity;
	private final boolean formatValues;
	private final IOnLoadFilter recordingFilter;
	private final PrintWriter out;

	public RecordingPrinter(PrintWriter output, Verbosity verbosity, boolean formatValues,
			IOnLoadFilter recordingFilter) {
		out = output;
		this.verbosity = verbosity;
		this.formatValues = formatValues;
		this.recordingFilter = recordingFilter;
	}

	public RecordingPrinter(PrintWriter output, Verbosity verbosity, boolean formatValues) {
		this(output, verbosity, formatValues, null);
	}

	/**
	 * Main method that will print a recording Usage:
	 *
	 * <pre>
	 * java org.openjdk.jmc.flightrecorder [-formatValues] [-brief] fileName
	 *
	 * - formatValues will format values in a suitable unit (may loose precision)
	 *
	 * - brief will print only parts of all event values.
	 * </pre>
	 */
	public static void main(String[] args) throws IOException, InterruptedException, CouldNotLoadRecordingException {
		if (args.length > 0) {
			try {
				RecordingPrinter printer = buildFromOptions(new PrintWriter(System.out), args);
				File file = new File(args[args.length - 1]);
				IItemCollection events;
				if (printer.recordingFilter != null) {
					List<IParserExtension> extensions = new ArrayList<>(ParserExtensionRegistry.getParserExtensions());
					extensions.add(new FilterExtension(printer.recordingFilter));
					events = JfrLoaderToolkit.loadEvents(Arrays.asList(file), extensions);
				} else {
					events = JfrLoaderToolkit.loadEvents(Arrays.asList(file));
				}
				printer.print(events);
				return;
			} catch (ParseException pe) {
				// fall through
			}
		}
		printHelp();
	}

	public void print(IItemCollection events) {
		out.println("<?xml version=\"1.0\"?>"); //$NON-NLS-1$
		Iterator<IItemIterable> itemIterable = events.iterator();
		while (itemIterable.hasNext()) {
			Iterator<IItem> itemIterator = itemIterable.next().iterator();
			while (itemIterator.hasNext()) {
				printEvent(itemIterator.next());
				out.println();
			}
		}
		out.flush();
	}

	private static RecordingPrinter buildFromOptions(PrintWriter output, String[] args) throws ParseException {
		Verbosity verbosity = Verbosity.HIGH;
		IOnLoadFilter recordingFilter = null;
		boolean formatValues = false;
		for (int n = 0; n < args.length - 1; n++) {
			if (args[n].equals("-formatValues")) { //$NON-NLS-1$
				formatValues = true;
			} else if (args[n].equals("-brief")) { //$NON-NLS-1$
				verbosity = Verbosity.LOW;
			} else if (args[n].equals("-includeevents")) { //$NON-NLS-1$
				recordingFilter = OnLoadFilters.includeEvents(Arrays.asList(args[++n].split(","))); //$NON-NLS-1$
			} else {
				throw new ParseException("Unknown command " + args[n], n); //$NON-NLS-1$
			}
		}
		return new RecordingPrinter(output, verbosity, formatValues, recordingFilter);
	}

	private static void printHelp() {
		System.out.println("Usage:"); //$NON-NLS-1$
		System.out.println("filename The name of the flight recording file to print."); //$NON-NLS-1$
	}

	public void printEvent(IItem e) {
		out.println("<event name=\"" + e.getType().getName() + "\" path=\"" + e.getType().getIdentifier() + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (verbosity != Verbosity.LOW) {
			printValues(e);
		}
		out.print("</event>"); //$NON-NLS-1$
	}

	private void printValues(IItem event) {
		IType<IItem> itemType = ItemToolkit.getItemType(event);
		for (Entry<IAccessorKey<?>, ? extends IDescribable> e : itemType.getAccessorKeys().entrySet()) {
			IMemberAccessor<?, IItem> accessor = itemType.getAccessor(e.getKey());
			printValue(e.getKey(), e.getValue(), accessor.getMember(event));
		}
	}

	private void printValue(IAccessorKey<?> attribute, IDescribable desc, Object value) {
		if (value instanceof IMCStackTrace) {
			printTrace((IMCStackTrace) value);
		} else {
			out.print("  <" + attribute.getIdentifier() + " name=\"" + desc.getName() + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (value == null) {
				out.print("null"); //$NON-NLS-1$
			} else {
				out.print(stringify("", value)); //$NON-NLS-1$
			}
			out.println("</" + attribute.getIdentifier() + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private String stringify(String indent, Object value) {
		if (value instanceof IMCMethod) {
			return indent + stringifyMethod((IMCMethod) value);
		}
		if (value instanceof IMCType) {
			return indent + stringifyType((IMCType) value);
		}
		if (value instanceof IQuantity) {
			if (formatValues) {
				return ((IQuantity) value).displayUsing(IDisplayable.AUTO);
			} else {
				return ((IQuantity) value).persistableString();
			}
		}
		// Workaround to maintain output after changed EventType.toString().
		if (value instanceof IDescribable) {
			String name = ((IDescribable) value).getName();
			return (name != null) ? name : value.toString();
		}
		if (value == null) {
			return "null"; //$NON-NLS-1$
		}
		if (value.getClass().isArray()) {
			StringBuffer buffer = new StringBuffer();
			Object[] values = (Object[]) value;
			buffer.append(" [" + values.length + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			for (Object o : values) {
				buffer.append(indent);
				buffer.append(stringify(indent + "  ", o)); //$NON-NLS-1$
			}
			return buffer.toString();
		}
		return value.toString();
	}

	private void printTrace(IMCStackTrace trace) {
		out.println("  <stackTrace>"); //$NON-NLS-1$
		if (verbosity == Verbosity.HIGH) {
			for (IMCFrame frame : trace.getFrames()) {
				printFrame("     ", frame, out); //$NON-NLS-1$
			}
		}
		out.println("  </stackTrace>"); //$NON-NLS-1$
	}

	private static void printFrame(String indent, IMCFrame frame, PrintWriter out) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(indent);
		buffer.append("<frame "); //$NON-NLS-1$
		Integer lineNumber = frame.getFrameLineNumber();
		IMCMethod method = frame.getMethod();
		buffer.append("method=\""); //$NON-NLS-1$
		if (method != null) {
			buffer.append(stringifyMethod(method));
		} else {
			buffer.append("null"); //$NON-NLS-1$
		}
		buffer.append(" line=\""); //$NON-NLS-1$
		buffer.append(String.valueOf(lineNumber));
		buffer.append("\" type=\"" + frame.getType() + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println(buffer.toString());
	}

	private static String stringifyType(IMCType type) {
		StringBuffer sb = new StringBuffer();
		sb.append(formatPackage(type.getPackage()));
		sb.append("."); //$NON-NLS-1$
		sb.append(String.valueOf(type.getTypeName()));
		return sb.toString();
	}

	private static String stringifyMethod(IMCMethod method) {
		StringBuffer buffer = new StringBuffer();
		Integer modifier = method.getModifier();
		buffer.append(formatPackage(method.getType().getPackage()));
		buffer.append("."); //$NON-NLS-1$
		buffer.append(method.getType().getTypeName());
		buffer.append("#"); //$NON-NLS-1$
		buffer.append(method.getMethodName());
		buffer.append(method.getFormalDescriptor());
		buffer.append("\""); //$NON-NLS-1$
		if (modifier != null) {
			buffer.append(" modifier=\""); //$NON-NLS-1$
			buffer.append(Modifier.toString(method.getModifier()));
			buffer.append("\""); //$NON-NLS-1$
		}
		return buffer.toString();
	}

	private static String formatPackage(IMCPackage mcPackage) {
		return FormatToolkit.getPackage(mcPackage);
	}
}
