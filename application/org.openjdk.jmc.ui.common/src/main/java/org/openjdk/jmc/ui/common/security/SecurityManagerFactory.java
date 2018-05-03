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
package org.openjdk.jmc.ui.common.security;

/**
 * This is the global security manager factory for Mission Control. You can only have one
 * SecurityManager, and it is initialized at start. It can not be changed once initialized. The only
 * way to change security manager is to set the system property
 * org.openjdk.jmc.rjmx.security.manager=&lt;class&gt; before this factory class is instantiated. The
 * class must implement ISecurityManager, and it must have a default constructor.
 */
public final class SecurityManagerFactory {

	private static ISecurityManager instance;

	static {
		String className = System.getProperty("org.openjdk.jmc.common.security.manager"); //$NON-NLS-1$
		try {
			if (className != null) {
				Class<? extends Object> c = Class.forName(className);
				instance = (ISecurityManager) c.newInstance();
			}
		} catch (Exception e) {
			System.out.println("Could not create Security manager for className. Using default! Exception was:"); //$NON-NLS-1$
			e.printStackTrace();
		}
	}

	public synchronized final static void setDefaultSecurityManager(ISecurityManager manager) {
		if (instance == null) {
			instance = manager;
		}
	}

	public synchronized final static ISecurityManager getSecurityManager() {
		return instance;
	}

	private SecurityManagerFactory() {
		throw new AssertionError("This class is not to be instantiated!"); //$NON-NLS-1$
	}

}
