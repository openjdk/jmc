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
package org.openjdk.jmc.ui.common.labelingrules;

import java.util.HashMap;

import org.openjdk.jmc.ui.common.labelingrules.NameConverter.ValueArrayInfo;

/**
 * Constant mapping database.
 */
final class Constants {
	// A mapping from constants to their expanded form.
	private final static HashMap<String, Object> constants = new HashMap<>();
	private final static Constants instance = new Constants();

	static Constants getInstance() {
		return instance;
	}

	public Constants() {
		initDefaultConstants();
	}

	private void initDefaultConstants() {
		// setConstant("ThisPID", MessageFormat.format("{0,number,#}", new Object[] {Integer.valueOf(MBeanToolkit.getThisPID())})); //$NON-NLS-1$ //$NON-NLS-2$
		for (ValueArrayInfo info : ValueArrayInfo.values()) {
			setConstant(info.getValueName(), info.getMatchExpression());
		}
		setConstant("StrConstDebug", Messages.NameConverter_DEBUG); //$NON-NLS-1$
		setConstant("StrConstJVMJRockit", Messages.NameConverter_JVM_TYPE_JROCKIT); //$NON-NLS-1$
		setConstant("StrConstJVMUnknown", Messages.NameConverter_JVM_TYPE_UNKNOWN); //$NON-NLS-1$
	}

	/**
	 * Adds a constant to this name converter.
	 *
	 * @param key
	 *            the name of the constant.
	 * @param value
	 *            the value to assign the constant.
	 */
	public void setConstant(String key, Object value) {
		if (value == null) {
			throw new IllegalArgumentException("You may not add a variable which has a value that is null!"); //$NON-NLS-1$
		}
		constants.put(key, value);
	}

	public Object getConstant(String key) {
		return constants.get(key);
	}

	public boolean containsConstant(String key) {
		return constants.containsKey(key);
	}
}
