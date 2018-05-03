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

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Dummy in memory implementation of {@link Preferences} for test. Only put(), get(), putByteArray()
 * and getByteArray() are implemented correctly.
 */
public class DummyPreferences implements Preferences {

	Map<String, String> map = new HashMap<>();
	Map<String, byte[]> byteMap = new HashMap<>();

	@Override
	public void put(String key, String value) {
		map.put(key, value);
	}

	@Override
	public String get(String key, String def) {
		String val = map.get(key);
		return val == null ? def : val;
	}

	@Override
	public void remove(String key) {
		map.remove(key);
		byteMap.remove(key);
	}

	@Override
	public void clear() throws BackingStoreException {
		map.clear();
		byteMap.clear();
	}

	@Override
	public void putInt(String key, int value) {
	}

	@Override
	public int getInt(String key, int def) {
		return 0;
	}

	@Override
	public void putLong(String key, long value) {

	}

	@Override
	public long getLong(String key, long def) {
		return 0;
	}

	@Override
	public void putBoolean(String key, boolean value) {
	}

	@Override
	public boolean getBoolean(String key, boolean def) {
		return false;
	}

	@Override
	public void putFloat(String key, float value) {

	}

	@Override
	public float getFloat(String key, float def) {
		return 0;
	}

	@Override
	public void putDouble(String key, double value) {
	}

	@Override
	public double getDouble(String key, double def) {
		return 0;
	}

	@Override
	public void putByteArray(String key, byte[] value) {
		byteMap.put(key, value);
	}

	@Override
	public byte[] getByteArray(String key, byte[] def) {
		byte[] val = byteMap.get(key);
		return val == null ? def : val;
	}

	@Override
	public String[] keys() throws BackingStoreException {
		return new String[0];
	}

	@Override
	public String[] childrenNames() throws BackingStoreException {
		return new String[0];
	}

	@Override
	public Preferences parent() {
		return this;
	}

	@Override
	public Preferences node(String pathName) {
		return this;
	}

	@Override
	public boolean nodeExists(String pathName) throws BackingStoreException {
		return true;
	}

	@Override
	public void removeNode() throws BackingStoreException {
	}

	@Override
	public String name() {
		return null;
	}

	@Override
	public String absolutePath() {
		return null;
	}

	@Override
	public void flush() throws BackingStoreException {
	}

	@Override
	public void sync() throws BackingStoreException {
	}

}
