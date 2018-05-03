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

import org.openjdk.jmc.ui.common.jvm.JVMArch;
import org.openjdk.jmc.ui.common.jvm.JVMType;
import org.openjdk.jmc.ui.common.labelingrules.NameConverter.ValueArrayInfo;

/**
 * Variable mapping database.
 */
class Variables {
	// A mapping from variables to IVariableEvaluator.
	private final static HashMap<String, IVariableEvaluator> variables = new HashMap<>();
	private final static Variables instance = new Variables();

	public Variables() {
		initDefaultVariables();
	}

	// Variables are late bound matching variables that (for performance reasons) will be applied to the resulting
	// string just before returning it.
	private void initDefaultVariables() {
		// FIXME: Could possibly be a good idea to combine JVMType and JVMArch into one enum, at least to avoid "[Unknown][Unknown]"
		// This occurs for JDP connections.
		variables.put("StrJVMType", new IVariableEvaluator() { //$NON-NLS-1$
			@Override
			public String evaluate(Object[] input) {
				JVMType type = (JVMType) input[ValueArrayInfo.JVMTYPE.getIndex()];
				String typeStr = getLocalizedTypeStr(type);
				if (typeStr == null) {
					return ""; //$NON-NLS-1$
				}
				return "[" + typeStr + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			}

			private String getLocalizedTypeStr(JVMType type) {
				if (type == JVMType.JROCKIT) {
					return Messages.NameConverter_JVM_TYPE_JROCKIT;
				}
				if (type == JVMType.HOTSPOT) {
					return Messages.NameConverter_JVM_TYPE_HOTSPOT;
				}
				if (type == JVMType.OTHER) {
					return Messages.NameConverter_JVM_TYPE_OTHER;
				}
				return Messages.NameConverter_JVM_TYPE_UNKNOWN;
			}
		});
		variables.put("UnsupportedStrJVMType", new IVariableEvaluator() { //$NON-NLS-1$
			@Override
			public String evaluate(Object[] input) {
				JVMType type = (JVMType) input[ValueArrayInfo.JVMTYPE.getIndex()];
				String typeStr = getLocalizedTypeStr(type);
				if (typeStr == null) {
					return ""; //$NON-NLS-1$
				}
				return "[" + typeStr + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			}

			private String getLocalizedTypeStr(JVMType type) {
				if (type == JVMType.JROCKIT) {
					return Messages.NameConverter_JVM_TYPE_JROCKIT;
				}
				if (type == JVMType.HOTSPOT) {
					return null;
				}
				if (type == JVMType.OTHER) {
					return null;
				}
				return Messages.NameConverter_JVM_TYPE_UNKNOWN;
			}
		});
		variables.put("StrJVMArch", new IVariableEvaluator() { //$NON-NLS-1$
			@Override
			public String evaluate(Object[] input) {
				JVMArch arch = (JVMArch) input[ValueArrayInfo.JVMARCH.getIndex()];
				String archStr = getLocalizedArchStr(arch);
				if (archStr == null) {
					return ""; //$NON-NLS-1$
				}
				return "[" + archStr + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			}

			private String getLocalizedArchStr(JVMArch arch) {
				if (arch == JVMArch.BIT32) {
					return Messages.NameConverter_JVM_ARCH_32BIT;
				}
				if (arch == JVMArch.BIT64) {
					return Messages.NameConverter_JVM_ARCH_64BIT;
				}
				if (arch == JVMArch.OTHER) {
					return Messages.NameConverter_JVM_ARCH_OTHER;
				}
				return Messages.NameConverter_JVM_ARCH_UNKNOWN;
			}
		});
		variables.put("UnsupportedStrJVMArch", new IVariableEvaluator() { //$NON-NLS-1$
			@Override
			public String evaluate(Object[] input) {
				JVMArch arch = (JVMArch) input[ValueArrayInfo.JVMARCH.getIndex()];
				JVMType type = (JVMType) input[ValueArrayInfo.JVMTYPE.getIndex()];
				String archStr = getLocalizedArchStr(arch, type);
				if (archStr == null) {
					return ""; //$NON-NLS-1$
				}
				return "[" + archStr + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			}

			private String getLocalizedArchStr(JVMArch arch, JVMType type) {
				JVMArch thisArch = JVMArch.getCurrentJVMArch();
				if (arch == thisArch) {
					return null;
				}
				if (arch == JVMArch.BIT32) {
					return Messages.NameConverter_JVM_ARCH_32BIT;
				}
				if (arch == JVMArch.BIT64) {
					return Messages.NameConverter_JVM_ARCH_64BIT;
				}
				if (arch == JVMArch.OTHER) {
					return null;
				}
				// Avoid showing double [Unknown]
				return type == JVMType.UNKNOWN ? null : Messages.NameConverter_JVM_ARCH_UNKNOWN;
			}
		});
		variables.put("StrDebug", new IVariableEvaluator() { //$NON-NLS-1$
			@Override
			public String evaluate(Object[] input) {
				return "true".equals(input[ValueArrayInfo.DEBUG.getIndex()]) ? "[" + Messages.NameConverter_DEBUG + "]" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						: ""; //$NON-NLS-1$
			}
		});
		variables.put("StrJDK", new IVariableEvaluator() { //$NON-NLS-1$
			@Override
			public String evaluate(Object[] input) {
				String version = (String) input[ValueArrayInfo.JAVAVERSION.getIndex()];
				return version == null ? "" : "[" + version + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		});
		variables.put("JVMArgs", new IVariableEvaluator() { //$NON-NLS-1$
			@Override
			public String evaluate(Object[] input) {
				return (String) input[ValueArrayInfo.JVMARGS.getIndex()];
			}
		});
	}

	public static Variables getInstance() {
		return instance;
	}

	public boolean containsVariable(String content) {
		return variables.containsKey(content);
	}

	public IVariableEvaluator getVariable(String content) {
		return variables.get(content);
	}
}
