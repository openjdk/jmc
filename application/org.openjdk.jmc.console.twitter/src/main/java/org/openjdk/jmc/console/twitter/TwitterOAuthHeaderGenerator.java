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

import org.owasp.encoder.Encode;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.openjdk.jmc.rjmx.triggers.actions.internal.Messages;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

class TwitterOAuthHeaderGenerator {

	private String consumerKey;
	private String consumerSecret;
	private String signatureMethod;
	private String token;
	private String tokenSecret;
	private String version;

	final static Logger LOGGER = Logger.getLogger("TwitterOAuthHeaderGenerator");

	public TwitterOAuthHeaderGenerator(String consumerKey, String consumerSecret, String token, String tokenSecret) {
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.token = token;
		this.tokenSecret = tokenSecret;
		this.signatureMethod = "HMAC-SHA1";
		this.version = "1.0";
	}

	private static final String oauth_consumer_key = "oauth_consumer_key";
	private static final String oauth_token = "oauth_token";
	private static final String oauth_signature_method = "oauth_signature_method";
	private static final String oauth_timestamp = "oauth_timestamp";
	private static final String oauth_nonce = "oauth_nonce";
	private static final String oauth_version = "oauth_version";
	private static final String oauth_signature = "oauth_signature";
	private static final String HMAC_SHA1 = "HmacSHA1";

	/**
	 * Generates oAuth 1.0a header which can be passed as Authorization header
	 * 
	 * @param httpMethod
	 * @param url
	 * @param requestParams
	 * @return
	 */
	public String generateHeader(String httpMethod, String url, Map<String, String> requestParams) {
		StringBuilder base = new StringBuilder();
		String nonce = getNonce();
		String timestamp = getTimestamp();
		String baseSignatureString = generateSignatureBaseString(httpMethod, url, requestParams, nonce, timestamp);
		String signature = encryptUsingHmacSHA1(baseSignatureString);
		base.append("OAuth ");
		append(base, oauth_consumer_key, consumerKey);
		append(base, oauth_token, token);
		append(base, oauth_signature_method, signatureMethod);
		append(base, oauth_timestamp, timestamp);
		append(base, oauth_nonce, nonce);
		append(base, oauth_version, version);
		append(base, oauth_signature, signature);
		base.deleteCharAt(base.length() - 1);
		return base.toString();
	}

	/**
	 * Generate base string to generate the oauth_signature
	 * 
	 * @param httpMethod
	 * @param url
	 * @param requestParams
	 * @return
	 */
	private String generateSignatureBaseString(
		String httpMethod, String url, Map<String, String> requestParams, String nonce, String timestamp) {
		Map<String, String> params = new HashMap<>();
		requestParams.entrySet().forEach(entry -> {
			put(params, entry.getKey(), entry.getValue());
		});
		put(params, oauth_consumer_key, consumerKey);
		put(params, oauth_nonce, nonce);
		put(params, oauth_signature_method, signatureMethod);
		put(params, oauth_timestamp, timestamp);
		put(params, oauth_token, token);
		put(params, oauth_version, version);
		Map<String, String> sortedParams = params.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue,
						LinkedHashMap::new));
		StringBuilder base = new StringBuilder();
		sortedParams.entrySet().forEach(entry -> {
			base.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
		});
		base.deleteCharAt(base.length() - 1);
		String baseString = httpMethod.toUpperCase() + "&" + encode(url) + "&" + encode(base.toString());
		return baseString;
	}

	private String encryptUsingHmacSHA1(String input) {
		String secret = new StringBuilder().append(encode(consumerSecret)).append("&").append(encode(tokenSecret))
				.toString();
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		SecretKey key = new SecretKeySpec(keyBytes, HMAC_SHA1);
		Mac mac;
		try {
			mac = Mac.getInstance(HMAC_SHA1);
			mac.init(key);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			LOGGER.log(Level.SEVERE, Messages.TriggerActionTwitterEncryption_Exception, e);
			return null;
		}
		byte[] signatureBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
		return new String(Base64.getEncoder().encode(signatureBytes));
	}

	/**
	 * Percentage encode String as per RFC 3986, Section 2.1
	 * 
	 * @param value
	 * @return
	 */
	public String encode(String value) {
		String encoded = "";
		try {
			encoded = Encode.forUriComponent(value);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, Messages.TriggerActionTwitterEncoding_Exception, e);
		}
		return encoded;
	}

	private void put(Map<String, String> map, String key, String value) {
		map.put(encode(key), encode(value));
	}

	private void append(StringBuilder builder, String key, String value) {
		builder.append(encode(key)).append("=\"").append(encode(value)).append("\",");
	}

	private String getNonce() {
		int leftLimit = 48; // numeral '0'
		int rightLimit = 122; // letter 'z'
		int targetStringLength = 10;
		Random random = new Random();

		return random.ints(leftLimit, rightLimit + 1).filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
				.limit(targetStringLength)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
	}

	private String getTimestamp() {
		return Math.round((new Date()).getTime() / 1000.0) + "";
	}

}
