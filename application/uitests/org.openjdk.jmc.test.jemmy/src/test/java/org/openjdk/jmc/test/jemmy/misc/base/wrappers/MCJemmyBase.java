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
package org.openjdk.jmc.test.jemmy.misc.base.wrappers;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.im.InputContext;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Widget;
import org.jemmy.action.AbstractExecutor;
import org.jemmy.control.Wrap;
import org.jemmy.env.Environment;
import org.jemmy.env.TestOut;
import org.jemmy.image.AWTImage;
import org.jemmy.image.AWTRobotCapturer;
import org.jemmy.image.AverageDistanceImageComparator;
import org.jemmy.image.FilesystemImageLoader;
import org.jemmy.image.Image;
import org.jemmy.image.ImageComparator;
import org.jemmy.image.StrictImageComparator;
import org.jemmy.input.AWTRobotInputFactory;
import org.jemmy.input.DefaultCharBindingMap;
import org.jemmy.interfaces.Focusable;
import org.jemmy.interfaces.Keyboard.KeyboardButton;
import org.jemmy.interfaces.Keyboard.KeyboardButtons;
import org.jemmy.interfaces.Keyboard.KeyboardModifiers;
import org.jemmy.interfaces.Parent;
import org.jemmy.lookup.AbstractLookup;
import org.jemmy.lookup.Lookup;
import org.jemmy.operators.Screen;
import org.jemmy.resources.StringComparePolicy;
import org.jemmy.swt.SWTMenu;
import org.jemmy.swt.Shells;
import org.jemmy.swt.lookup.ByItemLookup;
import org.jemmy.swt.lookup.ByName;
import org.jemmy.swt.lookup.ByTextControlLookup;
import org.jemmy.swt.lookup.ByTextShell;
import org.junit.Assert;

import org.openjdk.jmc.test.jemmy.misc.fetchers.Fetcher;
import org.openjdk.jmc.test.jemmy.misc.fetchers.FetcherWithInput;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCProgressIndicator;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTabFolder;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable;

/**
 * The base class for the Mission Control Jemmy wrappers
 */
public class MCJemmyBase {
	private static final boolean VERBOSE_JEMMY_LOGGING = "true"
			.equalsIgnoreCase(System.getProperty("mc.test.jemmy.verbose.logging"));
	private static final long EDITOR_LOAD_WAIT_DEFAULT_TIMEOUT_MS = 30000;
	private static final long EDITOR_LOAD_WAIT_TIMEOUT_MS = Long.getLong("jmc.test.editor.load.wait.timeout",
			EDITOR_LOAD_WAIT_DEFAULT_TIMEOUT_MS);
	public static final long VISIBLE_LOOKUP_DEFAULT_TIMEOUT_MS = 10000;
	private static final long VISIBLE_LOOKUP_TIMEOUT_MS = Long.getLong("jmc.test.visible.lookup.timeout",
			VISIBLE_LOOKUP_DEFAULT_TIMEOUT_MS);
	private static final int BETWEEN_KEYSTROKES_SLEEP = 100;
	public static final int LOOKUP_SLEEP_TIME_MS = 100;
	protected static Wrap<? extends Shell> shell;
	protected Wrap<? extends Control> control;
	public static final KeyboardButtons SELECTION_BUTTON;
	public static final KeyboardButtons EXPAND_BUTTON;
	public static final KeyboardButtons COLLAPSE_BUTTON;
	public static final KeyboardButtons CLOSE_BUTTON;
	public static final KeyboardModifiers SHORTCUT_MODIFIER;
	public static final String OS_NAME;
	private static final int IDLE_LOOP_COUNT = 3;
	private static final int IDLE_LOOP_TIME_STEP = 100;
	private static final int IDLE_LOOP_TIMEOUT_MS = 10000;
	private static Integer unnamedImageCounter = 0;
	private static final Map<Class<?>, Class<?>> primitiveMap = new HashMap<>();
	protected static Wrap<? extends Shell> focusedSection;

