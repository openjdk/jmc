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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.misc.CompositeToolkit;

public class ItemAggregateViewer {

	private final static boolean SCROLL_WORKAROUND = !Boolean.getBoolean("org.openjdk.jmc.ui.disablescrollworkaround"); //$NON-NLS-1$
	private final ScrolledComposite scroller;
	private final Composite infoContainer;
	private final FormToolkit toolkit;
	private final List<Pair<Function<IItemCollection, ?>, Text>> infos = new ArrayList<>();
	private int lineHeight;

	public ItemAggregateViewer(Composite container, FormToolkit toolkit) {
		this(container, toolkit, 1);
	}

	public ItemAggregateViewer(Composite container, FormToolkit toolkit, int columns) {
		this.toolkit = toolkit;
		scroller = CompositeToolkit.createVerticalScrollComposite(container);
		infoContainer = createInfoContainer(scroller, toolkit, columns);
		scroller.setContent(infoContainer);
	}

	public Control getControl() {
		return scroller;
	}

	public void addCaption(String text) {
		addCaption(text, null);
	}

	public void addCaption(String text, String tooltip) {
		Label caption = toolkit.createLabel(infoContainer, text);
		caption.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		caption.setLayoutData(new TableWrapData(TableWrapData.FILL, TableWrapData.MIDDLE, 1, 2));
		caption.setToolTipText(tooltip);
	}

	public void addAggregate(IAggregator<?, ?> aggregator) {
		addAggregate(aggregator, aggregator.getName(), aggregator.getDescription());
	}

	public void addAggregate(IAggregator<?, ?> aggregator, String name, String description) {
		addValueFunction(i -> i.getAggregate(aggregator), name, description);
	}

	public void addValueFunction(Function<IItemCollection, ?> valueFunction, String name, String description) {
		Text valueText = createInfoText(infoContainer, toolkit, name, description,
				SWT.READ_ONLY | SWT.NO_SCROLL | SWT.BORDER | SWT.MULTI | SWT.WRAP);
		infos.add(new Pair<>(valueFunction, valueText));

		if (SCROLL_WORKAROUND) {
			if (lineHeight <= 1) {
				// Use a consistent line height for all mouse wheel listeners. Also avoids repeating slightly
				// expensive calculations on some platforms.
				lineHeight = valueText.getLineHeight();
			}
			valueText.addListener(SWT.MouseVerticalWheel, new Listener() {
				@Override
				public void handleEvent(Event event) {
					Point pos = scroller.getOrigin();
					int deltaPoints;
					switch (event.detail) {
					case SWT.SCROLL_PAGE:
						deltaPoints = event.count * scroller.getClientArea().height;
						break;
					case SWT.SCROLL_LINE:
						deltaPoints = event.count * lineHeight;
						break;
					default:
						// Assume this means better vertical resolution (pixels or points) in the future.
						deltaPoints = event.count;
					}
					scroller.setOrigin(pos.x, pos.y - deltaPoints);
					event.doit = false;
				}
			});
		}
	}

	public void setValues(IItemCollection items) {
		for (Pair<Function<IItemCollection, ?>, Text> i : infos) {
			Object aggregate = i.left.apply(items);
			i.right.setText(aggregate == null ? Messages.INFORMATION_COMPONENT_NOT_AVAILABLE
					: TypeHandling.getValueString(aggregate));
			i.right.setToolTipText(aggregate == null ? Messages.INFORMATION_COMPONENT_NOT_AVAILABLE
					: TypeHandling.getVerboseString(aggregate));
		}
	}

	public static Composite createInfoContainer(Composite container, FormToolkit toolkit, int columns) {
		Composite infoContainer = toolkit.createComposite(container, SWT.NONE);
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = columns * 2;
		infoContainer.setLayout(layout);
		return infoContainer;
	}

	public static Text createInfoText(
		Composite infoContainer, FormToolkit toolkit, String text, String tooltip, int style) {
		Label nameLabel = toolkit.createLabel(infoContainer, text);
		nameLabel.setLayoutData(new TableWrapData(TableWrapData.FILL, TableWrapData.MIDDLE));
		nameLabel.setToolTipText(tooltip);
		Text valueText = toolkit.createText(infoContainer, "", style); //$NON-NLS-1$
		valueText.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.FILL_GRAB));
		return valueText;
	}
}
