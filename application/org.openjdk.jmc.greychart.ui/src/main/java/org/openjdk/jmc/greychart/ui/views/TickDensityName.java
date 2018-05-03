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
package org.openjdk.jmc.greychart.ui.views;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openjdk.jmc.greychart.ui.messages.internal.Messages;

import org.openjdk.jmc.greychart.TickDensity;

/**
 * Class responsible for mapping the TickDensity enum to strings that can be used in a configuration
 * UI
 */
public enum TickDensityName {

	VARIABLE("variable", Messages.TICK_DENSITY_NAME_VARIABLE), //$NON-NLS-1$
	VERY_DENSE("veryDense", Messages.TICK_DENSITY_NAME_VERY_DENSE), //$NON-NLS-1$
	DENSE("dense", Messages.TICK_DENSITY_NAME_DENSE), //$NON-NLS-1$
	NORMAL("normal", Messages.TICK_DENSITY_NAME_NORMAL), //$NON-NLS-1$
	SPARSE("sparse", Messages.TICK_DENSITY_NAME_SPARSE), //$NON-NLS-1$
	VERY_SPARSE("verySparse", Messages.TICK_DENSITY_NAME_VERY_SPARSE); //$NON-NLS-1$

	final String key;
	private final String readableName;

	TickDensityName(String key, String name) {
		this.key = key;
		readableName = name;
	}

	public static TickDensityName toObject(String key) {
		for (TickDensityName s : values()) {
			if (s.key.equals(key)) {
				return s;
			}
		}
		return VARIABLE;
	}

	public static String getReadableName(TickDensity density) {
		for (TickDensityName tickDensityName : values()) {
			if (toDensity(tickDensityName).equals(density)) {
				return tickDensityName.readableName;
			}
		}
		return VARIABLE.readableName;
	}

	public static TickDensity toDensity(String key) {
		return toDensity(toObject(key));
	}

	public static TickDensity toDensity(TickDensityName tickDensityName) {
		switch (tickDensityName) {
		case VERY_DENSE:
			return TickDensity.VERY_DENSE;
		case DENSE:
			return TickDensity.DENSE;
		case NORMAL:
			return TickDensity.NORMAL;
		case SPARSE:
			return TickDensity.SPARSE;
		case VERY_SPARSE:
			return TickDensity.VERY_SPARSE;
		case VARIABLE:
		default:
			return TickDensity.VARIABLE;
		}
	}

	public static Map<String, String> createNameKeyMap() {
		Map<String, String> styleMap = new LinkedHashMap<>();
		styleMap.put(TickDensityName.VARIABLE.key, TickDensityName.VARIABLE.readableName);
		styleMap.put(TickDensityName.VERY_DENSE.key, TickDensityName.VERY_DENSE.readableName);
		styleMap.put(TickDensityName.DENSE.key, TickDensityName.DENSE.readableName);
		styleMap.put(TickDensityName.NORMAL.key, TickDensityName.NORMAL.readableName);
		styleMap.put(TickDensityName.SPARSE.key, TickDensityName.SPARSE.readableName);
		styleMap.put(TickDensityName.VERY_SPARSE.key, TickDensityName.VERY_SPARSE.readableName);
		return styleMap;
	}

}
