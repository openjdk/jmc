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
package org.openjdk.jmc.console.twitter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

import org.openjdk.jmc.rjmx.triggers.TriggerEvent;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.MementoToolkit;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

/**
 * The activator class controls the plug-in life cycle for the Twitter plug-in.-
 */
public class TwitterPlugin extends AbstractUIPlugin {
	private static final String VARIABLE_VALUE = "{value}";
	private static final String VARIABLE_STATE = "{state}";

	private static final String TWEETERS_PREF_KEY = "tweeters";
	private static final String TWEETER_TAG = "tweeter";

	private static final String CONSUMER_KEY_PREF_KEY = "consumer_key";
	private static final String CONSUMER_SECRET_PREF_KEY = "consumer_secret";

	// The plug-in ID
	public static final String PLUGIN_ID = "org.openjdk.jmc.twitter"; //$NON-NLS-1$

	final static Logger LOGGER = Logger.getLogger(PLUGIN_ID);

	// The shared instance
	private static TwitterPlugin plugin;

	private String consumerKey;
	private String consumerSecret;
	private List<Tweeter> tweeters = new ArrayList<>();
	private TwitterFactory twitterFactory;

	private final IProxyChangeListener proxyListener;
	private IProxyService proxyService;

	/**
	 * The constructor
	 */
	public TwitterPlugin() {
		proxyListener = new IProxyChangeListener() {
			@Override
			public void proxyInfoChanged(IProxyChangeEvent event) {
				recreateFactory();
			}
		};
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		proxyService = setupProxyService(context);
		plugin = this;
		consumerKey = InstanceScope.INSTANCE.getNode(PLUGIN_ID).get(CONSUMER_KEY_PREF_KEY, null);
		consumerSecret = InstanceScope.INSTANCE.getNode(PLUGIN_ID).get(CONSUMER_SECRET_PREF_KEY, null);

		IMemento state = MementoToolkit
				.fromString(InstanceScope.INSTANCE.getNode(PLUGIN_ID).get(TWEETERS_PREF_KEY, null));
		if (state != null) {
			for (IMemento tweeter : state.getChildren(TWEETER_TAG)) {
				tweeters.add(new Tweeter(tweeter));
			}
		}
		twitterFactory = createTwitterFactory();
	}

	private IProxyService setupProxyService(BundleContext context) {
		IProxyService service = context.getService(context.getServiceReference(IProxyService.class));
		service.addProxyChangeListener(proxyListener);
		return service;
	}

