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
package org.openjdk.jmc.flightrecorder.ui.common;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.charts.IChartInfoVisitor;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;

/**
 * Default chart tooltip provider. Note that each instance is only used once.
 */
// FIXME: Rework to use StyledText instead, since FormText has overflow problems on HiDPI screens
// (at least up to and including Eclipse 4.5). Also, use indentation for scopes and "At" sections.
public class ChartToolTipProvider implements IChartInfoVisitor {
	private static final int MAXIMUM_VISIBLE_STACK_TRACE_ELEMENTS = 10;
	private static final String INDENT_4_NBSP = "&#160;&#160;&#160;&#160;"; //$NON-NLS-1$
	private static final String INITIAL_HTML = "<form>"; //$NON-NLS-1$

	protected StringBuilder text = new StringBuilder(INITIAL_HTML);
	private final Map<String, Image> imageMap = new HashMap<>();
	private int colorNum;
	protected int bulletIndent;
	protected String lastAt;

	/**
	 * Return the HTML. This method should typically only be called once. (Though technically, with
	 * the current implementation, as long as it returns null, it could be called again. There's a
	 * potential use case where this could be useful. If that is ever used, change this statement.)
	 *
	 * @return the HTML text or null if no tooltip should be shown.
	 */
	public String getHTML() {
		if (text.length() <= INITIAL_HTML.length()) {
			return null;
		}
		text.append("</form>"); //$NON-NLS-1$
		return text.toString();
	}

	public Map<String, Image> getImages() {
		return imageMap;
	}

	protected Stream<IAttribute<?>> getAttributeStream(IType<IItem> type) {
		return type.getAttributes().stream();
	}

	@Override
	public boolean enterScope(String context, boolean fullyShown) {
		if (!fullyShown) {
			text.append("<p>").append(htmlify(context)).append("</p>"); //$NON-NLS-1$ //$NON-NLS-2$
			bulletIndent += 16;
			return true;
		}
		return false;
	}

	@Override
	public void leaveScope() {
		bulletIndent -= 16;
	}

	protected String format(IDisplayable value) {
		if (value != null) {
			// FIXME: Add formatter that does AUTO (EXACT) or so.
			String auto = value.displayUsing(IDisplayable.AUTO);
//			String exact = value.displayUsing(IDisplayable.EXACT);
//			return (auto.equals(exact)) ? htmlify(auto) : htmlify(auto + " (" + exact + ')');
			return htmlify(auto);
		} else {
			return Messages.N_A;
		}
	}

	protected void appendTagLI(Color color) {
		if (color != null) {
			imageMap.put("color." + colorNum, SWTColorToolkit.getColorThumbnail(SWTColorToolkit.asRGB(color))); //$NON-NLS-1$
			text.append("<li style='image' value='color.").append(colorNum).append("' "); //$NON-NLS-1$ //$NON-NLS-2$
			colorNum++;
		} else {
			text.append("<li "); //$NON-NLS-1$
		}
//		text.append("bindent='").append(bulletIndent).append("'> "); //$NON-NLS-1$ //$NON-NLS-2$
		text.append("bindent='0'> "); //$NON-NLS-1$
	}

	protected void appendTitle(String title) {
		text.append("<p><b>").append(title).append("</b></p>");
	}

	protected void appendAtIfNew(IDisplayable newAt) {
		String newAtAsString = format(newAt);
		if (!newAtAsString.equals(lastAt)) {
			text.append("<p><span nowrap='true'>At ").append(newAtAsString).append(":<br/></span></p>"); //$NON-NLS-1$ //$NON-NLS-2$
			lastAt = newAtAsString;
		}
	}

	@Override
	public void visit(IPoint point) {
		appendAtIfNew(point.getX());
		appendTagLI(point.getColor());
		String name = point.getName();
		text.append("<span nowrap='true'>"); //$NON-NLS-1$
		if (name != null) {
			text.append(htmlify(name)).append(" = "); //$NON-NLS-1$
		}
		text.append(format(point.getY()));
		text.append("</span></li>"); //$NON-NLS-1$
	}

	@Override
	public void visit(IBucket bucket) {
		appendAtIfNew(bucket.getRange());
		appendTagLI(bucket.getColor());
		text.append("<span nowrap='true'>"); //$NON-NLS-1$
		String name = bucket.getName();
		if (name != null) {
			text.append(htmlify(name));
			text.append(" [").append(format(bucket.getWidth())).append("] = "); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			text.append("[").append(format(bucket.getWidth())).append("]: "); //$NON-NLS-1$ //$NON-NLS-2$
		}

		text.append(format(bucket.getY()));
		text.append("</span></li>"); //$NON-NLS-1$
	}

