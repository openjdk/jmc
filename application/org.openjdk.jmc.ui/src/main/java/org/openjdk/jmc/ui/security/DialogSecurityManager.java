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
package org.openjdk.jmc.ui.security;

import java.util.Set;
import java.util.logging.Level;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.common.security.ActionNotGrantedException;
import org.openjdk.jmc.ui.common.security.FailedToSaveException;
import org.openjdk.jmc.ui.common.security.ISecurityManager;
import org.openjdk.jmc.ui.common.security.SecureStore;
import org.openjdk.jmc.ui.common.security.SecurityException;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.wizards.OnePageWizardDialog;

public class DialogSecurityManager implements ISecurityManager {

	private final SecureStore secureStore = SecureStore.createDefault();

	public void showUi(final boolean replacePwd) throws ActionNotGrantedException {
		final boolean[] accessNotGranted = new boolean[] {false};
		DisplayToolkit.safeSyncExec(new Runnable() {
			@Override
			public void run() {
				Shell shell = Display.getCurrent().getActiveShell();
				if (!secureStore.isPersistable()) {
					MessageDialog.openError(shell, Messages.SecurityDialogs_NO_CIPHER_AVAILABLE_TEXT,
							Messages.SecurityDialogs_SECURE_STORE_WILL_NOT_BE_ABLE_TO_SAVE);
					return;
				}
				if (secureStore.isEncrypted()) {
					String cipher = secureStore.getEncryptionCipher();
					if (SecureStore.ENCRYPTION_CIPHERS.contains(cipher)) {
						while (true) {
							try {
								String pwd = getPassword(shell, false, false);
								if (pwd != null) {
									secureStore.initialize(pwd);
								}
								break;
							} catch (Exception e) {
								UIPlugin.getDefault().getLogger().log(Level.FINE, "Could not decrypt store"); //$NON-NLS-1$
								// Wrong password
							}
						}
					} else {
						if (!MessageDialog.openQuestion(shell,
								NLS.bind(Messages.SecurityDialogs_CIPHER_NOT_AVAILABLE_TITLE, '\'' + cipher + '\''),
								Messages.SecurityDialogs_CIPHER_NOT_AVAILABLE_TEXT)) {
							accessNotGranted[0] = true;
							return;
						}
					}
				}
				if (!secureStore.isInitialized() || replacePwd) {
					String pwd = getPassword(shell, true, secureStore.isEncrypted());
					if (pwd == null) {
						accessNotGranted[0] = true;
						return;
					} else {
						secureStore.initialize();
						try {
							secureStore.setPassword(pwd);
						} catch (FailedToSaveException e) {
							MessageDialog.openError(shell, Messages.SecurityDialogs_FAILED_TO_SAVE,
									Messages.SecurityDialogs_SECURE_STORE_WILL_NOT_BE_ABLE_TO_SAVE);
						}
					}
				}
			}
		});
		if (accessNotGranted[0]) {
			throw new ActionNotGrantedException();
		}
	}

	@Override
	public Object withdraw(String key) throws SecurityException {
		return secureStore.remove(key);
	}

	@Override
	public void clearFamily(String family, Set<String> keys) throws FailedToSaveException {
		secureStore.clearFamily(family, keys);
	}

	@Override
	public Object get(String key) throws SecurityException {
		return hasKey(key) ? getInitialised().get(key) : null;
	}

	@Override
	public String store(byte ... value) throws SecurityException {
		return getInitialised().insert(null, true, value);
	}

	@Override
	public String store(String ... value) throws SecurityException {
		return getInitialised().insert(null, true, value);
	}

	@Override
	public String storeInFamily(String family, byte ... value) throws SecurityException {
		return getInitialised().insert(family, true, value);
	}

	@Override
	public String storeInFamily(String family, String ... value) throws SecurityException {
		return getInitialised().insert(family, true, value);
	}

	@Override
	public void storeWithKey(String key, byte ... value) throws SecurityException {
		getInitialised().insert(key, false, value);
	}

	@Override
	public void storeWithKey(String key, String ... value) throws SecurityException {
		getInitialised().insert(key, false, value);
	}

	@Override
	public Set<String> getEncryptionCiphers() {
		return SecureStore.ENCRYPTION_CIPHERS;
	}

	@Override
	public String getEncryptionCipher() {
		return secureStore.getEncryptionCipher();
	}

	@Override
	public void setEncryptionCipher(String encryptionCipher) throws SecurityException {
		getInitialised().setEncryptionCipher(encryptionCipher);
	}

	@Override
	public void changeMasterPassword() throws SecurityException {
		showUi(true);
	}

	private SecureStore getInitialised() throws ActionNotGrantedException {
		if (!secureStore.isInitialized()) {
			showUi(false);
		}
		return secureStore;
	}

	private String getPassword(Shell shell, boolean usePasswordVerification, boolean warnForDataClear) {
		MasterPasswordWizardPage page = new MasterPasswordWizardPage(usePasswordVerification, warnForDataClear);
		OnePageWizardDialog owp = new OnePageWizardDialog(shell, page,
				JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
		owp.open();
		return page.getMasterPassword();
	}

	@Override
	public boolean hasKey(String key) {
		return secureStore.hasKey(key);
	}

	@Override
	public boolean isLocked() {
		return !secureStore.isInitialized();
	}

	@Override
	public void unlock() throws ActionNotGrantedException {
		getInitialised();
	}

}