	static {
		Environment.getEnvironment().setOutput(AbstractLookup.OUTPUT,
				(VERBOSE_JEMMY_LOGGING) ? new TestOut() : TestOut.getNullOutput());
		Environment.getEnvironment().setOutput(AbstractExecutor.NON_QUEUE_ACTION_OUTPUT,
				(VERBOSE_JEMMY_LOGGING) ? new TestOut() : TestOut.getNullOutput());
		Environment.getEnvironment().setOutput(AbstractExecutor.QUEUE_ACTION_OUTPUT,
				(VERBOSE_JEMMY_LOGGING) ? new TestOut() : TestOut.getNullOutput());
		Environment.getEnvironment().setOutput(Wrap.OUTPUT,
				(VERBOSE_JEMMY_LOGGING) ? new TestOut() : TestOut.getNullOutput());
		Environment.getEnvironment().setInputFactory(new AWTRobotInputFactory());
		Environment.getEnvironment().setImageCapturer(new AWTRobotCapturer());
		Environment.getEnvironment().setImageLoader(new FilesystemImageLoader());

		/**
		 * Overriding because on Linux it is wrongly assumed that SPACE does a proper selection
		 * (which is not the case with check or radio style where the menu isn't closed)
		 */
		Environment.getEnvironment().setProperty(KeyboardButton.class, SWTMenu.SELECTION_BUTTON_PROP,
				KeyboardButtons.ENTER);

		if ("sv".equals(InputContext.getInstance().getLocale().getLanguage())) {
			Environment.getEnvironment().setProperty("LANG", "sv");
		}
		AWTImage.setImageRoot(getResultDir());
		AWTImage.setComparator(new AverageDistanceImageComparator());
		Screen.SCREEN.getEnvironment().setInputFactory(new AWTRobotInputFactory());

		OS_NAME = System.getProperty("os.name").toLowerCase();
		SELECTION_BUTTON = OS_NAME.contains("linux") ? KeyboardButtons.SPACE : KeyboardButtons.ENTER;
		EXPAND_BUTTON = OS_NAME.contains("os x") ? KeyboardButtons.RIGHT : KeyboardButtons.ADD;
		COLLAPSE_BUTTON = OS_NAME.contains("os x") ? KeyboardButtons.LEFT : KeyboardButtons.SUBTRACT;
		CLOSE_BUTTON = KeyboardButtons.W;
		SHORTCUT_MODIFIER = OS_NAME.contains("os x") ? KeyboardModifiers.META_DOWN_MASK
				: KeyboardModifiers.CTRL_DOWN_MASK;

		Environment.getEnvironment().setProperty(Boolean.class, SWTMenu.SKIPS_DISABLED_PROP,
				(OS_NAME.contains("windows")) ? false : true);

		// keyboard re-mapping for Mac OS X with Swedish keyboard
		if ("sv".equalsIgnoreCase(InputContext.getInstance().getLocale().getLanguage()) && OS_NAME.contains("os x")) {
			// first making sure that the DefaultCharBindingMap has been loaded and initialized
			getShell().keyboard();
			DefaultCharBindingMap map = (DefaultCharBindingMap) Environment.getEnvironment().getBindingMap();
			map.removeChar('+');
			map.addChar('+', KeyboardButtons.MINUS);
			map.removeChar('-');
			map.addChar('-', KeyboardButtons.SLASH);
			map.removeChar('_');
			map.addChar('_', KeyboardButtons.SLASH, KeyboardModifiers.SHIFT_DOWN_MASK);
			map.removeChar('/');
			map.addChar('/', KeyboardButtons.D7, KeyboardModifiers.SHIFT_DOWN_MASK);
			map.removeChar('\\');
			map.addChar('\\', KeyboardButtons.D7, KeyboardModifiers.SHIFT_DOWN_MASK, KeyboardModifiers.ALT_DOWN_MASK);
			map.removeChar(':');
			map.addChar(':', KeyboardButtons.PERIOD, KeyboardModifiers.SHIFT_DOWN_MASK);
			map.removeChar(';');
			map.addChar(';', KeyboardButtons.COMMA, KeyboardModifiers.SHIFT_DOWN_MASK);
			map.removeChar('~');
			map.addChar('~', KeyboardButtons.CLOSE_BRACKET, KeyboardModifiers.ALT_DOWN_MASK);
			map.removeChar('=');
			map.addChar('=', KeyboardButtons.D0, KeyboardModifiers.SHIFT_DOWN_MASK);
		}

		primitiveMap.put(boolean.class, Boolean.class);
		primitiveMap.put(byte.class, Byte.class);
		primitiveMap.put(char.class, Character.class);
		primitiveMap.put(short.class, Short.class);
		primitiveMap.put(int.class, Integer.class);
		primitiveMap.put(long.class, Long.class);
		primitiveMap.put(float.class, Float.class);
		primitiveMap.put(double.class, Double.class);
	}

	protected static File getResultDir() {
		if (System.getProperty("results.dir") != null) {
			return new File(System.getProperty("results.dir"));
		} else {
			return new File(System.getProperty("user.dir"));
		}
	}

	/**
	 * Gets the main shell of Mission Control
	 *
	 * @return the main shell of Mission Control
	 */
	protected static Wrap<? extends Shell> getShell() {
		return Shells.SHELLS.lookup(Shell.class, new ByTextShell<>("JDK Mission Control")).wrap();
	}

	/**
	 * Tries to set focus on Mission Control
	 */
	public static void focusMc() {
		getShell().as(Focusable.class).focuser().focus();
	}

	/**
	 * Checks if supplied control is identical to our control
	 *
	 * @param otherControl
	 *            the control shall be compared with this control
	 * @return {@code true} if the controls are equal
	 */
	public Boolean controlsAreEqual(Control otherControl) {
		return (otherControl.equals(this.control.getControl()));
	}

