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
package org.openjdk.jmc.console.ui.mbeanbrowser.notifications;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import javax.management.Notification;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import org.openjdk.jmc.rjmx.services.IAttributeInfo;
import org.openjdk.jmc.rjmx.services.IReadOnlyAttribute;
import org.openjdk.jmc.rjmx.ui.attributes.AttributeTreeBuilder;
import org.openjdk.jmc.rjmx.util.internal.DefaultAttribute;
import org.openjdk.jmc.rjmx.util.internal.SimpleAttributeInfo;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.ArrayProxy;
import org.openjdk.jmc.ui.misc.MementoToolkit;
import org.openjdk.jmc.ui.misc.TreeStructureContentProvider;

/**
 * A log section for notification where the user data can be expanded and introspected.
 */
public class MBeanNotificationLogInspector {
	public static final String MBEANBROWSER_NOTIFICATIONSTAB_LOGTREE_NAME = "mbeanbrowser.notificationsTab.LogInspectorTree"; //$NON-NLS-1$

	private static class NotificationValue extends DefaultAttribute {

		private static final DateFormat DATE_FORMAT;
		private static final String SECOND_FORMATTING = "ss"; //$NON-NLS-1$
		private static final String MILLIS_FORMATTING = ".SSS"; //$NON-NLS-1$

		static {
			DateFormat defaultFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
			if (defaultFormat instanceof SimpleDateFormat) {
				SimpleDateFormat format = (SimpleDateFormat) defaultFormat;
				String pattern = format.toPattern();
				int index = pattern.indexOf(SECOND_FORMATTING);
				if (index >= 0) {
					defaultFormat = new SimpleDateFormat(
							pattern.replace(SECOND_FORMATTING, SECOND_FORMATTING + MILLIS_FORMATTING));
				} else {
					defaultFormat = new SimpleDateFormat(pattern + MILLIS_FORMATTING);
				}
			}
			DATE_FORMAT = defaultFormat;
		}

		private final Notification m_notification;
		private Collection<IReadOnlyAttribute> m_children;

		public NotificationValue(Notification notification) {
			super(createAttributeInfo(notification));
			m_notification = notification;
		}

		private synchronized static IAttributeInfo createAttributeInfo(Notification notification) {
			return new SimpleAttributeInfo(DATE_FORMAT.format(new Date(notification.getTimeStamp())),
					notification.getType());
		}

		@Override
		public Object getValue() {
			return m_notification.getMessage();
		}

		@Override
		public boolean hasChildren() {
			return true;
		}

		@Override
		public Collection<IReadOnlyAttribute> getChildren() {
			if (m_children == null) {
				m_children = new ArrayList<>();
				m_children.add(new DefaultAttribute(Messages.MBeanNotificationLogInspector_TIME_STAMP_LABEL,
						m_notification.getTimeStamp()));
				m_children.add(new DefaultAttribute(Messages.MBeanNotificationLogInspector_TYPE_LABEL,
						m_notification.getType()));
				m_children.add(new DefaultAttribute(Messages.MBeanNotificationLogInspector_SEQUENCE_NUMBER_LABEL,
						m_notification.getSequenceNumber()));
				m_children.add(new DefaultAttribute(Messages.MBeanNotificationLogInspector_MESSAGE_LABEL,
						m_notification.getMessage()));
				m_children.add(new DefaultAttribute(Messages.MBeanNotificationLogInspector_USER_DATA_LABEL,
						m_notification.getUserData()));
			}
			return m_children;
		}

		@Override
		public int hashCode() {
			return m_notification.hashCode();
		}

		@Override
		public boolean equals(Object that) {
			return that instanceof NotificationValue
					&& m_notification.equals(((NotificationValue) that).m_notification);
		}

		public synchronized static DateFormat getDateFormat() {
			return (DateFormat) DATE_FORMAT.clone();
		}
	}

	private final ColumnManager columnManager;

	private Object[] values = new Object[0];

	public MBeanNotificationLogInspector(Composite parent, FormToolkit toolkit, IMemento state) {
		Section section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
		section.setText(Messages.MBeanNotificationLogSectionPart_LOG_SECTION_TITLE);
		Tree tree = new Tree(section, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		section.setClient(tree);

		TreeViewer viewer = new TreeViewer(tree);
		viewer.setContentProvider(new TreeStructureContentProvider());
		viewer.setInput(new ArrayProxy<>(this::getValues));
		ColumnViewerToolTipSupport.enableFor(viewer);
		List<IColumn> columns = Arrays.asList(AttributeTreeBuilder.NAME, AttributeTreeBuilder.VALUE,
				AttributeTreeBuilder.TYPE);
		columnManager = ColumnManager.build(viewer, columns, TableSettings.forState(MementoToolkit.asState(state)));
		ColumnMenusFactory.addDefaultMenus(columnManager, MCContextMenuManager.create(tree));

		// at least one uitest depends on this widget being named (MBeanBrowser, Jemmy code)
		tree.setData("name", MBEANBROWSER_NOTIFICATIONSTAB_LOGTREE_NAME); //$NON-NLS-1$s
	}

	public void saveState(IMemento state) {
		columnManager.getSettings().saveState(MementoToolkit.asWritableState(state));
	}

	private Object[] getValues() {
		return values;
	}

	public void show(Stream<Notification> notifications) {
		values = notifications.map(NotificationValue::new).toArray();
		columnManager.getViewer().refresh();
	}

	public static DateFormat getDateFormat() {
		return NotificationValue.getDateFormat();
	}
}
