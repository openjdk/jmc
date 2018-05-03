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
package org.openjdk.jmc.test.jemmy.misc.wrappers;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jemmy.control.Wrap;
import org.jemmy.input.StringPopupOwner;
import org.jemmy.interfaces.Parent;
import org.jemmy.interfaces.Selectable;
import org.jemmy.lookup.Lookup;
import org.jemmy.resources.StringComparePolicy;
import org.jemmy.swt.TextWrap;
import org.jemmy.swt.lookup.ByItemLookup;
import org.jemmy.swt.lookup.ByName;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.fetchers.Fetcher;

/**
 * the Jemmy wrapper for CTabFolder widgets
 */
public class MCTabFolder extends MCJemmyBase {
	private Wrap<? extends Control> shell;
	private static final StringComparePolicy policy = StringComparePolicy.SUBSTRING;
	private final String name;
	private final String itemName;

	private MCTabFolder(Wrap<? extends CTabFolder> folder, Wrap<? extends Control> shell, String name,
			String itemName) {
		this.control = folder;
		this.shell = shell;
		this.name = name;
		this.itemName = itemName;
	}

	public MCTabFolder(Wrap<? extends CTabFolder> folder, Wrap<? extends Control> shell) {
		this(folder, shell, null, null);
	}

	/**
	 * Attempts to find a CTabFolder of the given name in the default shell and returns a
	 * {@link @McTabFolder} representing it.
	 *
	 * @param name
	 *            the name of the folder
	 * @return the {@link @McTabFolder} representing it
	 */
	public static MCTabFolder getByName(String name) {
		return getByName(getShell(), name);
	}

	/**
	 * Attempts to find a CTabFolder of the given name and returns a {@link @McTabFolder} representing it.
	 *
	 * @param shell
	 *            the shell wrap to find the folder in
	 * @param name
	 *            the name of the folder
	 * @return the {@link @McTabFolder} representing it
	 */
	public static MCTabFolder getByName(Wrap<? extends Shell> shell, String name) {
		Wrap<? extends CTabFolder> folder = doLookup(shell, name, null, false);
		if (folder != null) {
			return new MCTabFolder(folder, shell, name, null);
		} else {
			return null;
		}
	}

	/**
	 * Attempts to find a CTabFolder with the given child tab folder name in the default shell and
	 * returns a {@link @McTabFolder} representing it.
	 *
	 * @param tabName
	 *            the name (text) of the child tab folder
	 * @return the {@link @McTabFolder} representing it
	 */
	public static MCTabFolder getByTabName(String tabName) {
		return getByTabName(getShell(), tabName);
	}

	/**
	 * Attempts to find a CTabFolder with the given child tab folder name and returns a {@link @McTabFolder}
	 * representing it.
	 *
	 * @param shell
	 *            the shell wrap to find the folder in
	 * @param tabName
	 *            the name (text) of the child tab folder
	 * @return the {@link @McTabFolder} representing it
	 */
	public static MCTabFolder getByTabName(Wrap<? extends Shell> shell, String tabName) {
		Wrap<? extends CTabFolder> folder = doLookup(shell, null, tabName, false);
		if (folder != null) {
			return new MCTabFolder(folder, shell, null, tabName);
		} else {
			return null;
		}
	}

	/**
	 * @return a list of all visible tab folders
	 */
	public static List<MCTabFolder> getAllVisible() {
		return getAllVisible(getShell());
	}

	/**
	 * @param parent
	 *            the starting point in the SWT hierachy from where to start the lookup
	 * @return a list of all visible tabfolders
	 */
	@SuppressWarnings("unchecked")
	public static List<MCTabFolder> getAllVisible(Wrap<? extends Control> parent) {
		List<Wrap<? extends CTabFolder>> folderList = getVisible(
				parent.as(Parent.class, CTabFolder.class).lookup(CTabFolder.class), true, false);
		List<MCTabFolder> folders = new ArrayList<>();
		for (Wrap<? extends CTabFolder> folder : folderList) {
			folders.add(new MCTabFolder(folder, parent, null, null));
		}
		return folders;
	}

	/**
	 * Selects a CTabItem in this CTabFolder (if not already selected)
	 *
	 * @param item
	 *            The title of the item to select
	 */
	@SuppressWarnings("unchecked")
	public void select(String item) {
		ensureAlive();
		Selectable<String> selectable = control.as(Selectable.class);
		if (selectable.getState() != item) {
			selectable.selector().select(item);
		}

	}