	/**
	 * Determines if this control has a specific control as an ancestor or not
	 *
	 * @param possibleAncestor
	 *            the control that should be checked for among the ancestors
	 * @return {@code true} if the control provided as parameter is an ancestor of this control
	 */
	public Boolean hasAsAncestor(MCJemmyBase possibleAncestor) {
		final Control control = this.control.getControl();
		Fetcher<Boolean> fetcher = new Fetcher<Boolean>() {
			@Override
			public void run() {
				Boolean parentFound = false;
				try {
					Composite ancestor = control.getParent();
					while (!(ancestor instanceof Shell)) {
						if (possibleAncestor.controlsAreEqual(ancestor)) {
							parentFound = true;
							break;
						}
						ancestor = ancestor.getParent();
					}
				} catch (SWTException e) {
					suppressWidgetDisposedException(e);
				}
				setOutput(parentFound);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Does a lookup waiting for the editor with the supplied title to show up before returning
	 *
	 * @param title
	 *            the title of the editor
	 * @return {@code true} if the editor was found, otherwise {@code false}
	 */
	public static boolean waitForEditor(String title) {
		return waitForEditor(EDITOR_LOAD_WAIT_TIMEOUT_MS, title);
	}

	/**
	 * Search for an editor with a time limit on retries
	 *
	 * @param maxWait
	 *            the max number of milliseconds to repeat the lookup of the editor (unless found).
	 * @param title
	 *            the title of the editor to find (exact match)
	 * @return {@code true} if the editor was found, otherwise {@code false}
	 */
	public static boolean waitForEditor(long maxWait, String title) {
		return waitForEditor(title, StringComparePolicy.EXACT, maxWait);
	}

	/**
	 * Search for an editor with a retry count
	 *
	 * @param title
	 *            the title of the editor to find (exact match)
	 * @param maxRetries
	 *            The max number of times to retry the lookup of the editor. Each retry might take a
	 *            considerable amount of time so use this wisely.
	 * @return {@code true} if the editor was found, otherwise {@code false}
	 */
	public static boolean waitForEditor(String title, int maxRetries) {
		return waitForEditor(title, StringComparePolicy.EXACT, maxRetries);
	}

	/**
	 * Search for an editor with a retry count
	 *
	 * @param title
	 *            the title of the editor to find
	 * @param policy
	 *            a {@link StringComparePolicy} used for matching the title text
	 * @param maxRetries
	 *            The max number of times to retry the lookup of the editor. Each retry might take a
	 *            considerable amount of time so use this wisely.
	 * @return {@code true} if the editor was found, otherwise {@code false}
	 */
	public static boolean waitForEditor(String title, StringComparePolicy policy, int maxRetries) {
		boolean found = false;
		while (!found && maxRetries > 0) {
			found = isEditorLoadComplete(getEditorLookup(title, policy));
			if (!found) {
				sleep(LOOKUP_SLEEP_TIME_MS);
			}
			maxRetries--;
		}
		return found;
	}

	/**
	 * Search for an editor with a time limit on retries
	 *
	 * @param title
	 *            the title of the editor to find
	 * @param policy
	 *            a {@link StringComparePolicy} used for matching the title with the title text
	 * @param maxWait
	 *            the max number of milliseconds to repeat the lookup of the editor (unless found)
	 * @return {@code true} if the editor was found, otherwise {@code false}
	 */
	public static boolean waitForEditor(String title, StringComparePolicy policy, long maxWait) {
		boolean found = false;
		long maxTimeStamp = System.currentTimeMillis() + maxWait;
		while (!found && System.currentTimeMillis() < maxTimeStamp) {
			found = isEditorLoadComplete(getEditorLookup(title, policy));
			if (!found) {
				sleep(LOOKUP_SLEEP_TIME_MS);
			}
		}
		return found;
	}

	@SuppressWarnings("unchecked")
	private static Lookup<CTabFolder> getEditorLookup(String title, StringComparePolicy policy) {
		return getShell().as(Parent.class, CTabFolder.class).lookup(CTabFolder.class,
				new ByItemLookup<CTabFolder>(title, policy));
	}

	private static boolean isEditorLoadComplete(Lookup<CTabFolder> editorLookup) {
		boolean hasProgressIndicator = false;
		int numOfEditors = editorLookup.size();
		if (numOfEditors == 1) {
			MCTabFolder tf = new MCTabFolder(editorLookup.wrap(), getShell());
			List<MCProgressIndicator> jpis = MCProgressIndicator.getVisible(getShell());
			for (MCProgressIndicator jpi : jpis) {
				try {
					if (jpi.hasAsAncestor(tf)) {
						hasProgressIndicator = true;
						break;
					}
				} catch (SWTException e) {
					suppressWidgetDisposedException(e);
				}
			}
		}
		return numOfEditors == 1 && !hasProgressIndicator;
	}

	/**
	 * Search for an editor with a retry count
	 *
	 * @param title
	 *            the title of the editor to find (substring match)
	 * @param maxRetries
	 *            The max number of times to retry the lookup of the editor. Each retry might take a
	 *            considerable amount of time so use this wisely.
	 * @return {@code true} if found, otherwise {@code false}
	 */
	public static boolean waitForSubstringMatchedEditor(String title, int maxRetries) {
		return waitForEditor(title, StringComparePolicy.SUBSTRING, maxRetries);
	}

	/**
	 * Search for an editor with a time limit on retries
	 *
	 * @param maxWait
	 *            the max number of milliseconds to repeat the lookup of the editor.
	 * @param title
	 *            the title of the editor to find (substring match)
	 * @return {@code true} if found, otherwise {@code false}
	 */
	public static boolean waitForSubstringMatchedEditor(long maxWait, String title) {
		return waitForEditor(title, StringComparePolicy.SUBSTRING, maxWait);
	}

	/**
	 * Search for an editor with a time limit (defined by {@code EDITOR_LOAD_WAIT_TIMEOUT_MS}) on
	 * retries
	 *
	 * @param title
	 *            the title of the editor to find (substring match)
	 * @return {@code true} if found, otherwise {@code false}
	 */
	public static boolean waitForSubstringMatchedEditor(String title) {
		return waitForSubstringMatchedEditor(EDITOR_LOAD_WAIT_TIMEOUT_MS, title);
	}

	/**
	 * Convenience method to put test execution on hold by putting the thread to sleep
	 *
	 * @param millis
	 *            the time to sleep in milliseconds.
	 * @return The actual time slept in milliseconds. Should be equal to millis unless an
	 *         {@link InterruptedException} was thrown.
	 */
	public static long sleep(long millis) {
		long time = System.currentTimeMillis();
		long slept = millis;
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			slept = System.currentTimeMillis() - time;
		}
		return slept;
	}

	/**
	 * This method finds a widget that may or may not exist by means of doing a lookup and then
	 * walking through the results to see if there is a match. The difference to lookups with
	 * specific criterion is that this doesn't timeout and throw an exception if not found. Instead
	 * null is returned. Note: This method expects the property to hold a String or a list of
	 * Strings
	 *
	 * @param clazz
	 *            the class of the widget to look for
	 * @param text
	 *            the string value used for matching
	 * @param property
	 *            the property to match with the supplied text parameter (has to return a String or
	 *            List of String to be properly matched)
	 * @param policy
	 *            a {@link StringComparePolicy} to use when matching the widget
	 * @return the control searched for or {@code null} if not found
	 */
	@SuppressWarnings("unchecked")
	protected static <T> Wrap<? extends T> findWrap(
		Class<T> clazz, String text, String property, StringComparePolicy policy) {
		Lookup<? extends T> lookup = getShell().as(Parent.class, Control.class).lookup(clazz);
		Wrap<? extends T> result = null;
		for (int i = 0; i < lookup.size() && result == null; i++) {
			Wrap<? extends T> wrap = lookup.wrap(i);
			Object object = wrap.getProperty(property);
			if (object instanceof List) {
				List<String> values = new ArrayList<String>().getClass().cast(object);
				for (String value : values) {
					if (policy.compare(value, text)) {
						result = wrap;
						break;
					}
				}
			} else {
				if (policy.compare(String.class.cast(object), text)) {
					result = wrap;
				}
			}
		}
		return result;
	}

	/**
	 * Convenience method to find out if a widget is disposed
	 *
	 * @param widgetWrap
	 *            the wrapped widget to check
	 * @return {@code true} if disposed (or disposing), otherwise {@code false}
	 */
	protected static boolean isDisposed(final Wrap<? extends Widget> widgetWrap) {
		return isDisposed(widgetWrap.getControl());
	}

	/**
	 * Convenience method to find out if a widget is disposed
	 *
	 * @param widget
	 *            the widget to check
	 * @return {@code true} if disposed (or disposing), otherwise {@code false}
	 */
	protected static boolean isDisposed(final Widget widget) {
		Fetcher<Boolean> fetcher = new Fetcher<Boolean>() {
			@Override
			public void run() {
				if (widget != null) {
					setOutput(widget.isDisposed());
				} else {
					setOutput(true);
				}
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Convenience method to find out if the widget of this wrapper is disposed
	 *
	 * @return {@code true} if disposed (or disposing), otherwise {@code false}
	 */
	public boolean isDisposed() {
		return isDisposed(control);
	}

	/**
	 * Convenience method to find out if this control is enabled
	 *
	 * @return {@code true} if enabled, otherwise {@code false}
	 */
	public boolean isEnabled() {
		Fetcher<Boolean> fetcher = new Fetcher<Boolean>() {
			@Override
			public void run() {
				setOutput(control.getControl().isEnabled());
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Convenience method to find out if a control is visible
	 *
	 * @param controlWrap
	 *            the control to check
	 * @return {@code true} if visible, otherwise {@code false}
	 */
	protected static boolean isVisible(final Wrap<? extends Control> controlWrap) {
		Fetcher<Boolean> fetcher = new Fetcher<Boolean>() {
			@Override
			public void run() {
				try {
					setOutput(controlWrap.getControl().isVisible());
				} catch (SWTException e) {
					suppressWidgetDisposedException(e);
				}
			}
		};
		Display.getDefault().syncExec(fetcher);
		return (fetcher.getOutput() == null) ? false : fetcher.getOutput();
	}

	/**
	 * Convenience method to find out if this control is visible
	 *
	 * @return {@code true} if visible, otherwise {@code false}
	 */
	public boolean isVisible() {
		return isVisible(control);
	}

	/**
	 * Iterates through the wraps found by a lookup and returns a list of all visible wraps.
	 *
	 * @param lookup
	 *            the {@link Lookup} to search through
	 * @return a list of all visible wraps found by the lookup
	 */
	protected static <T extends Control> List<Wrap<? extends T>> getVisible(Lookup<T> lookup) {
		return getVisible(lookup, true);
	}

	/**
	 * Iterates through the wraps found by a lookup and returns a list of all visible wraps.
	 *
	 * @param lookup
	 *            the {@link Lookup} to search through
	 * @param waitForIdle
	 *            {@code true} if "UI-update queue" should be empty before looking for controls
	 * @return a list of all visible wraps found by the {@link Lookup}
	 */
	protected static <T extends Control> List<Wrap<? extends T>> getVisible(Lookup<T> lookup, boolean waitForIdle) {
		return getVisible(lookup, waitForIdle, VISIBLE_LOOKUP_TIMEOUT_MS);
	}

	/**
	 * Iterates through the wraps found by a lookup and returns a list of all visible wraps.
	 *
	 * @param lookup
	 *            the {@link Lookup} to search through
	 * @param waitForIdle
	 *            {@code true} if "UI-update queue" should be empty before looking for controls
	 * @param maxWaitMs
	 *            the timeout in milliseconds before ending the lookup
	 * @return a list of all visible wraps found by the {@link Lookup}
	 */
	protected static <T extends Control> List<Wrap<? extends T>> getVisible(
		Lookup<T> lookup, boolean waitForIdle, long maxWaitMs) {
		return getVisible(lookup, waitForIdle, maxWaitMs, true);
	}

	/**
	 * Iterates through the wraps found by a lookup and returns a list of all visible wraps. Will
	 * retry a maximum of {@code VISIBLE_LOOKUP_MAX_RETRY_COUNT} times with a
	 * {@code VISIBLE_LOOKUP_SLEEP_TIME_MS} ms sleep in between if lookup has zero length
	 *
	 * @param lookup
	 *            the {@link Lookup} to search through
	 * @param waitForIdle
	 *            {@code true} if "UI-update queue" should be empty before looking for controls
	 * @param maxWaitMs
	 *            the timeout in milliseconds before ending the lookup
	 * @param assertEmpty
	 *            if {@code true} will assert the the resulting list isn't empty assertion will be
	 *            done
	 * @return a list of all visible wraps found by the {@link Lookup}
	 */
	@SuppressWarnings("unchecked")
	protected static <T extends Control> List<Wrap<? extends T>> getVisible(
		Lookup<T> lookup, boolean waitForIdle, long maxWaitMs, boolean assertEmpty) {
		if (waitForIdle) {
			waitForIdle();
		}
		List<Wrap<? extends T>> list = new ArrayList<>();
		long lookupEndTime = System.currentTimeMillis() + maxWaitMs;
		do {
			for (int i = 0; i < lookup.size(); i++) {
				Wrap<T> wrap = (Wrap<T>) lookup.wrap(i);
				if (isVisible(wrap)) {
					list.add(wrap);
				}
			}
			if (list.size() == 0) {
				sleep(LOOKUP_SLEEP_TIME_MS);
			}
		} while (list.size() == 0 && lookupEndTime > System.currentTimeMillis());
		if (assertEmpty) {
			Assert.assertTrue("No visible controls found", list.size() > 0);
		}
		return list;
	}

	/**
	 * Iterates through the wraps found by a lookup and returns a list of all visible wraps. Will
	 * retry a maximum of {@code VISIBLE_LOOKUP_MAX_RETRY_COUNT} times with a
	 * {@code VISIBLE_LOOKUP_SLEEP_TIME_MS} ms sleep in between if lookup has zero length
	 *
	 * @param lookup
	 *            the {@link Lookup} to search through
	 * @param waitForIdle
	 *            {@code true} if "UI-update queue" should empty before looking for controls
	 * @param assertEmpty
	 *            if {@code true} will assert the the resulting list isn't empty. Otherwise no
	 *            assertion will be done
	 * @return a list of all visible wraps found by the {@link Lookup}
	 */
	protected static <T extends Control> List<Wrap<? extends T>> getVisible(
		Lookup<T> lookup, boolean waitForIdle, boolean assertEmpty) {
		return getVisible(lookup, waitForIdle, VISIBLE_LOOKUP_TIMEOUT_MS, assertEmpty);
	}

	/**
	 * Convenience method to find out if Mission Control UI is busy
	 *
	 * @return {@code true} if busy
	 */
	public static boolean isBusy() {
		Fetcher<Boolean> fetcher = new Fetcher<Boolean>() {
			@Override
			public void run() {
				setOutput(!Job.getJobManager().isIdle());
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Inspects a Widget (graphically) for updates by comparing two snapshots of it
	 *
	 * @param widget
	 *            the widget to inspect for change (graphically)
	 * @param waitTimeMillis
	 *            the time to wait between the snapshots of the Widget
	 * @return {@code true} if the Widget has changed
	 */
	public boolean isWidgetUpdating(Wrap<? extends Widget> widget, int waitTimeMillis) {
		sleep(waitTimeMillis);
		org.jemmy.image.Image firstImage = widget.getScreenImage();
		sleep(waitTimeMillis);
		org.jemmy.image.Image secondImage = widget.getScreenImage();

		// Set the Comparator to be strict, first get and save the current
		// Comparator for later restoral
		ImageComparator current = AWTImage.getComparator();
		AWTImage.setComparator(new StrictImageComparator());
		// diff will be null if the Images are identical
		org.jemmy.image.Image diff = secondImage.compareTo(firstImage);
		AWTImage.setComparator(current);

		return (diff == null) ? false : true;
	}

	/**
	 * Inspects this control for updates by comparing two snapshots of it
	 *
	 * @param waitTimeMillis
	 *            the time to wait between the snapshots of the Widget
	 * @return {@code true} if the Widget has changed
	 */
	public boolean isWidgetUpdating(int waitTimeMillis) {
		return isWidgetUpdating(control, waitTimeMillis);
	}

	/**
	 * Waits for background jobs to finish before executing a script line. Since background jobs
	 * post to the UI-thread asynchronously we must ensure they get a chance to run, so we we spin
	 * asynchronously in a count down process. IDLE_LOOP_COUNT specifies the number iterations
	 * without the job manager being activated as being enough to make sure we are in a calm/idle
	 * state.
	 */
	public static void waitForIdle() {
		int counter = IDLE_LOOP_COUNT;
		long startTimestamp = System.currentTimeMillis();
		while (counter > 0) {
			if ((System.currentTimeMillis() - startTimestamp) > IDLE_LOOP_TIMEOUT_MS) {
				break;
			}
			if (isBusy()) {
				counter = IDLE_LOOP_COUNT;
			} else {
				counter--;
			}
			sleep(IDLE_LOOP_TIME_STEP);
		}
	}

	/**
	 * Saves a picture of Mission Control's shell. If Mission Control is behind other applications,
	 * the rectangle will show what's really on top.
	 *
	 * @param imageName
	 *            the name of the saved file, ".png" will be added at the end
	 */
	public static void saveMcImage(String imageName) {
		Image pic = getMcImage();
		pic.save(imageName + ".png");
	}

	/**
	 * Saves a picture of Mission Control's shell. If Mission Control is behind other applications,
	 * the rectangle will show what's really on top. The image will be given a name which is unique
	 * for the execution.
	 */
	public static void saveMcImage() {
		unnamedImageCounter++;
		saveMcImage("unnamed_mc_image_" + String.format("%03d", unnamedImageCounter) + ".png");
	}

	/**
	 * Returns an image of the Mission Control shell
	 *
	 * @return an {@link Image}
	 */
	public static Image getMcImage() {
		return getShell().getScreenImage();
	}

	/**
	 * Returns an image of this control
	 *
	 * @return an {@link Image} of this control
	 */
	public Image getThisImage() {
		return control.getScreenImage();
	}

	/**
	 * Saves a picture of this control. If the control is behind other applications or Mission
	 * Control controls, the rectangle will show what's really on top.
	 *
	 * @param imageName
	 *            the name of the saved file. ".png" will be added at the end
	 */
	public void saveThisImage(String imageName) {
		Image pic = getThisImage();
		saveImage(imageName + ".png", pic);
	}

	/**
	 * Saves a picture of this control. If the control is behind other applications or Mission
	 * Control controls, the rectangle will show what's really on top. The image will be given a
	 * name which is unique for the execution.
	 */
	public void saveThisImage() {
		unnamedImageCounter++;
		saveThisImage("unnamed_mc_image_" + String.format("%03d", unnamedImageCounter) + ".png");
	}

	/**
	 * Saves the image with the specified name
	 *
	 * @param fileName
	 *            the name of the image file
	 * @param image
	 *            the image
	 */
	public static void saveImage(String fileName, Image image) {
		image.save(fileName);
	}

	/**
	 * Focuses on a specific section to use instead of the main shell.
	 *
	 * @param name
	 *            the name of the section to focus on
	 */
	@SuppressWarnings("unchecked")
	public static void focusSectionByName(String name) {
		focusedSection = (Wrap<? extends Shell>) getVisible(
				getShell().as(Parent.class, Control.class).lookup(new ByName<Shell>(name))).get(0);
	}

	/**
	 * Focuses on a specific section to use instead of the main shell.
	 *
	 * @param title
	 *            the title of the section to focus on
	 */
	@SuppressWarnings("unchecked")
	public static void focusSectionByTitle(String title) {
		focusedSection = (Wrap<? extends Shell>) getVisible(
				getShell().as(Parent.class, Control.class).lookup(new ByTextControlLookup<>(title))).get(0);
	}

	/**
	 * Focuses on a specific section to use instead of the main shell.
	 *
	 * @param title
	 *            the title of the section to focus on
	 * @param waitForIdle
	 *            if {@code true} will first wait for the UI to be idle before setting focus
	 */
	@SuppressWarnings("unchecked")
	public static void focusSectionByTitle(String title, boolean waitForIdle) {
		focusedSection = (Wrap<? extends Shell>) getVisible(
				getShell().as(Parent.class, Control.class).lookup(new ByTextControlLookup<>(title)), waitForIdle)
						.get(0);
	}

	/**
	 * House keeping: clearing the focusedSection reference so that it won't be used in further
	 * lookups
	 */
	public static void clearFocus() {
		focusedSection = null;
	}

	/**
	 * @return a {@link List} of {@link MCTable} either in the currently focused section or
	 *         globally in the shell
	 */
	public static List<MCTable> getTables() {
		if (focusedSection != null) {
			return MCTable.getAll(focusedSection);
		} else {
			return MCTable.getAll(getShell());
		}
	}

	/**
	 * Get all tables in the focused section (if set), otherwise from the Mission Control main shell
	 * 
	 * @param waitForIdle
	 *            {@code true} if "UI-update queue" should be empty before looking for controls
	 * @return a {@link List} of {@link MCTable} either in the currently focused section or
	 *         globally in the shell
	 */
	public static List<MCTable> getTables(boolean waitForIdle) {
		if (focusedSection != null) {
			return MCTable.getAll(focusedSection, waitForIdle);
		} else {
			return MCTable.getAll(getShell(), waitForIdle);
		}
	}

	/**
	 * Runs the method and returns the result if a matching method is found. If not, null will
	 * always be returned. Note that the method could return null as well if the operation succeeds
	 * so this needs to be handled in a proper way by the caller.
	 *
	 * @param returnType
	 *            the type of the returned object
	 * @param object
	 *            the object on which to run the method
	 * @param methodName
	 *            the name of the method to run
	 * @param params
	 *            an object array of parameters for the method. null if no parameters
	 * @return The result of running the method. null if no matching method is found (name,
	 *         parameters and return type). Note that the method could return null as well if the
	 *         operation succeeds so this needs to be handled in a proper way by the caller.
	 */
	public static <T> T runMethod(Class<T> returnType, Object object, String methodName, Object ... params) {
		T result = null;
		try {
			Class<?>[] paramTypes = null;

			if (params != null) {
				paramTypes = new Class<?>[params.length];
				for (int i = 0; i < params.length; i++) {
					paramTypes[i] = params[i].getClass();
				}
			}

			Method method = getCompatibleMethod(object, methodName, paramTypes);
			Class<?> methodReturnType = method.getReturnType();

			if (methodReturnType.isPrimitive()) {
				methodReturnType = primitiveMap.get(methodReturnType);
			}

			if (returnType.equals(Void.class) || returnType.isAssignableFrom(methodReturnType)) {
				result = returnType.cast(method.invoke(object, params));
			}
		} catch (Exception e) {
			// do nothing, just return null
		}
		return result;
	}

	private static Method getCompatibleMethod(Object object, String methodName, Class<?> ... paramTypes)
			throws SecurityException, NoSuchMethodException {
		if (paramTypes != null) {
			Method[] methods = object.getClass().getMethods();
			for (Method method : methods) {
				Method m = method;

				if (!m.getName().equals(methodName)) {
					continue;
				}

				Class<?>[] actualTypes = m.getParameterTypes();
				if (actualTypes.length != paramTypes.length) {
					continue;
				}

				boolean found = true;
				for (int j = 0; j < actualTypes.length; j++) {
					if (!actualTypes[j].isAssignableFrom(paramTypes[j])) {
						if (actualTypes[j].isPrimitive()) {
							found = primitiveMap.get(actualTypes[j]).equals(paramTypes[j]);
						} else if (paramTypes[j].isPrimitive()) {
							found = primitiveMap.get(paramTypes[j]).equals(actualTypes[j]);
						} else {
							found = false;
						}
					}

					if (!found) {
						break;
					}
				}

				if (found) {
					return m;
				}
			}

			throw new NoSuchMethodException("Could not find method " + methodName + " with parameters " + paramTypes);
		} else {
			return object.getClass().getMethod(methodName);
		}
	}

	/**
	 * Returns the name of this control
	 *
	 * @return the name of this control. Null if no name has been set
	 */
	public String getName() {
		return control.getProperty(String.class, Wrap.NAME_PROP_NAME);
	}

	/**
	 * @return the text of this control
	 */
	public String getText() {
		return control.getProperty(String.class, Wrap.TEXT_PROP_NAME);
	}

	/**
	 * Clicks this control (once)
	 */
	public void click() {
		click(1);
	}

	/**
	 * Clicks this control {@code times} times
	 *
	 * @param times
	 *            the number of times to mouse click this control
	 */
	public void click(int times) {
		control.mouse().click(times);
	}

	/**
	 * Default implementation of a copy to clipboard for any wrapper
	 */
	public void copyToClipboard() {
		control.as(Focusable.class).focuser().focus();
		sleep(BETWEEN_KEYSTROKES_SLEEP);
		control.keyboard().pushKey(KeyboardButtons.A, SHORTCUT_MODIFIER);
		sleep(BETWEEN_KEYSTROKES_SLEEP);
		control.keyboard().pushKey(KeyboardButtons.C, SHORTCUT_MODIFIER);
		sleep(BETWEEN_KEYSTROKES_SLEEP);
	}

	/**
	 * Gets the system clipboard contents as a string
	 *
	 * @return the clipboard contents
	 */
	public static String getStringFromClipboard() {
		return getFromClipBoard(String.class);
	}

	/**
	 * Finds out if content assist is present. The assumption is that content assist is present if
	 * there is a Shell with a single Composite that in turn has a single child of type Table
	 *
	 * @return {@code true} if content assist is present, otherwise {@code false}
	 */
	public static boolean isContentAssistPresent() {
		boolean found = true;
		long endTime = System.currentTimeMillis() + 1000;
		while (found && System.currentTimeMillis() < endTime) {
			FetcherWithInput<List<Wrap<? extends Shell>>, Boolean> fetcher = new FetcherWithInput<List<Wrap<? extends Shell>>, Boolean>(
					getVisible(Shells.SHELLS.lookup(Shell.class), false, false)) {

				@Override
				public void run() {
					boolean isPresent = false;
					for (Wrap<? extends Shell> shellWrap : getInput()) {
						Control[] shellChildren = shellWrap.getControl().getChildren();
						if (shellChildren.length == 1 && shellChildren[0] instanceof Composite) {
							Control[] compositeChildren = Composite.class.cast(shellChildren[0]).getChildren();
							if (compositeChildren.length == 1 && compositeChildren[0] instanceof Table) {
								isPresent = true;
								break;
							}
						}
					}
					setOutput(isPresent);
				}
			};
			Display.getDefault().syncExec(fetcher);
			found = fetcher.getOutput();
			if (found) {
				sleep(100);
			}
		}
		return found;
	}

	private static <T> T getFromClipBoard(Class<T> returnType) {
		Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		DataFlavor thisFlavor = new DataFlavor(returnType, returnType.getName());
		try {
			if (transferable != null && transferable.isDataFlavorSupported(thisFlavor)) {
				return returnType.cast(transferable.getTransferData(thisFlavor));
			}
		} catch (UnsupportedFlavorException e) {
			System.out.println("Clipboard content flavor is not supported " + e.getMessage());
		} catch (IOException e) {
			System.out.println("Clipboard content could not be retrieved " + e.getMessage());
		}
		return null;
	}

	private static void suppressWidgetDisposedException(SWTException e) {
		if (!e.getMessage().contains("Widget is disposed")) {
			// Unexpected exception. Re-throw it
			throw e;
		}
	}

	/**
	 * Setting focus on this control programmatically (if not already focused).
	 */
	protected void ensureFocus() {
		Fetcher<Boolean> fetcher = new Fetcher<Boolean>() {
			@Override
			public void run() {
				if (!control.getControl().isFocusControl()) {
					control.getControl().setFocus();
				}
				setOutput(control.getControl().isFocusControl());
			}
		};
		Display.getDefault().syncExec(fetcher);
		if (!fetcher.getOutput()) {
			// fallback if the programmatic focusing didn't work
			control.as(Focusable.class).focuser().focus();
		}
	}
}
