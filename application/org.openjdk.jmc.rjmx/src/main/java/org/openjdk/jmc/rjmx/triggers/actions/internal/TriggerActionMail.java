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
package org.openjdk.jmc.rjmx.triggers.actions.internal;

import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.preferences.PreferencesKeys;
import org.openjdk.jmc.rjmx.triggers.ITriggerAction;
import org.openjdk.jmc.rjmx.triggers.TriggerAction;
import org.openjdk.jmc.rjmx.triggers.TriggerEvent;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationToolkit;
import org.openjdk.jmc.ui.common.security.PersistentCredentials;
import org.openjdk.jmc.ui.common.security.SecurityException;
import org.openjdk.jmc.ui.common.util.Environment;

/**
 * NotificationAction that emails a certain target the NotificationEvent and the details of what has
 * happened.
 */
public class TriggerActionMail extends TriggerAction {
	public static final String XML_ELEMENT_EMAIL_FROM = "email_from"; //$NON-NLS-1$
	public static final String XML_ELEMENT_EMAIL_TO = "email_to"; //$NON-NLS-1$
	public static final String XML_ELEMENT_EMAIL_CC = "email_cc"; //$NON-NLS-1$

	private static final String MAILER = Messages.TriggerActionMail_MAIL_HEADER_MAILER;

	/**
	 * Constructor.
	 */
	public TriggerActionMail() {
	}

	/**
	 * @throws MessagingException
	 * @see ITriggerAction#handleNotificationEvent(TriggerEvent)
	 */
	@Override
	public void handleNotificationEvent(TriggerEvent e) throws MessagingException {
		String subject = getSubject(e);
		sendEMail(subject, NotificationToolkit.prettyPrint(e));
	}

	/**
	 * Sends an e-mail with the specified subject and content.
	 *
	 * @param subject
	 *            the subject line.
	 * @param content
	 *            the content of the e-mail.
	 * @throws MessagingException
	 */
	public void sendEMail(String subject, String content) throws MessagingException {
		Properties props = new Properties();
		Session session = Session.getInstance(props, null);
		UserPassword credentials = getSmtpCredentials();
		URLName urlName = createURLName(credentials);
		session.setPasswordAuthentication(urlName,
				new PasswordAuthentication(credentials.getUser(), credentials.getPassword()));
		if (Environment.isDebug()) {
			session.setDebug(true);
		}

		Message msg = new MimeMessage(session);
		if (getFrom() != null && getFrom().length() > 0) {
			msg.setFrom(new InternetAddress(getFrom()));
		}
		if (getTo() != null && getTo().length() > 0) {
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(getTo(), false));
		}
		if (getCc() != null && getCc().length() > 0) {
			msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(getCc(), false));
		}
		msg.setSubject(subject);
		msg.setText(content);
		msg.setHeader("X-Mailer", MAILER); //$NON-NLS-1$
		msg.setSentDate(new Date());

		// send the thing off
		Transport transport = session.getTransport(urlName);
		transport.connect();
		transport.sendMessage(msg, msg.getAllRecipients());
		transport.close();
	}

	private URLName createURLName(UserPassword credentials) {
		String protocol = getSmtpSSL() ? "smtps" : "smtp"; //$NON-NLS-1$ //$NON-NLS-2$
		return new URLName(protocol, getSmtpServer(), getSmtpPort(), null, credentials.getUser(),
				credentials.getPassword());
	}

	/**
	 * Gets the smtpServer.
	 *
	 * @return Returns a String
	 */
	private String getSmtpServer() {
		return RJMXPlugin.getDefault().getRJMXPreferences().get(PreferencesKeys.PROPERTY_MAIL_SERVER,
				PreferencesKeys.DEFAULT_MAIL_SERVER);
	}

	/**
	 * @return Secure or default credentials
	 */
	private UserPassword getSmtpCredentials() {
		try {
			UserPassword userPassword = getSecureSmtpCredentials();
			if (userPassword != null) {
				return userPassword;
			}
		} catch (SecurityException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Could not load SMTP credentials", e); //$NON-NLS-1$
		}
		return new UserPassword(PreferencesKeys.DEFAULT_MAIL_SERVER_USER, PreferencesKeys.DEFAULT_MAIL_SERVER_PASSWORD);
	}

	/**
	 * @return A UserPassword object with secure credentials or null if default credentials should
	 *         be used.
	 * @throws SecurityException
	 *             If the secure storage could not be opened
	 */
	private UserPassword getSecureSmtpCredentials() throws SecurityException {
		String key = RJMXPlugin.getDefault().getRJMXPreferences().get(PreferencesKeys.PROPERTY_MAIL_SERVER_CREDENTIALS,
				PreferencesKeys.DEFAULT_MAIL_SERVER_CREDENTIALS);
		if (key != null && !PreferencesKeys.DEFAULT_MAIL_SERVER_CREDENTIALS.equals(key)) {
			PersistentCredentials credentials = new PersistentCredentials(key);
			return new UserPassword(credentials.getUsername(), credentials.getPassword());
		}
		return null;
	}

	private Integer getSmtpPort() {
		return RJMXPlugin.getDefault().getRJMXPreferences().getInt(PreferencesKeys.PROPERTY_MAIL_SERVER_PORT,
				PreferencesKeys.DEFAULT_MAIL_SERVER_PORT);
	}

	private Boolean getSmtpSSL() {
		return RJMXPlugin.getDefault().getRJMXPreferences().getBoolean(PreferencesKeys.PROPERTY_MAIL_SERVER_SECURE,
				PreferencesKeys.DEFAULT_MAIL_SERVER_SECURE);
	}

	/**
	 * Gets the cc.
	 *
	 * @return Returns a String
	 */
	private String getCc() {
		return getSetting(XML_ELEMENT_EMAIL_CC).getString();
	}

	/**
	 * Gets the from.
	 *
	 * @return Returns a String
	 */
	private String getFrom() {
		return getSetting(XML_ELEMENT_EMAIL_FROM).getString();
	}

	/**
	 * Gets the to.
	 *
	 * @return Returns a String
	 */
	private String getTo() {
		return getSetting(XML_ELEMENT_EMAIL_TO).getString(); // return m_to;
	}

	/**
	 * Generated a subject automatically from the action event.
	 *
	 * @param e
	 *            the event to generate the subject line from.
	 * @return String the generated subject line.
	 */
	private String getSubject(TriggerEvent e) {
		if (e.wasTriggered()) {
			return NLS.bind(Messages.TriggerActionMail_SUBJECT_TRIGGERED, e.getRule().getName());
		} else if (e.wasRecovered()) {
			return NLS.bind(Messages.TriggerActionMail_SUBJECT_RECOVERED, e.getRule().getName());
		} else {
			return NLS.bind(Messages.TriggerActionMail_SUBJECT_INVOKED, e.getRule().getName());
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return super.toString() + "{From: " + getFrom() + "\nTo: " + getTo() + "\nSMTP: " + getSmtpServer() + '}'; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private static class UserPassword {
		private final String user;
		private final String password;

		private UserPassword(String user, String password) {
			this.user = user;
			this.password = password;
		}

		public String getUser() {
			return user;
		}

		public String getPassword() {
			return password;
		}
	}

	@Override
	public boolean isReady() {
		// Retrieve SMTP credentials in order to trigger secure storage access
		try {
			getSecureSmtpCredentials();
		} catch (SecurityException e) {
			return false;
		}
		return true;
	}
}
