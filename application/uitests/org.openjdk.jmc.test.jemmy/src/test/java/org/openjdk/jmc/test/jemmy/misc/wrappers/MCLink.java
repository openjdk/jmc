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

import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.jemmy.control.Wrap;
import org.jemmy.interfaces.Parent;
import org.jemmy.resources.StringComparePolicy;
import org.jemmy.swt.lookup.ByTextControlLookup;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;

/**
 * The Jemmy base wrapper for links
 */
public class MCLink extends MCJemmyBase {

	private MCLink(Wrap<? extends Link> link) {
		this.control = link;
	}

	/**
	 * Returns a {@link MCLink} with the supplied text
	 * 
	 * @param text
	 *            the text of the link
	 * @return a {@link MCLink}
	 */
	public static MCLink getByText(String text) {
		return getByText(getShell(), text);
	}

	/**
	 * Returns a {@link MCLink} with the supplied text
	 * 
	 * @param dialog
	 *            the {@link MCDialog} to use as the starting point for the lookup
	 * @param text
	 *            the text of the link
	 * @return a {@link MCLink}
	 */
	public static MCLink getByText(MCDialog dialog, String text) {
		return getByText(dialog.getDialogShell(), text);
	}

	/**
	 * @param shell
	 *            the {@link Wrap} to use as the starting point for the lookup
	 * @param text
	 *            the text of the link
	 * @return a {@link MCLink}
	 */
	public static MCLink getByText(Wrap<? extends Shell> shell, String text) {
		return getByText(shell, text, StringComparePolicy.SUBSTRING);
	}

	/**
	 * @param text
	 *            the text of the link
	 * @param policy
	 *            the {@link StringComparePolicy} to use for the matching of the link text
	 * @return a {@link MCLink}
	 */
	public static MCLink getByText(String text, StringComparePolicy policy) {
		return getByText(getShell(), text, policy);
	}

	/**
	 * @param dialog
	 *            the {@link MCDialog} to use as the starting point for the lookup
	 * @param text
	 *            the text of the link
	 * @param policy
	 *            the {@link StringComparePolicy} to use for the matching of the link text
	 * @return a {@link MCLink}
	 */
	public static MCLink getByText(MCDialog dialog, String text, StringComparePolicy policy) {
		return getByText(dialog.getDialogShell(), text, policy);
	}

	/**
	 * @param shell
	 *            the {@link Wrap} to use as the starting point for the lookup
	 * @param text
	 *            the text of the link
	 * @param policy
	 *            the {@link StringComparePolicy} to use for the matching of the link text
	 * @return a {@link MCLink}
	 */
	@SuppressWarnings("unchecked")
	public static MCLink getByText(Wrap<? extends Shell> shell, String text, StringComparePolicy policy) {
		return new MCLink(
				shell.as(Parent.class, Link.class).lookup(Link.class, new ByTextControlLookup<>(text, policy)).wrap());
	}

	/**
	 * Find out if the link exists
	 * 
	 * @param text
	 *            the text of the link to find
	 * @return {@code true} if a matching link can be found
	 */
	public static boolean exists(String text) {
		return exists(getShell(), text);
	}

	/**
	 * Find out if the link exists
	 * 
	 * @param dialog
	 *            the {@link MCDialog} to use as the starting point for the lookup
	 * @param text
	 *            the text of the link to find
	 * @return {@code true} if a matching link can be found
	 */
	public static boolean exists(MCDialog dialog, String text) {
		return exists(dialog.getDialogShell(), text);
	}

	/**
	 * Find out if the link exists
	 * 
	 * @param shell
	 *            the {@link Wrap} to use as the starting point for the lookup
	 * @param text
	 *            the text of the link to find
	 * @return {@code true} if a matching link can be found
	 */
	public static boolean exists(Wrap<? extends Shell> shell, String text) {
		return exists(shell, text, 0, 20);
	}

	/**
	 * Find out if the link exists
	 * 
	 * @param text
	 *            the text of the link to find
	 * @param fallbackStart
	 *            starting position of the substring of {@code text} when the whole of it doesn't
	 *            match
	 * @param fallbackEnd
	 *            end position of the substring of {@code text} when the whole of it doesn't match
	 * @return {@code true} if a matching link can be found
	 */
	public static boolean exists(String text, int fallbackStart, int fallbackEnd) {
		return exists(getShell(), text, fallbackStart, fallbackEnd);
	}

	/**
	 * Find out if the link exists
	 * 
	 * @param dialog
	 *            the {@link MCDialog} to use as the starting point for the lookup
	 * @param text
	 *            the text of the link to find
	 * @param fallbackStart
	 *            starting position of the substring of {@code text} when the whole of it doesn't
	 *            match
	 * @param fallbackEnd
	 *            end position of the substring of {@code text} when the whole of it doesn't match
	 * @return {@code true} if a matching link can be found
	 */
	public static boolean exists(MCDialog dialog, String text, int fallbackStart, int fallbackEnd) {
		return exists(dialog.getDialogShell(), text, fallbackStart, fallbackEnd);
	}

	/**
	 * Find out if the link exists
	 * 
	 * @param shell
	 *            the {@link Wrap} to use as the starting point for the lookup
	 * @param text
	 *            the text of the link to find
	 * @param fallbackStart
	 *            starting position of the substring of {@code text} when the whole of it doesn't
	 *            match
	 * @param fallbackEnd
	 *            end position of the substring of {@code text} when the whole of it doesn't match
	 * @return {@code true} if a matching link can be found
	 */
	public static boolean exists(Wrap<? extends Shell> shell, String text, int fallbackStart, int fallbackEnd) {
		try {
			getByText(shell, text);
			return true;
		} catch (Exception e) {
			try {
				getByText(shell, text.substring(fallbackStart, fallbackEnd));
				return true;
			} catch (Exception ex) {
				return false;
			}
		}
	}
}
