/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.openjdk.jmc.rjmx.triggers.actions.internal.Messages;

class TwitterOAuthAunthenticator {

	private static final String OAUTH_REQUEST_TOKEN_URL = "https://api.twitter.com/oauth/request_token";
	private static final String OAUTH_AUTHORIZE_URL = "https://api.twitter.com/oauth/authorize";
	private static final String OAUTH_ACCESS_TOKEN_URL = "https://api.twitter.com/oauth/access_token";
	private static final String REQUEST_METHOD_POST = "POST";
	private static final String OAUTH_SIGNATURE_METHOD = "HMAC-SHA1";
	private static final String SIGNATURE_ALGORITHM = "HmacSHA1";
	private static String proxyHost;
	private static int proxyPort;
	final static Logger LOGGER = Logger.getLogger("TwitterOAuthAunthenticator");

	TwitterOAuthAunthenticator() {

	}

	private String encode(String httpMethod, String url) {
		String encodedUrl = "";
		try {
			encodedUrl = URLEncoder.encode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOGGER.log(Level.SEVERE, Messages.TriggerActionTwitterEncoding_Exception, e);
		}

		return httpMethod.toUpperCase() + "&" + encodedUrl + "&";
	}

	public String getRequestToken(String consumerKey, String consumerKeySecret) {
		try {
			String uuid_string = UUID.randomUUID().toString();
			uuid_string = uuid_string.replaceAll("-", "");
			String oauth_nonce = uuid_string; // any relatively random alphanumeric string will work here. I used UUID minus "-" signs
			String oauth_timestamp = Math.round((new Date()).getTime() / 1000.0) + ""; // get current time in milliseconds, then divide by 1000 to get seconds
			String parameter_string = "oauth_consumer_key=" + consumerKey + "&oauth_nonce=" + oauth_nonce
					+ "&oauth_signature_method=" + OAUTH_SIGNATURE_METHOD + "&oauth_timestamp=" + oauth_timestamp
					+ "&oauth_version=1.0";
			String signature_base_string = encode(REQUEST_METHOD_POST, OAUTH_REQUEST_TOKEN_URL)
					+ URLEncoder.encode(parameter_string, "UTF-8");
			String oauth_signature = "";

			oauth_signature = computeSignature(signature_base_string, consumerKeySecret + "&"); // note the & at the end. Normally the user access_token would go here, but we don't know it yet for request_token

			String authorization_header_string = "OAuth oauth_consumer_key=\"" + consumerKey
					+ "\",oauth_signature_method=\"" + OAUTH_SIGNATURE_METHOD + "\",oauth_timestamp=\""
					+ oauth_timestamp + "\",oauth_nonce=\"" + oauth_nonce
					+ "\",oauth_version=\"1.0\",oauth_signature=\"" + URLEncoder.encode(oauth_signature, "UTF-8")
					+ "\"";

			String oauth_token = "";

			HttpRequest request = HttpRequest.newBuilder().POST(ofFormData(new HashMap<String, String>()))
					.uri(URI.create(OAUTH_REQUEST_TOKEN_URL)).setHeader("Authorization", authorization_header_string)
					.header("Content-Type", "application/x-www-form-urlencoded").build();

			HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
			String responseBody = response.body();

			if (response.statusCode() == 200) {
				oauth_token = responseBody.substring(responseBody.indexOf("oauth_token=") + 12,
						responseBody.indexOf("&oauth_token_secret="));
			}

			return oauth_token;
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, Messages.TriggerActionTwitterRequestToken_Exception, e);
		}
		return null;
	}

	public void authorization(String oauth_token) {

		String url_open = OAUTH_AUTHORIZE_URL + "?oauth_token=" + oauth_token;
		try {
			java.awt.Desktop.getDesktop().browse(java.net.URI.create(url_open));
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, Messages.TriggerActionTwitterAuthorization_Exception, e);
		}
	}

	public AccessToken authentication(
		String consumerKey, String consumerSecret, String oauth_token, String oauth_verifier) {
		try {
			String uuid_string = UUID.randomUUID().toString();
			uuid_string = uuid_string.replaceAll("-", "");
			String oauth_nonce = uuid_string; // any relatively random alphanumeric string will work here. I used UUID minus "-" signs
			String oauth_timestamp = Math.round((new Date()).getTime() / 1000.0) + ""; // get current time in milliseconds, then divide by 1000 to get seconds
			String parameter_string = "oauth_consumer_key=" + consumerKey + "&oauth_nonce=" + oauth_nonce
					+ "&oauth_signature_method=" + OAUTH_SIGNATURE_METHOD + "&oauth_timestamp=" + oauth_timestamp
					+ "&oauth_token=" + oauth_token + "&oauth_verifier=" + oauth_verifier + "&oauth_version=1.0";
			String signature_base_string = encode(REQUEST_METHOD_POST, OAUTH_ACCESS_TOKEN_URL)
					+ URLEncoder.encode(parameter_string, "UTF-8");
			String oauth_signature = "";

			oauth_signature = computeSignature(signature_base_string, consumerSecret); // note the & at the end. Normally the user access_token would go here, but we don't know it yet for request_token

			String authorization_header_string = "OAuth oauth_consumer_key=\"" + consumerKey
					+ "\",oauth_signature_method=\"" + OAUTH_SIGNATURE_METHOD + "\",oauth_timestamp=\""
					+ oauth_timestamp + "\",oauth_token=\"" + oauth_token + "\",oauth_verifier=\"" + oauth_verifier
					+ "\",oauth_nonce=\"" + oauth_nonce + "\",oauth_version=\"1.0\",oauth_signature=\""
					+ URLEncoder.encode(oauth_signature, "UTF-8") + "\"";

			HttpRequest request = HttpRequest.newBuilder().POST(ofFormData(new HashMap<String, String>()))
					.uri(URI.create(OAUTH_ACCESS_TOKEN_URL)).setHeader("Authorization", authorization_header_string)
					.header("Content-Type", "application/x-www-form-urlencoded").build();

			HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
			String responseBody = response.body();
			AccessToken accessToken = null;
			if (response.statusCode() == 200) {
				String final_oauth_token = responseBody.substring(responseBody.indexOf("oauth_token=") + 12,
						responseBody.indexOf("&oauth_token_secret="));
				String final_oauth_token_secret = responseBody
						.substring(responseBody.indexOf("oauth_token_secret=") + 19, responseBody.indexOf("&user_id="));
				String userName = responseBody.substring(responseBody.indexOf("screen_name=") + 12,
						responseBody.length());
				accessToken = new AccessToken(final_oauth_token, final_oauth_token_secret, userName);
			}

			return accessToken;
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, Messages.TriggerActionTwitterAuthentication_Exception, e);
		}
		return null;

	}

	private static void configureProxySettings() {
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

	private static HttpClient getHttpClient() {
		configureProxySettings();
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

	private static String computeSignature(String baseString, String keyString) throws Exception {

		SecretKey secretKey = null;

		byte[] keyBytes = keyString.getBytes();
		secretKey = new SecretKeySpec(keyBytes, SIGNATURE_ALGORITHM);

		Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);

		mac.init(secretKey);

		byte[] text = baseString.getBytes();

		byte[] signatureBytes = mac.doFinal(text);
		return new String(Base64.getEncoder().encode(signatureBytes));
	}

	public static HttpRequest.BodyPublisher ofFormData(Map<String, String> data) {
		StringBuilder builder = new StringBuilder();
		return HttpRequest.BodyPublishers.ofString(builder.toString());
	}
}
