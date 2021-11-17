/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

import org.openjdk.jmc.rjmx.triggers.TriggerEvent;
import org.openjdk.jmc.rjmx.triggers.actions.internal.Messages;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.MementoToolkit;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;

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

	private static final String UPDATE_STATUS_URL = "https://api.twitter.com/1.1/statuses/update.json";
	private static final String SEND_DIRECT_MESSAGE_URL = "https://api.twitter.com/1.1/direct_messages/events/new.json";
	private static final String GET_USER_ID = "https://api.twitter.com/1.1/users/lookup.json";

	private static TwitterOAuthHeaderGenerator oAuthHeaderGenerator;
	private static TwitterOAuthAunthenticator oauthAuthenticator = new TwitterOAuthAunthenticator();

	// The plug-in ID
	public static final String PLUGIN_ID = "org.openjdk.jmc.twitter"; //$NON-NLS-1$

	final static Logger LOGGER = Logger.getLogger(PLUGIN_ID);

	// The shared instance
	private static TwitterPlugin plugin;

	private String consumerKey;
	private String consumerSecret;
	private static List<Tweeter> tweeters = new ArrayList<>();

	private final IProxyChangeListener proxyListener;
	private IProxyService proxyService;
	private static String proxyHost;
	private static int proxyPort;

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

		configureProxySettings();
	}

	private IProxyService setupProxyService(BundleContext context) {
		IProxyService service = context.getService(context.getServiceReference(IProxyService.class));
		service.addProxyChangeListener(proxyListener);
		return service;
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

	private void configureProxySettings() {
		System.setProperty("java.net.useSystemProxies", "true");
		Proxy proxy;
		try {
			proxy = (Proxy) ProxySelector.getDefault().select(new URI("http://api.twitter.com")).iterator().next();

			InetSocketAddress addr = (InetSocketAddress) proxy.address();
			if (addr != null) {
				if (addr.getHostName() != null) {
					proxyHost = addr.getHostName();
				}
				if (addr.getPort() != -1) {
					proxyPort = addr.getPort();
				} else {
					proxyPort = 80;
				}
			}
		} catch (URISyntaxException e) {
			// Should never happen...
			LOGGER.log(Level.SEVERE, Messages.TriggerActionTwitterURIParsing_Exception, e);
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

	// This method will authorize the twitter user from preference page.
	public RequestToken authorize() {
		String oauth_token = "";
		oauth_token = oauthAuthenticator.getRequestToken(consumerKey, consumerSecret); // Step 1 of 3-legged OAuth Authentication flow
		oauthAuthenticator.authorization(oauth_token); // Step 2 of 3-legged OAuth Authentication flow
		return new RequestToken(oauth_token, "");

	}

	public static Tweeter getAuthorizedTweeter(String username) {
		for (Tweeter tweeter : tweeters) {
			if (username.equals(tweeter.getUsername())) {
				return tweeter;
			}
		}
		return null;
	}

	private static HttpClient getHttpClient() {
		InetSocketAddress addr;
		if (proxyHost != null) {
			addr = new InetSocketAddress(proxyHost, proxyPort);
		} else {
			addr = null;
		}
		HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
				.connectTimeout(Duration.ofSeconds(100)).proxy(ProxySelector.of(addr)).build();

		return httpClient;
	}

	public void updateStatus(String username, String message) throws Exception {
		verifyTweeter(username);
		// form parameters
		Map<String, String> data = new HashMap<String, String>();
		data.put("status", message);

		HttpRequest request = HttpRequest.newBuilder().POST(ofFormData(data)).uri(URI.create(UPDATE_STATUS_URL))
				.setHeader("Authorization", getHeader("POST", UPDATE_STATUS_URL, data))
				.header("Content-Type", "application/x-www-form-urlencoded").build();

		HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			JOptionPane.showMessageDialog(null,
					NLS.bind(Messages.TriggerActionTwitterSendUpdateStatus_ErrorMessage, response.statusCode()));
		}
	}

	// This method will be used to fetch the userid of the direct message recipient.
	// On JMC UI, user will enter the username but direct message API is expecting corresponding userid.
	public static Long getUserId(String username) throws Exception {
		Map<String, String> data = new HashMap<String, String>();
		data.put("screen_name", username);

		HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(GET_USER_ID + "?screen_name=" + username))
				.setHeader("Authorization", getHeader("GET", GET_USER_ID, data))
				.header("Content-Type", "application/x-www-form-urlencoded").build();

		HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() == 200) {
			String responseBody = response.body();
			String userId = responseBody.substring(responseBody.indexOf("id") + 4, responseBody.indexOf("id_str") - 2); // FIXME: We can use some JSON third party library in future.
			return Long.valueOf(userId);
		}
		return Long.valueOf(0);

	}

	public static HttpRequest.BodyPublisher ofFormData(Map<String, String> data) {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<String, String> entry : data.entrySet()) {
			if (builder.length() > 0) {
				builder.append("&");
			}
			builder.append(entry.getKey());
			builder.append("=");
			builder.append(oAuthHeaderGenerator.encode(entry.getValue()));
		}
		return HttpRequest.BodyPublishers.ofString(builder.toString());
	}

	private static AccessToken createAccessToken(Tweeter tweeter) {
		return new AccessToken(tweeter.getToken(), tweeter.getTokenSecret(), tweeter.getUsername());
	}

	// Once the authorization is successful, we will add that tweeter in JMC preference.
	public void addTweeter(RequestToken rt, String pin) {
		AccessToken accessToken;
		if (pin.length() > 0) {
			accessToken = oauthAuthenticator.authentication(consumerKey, consumerSecret, rt.getToken(), pin); // Step 3 of 3-legged OAuth Authentication flow
			if (accessToken != null) {
				String screenName = accessToken.getAccessUserName();
				if (screenName != null && screenName.length() > 0) {
					Tweeter tweeter = new Tweeter(accessToken.getAccessUserName(), accessToken.getAccessToken(),
							accessToken.getAccessTokenSecret());
					if (!tweeters.contains(tweeter)) {
						tweeters.add(tweeter);
					}
					storeAndSavePrefs();
				} else {
					DialogToolkit.showError(null, Messages.TriggerActionTwitterInvalidUser_Title,
							Messages.TriggerActionTwitterInvalidUser_ErrorMessage);
				}
			}
		}
	}

	private void storeAndSavePrefs() {
		storeStateToPreferences();
		try {
			InstanceScope.INSTANCE.getNode(PLUGIN_ID).flush();
		} catch (BackingStoreException e) {
			getLogger().log(Level.WARNING, Messages.TriggerActionTwitterPreferenceStorage_Exception, e);
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

	public Logger getLogger() {
		return LOGGER;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	private void recreateFactory() {
		configureProxySettings();
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
			DialogToolkit.showError(null, Messages.TriggerActionTwitterUnauthorizedUser_Title,
					NLS.bind(Messages.TriggerActionTwitterUnauthorizedUser_ErrorMessage, username));
			return false;
		}
	}

	private void verifyTweeter(String fromUser) throws Exception {
		Tweeter tweeter = getAuthorizedTweeter(fromUser);
		if (tweeter != null) {
			AccessToken at = createAccessToken(tweeter);
			oAuthHeaderGenerator = new TwitterOAuthHeaderGenerator(consumerKey, consumerSecret, at.getAccessToken(),
					at.getAccessTokenSecret());
		} else {
			throw new Exception(NLS.bind(Messages.TriggerActionTwitterVerifyTweeter_ErrorMessage, fromUser));
		}
	}

	public void sendDirectMessage(String from, String to, String message) throws Exception {
		verifyTweeter(from);
		Map<String, String> requestParams = new HashMap<String, String>();
		String json = createJSONObject(getUserId(to), message);
		HttpRequest request = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(json))
				.uri(URI.create(SEND_DIRECT_MESSAGE_URL))
				.setHeader("Authorization", getHeader("POST", SEND_DIRECT_MESSAGE_URL, requestParams))
				.header("Content-Type", "application/json").build();

		HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			JOptionPane.showMessageDialog(null,
					NLS.bind(Messages.TriggerActionTwitterSendDirectMessage_ErrorMessage, response.statusCode()));
		}
	}

	public static String createJSONObject(long recipientId, String text) throws Exception {
		String json = "{\"event\":{\"message_create\":{\"message_data\":{\"text\":\"" + text
				+ "\"},\"target\":{\"recipient_id\":\"" + Long.valueOf(recipientId).toString()
				+ "\"}},\"type\":\"message_create\"}}";
		return json;
	}

	private static String getHeader(String callMethod, String url, Map<String, String> requestParams) {
		return oAuthHeaderGenerator.generateHeader(callMethod, url, requestParams);
	}
}
