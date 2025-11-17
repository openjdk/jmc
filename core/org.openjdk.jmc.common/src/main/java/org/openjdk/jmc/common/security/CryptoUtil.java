/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.common.security;

import java.text.MessageFormat;
import org.openjdk.jmc.common.messages.internal.Messages;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CryptoUtil {

	private static final String ACTION = Messages.getString(Messages.Crypto_ACTION);
	private static final String ATTENTION = Messages.getString(Messages.Crypto_ATTENTION);
	private static final String OK = Messages.getString(Messages.Crypto_OK);

	public static String getCryptoRemark(
		String signatureAlgorithm, String keyType, Long keyLength, IQuantity expiryDate) {

		String remark = "";

		if (signatureAlgorithm.contains("SHA1")) {
			remark = ATTENTION.concat(Messages.getString(Messages.Crypto_SHA1));
		} else if (signatureAlgorithm.contains("MD2")) {
			remark = ACTION.concat(Messages.getString(Messages.Crypto_MD2));
		} else if (signatureAlgorithm.contains("MD5")) {
			remark = ACTION.concat(Messages.getString(Messages.Crypto_MD5));
		}

		if (keyType.contains("RSA")) {
			if (keyLength < 1024) {
				remark = ACTION.concat(Messages.getString(Messages.Crypto_RSA_INSUFFICIENT_KEY_SIZE)).concat(remark);
			} else if (keyLength == 1024) {
				remark = ACTION.concat(Messages.getString(Messages.Crypto_RSA_KEY_SIZE_1024)).concat(remark);
			} else if (keyLength < 2048) {
				remark = remark.concat(ATTENTION).concat(Messages.getString(Messages.Crypto_RSA_KEY_SIZE_LESS_2048));
			}
		}

		if (keyType.contains("DSA")) {
			if (keyLength < 1024) {
				remark = ACTION.concat(Messages.getString(Messages.Crypto_DSA_INSUFFICIENT_KEY_SIZE)).concat(remark);
			} else if (keyLength < 2048) {
				remark = remark.concat(ATTENTION).concat(Messages.getString(Messages.Crypto_DSA_KEY_SIZE_LESS_2048));
			}
		}

		if (keyType.contains("EC")) {
			if (keyLength < 224) {
				remark = ACTION.concat(Messages.getString(Messages.Crypto_EC_INSUFFICIENT_KEY_SIZE)).concat(remark);
			}
		}

		if (expiryDate != null) {

			IQuantity duration = expiryDate.subtract(UnitLookup.EPOCH_MS.quantity(System.currentTimeMillis()));
			long expiringInDays = TimeUnit.MILLISECONDS.toDays(duration.longValue());

			if ((expiringInDays > 0) && (expiringInDays < 90)) {
				remark = remark.concat(ATTENTION).concat(
						MessageFormat.format(Messages.getString(Messages.Crypto_Certificate_Expiring), expiringInDays));
			} else if (expiringInDays < 0) {
				remark = ACTION.concat(MessageFormat.format(Messages.getString(Messages.Crypto_Certificate_Expired),
						(expiringInDays * -1))).concat(remark);
			}

		}

		if (remark.equals("")) {
			remark = OK;
		}

		return remark;
	}

	public static String getCryptoIcon(
		String signatureAlgorithm, String keyType, Long keyLength, IQuantity expiryDate) {

		String icon = "";

		if (signatureAlgorithm.contains("SHA1")) {
			icon = "ATTENTION";
		} else if (signatureAlgorithm.contains("MD2")) {
			icon = "ACTION";
		} else if (signatureAlgorithm.contains("MD5")) {
			icon = "ACTION";
		}

		if (keyType.contains("RSA")) {
			if (keyLength < 1024) {
				icon = "ACTION".concat(icon);
			} else if (keyLength == 1024) {
				icon = "ACTION".concat(icon);
			} else if (keyLength < 2048) {
				icon = icon.concat("ATTENTION");
			}
		}

		if (keyType.contains("DSA")) {
			if (keyLength < 1024) {
				icon = "ACTION".concat(icon);
			} else if (keyLength < 2048) {
				icon = icon.concat("ATTENTION");
			}
		}

		if (keyType.contains("EC")) {
			if (keyLength < 224) {
				icon = "ACTION".concat(icon);
			}
		}

		if (expiryDate != null) {

			IQuantity duration = expiryDate.subtract(UnitLookup.EPOCH_MS.quantity(System.currentTimeMillis()));
			long expiringInDays = TimeUnit.MILLISECONDS.toDays(duration.longValue());

			if ((expiringInDays > 0) && (expiringInDays < 90)) {
				icon = icon.concat("ATTENTION");
			} else if (expiringInDays < 0) {
				icon = "ACTION".concat(icon);
			}

		}

		if (icon.equals("")) {
			icon = "OK";
		}

		return icon;
	}

	public static String getCryptoRuleResult(
		String signatureAlgorithm, String keyType, Long keyLength, IQuantity expiryDate, Number certificateId) {

		List<Map.Entry<String, String>> remarks = new ArrayList<>();
		String strCertificateId = "";
		if (!certificateId.equals(0)) {
			strCertificateId = "Certificate Id : ".concat(certificateId.toString()).concat(" - ");
		}

		if (signatureAlgorithm.contains("SHA1")) {
			remarks.add(Map.entry(ATTENTION, strCertificateId.concat(Messages.getString(Messages.Crypto_SHA1))));
		} else if (signatureAlgorithm.contains("MD2")) {
			remarks.add(Map.entry(ACTION, strCertificateId.concat(Messages.getString(Messages.Crypto_MD2))));
		} else if (signatureAlgorithm.contains("MD5")) {
			remarks.add(Map.entry(ACTION, strCertificateId.concat(Messages.getString(Messages.Crypto_MD5))));
		}

		if (keyType.contains("RSA")) {
			if (keyLength < 1024) {
				remarks.add(Map.entry(ACTION,
						strCertificateId.concat(Messages.getString(Messages.Crypto_RSA_INSUFFICIENT_KEY_SIZE))));
			} else if (keyLength == 1024) {
				remarks.add(Map.entry(ACTION,
						strCertificateId.concat(Messages.getString(Messages.Crypto_RSA_KEY_SIZE_1024))));
			} else if (keyLength < 2048) {
				remarks.add(Map.entry(ATTENTION,
						strCertificateId.concat(Messages.getString(Messages.Crypto_RSA_KEY_SIZE_LESS_2048))));
			}
		}

		if (keyType.contains("DSA")) {
			if (keyLength < 1024) {
				remarks.add(Map.entry(ACTION,
						strCertificateId.concat(Messages.getString(Messages.Crypto_DSA_INSUFFICIENT_KEY_SIZE))));
			} else if (keyLength < 2048) {
				remarks.add(Map.entry(ATTENTION,
						strCertificateId.concat(Messages.getString(Messages.Crypto_DSA_KEY_SIZE_LESS_2048))));
			}
		}

		if (keyType.contains("EC")) {
			if (keyLength < 224) {
				remarks.add(Map.entry(ACTION,
						strCertificateId.concat(Messages.getString(Messages.Crypto_EC_INSUFFICIENT_KEY_SIZE))));
			}
		}

		if (expiryDate != null) {

			IQuantity duration = expiryDate.subtract(UnitLookup.EPOCH_MS.quantity(System.currentTimeMillis()));
			long expiringInDays = TimeUnit.MILLISECONDS.toDays(duration.longValue());

			if ((expiringInDays > 0) && (expiringInDays < 90)) {
				remarks.add(Map.entry(ATTENTION, strCertificateId.concat(MessageFormat
						.format(Messages.getString(Messages.Crypto_Certificate_Expiring), expiringInDays))));
			} else if (expiringInDays < 0) {
				remarks.add(Map.entry(ACTION, strCertificateId.concat(MessageFormat
						.format(Messages.getString(Messages.Crypto_Certificate_Expired), (expiringInDays * -1)))));
			}

		}

		if (remarks.isEmpty()) {
			remarks.add(Map.entry(OK, "Everything is fine"));
		}

		return convertRemarksToFormattedString(remarks);
	}

	public static String convertRemarksToFormattedString(List<Map.Entry<String, String>> entries) {
		Map<String, List<String>> groupedMap = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : entries) {
			groupedMap.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
					.add(entry.getKey().concat(entry.getValue()));
		}

		StringBuilder result = new StringBuilder();

		for (Map.Entry<String, List<String>> entry : groupedMap.entrySet()) {
			for (String value : entry.getValue()) {
				if (value.contains("Everything is fine"))
					continue;
				result.append("   • ").append(value).append("\n");
			}
		}
		return result.toString().trim();
	}

}
