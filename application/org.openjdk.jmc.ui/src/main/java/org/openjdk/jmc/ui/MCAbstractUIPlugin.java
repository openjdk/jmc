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
package org.openjdk.jmc.ui;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.IProgressService;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.Preferences;

/**
 * Base class for Mission Control Plugins
 */
abstract public class MCAbstractUIPlugin extends AbstractUIPlugin {
	public static final String ICON_DIRECTORY = "icons/"; //$NON-NLS-1$
	private static final int HIGH_CONTRAST_CUTOFF = 100;

	final protected Logger m_logger;
	final protected String m_pluginId;
	protected FormColors m_formColors;
	volatile private Vector<String> m_preloadImages = new Vector<>();
	private FormToolkit m_formToolkit;

	public MCAbstractUIPlugin(String pluginId) {
		m_logger = Logger.getLogger(pluginId);
		m_pluginId = pluginId;
		// schedulePreloading();
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		attemptToInitAWT();
	}

	static volatile boolean hasInitiatedAWT;

	/**
	 * If we are in the main thread, and the SWT Display has been created, initialize the AWT native
	 * library, by referencing some method of a suitable AWT class. This is to reduce the
	 * possibility of AWT being initialized on another thread, which has been known to cause
	 * deadlocks on OS X.
	 */
	@SuppressWarnings("serial")
	private void attemptToInitAWT() {
		if (Display.getCurrent() != null) {
			if (!hasInitiatedAWT) {
				getLogger().log(Level.INFO, "Initiating AWT from " + getPluginId() + " activator."); //$NON-NLS-1$ //$NON-NLS-2$
				Color.GREEN.toString();
				hasInitiatedAWT = true;
			}
		} else if (!hasInitiatedAWT) {
			getLogger().log(Level.WARNING, "Not in main thread with SWT Display when activating " + getPluginId(), //$NON-NLS-1$
					new Exception() {
						@Override
						public String toString() {
							return "Diagnostic stack trace"; //$NON-NLS-1$
						}
					});
		}
	}

