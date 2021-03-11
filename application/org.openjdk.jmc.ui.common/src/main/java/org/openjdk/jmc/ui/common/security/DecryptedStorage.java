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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.openjdk.jmc.common.io.ValidatingObjectInputStream;

class DecryptedStorage {
	private static final String HMAC_ALGORITHM = "HmacSHA1"; //$NON-NLS-1$
	private static final String RANDOM_GEN_ALGORITHM = "SHA1PRNG"; //$NON-NLS-1$
	static final int SALT_LEN = 8;
	private static final int ITERATION_COUNT_MIN = 100000;
	private static final int ITERATION_COUNT_RANGE = 10000;
	private static final String SEP = "_"; //$NON-NLS-1$ Not part of the RFC 2045 Base64 alphabet
	UUID storeId;
	long nextId;
	Map<String, Object> objects;

	DecryptedStorage() {
		objects = new HashMap<>();
		storeId = UUID.randomUUID();
	}

	DecryptedStorage(String encryptedStore, String cipher, String pwd) throws Exception {
		String[] parts = encryptedStore.split(SEP, 4);
		byte[] encodedData = fromBase64(parts[0]);
		String encryptedStoreSign = parts[1];
		byte[] encodedParameter = fromBase64(parts[2]);
		String parameterAlgorithm = parts[3];

		Cipher pbeCipher = Cipher.getInstance(cipher);
		AlgorithmParameters parameters = AlgorithmParameters.getInstance(parameterAlgorithm);
		parameters.init(encodedParameter);
		PBEParameterSpec pbeParamSpec = parameters.getParameterSpec(PBEParameterSpec.class);
		byte[] salt = pbeParamSpec.getSalt();
		int iterationCount = pbeParamSpec.getIterationCount();
		pbeCipher.init(Cipher.DECRYPT_MODE, getKey(cipher, pwd, salt, iterationCount), parameters);
		byte[] decrypted = pbeCipher.doFinal(encodedData);
		if (!calculateSignature(decrypted, pwd).equals(encryptedStoreSign)) {
			throw new SecurityException("Incorrect signature"); //$NON-NLS-1$
		}
		try (ValidatingObjectInputStream ois = ValidatingObjectInputStream.build(new ByteArrayInputStream(decrypted))) {
			nextId = ois.safeReadLong();
			storeId = ois.safeReadObject(UUID.class, null, 1, 100);
			@SuppressWarnings("unchecked")
			// The maxObjects and maxBytes values are reasonably large numbers that should be enough for our use case
			Map<String, Object> objs = ois.safeReadObject(HashMap.class, Arrays.asList(String.class), 100000, 1000000);
			objects = objs;
		}
	}

	String getEncrypted(String cipher, String pwd) throws Exception {
		SecureRandom random = SecureRandom.getInstance(RANDOM_GEN_ALGORITHM, "SUN"); //$NON-NLS-1$
		byte[] salt = new byte[SALT_LEN];
		random.nextBytes(salt);
		int iterationCount = ITERATION_COUNT_MIN + random.nextInt(ITERATION_COUNT_RANGE);
		return getEncrypted(cipher, pwd, salt, iterationCount);
	}

	String getEncrypted(String cipher, String pwd, byte[] salt, int iterationCount) throws Exception {
		PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, iterationCount);
		Cipher pbeCipher = Cipher.getInstance(cipher);
		pbeCipher.init(Cipher.ENCRYPT_MODE, getKey(cipher, pwd, salt, iterationCount), pbeParamSpec);
		byte[] data = asByteArray();
		byte[] encodedData = pbeCipher.doFinal(data);
		AlgorithmParameters param = pbeCipher.getParameters();
		return toBase64(encodedData) + SEP + calculateSignature(data, pwd) + SEP + toBase64(param.getEncoded()) + SEP
				+ param.getAlgorithm(); // Keep algorithm last in case it contains SEP
	}

	private byte[] asByteArray() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeLong(nextId);
		oos.writeObject(storeId);
		oos.writeObject(objects);
		oos.flush();
		return bos.toByteArray();
	}

	private static String calculateSignature(byte[] data, String pwd) throws Exception {
		SecretKeySpec signingKey = new SecretKeySpec(pwd.getBytes(), HMAC_ALGORITHM);
		Mac mac = Mac.getInstance(HMAC_ALGORITHM);
		mac.init(signingKey);
		return toBase64(mac.doFinal(data));
	}

	private static String toBase64(byte[] data) throws UnsupportedEncodingException {
		// The default encoder encodes according to RFC 4648. To use RFC 2045 (with line breaks), use getMimeEncoder()
		return Base64.getEncoder().encodeToString(data);
	}

	private static byte[] fromBase64(String data) throws UnsupportedEncodingException {
		// Note that the decoder you get with getDecoder() can only decode RFC 4648, not RFC 2045 (it stops on line
		// breaks). MimeDecoder works on both but may be slower.
		return Base64.getDecoder().decode(data);
	}

	private static Key getKey(String cipher, String pwd, byte[] salt, int iterationCount) throws Exception {
		PBEKeySpec keySpec = new PBEKeySpec(pwd.toCharArray(), salt, iterationCount);
		SecretKeyFactory keyFac = SecretKeyFactory.getInstance(cipher);
		return keyFac.generateSecret(keySpec);
	}
}
