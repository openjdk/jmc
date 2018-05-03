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
package org.openjdk.jmc.ui.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.accessibility.SimpleTraverseListener;
import org.openjdk.jmc.ui.preferences.PreferenceConstants;

/**
 * Toolbar manager that can be used with a Section
 */
public class MCToolBarManager {
	public static final int ALIGN_LEFT = 0;
	public static final int ALIGN_CENTER_LEFT = 10;
	public static final int ALIGN_CENTER = 20;
	public static final int ALIGN_CENTER_RIGHT = 30;
	public static final int ALIGN_RIGHT = 40;

	private static class ToolbarItem implements Comparable<ToolbarItem> {
		final private int m_alignment;
		final private IAction m_action;

		public ToolbarItem(IAction action, int alignMent) {
			m_alignment = alignMent;
			m_action = action;
		}

		public IAction getAction() {
			return m_action;
		}

		public int getAlignment() {
			return m_alignment;
		}

		@Override
		public int compareTo(ToolbarItem t) {
			return getAlignment() - t.getAlignment();
		}

		@Override
		public int hashCode() {
			return m_action.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			if (o instanceof ToolbarItem) {
				if (((ToolbarItem) o).getAction() == m_action) {
					return true;
				}
			}
			return false;
		}
	}

	public class HyperLinkListener implements IHyperlinkListener {
		@Override
		public void linkActivated(HyperlinkEvent e) {
			ToolbarItem activatedItem = getToolbarItem(e);
			if (activatedItem != null) {
				IAction activatedAction = activatedItem.getAction();
				if (isRadioable(activatedAction)) {
					// Go through all radioable items and disable them
					Control[] children = getToolBar().getChildren();
					int n = 0;
					for (Object element : m_list) {
						ToolbarItem item = (ToolbarItem) element;
						ImageHyperlink imageHyperLink = (ImageHyperlink) children[n++];
						IAction action = item.getAction();
						if (isRadioable(action)) {
							if (action != activatedItem.getAction()) {
								action.setChecked(false);
								updateImage(imageHyperLink, action);
							}
						}
					}
					// check the selected
					activatedAction.setChecked(true);
					updateImage((ImageHyperlink) e.widget, activatedAction);
				}
				if (isCheckable(activatedAction)) {
					activatedAction.setChecked(!activatedAction.isChecked());
					updateImage((ImageHyperlink) e.widget, activatedAction);
				}
				// run runnable
				if (activatedAction.isEnabled()) {
					activatedAction.run();
				}
			}
		}

		@Override
		public void linkEntered(HyperlinkEvent e) {
			ToolbarItem item = getToolbarItem(e);
			if (item != null) {
				setImage((ImageHyperlink) e.widget, item.getAction().getHoverImageDescriptor());
			}
		}

		@Override
		public void linkExited(HyperlinkEvent e) {
			ToolbarItem item = getToolbarItem(e);
			if (item != null) {
				updateImage((ImageHyperlink) e.widget, item.getAction());
			}
		}
	}

	/**
	 * Since {@link ImageHyperlink} never initializes the text, is is initially {@code null}, and
	 * this is handled as a special case where you only want an image link. You may change the text
	 * later using {@link #setText(String)} . However, you can never set the text back to
	 * {@code null}. If you attempt to, it is replaced with the (interned) empty string, courtesy of
	 * the {@link Hyperlink} class. The result is that the link takes up a lot more space. This
	 * subclass works around this limitation by returning null instead of empty strings.
	 */
	public static class ImprovedImageHyperlink extends ImageHyperlink {
		public ImprovedImageHyperlink(Composite parent, int style) {
			super(parent, style);
		}

		@Override
		public String getText() {
			String text = super.getText();
			return ((text == null) || (text.length() == 0)) ? null : text;
		}
	}

