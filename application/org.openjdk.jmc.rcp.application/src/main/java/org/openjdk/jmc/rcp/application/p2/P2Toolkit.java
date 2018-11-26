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
package org.openjdk.jmc.rcp.application.p2;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.openjdk.jmc.rcp.application.ApplicationPlugin;
import org.openjdk.jmc.rcp.application.Messages;

/**
 * Toolkit for managing P2 repositories.
 * 
 */
@SuppressWarnings("restriction")
public final class P2Toolkit {
	public static void addRepositories(IProvisioningAgent agent) {
		for (String site : UpdateSiteURLToolkit.getUpdateSites()) {
			addRepository(agent, site);
		}
	}

	private static void addRepository(IProvisioningAgent agent, String repo) {
		URI repoUri;
		try {
			repoUri = new URI(repo);
		} catch (URISyntaxException e) {
			ApplicationPlugin.getLogger().log(Level.WARNING, "Failed to add update site due to malformed URI", e); //$NON-NLS-1$
			return;
		}

		final ProvisioningUI ui = ProvUIActivator.getDefault().getProvisioningUI();
		IArtifactRepositoryManager artifactManager = ProvUI.getArtifactRepositoryManager(ui.getSession());
		artifactManager.addRepository(repoUri);

		IMetadataRepositoryManager metadataManager = ProvUI.getMetadataRepositoryManager(ui.getSession());
		metadataManager.addRepository(repoUri);

		metadataManager.setRepositoryProperty(repoUri, IRepository.PROP_NICKNAME, Messages.JMC_RCP_P2_UPDATESITE_NICK_NAME);
		artifactManager.setRepositoryProperty(repoUri, IRepository.PROP_NICKNAME, Messages.JMC_RCP_P2_UPDATESITE_NICK_NAME);
	}
}
