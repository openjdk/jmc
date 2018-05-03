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
package org.openjdk.jmc.browser.views;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.openjdk.jmc.browser.JVMBrowserPlugin;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.internal.ServerToolkit;
import org.openjdk.jmc.rjmx.servermodel.IDiscoveryInfo;
import org.openjdk.jmc.rjmx.servermodel.IServer;
import org.openjdk.jmc.ui.common.jvm.JVMDescriptor;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.misc.AdaptingLabelProvider;
import org.openjdk.jmc.ui.misc.OverlayImageDescriptor;

public class BrowserLabelProvider extends AdaptingLabelProvider {

	// Only highlight servers found more than 2s after creation
	private final long highlightServersFoundAfter = System.currentTimeMillis() + 2000;
	private final ImageDescriptor connectedOverlay = JVMBrowserPlugin.getDefault()
			.getMCImageDescriptor(JVMBrowserPlugin.ICON_OVERLAY_CONNECTED);
	private final ImageDescriptor disconnectedOverlay = JVMBrowserPlugin.getDefault()
			.getMCImageDescriptor(JVMBrowserPlugin.ICON_OVERLAY_DISCONNECTED);
	private final ImageDescriptor unconnectableOverlay = JVMBrowserPlugin.getDefault()
			.getMCImageDescriptor(JVMBrowserPlugin.ICON_OVERLAY_UNCONNECTABLE);

	private final Image folderImage = PlatformUI.getWorkbench().getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_FOLDER);
	private final Image lockedFolderImage = (Image) getResourceManager().get(new DecorationOverlayIcon(folderImage,
			JVMBrowserPlugin.getDefault().getMCImageDescriptor(JVMBrowserPlugin.ICON_PADLOCK),
			IDecoration.BOTTOM_RIGHT));

	@Override
	public Image getImage(Object obj) {
		if (obj instanceof Folder) {
			return ((Folder) obj).isModifiable() ? folderImage : lockedFolderImage;
		} else if (obj instanceof IServer) {
			IServer server = ((IServer) obj);
			boolean connected = server.getServerHandle().getState() == IServerHandle.State.CONNECTED;
			boolean unconnectable = ServerToolkit.isUnconnectable(server.getServerHandle());
			ImageDescriptor image = AdapterUtil.getAdapter(server, ImageDescriptor.class);
			if (image != null) {
				image = new OverlayImageDescriptor(image, !connected,
						connected ? connectedOverlay : unconnectable ? unconnectableOverlay : disconnectedOverlay);
				return (Image) getResourceManager().get(image);
			}
			return JVMBrowserPlugin.getDefault().getImage(connected ? JVMBrowserPlugin.ICON_CONNECT
					: unconnectable ? JVMBrowserPlugin.ICON_UNCONNECTABLE : JVMBrowserPlugin.ICON_DISCONNECT);
		}
		return super.getImage(obj);
	}

	@Override
	public Font getFont(Object o) {
		if (o instanceof IServer) {
			IDiscoveryInfo di = ((IServer) o).getDiscoveryInfo();
			if (di != null && di.getDiscoveredTimestamp() > highlightServersFoundAfter
					&& (di.getDiscoveredTimestamp() + JVMBrowserView.getHighlightTime()) > System.currentTimeMillis()) {
				return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
			}
		}
		return super.getFont(o);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof Folder) {
			return ((Folder) element).getName();
		} else if (element instanceof IServer) {
			return ((IServer) element).getServerHandle().getServerDescriptor().getDisplayName();
		}
		return super.getText(element);
	}

	@Override
	public String getToolTipText(Object element) {
		if (element instanceof IServer) {
			IServerHandle serverHandle = ((IServer) element).getServerHandle();
			JVMDescriptor jvmInfo = serverHandle.getServerDescriptor().getJvmInfo();
			boolean connected = serverHandle.getState() == IServerHandle.State.CONNECTED;
			boolean unconnectable = ServerToolkit.isUnconnectable(serverHandle);
			if (jvmInfo != null) {
				String tt = buildTooltip(null, Messages.JVMBrowserView_COMMAND_LINE, jvmInfo.getJavaCommand());
				tt = buildTooltip(tt, Messages.JVMBrowserView_JAVA_VERSION, jvmInfo.getJavaVersion());
				tt = buildTooltip(tt, Messages.JVMBrowserView_TOOLTIP_PID,
						jvmInfo.getPid() != null ? String.valueOf(jvmInfo.getPid()) : Messages.JVMBrowserView_UNKNOWN);
				return buildTooltip(tt, Messages.JVMBrowserView_CONNECTION_STATE,
						connected ? Messages.JVMBrowserView_CONNECTION_STATE_CONNECTED
								: unconnectable ? Messages.JVMBrowserView_CONNECTION_STATE_UNCONNECTABLE
										: Messages.JVMBrowserView_CONNECTION_STATE_NOT_CONNECTED);
			}
		}
		return super.getToolTipText(element);
	}

	private static String buildTooltip(String oldString, String title, String value) {
		if (value == null) {
			return oldString;
		}
		String newLine = title + ": " + value; //$NON-NLS-1$
		newLine = newLine.length() > 150 ? newLine.substring(0, 150) + "..." : newLine; //$NON-NLS-1$
		return oldString == null ? newLine : oldString + "\n" + newLine; //$NON-NLS-1$
	}
}