	/**
	 * Get all the tab names (text) of this {@link @McTabFolder}
	 *
	 * @return a List of String containing the text values of all the tabs
	 */
	@SuppressWarnings("unchecked")
	public List<String> getItems() {
		return (List<String>) control.getProperty(Selectable.STATES_PROP_NAME);
	}

	/**
	 * Get the tab names (text) of this {@link @McTabFolder} that are visible
	 *
	 * @return a List of String containing the text values of all the visible tabs
	 */
	public List<String> getVisibleItems() {
		Fetcher<List<String>> fetcher = new Fetcher<List<String>>() {
			@Override
			public void run() {
				List<String> visibleTabNames = new ArrayList<>();
				for (CTabItem ti : Arrays.asList(((CTabFolder) control.getControl()).getItems())) {
					if (ti.isShowing()) {
						visibleTabNames.add(ti.getText());
					}
				}
				setOutput(visibleTabNames);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Closes all tabs in the tab folder (using the context menu)
	 */
	@SuppressWarnings("unchecked")
	public void closeAll() {
		ensureAlive();
		click();
		StringPopupOwner<Shell> popupMenu = control.as(StringPopupOwner.class);
		Wrap<? extends CTabItem> item = control.as(Parent.class, CTabItem.class).lookup(CTabItem.class).wrap();
		popupMenu.setPolicy(policy);
		popupMenu.push(item.getClickPoint(), "Close All");
	}

	/**
	 * @return The text of the Text control underneath the CTabFolder currently selected,
	 *         {@code null} if no CTabItem is selected.
	 */
	@Override
	public String getText() {
		ensureAlive();
		return new TextWrap<>(control.getEnvironment(), Text.class.cast(getSelected())).getProperty(String.class,
				Wrap.TEXT_PROP_NAME);
	}

	private boolean needReinit() {
		return (control == null || isDisposed(control) || isDisposed(getSelected()));
	}

	private void ensureAlive() {
		if (needReinit()) {
			control = doLookup(shell, name, itemName);
		}
	}

	private Control getSelected() {
		Fetcher<Control> fetcher = new Fetcher<Control>() {
			@Override
			public void run() {
				CTabFolder tabFolder = CTabFolder.class.cast(control.getControl());
				CTabItem tabItem = (tabFolder != null) ? tabFolder.getSelection() : null;
				Control result = (tabItem != null) ? tabItem.getControl() : null;
				setOutput(result);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	protected Image getSelectedTabImage() {
		Fetcher<Image> fetcher = new Fetcher<Image>() {
			@Override
			public void run() {
				CTabFolder tabFolder = CTabFolder.class.cast(control.getControl());
				Item tabItem = (tabFolder != null) ? tabFolder.getSelection() : null;
				Image result = (tabItem != null) ? tabItem.getImage() : null;
				setOutput(result);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Saves an image of the currently selected tab to the path provided
	 * 
	 * @param path
	 *            the desired path of the image file
	 */
	public void saveImageToFile(String path) {
		ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] {getSelectedTabImage().getImageData()};
		File parent = new File(path).getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		loader.save(path, SWT.IMAGE_PNG);
	}

	/**
	 * @return The title of the currently selected tab
	 */
	public String getState() {
		return String.class.cast(control.getProperty(Selectable.STATE_PROP_NAME));
	}

	/**
	 * Compares the currently selected tab title and image for equality with the supplied form
	 *
	 * @param form
	 *            The form to compare with
	 * @return {@code true} if the title and image matches. Otherwise {@code false}
	 */
	public boolean selectedTabMatches(MCForm form) {
		return getState().equals(form.getText()) && getSelectedTabImage().equals(form.getImage());
	}

	private static Wrap<? extends CTabFolder> doLookup(Wrap<? extends Control> shell, String name, String itemName) {
		return doLookup(shell, name, itemName, true);
	}

	@SuppressWarnings("unchecked")
	private static Wrap<? extends CTabFolder> doLookup(
		Wrap<? extends Control> shell, String name, String itemName, boolean assertEmpty) {
		Lookup<CTabFolder> lookup = null;
		if (itemName != null) {
			lookup = shell.as(Parent.class, CTabFolder.class).lookup(CTabFolder.class,
					new ByItemLookup<CTabFolder>(itemName, policy));
		} else {
			lookup = shell.as(Parent.class, CTabFolder.class).lookup(CTabFolder.class, new ByName<CTabFolder>(name));
		}
		List<Wrap<? extends CTabFolder>> folderList = getVisible(lookup, true, assertEmpty);
		if (folderList.size() > 0) {
			return folderList.get(0);
		} else {
			return null;
		}
	}
}