	final private IPropertyChangeListener actionListener = new IPropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (getToolBar().isDisposed()) {
				return;
			}
			String property = event.getProperty();
			IAction action = (IAction) event.getSource();
			Control[] controls = getToolBar().getChildren();
			for (Control control : controls) {
				ToolbarItem tbi = (ToolbarItem) control.getData();
				if (tbi.getAction() == action) {
					ImageHyperlink imageHyperlink = (ImageHyperlink) control;
					if (IAction.ENABLED.equals(property)) {
						updateImage(imageHyperlink, action);
					} else if (IAction.TOOL_TIP_TEXT.equals(property)) {
						imageHyperlink.setToolTipText(action.getToolTipText());
					} else if (IAction.TEXT.equals(property)) {
						updateText(imageHyperlink, action);
					}

					return;
				}
			}
		}
	};

	final private Composite m_parent;
	final private ArrayList<ToolbarItem> m_list = new ArrayList<>();

	private Composite m_toolbar;
	private LocalResourceManager m_localResourceManager;

	public MCToolBarManager(Composite parent) {
		m_parent = parent;
	}

	public Composite getParent() {
		return m_parent;
	}

	public void dispose() {
		if (m_toolbar != null && !m_toolbar.isDisposed()) {
			m_toolbar.dispose();
		}
		if (m_localResourceManager != null) {
			m_localResourceManager.dispose();
		}
		if (m_list != null) {
			for (ToolbarItem tbi : m_list) {
				tbi.getAction().removePropertyChangeListener(actionListener);
			}
			m_list.clear();
		}

		m_localResourceManager = null;
	}

	public List<IAction> getActions() {
		ArrayList<IAction> list = new ArrayList<>();
		for (ToolbarItem item : m_list) {
			list.add(item.getAction());
		}
		return Collections.unmodifiableList(list);
	}

	public void update() {
		Composite layoutRoot = getToolBar().getParent();
		layoutRoot.setLayoutDeferred(true);
		try {
			disposeToolbarItems();
			for (Object element : m_list) {
				ToolbarItem item = (ToolbarItem) element;
				createWidget(item, getToolBar());
			}
		} finally {
			layoutRoot.setLayoutDeferred(false);
		}
	}

	public Composite getToolBar() {
		if (m_toolbar == null) {
			m_toolbar = new Composite(getParent(), SWT.NONE);
			RowLayout layout = new RowLayout(SWT.HORIZONTAL);
			layout.marginLeft = 0;
			layout.marginRight = 0;
			layout.spacing = 0;
			layout.marginTop = 0;
			layout.marginBottom = 0;
			m_toolbar.setLayout(layout);
		}
		return m_toolbar;
	}

	public void add(IAction action, int alignment) {
		m_list.add(new ToolbarItem(action, alignment));
		Collections.sort(m_list);
		update();
		action.addPropertyChangeListener(actionListener);
	}

	public void add(IAction action) {
		add(action, ALIGN_CENTER);
	}

	public void remove(IAction action) {
		action.removePropertyChangeListener(actionListener);
		Control[] children = getToolBar().getChildren();
		for (Control element : children) {
			ToolbarItem item = (ToolbarItem) element.getData();
			if (action.equals(item.getAction())) {
				m_list.remove(item);
				update();
				return;
			}
		}
	}

	private ToolbarItem getToolbarItem(HyperlinkEvent e) {
		return (ToolbarItem) e.widget.getData();
	}

	private void setImage(ImageHyperlink imageHyperLink, ImageDescriptor imageDescriptor) {
		if (imageHyperLink != null && imageDescriptor != null) {
			try {
				imageHyperLink.setImage(getResourceManager().createImage(imageDescriptor));
				imageHyperLink.redraw();
				/*
				 * Explicit calls to update() should be avoided unless absolutely necessary. They
				 * may have a negative performance impact and may cause issues on Mac OS X Cocoa
				 * (SWT 3.6). If it is required here, there must be a justifying comment.
				 */
				// imageHyperLink.update();
			} catch (DeviceResourceException e) {
				// this is probably pretty bad, but let's ignore it for now
			}
		}
	}

	private LocalResourceManager getResourceManager() {
		if (m_localResourceManager == null) {
			m_localResourceManager = new LocalResourceManager(JFaceResources.getResources());
			;
		}
		return m_localResourceManager;
	}

	private boolean isCheckable(IAction action) {
		return (action != null && (action.getStyle() & IAction.AS_CHECK_BOX) != 0);
	}

	private boolean isRadioable(IAction action) {
		return (action != null && (action.getStyle() & IAction.AS_RADIO_BUTTON) != 0);
	}

	protected boolean isHoverable(IAction action) {
		return (action != null && action.getHoverImageDescriptor() != null);
	}

	private void updateImage(ImageHyperlink imageHyperLink, IAction action) {
		if (action.isEnabled()) {
			if (isCheckable(action) || isRadioable(action)) {
				if (action.isChecked()) {
					setImage(imageHyperLink, action.getImageDescriptor());
				} else {
					setImage(imageHyperLink, action.getDisabledImageDescriptor());
				}
			} else {
				setImage(imageHyperLink, action.getImageDescriptor());
			}
		} else {
			setImage(imageHyperLink, action.getDisabledImageDescriptor());
		}
	}

	private void disposeToolbarItems() {
		if (!getToolBar().isDisposed()) {
			Control[] controls = getToolBar().getChildren();
			for (Control control : controls) {
				control.setData(null);
				control.dispose();
			}
		}
	}

	private void createWidget(ToolbarItem item, Composite parent) {
		ImageHyperlink imageHyperLink = new ImprovedImageHyperlink(parent, SWT.CENTER);
		IAction action = item.getAction();
		imageHyperLink.setToolTipText(action.getToolTipText());
		updateText(imageHyperLink, action);
		imageHyperLink.setBackgroundImage(getParent().getBackgroundImage());
		imageHyperLink.addHyperlinkListener(new HyperLinkListener());
		imageHyperLink.setData("name", action.getId()); //$NON-NLS-1$
		imageHyperLink.setData(item);
		imageHyperLink.addTraverseListener(new SimpleTraverseListener(true));
		updateImage(imageHyperLink, action);
	}

	private void updateText(ImageHyperlink imageHyperLink, IAction action) {
		IPreferenceStore corePreferenceStore = UIPlugin.getDefault().getPreferenceStore();
		boolean buttonsAsText = corePreferenceStore.getBoolean(PreferenceConstants.P_ACCESSIBILITY_BUTTONS_AS_TEXT);
		imageHyperLink.setText(buttonsAsText ? action.getText() : null);
		imageHyperLink.getParent().getParent().layout(true);
	}
}