	// FIXME: One idea was to let the user see the details in Properties/StackTrace views by click-selecting an event.
	@Override
	public void visit(ISpan span) {
		if (span.getDescription() != null) {
			appendTitle(span.getDescription());
		}
		appendAtIfNew(span.getRange());
		appendTagLI(span.getColor());
		// Would normally insert <span nowrap='true'> here, but since bold text is not displayed,
		// it is inserted after the <b> element instead.
		Object payload = span.getPayload();
		IItem item = AdapterUtil.getAdapter(payload, IItem.class);
		if (payload instanceof IDisplayable) {
			text.append("<span nowrap='true'>"); //$NON-NLS-1$
			text.append(format((IDisplayable) payload)).append(": "); //$NON-NLS-1$
		} else if (item != null) {
			IType<IItem> type = ItemToolkit.getItemType(item);
			text.append("<b>").append(htmlify(type.getName())).append("</b><span nowrap='true'>: "); //$NON-NLS-1$ //$NON-NLS-2$
		}
		text.append(format(span.getWidth()));
		text.append("</span></li>"); //$NON-NLS-1$
		if (item != null) {
			IType<IItem> type = ItemToolkit.getItemType(item);
			IMCStackTrace trace = null;
			Iterator<IAttribute<?>> attributes = getAttributeStream(type).iterator();
			while (attributes.hasNext()) {
				IAttribute<?> attribute = attributes.next();
				if (attribute.equals(JfrAttributes.EVENT_STACKTRACE)) {
					trace = JfrAttributes.EVENT_STACKTRACE.getAccessor(type).getMember(item);
					continue;
				}
				text.append("<p vspace='false'><span nowrap='true'>"); //$NON-NLS-1$
				text.append(htmlify(attribute.getName())).append(": "); //$NON-NLS-1$
				// FIXME: Format timestamp with higher precision
				Object value = attribute.getAccessor(type).getMember(item);
				String valueString = TypeHandling.getValueString(value);
				text.append(htmlify(valueString));
				text.append("</span></p>"); //$NON-NLS-1$
				// Get value
			}
			if (trace != null) {
				text.append("<p vspace='false'/>"); //$NON-NLS-1$
				text.append("<p vspace='false'>"); //$NON-NLS-1$
				text.append(htmlify(JfrAttributes.EVENT_STACKTRACE.getName())).append(":<br/>"); //$NON-NLS-1$
				appendStackTrace(trace, true, false, true, true, true, false);
				text.append("</p>"); //$NON-NLS-1$

			}
		}
	}

	private void appendStackTrace(
		IMCStackTrace trace, boolean showReturnValue, boolean showReturnValuePackage, boolean showClassName,
		boolean showClassPackageName, boolean showArguments, boolean showArgumentsPackage) {
		String indent = "    "; //$NON-NLS-1$
		String lineSeparator = System.getProperty("line.separator"); //$NON-NLS-1$
		String stackTraceString = FormatToolkit.getHumanReadable(trace, showReturnValue, showReturnValuePackage,
				showClassName, showClassPackageName, showArguments, showArgumentsPackage,
				MAXIMUM_VISIBLE_STACK_TRACE_ELEMENTS, indent, null, lineSeparator);
		stackTraceString = htmlify(stackTraceString);
		stackTraceString = stackTraceString.replace(indent, INDENT_4_NBSP);
		stackTraceString = stackTraceString.replace(lineSeparator, "<br/>"); //$NON-NLS-1$
		text.append(stackTraceString);
	}

	@Override
	public void visit(ITick tick) {
		text.append("<p><span nowrap='true'>"); //$NON-NLS-1$
		text.append(htmlify(tick.getValue().displayUsing(IDisplayable.VERBOSE)));
		text.append("</span><br/></p>"); //$NON-NLS-1$
	}

	private static String htmlify(String text) {
		return XmlToolkit.escapeTagContent(text);
	}

	@Override
	public void visit(ILane lane) {
		text.append("<p><span nowrap='true'>"); //$NON-NLS-1$
		text.append(htmlify(NLS.bind(Messages.ChartToolTipProvider_CAPTION_NAME, lane.getLaneName())));
		text.append("</span><br/><span nowrap='true'>"); //$NON-NLS-1$
		if (lane.getLaneDescription() != null && !lane.getLaneDescription().isEmpty()) {
			text.append(
					htmlify(NLS.bind(Messages.ChartToolTipProvider_CAPTION_DESCRIPTION, lane.getLaneDescription())));
		}
		text.append("</span></p>"); //$NON-NLS-1$
	}
}