	private TwitterFactory createTwitterFactory() {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		try {
			IProxyData[] data = proxyService.select(new URI("http://api.twitter.com")); //$NON-NLS-1$
			String host = null;
			int port = -1;
			for (IProxyData proxyData : data) {
				if (proxyData.getHost() != null) {
					host = proxyData.getHost();
					port = proxyData.getPort();
					break;
				}
			}
			if (host != null) {
				cb.setHttpProxyHost(host);
			}
			if (port != -1) {

				cb.setHttpProxyPort(port);
			} else {
				cb.setHttpProxyPort(80);
			}
		} catch (URISyntaxException e) {
			// Should never happen...
			e.printStackTrace();
		}
		return new TwitterFactory(cb.build());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			plugin = null;
			proxyService.removeProxyChangeListener(proxyListener);
			proxyService = null;
			storeStateToPreferences();
		} finally {
			// Implicitly stores preference store.
			super.stop(context);
		}
	}

	private void storeStateToPreferences() {
		XMLMemento tweeterState = XMLMemento.createWriteRoot("root");
		for (Tweeter t : tweeters) {
			t.saveState(tweeterState.createChild(TWEETER_TAG));
		}
		InstanceScope.INSTANCE.getNode(PLUGIN_ID).put(TWEETERS_PREF_KEY, MementoToolkit.asString(tweeterState));
		if (consumerKey != null) {
			InstanceScope.INSTANCE.getNode(PLUGIN_ID).put(CONSUMER_KEY_PREF_KEY, consumerKey);
		}
		if (consumerSecret != null) {
			InstanceScope.INSTANCE.getNode(PLUGIN_ID).put(CONSUMER_SECRET_PREF_KEY, consumerSecret);
		}
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static TwitterPlugin getDefault() {
		return plugin;
	}

	public RequestToken authorize(Twitter twitter) {
		try {
			twitter.setOAuthConsumer(consumerKey, consumerSecret);
		} catch (IllegalStateException e) {
			// Using log level info, since it's probably only since this was done before.
			TwitterPlugin.LOGGER.log(Level.INFO,
					"Could not set OAuth Consumer. Most likely this has already been done before.", e);
		}
		RequestToken requestToken;
		try {
			requestToken = twitter.getOAuthRequestToken();
			Program.launch(requestToken.getAuthorizationURL());
			return requestToken;
		} catch (TwitterException e) {
			TwitterPlugin.LOGGER.log(Level.SEVERE, "Could not request token!", e);
			DialogToolkit.showException(null, "Problem when Authorizing",
					"Problem when trying to connect to Twitter for authorization. Check your network and proxy settings. If you make any changes, a restart may be required. \n\nException message: "
							+ e.getMessage(),
					e);
		}
		return null;
	}

	public Tweeter getAuthorizedTweeter(String username) {
		for (Tweeter tweeter : tweeters) {
			if (username.equals(tweeter.getUsername())) {
				return tweeter;
			}
		}
		return null;
	}

	public void updateStatus(String username, String message) throws TwitterException {
		Tweeter tweeter = getAuthorizedTweeter(username);
		if (tweeter != null) {
			AccessToken at = createAccessToken(tweeter);
			Twitter twitter = twitterFactory.getInstance();
			twitter.setOAuthConsumer(consumerKey, consumerSecret);
			twitter.setOAuthAccessToken(at);
			twitter.updateStatus(message);
			return;
		} else {
			throw new TwitterException(String.format(
					"Attempted to update status for an account not defined in the preferences. Please set up the account (%s) in preferences first.",
					username));
		}
	}

	private AccessToken createAccessToken(Tweeter tweeter) {
		return new AccessToken(tweeter.getToken(), tweeter.getTokenSecret());
	}

	public void addTweeter(Twitter twitter, RequestToken rt, String pin) {
		try {
			AccessToken accessToken;
			if (pin.length() > 0) {
				accessToken = twitter.getOAuthAccessToken(rt, pin);
			} else {
				accessToken = twitter.getOAuthAccessToken();
			}

			String screenName = accessToken.getScreenName();
			if (screenName != null && screenName.length() > 0) {
				Tweeter tweeter = new Tweeter(accessToken.getScreenName(), accessToken.getToken(),
						accessToken.getTokenSecret());
				if (!tweeters.contains(tweeter)) {
					tweeters.add(tweeter);
				}
				storeAndSavePrefs();
			} else {
				DialogToolkit.showError(null, "Invalid User", "The authorized user is invalid.");
			}
			MessageDialog.openInformation(null, "Twitter Authorization",
					"Access credentials created successfully for user " + screenName + ".");
		} catch (TwitterException ex) {
			DialogToolkit.showException(null, "Error storing access token", ex);
		}
	}

	private void storeAndSavePrefs() {
		storeStateToPreferences();
		try {
			InstanceScope.INSTANCE.getNode(PLUGIN_ID).flush();
		} catch (BackingStoreException e) {
			getLogger().log(Level.WARNING, "Failed to store to preferences!", e);
		}
	}

	public void setConsumerKeyAndSecret(String consumerKey, String consumerSecret) {
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		storeAndSavePrefs();
	}

	public void removeTweeter(Tweeter tweeter) {
		tweeters.remove(tweeter);
	}

	public List<Tweeter> getTweeters() {
		return tweeters;
	}

	public Twitter getTwitter() {
		return twitterFactory.getInstance();
	}

	public Logger getLogger() {
		return LOGGER;
	}

	public void sendDirectMessage(String from, String to, String message) throws Exception {
		Tweeter tweeter = getAuthorizedTweeter(from);
		if (tweeter != null) {
			AccessToken at = createAccessToken(tweeter);
			Twitter twitter = twitterFactory.getInstance();
			twitter.setOAuthConsumer(consumerKey, consumerSecret);
			twitter.setOAuthAccessToken(at);
			twitter.sendDirectMessage(to, message);
		} else {
			throw new Exception(String.format(
					"Attempted to send direct message from account not defined in the preferences. Please set up the account (%s) in preferences first.",
					from));
		}
	}

	private void recreateFactory() {
		twitterFactory = createTwitterFactory();
	}

	public static String createMessage(String msg, TriggerEvent event) {
		String result = msg.replace(VARIABLE_STATE, getStateString(event));
		result = result.replace(VARIABLE_VALUE, String.valueOf(event.getTriggerValue()));
		if (result.length() > 140) {
			result = result.substring(0, 137) + "...";
		}
		return result;
	}

	private static String getStateString(TriggerEvent event) {
		return event.wasTriggered() ? "triggered" : "recovered";
	}

	public boolean verifyAuthorizedUser(String username) {
		if (getAuthorizedTweeter(username) != null) {
			return true;
		} else {
			DialogToolkit.showError(null, "Unauthorized Twitter User",
					"User " + username + " has not been authorized in Preferences");
			return false;
		}
	}
}
