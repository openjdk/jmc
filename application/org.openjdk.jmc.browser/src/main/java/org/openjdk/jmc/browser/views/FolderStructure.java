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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistable;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.rjmx.servermodel.IDiscoveryInfo;
import org.openjdk.jmc.rjmx.servermodel.internal.Server;
import org.openjdk.jmc.rjmx.servermodel.internal.ServerModel;
import org.openjdk.jmc.ui.common.tree.IArray;

public class FolderStructure implements IArray<Object>, IPersistable {
	private final static String EMPTY_FOLDER_MEMENTO_TYPE = "EmptyFolder"; //$NON-NLS-1$
	private final ServerModel model;

	private final Folder userRootFolder = new Folder(null, "UserFolderRoot") { //$NON-NLS-1$
		@Override
		protected void performInsert(Object o, Folder into) {
			if (o instanceof Server) {
				Server s = (Server) o;
				s.setPath(into.getPath(true));
				model.insert(s);
			} else if (o instanceof Folder) {
				Folder f = (Folder) o;
				f.setParent(into);
				FolderVisitor fv = new FolderVisitor();
				f.accept(fv);
				if (into == null) {
					model.remove(fv.getServers());
				} else {
					model.insert(fv.getServers());
				}

			}
		}
	};

	Folder getRootFolder() {
		return userRootFolder;
	}

	public FolderStructure(ServerModel model, IMemento state) {
		this.model = model;
		if (state != null) {
			for (IMemento folder : state.getChildren(EMPTY_FOLDER_MEMENTO_TYPE)) {
				userRootFolder.getFolder(folder.getTextData());
			}
		}
	}

	void addFolder(String name) {
		userRootFolder.getFolder(name);
	}

	@Override
	public boolean isEmpty() {
		return model.isEmpty();
	}

	@Override
	public Object[] elements() {
		userRootFolder.accept(Folder::clearLeafs);
		Map<IDescribable, Folder> discoveredFolders = new HashMap<>();
		for (Server server : model.elements()) {
			IDiscoveryInfo discovery = server.getDiscoveryInfo();
			if (discovery == null) {
				userRootFolder.getFolder(server.getPath()).addLeaf(server);
			} else {
				IDescribable provider = discovery.getProvider();
				Folder providerFolder = discoveredFolders.get(provider);
				if (providerFolder == null) {
					final String desc = provider.getDescription();
					providerFolder = new Folder(null, provider.getName()) {
						@Override
						public boolean isModifiable() {
							return false;
						};

						@Override
						public String getDescription() {
							return desc;
						}
					};
					discoveredFolders.put(provider, providerFolder);
				}
				providerFolder.getFolder(server.getPath()).addLeaf(server);
			}
		}
		Collection<Object> customChildren = userRootFolder.getChildren();
		customChildren.addAll(discoveredFolders.values());
		return customChildren.toArray();
	}

	@Override
	public void saveState(final IMemento memento) {
		// Save all empty folders since they are not in the server model
		userRootFolder.accept(new Consumer<Folder>() {

			@Override
			public void accept(Folder f) {
				if (!f.hasChildren() && f.getParent() != null) {
					memento.createChild(EMPTY_FOLDER_MEMENTO_TYPE).putTextData(f.getPath(true));
				}
			}
		});
	}

	private static class FolderVisitor implements Consumer<Folder> {
		List<Server> servers = new ArrayList<>();

		@Override
		public void accept(Folder f) {
			for (Object o : f.getLeafs()) {
				if (o instanceof Server) {
					Server s = (Server) o;
					s.setPath(f.getPath(true));
					servers.add(s);
				}
			}
		}

		Server[] getServers() {
			Server[] handles = new Server[servers.size()];
			Iterator<Server> it = servers.iterator();
			for (int i = 0; i < handles.length; i++) {
				handles[i] = it.next();
			}
			return handles;
		}
	};
}