	public void registerFromImageConstantClass(ImageRegistry registry, Class<?> classWithConstants) {
		for (Field field : classWithConstants.getFields()) {
			String iconName;
			try {
				iconName = (String) field.get(null);
				registerImage(registry, iconName, iconName);
			} catch (IllegalArgumentException e) {
				throw new RuntimeException("Could not access field icon constant class"); //$NON-NLS-1$
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Could not access field icon constant class"); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Return the id of the plugin
	 *
	 * @return the id
	 */
	public String getPluginId() {
		return m_pluginId;
	}

	/**
	 * Accessor method for the logger for this component.
	 *
	 * @return the logger for this component.
	 */
	public Logger getLogger() {
		return m_logger;
	}

	public FormToolkit getFormToolkit() {
		if (m_formToolkit == null) {
			m_formToolkit = new FormToolkit(getFormColors(Display.getCurrent()));
		}
		return m_formToolkit;
	}

	public FormColors getFormColors(Display display) {
		if (m_formColors == null) {
			m_formColors = new FormColors(display);
			m_formColors.markShared();
			RGB BLACK = new RGB(0, 0, 0);
			if (colorDistance(BLACK, m_formColors.getBackground().getRGB()) > HIGH_CONTRAST_CUTOFF) {
				m_formColors.createColor(IFormColors.TITLE, BLACK);
			}
		}
		return m_formColors;
	}

	private int colorDistance(RGB title, RGB background) {
		int r = Math.abs(title.red - background.red);
		int g = Math.abs(title.green - background.green);
		int b = Math.abs(title.blue - background.blue);
		return r + g + b;
	}

	public Image getImage(String key) {
		return getImageRegistry().get(key);
	}

	/**
	 * The benefit of preloading is that image construction doesn't occur when SWT is drawing the
	 * Form the first time. The ui feels somewhat snappier.
	 */
	public void schedulePreloading() {
		Runnable t = new Runnable() {
			@Override
			public void run() {
				try {
					ImageRegistry registry = getImageRegistry();
					for (String key : m_preloadImages) {
						UIPlugin.getDefault().getLogger().log(Level.INFO, "Preloading image" + key); //$NON-NLS-1$
						registry.get(key);
					}
				} catch (Exception t) {
					/*
					 * We want to find out during the development process if there are image path
					 * problems etc. without having to go through the user interface and check all
					 * images(no more call1.GIF).
					 * 
					 * That's why we are making it SEVERE. Images that are not loaded during preload
					 * will be loaded lazily later on.
					 */
					UIPlugin.getDefault().getLogger().log(Level.SEVERE, "Could not preload images", t); //$NON-NLS-1$
				}
			}
		};
		Display display = Display.getDefault();
		if (display != null && !display.isDisposed()) {
			display.asyncExec(t);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			if (m_formColors != null) {
				m_formColors.dispose();
				m_formColors = null;
			}
			if (m_formToolkit != null) {
				m_formToolkit.dispose();
				m_formToolkit = null;
			}
		} finally {
			super.stop(context);
		}
	}

	protected String getImageDescriptorPath(String imageName) {
		return ICON_DIRECTORY + imageName;
	}

	public ImageDescriptor getImageDescriptor(String path) {
		URL url = getBundle().getEntry(path);
		if (url == null) {
			return null;
		} else {
			return getImageDescriptor(url);
		}
	}

	public ImageDescriptor getImageDescriptor(URL url) {
		return ImageDescriptor.createFromURL(url);
	}

	public ImageDescriptor getMCImageDescriptor(String imageName) {
		ImageDescriptor descriptor = getImageDescriptor(getImageDescriptorPath(imageName));
		if (descriptor == null) {
			UIPlugin.getDefault().getLogger().log(Level.SEVERE, "Icon not found " + imageName); //$NON-NLS-1$
		}
		return descriptor;
	}

	public ImageDescriptor getMCImageDescriptor(URL imageUrl) {
		ImageDescriptor descriptor = getImageDescriptor(imageUrl);
		if (descriptor == null) {
			UIPlugin.getDefault().getLogger().log(Level.SEVERE, "Icon not found " + imageUrl); //$NON-NLS-1$
		}
		return descriptor;
	}

	protected void registerImage(ImageRegistry registry, String key, String imageName) {
		try {
			m_preloadImages.add(key);
			if (registry.get(key) != null) {
				getLogger().log(Level.INFO,
						"Warning! ImageDescriptor already in image registry. Current descriptor will be replaced with " //$NON-NLS-1$
								+ imageName);
			}

			registry.put(key, getMCImageDescriptor(imageName));
		} catch (Exception e) {
			getLogger().log(Level.CONFIG, "Could not load icon with file name " + imageName, e); //$NON-NLS-1$
		}
	}

	protected void registerImage(ImageRegistry registry, String key, URL imageUrl) {
		try {
			m_preloadImages.add(key);
			if (registry.get(key) != null) {
				getLogger().log(Level.INFO,
						"Warning! ImageDescriptor already in image registry. Current descriptor will be replaced with " //$NON-NLS-1$
								+ imageUrl);
			}

			registry.put(key, getMCImageDescriptor(imageUrl));
		} catch (Exception e) {
			getLogger().log(Level.CONFIG, "Could not load icon with URL " + imageUrl, e); //$NON-NLS-1$
		}
	}

	public void runProgressTask(
		boolean async, final boolean fork, final boolean canceable, final IRunnableWithProgress runnable) {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
					progressService.run(fork, canceable, runnable);
				} catch (InvocationTargetException e) {
					if (e.getCause() != null) {
						MessageDialog.openError(null, Messages.MCAbstractUIPlugin_ERROR_HAS_OCCURRED_TEXT,
								e.getCause().getMessage());
						getLogger().log(Level.INFO, Messages.MCAbstractUIPlugin_ERROR_RUNNING_TASK_TEXT, e.getCause());
					}
				} catch (InterruptedException e) {
					getLogger().log(Level.INFO, Messages.MCAbstractUIPlugin_PROGRESS_TASK_INTERRUPTED_TEXT, e);
				}
			}
		};
		Display display = Display.getDefault();
		if (async) {
			display.asyncExec(r);
		} else {
			r.run();
		}
	}

	public Preferences getPreferences(String ... path) {
		Preferences node = InstanceScope.INSTANCE.getNode(m_pluginId);
		for (String p : path) {
			node = node.node(p);
		}
		return node;
	}
}
