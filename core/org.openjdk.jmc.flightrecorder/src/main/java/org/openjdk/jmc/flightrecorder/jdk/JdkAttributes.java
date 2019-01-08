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
package org.openjdk.jmc.flightrecorder.jdk;

import static org.openjdk.jmc.common.item.Attribute.attr;
import static org.openjdk.jmc.common.unit.UnitLookup.ADDRESS;
import static org.openjdk.jmc.common.unit.UnitLookup.CLASS;
import static org.openjdk.jmc.common.unit.UnitLookup.CLASS_LOADER;
import static org.openjdk.jmc.common.unit.UnitLookup.FLAG;
import static org.openjdk.jmc.common.unit.UnitLookup.LABELED_IDENTIFIER;
import static org.openjdk.jmc.common.unit.UnitLookup.MEMORY;
import static org.openjdk.jmc.common.unit.UnitLookup.METHOD;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.OLD_OBJECT;
import static org.openjdk.jmc.common.unit.UnitLookup.PERCENTAGE;
import static org.openjdk.jmc.common.unit.UnitLookup.PLAIN_TEXT;
import static org.openjdk.jmc.common.unit.UnitLookup.THREAD;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESPAN;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESTAMP;
import static org.openjdk.jmc.common.unit.UnitLookup.UNKNOWN;

import java.util.List;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCClassLoader;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCModule;
import org.openjdk.jmc.common.IMCOldObject;
import org.openjdk.jmc.common.IMCOldObjectGcRoot;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IMCThreadGroup;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.common.util.LabeledIdentifier;
import org.openjdk.jmc.common.util.MCClassLoader;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.messages.internal.Messages;

/**
 * Various attributes based on JDK flight recorder data.
 */
public final class JdkAttributes {

	public static final IAttribute<String> EVENT_THREAD_NAME = Attribute
			.canonicalize(new Attribute<String>("(thread).name", Messages.getString(Messages.ATTR_EVENT_THREAD_NAME), //$NON-NLS-1$
					Messages.getString(Messages.ATTR_EVENT_THREAD_NAME_DESC), PLAIN_TEXT) {
				@Override
				public <U> IMemberAccessor<String, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCThread, U> accessor = JfrAttributes.EVENT_THREAD.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<String, U>() {
						@Override
						public String getMember(U i) {
							IMCThread thread = accessor.getMember(i);
							return thread == null ? null : thread.getThreadName();
						}
					};
				}
			});
	public static final IAttribute<String> EVENT_THREAD_GROUP_NAME = Attribute.canonicalize(
			new Attribute<String>("(thread).groupName", Messages.getString(Messages.ATTR_EVENT_THREAD_GROUP), //$NON-NLS-1$
					Messages.getString(Messages.ATTR_EVENT_THREAD_GROUP_DESC), PLAIN_TEXT) {
				@Override
				public <U> IMemberAccessor<String, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCThread, U> accessor = JfrAttributes.EVENT_THREAD.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<String, U>() {
						@Override
						public String getMember(U i) {
							IMCThread thread = accessor.getMember(i);
							if (thread != null) {
								IMCThreadGroup group = thread.getThreadGroup();
								if (group != null) {
									return group.getName();
								}
							}
							return null;
						}
					};
				}
			});
	public static final IAttribute<IQuantity> EVENT_THREAD_ID = Attribute.canonicalize(
			new Attribute<IQuantity>("(thread).javaThreadId", Messages.getString(Messages.ATTR_EVENT_THREAD_ID), //$NON-NLS-1$
					Messages.getString(Messages.ATTR_EVENT_THREAD_ID_DESC), NUMBER) {
				@Override
				public <U> IMemberAccessor<IQuantity, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCThread, U> accessor = JfrAttributes.EVENT_THREAD.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<IQuantity, U>() {
						@Override
						public IQuantity getMember(U i) {
							IMCThread thread = accessor.getMember(i);
							return (thread == null || thread.getThreadId() == null) ? null
									: UnitLookup.NUMBER_UNITY.quantity(thread.getThreadId());
						}
					};
				}
			});

	public static final IAttribute<String> STACK_TRACE_STRING = Attribute.canonicalize(
			new Attribute<String>("(stackTrace).string", Messages.getString(Messages.ATTR_STACK_TRACE_STRING), //$NON-NLS-1$
					Messages.getString(Messages.ATTR_STACK_TRACE_STRING_DESC), UnitLookup.PLAIN_TEXT) {
				@Override
				public <U> IMemberAccessor<String, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCStackTrace, U> accessor = JfrAttributes.EVENT_STACKTRACE.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<String, U>() {
						@Override
						public String getMember(U i) {
							IMCStackTrace trace = accessor.getMember(i);
							// Using default formatting
							return trace == null ? null : FormatToolkit.getHumanReadable(trace, "", "", null); //$NON-NLS-1$ //$NON-NLS-2$
						}
					};
				}
			});

	public static final IAttribute<IMCFrame> STACK_TRACE_TOP_FRAME = Attribute.canonicalize(
			new Attribute<IMCFrame>("(stackTrace).topframe", Messages.getString(Messages.ATTR_STACK_TRACE_FRAME), //$NON-NLS-1$
					Messages.getString(Messages.ATTR_STACK_TRACE_FRAME_DESC), UnitLookup.STACKTRACE_FRAME) {
				@Override
				public <U> IMemberAccessor<IMCFrame, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCStackTrace, U> accessor = JfrAttributes.EVENT_STACKTRACE.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<IMCFrame, U>() {
						@Override
						public IMCFrame getMember(U i) {
							IMCStackTrace trace = accessor.getMember(i);
							// FIXME: Fix train wreck to avoid NPEs and IndexOutOfBoundsException
							return trace == null || trace.getFrames().isEmpty() ? null : trace.getFrames().get(0);
						}
					};
				}
			});

	public static final IAttribute<String> STACK_TRACE_TOP_PACKAGE = Attribute.canonicalize(
			new Attribute<String>("(stackTrace).topPackage", Messages.getString(Messages.ATTR_STACK_TRACE_PACKAGE), //$NON-NLS-1$
					Messages.getString(Messages.ATTR_STACK_TRACE_PACKAGE_DESC), PLAIN_TEXT) {
				@Override
				public <U> IMemberAccessor<String, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCType, U> accessor = STACK_TRACE_TOP_CLASS.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<String, U>() {
						@Override
						public String getMember(U i) {
							IMCType type = accessor.getMember(i);
							return type == null ? null : FormatToolkit.getPackage(type.getPackage());
						}
					};
				}
			});

	public static final IAttribute<IMCType> STACK_TRACE_TOP_CLASS = Attribute.canonicalize(
			new Attribute<IMCType>("(stackTrace).topClass", Messages.getString(Messages.ATTR_STACK_TRACE_CLASS), //$NON-NLS-1$
					Messages.getString(Messages.ATTR_STACK_TRACE_CLASS_DESC), CLASS) {
				@Override
				public <U> IMemberAccessor<IMCType, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCMethod, U> accessor = STACK_TRACE_TOP_METHOD.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<IMCType, U>() {
						@Override
						public IMCType getMember(U i) {
							IMCMethod method = accessor.getMember(i);
							return method == null ? null : method.getType();
						}
					};
				}
			});
	public static final IAttribute<String> STACK_TRACE_TOP_CLASS_STRING = Attribute.canonicalize(
			new Attribute<String>("(stackTrace).topClass.string", Messages.getString(Messages.ATTR_STACK_TRACE_CLASS), //$NON-NLS-1$
					Messages.getString(Messages.ATTR_STACK_TRACE_CLASS_DESC), PLAIN_TEXT) {
				@Override
				public <U> IMemberAccessor<String, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCType, U> accessor = STACK_TRACE_TOP_CLASS.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<String, U>() {
						@Override
						public String getMember(U i) {
							IMCType type = accessor.getMember(i);
							return type == null ? null : type.getFullName();
						}
					};
				}
			});
	public static final IAttribute<IMCMethod> STACK_TRACE_TOP_METHOD = Attribute.canonicalize(
			new Attribute<IMCMethod>("(stackTrace).topMethod", Messages.getString(Messages.ATTR_STACK_TRACE_METHOD), //$NON-NLS-1$
					Messages.getString(Messages.ATTR_STACK_TRACE_METHOD_DESC), UnitLookup.METHOD) {
				@Override
				public <U> IMemberAccessor<IMCMethod, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCFrame, U> accessor = STACK_TRACE_TOP_FRAME.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<IMCMethod, U>() {
						@Override
						public IMCMethod getMember(U i) {
							IMCFrame frame = accessor.getMember(i);
							return frame == null ? null : frame.getMethod();
						}
					};
				}
			});
	public static final IAttribute<String> STACK_TRACE_TOP_METHOD_STRING = Attribute.canonicalize(
			new Attribute<String>("(stackTrace).topMethodString", Messages.getString(Messages.ATTR_STACK_TRACE_METHOD), //$NON-NLS-1$
					Messages.getString(Messages.ATTR_STACK_TRACE_METHOD_DESC), UnitLookup.PLAIN_TEXT) {
				@Override
				public <U> IMemberAccessor<String, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCMethod, U> accessor = STACK_TRACE_TOP_METHOD.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<String, U>() {
						@Override
						public String getMember(U i) {
							IMCMethod method = accessor.getMember(i);
							return method == null ? null : FormatToolkit.getHumanReadable(method);
						}
					};
				}
			});

	public static final IAttribute<IMCFrame> STACK_TRACE_BOTTOM_FRAME = Attribute
			.canonicalize(new Attribute<IMCFrame>("(stackTrace).bottomFrame", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_STACK_TRACE_BOTTOM_FRAME),
					Messages.getString(Messages.ATTR_STACK_TRACE_BOTTOM_FRAME_DESC), UnitLookup.STACKTRACE_FRAME) {
				@Override
				public <U> IMemberAccessor<IMCFrame, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCStackTrace, U> accessor = JfrAttributes.EVENT_STACKTRACE.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<IMCFrame, U>() {
						@Override
						public IMCFrame getMember(U i) {
							IMCStackTrace trace = accessor.getMember(i);
							if (trace != null) {
								List<? extends IMCFrame> frames = trace.getFrames();
								return frames == null || frames.size() == 0 ? null : frames.get(frames.size() - 1);
							}
							return null;
						}
					};
				}
			});

	public static final IAttribute<IMCMethod> STACK_TRACE_BOTTOM_METHOD = Attribute
			.canonicalize(new Attribute<IMCMethod>("(stackTrace).bottomMethod", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_STACK_TRACE_BOTTOM_METHOD),
					Messages.getString(Messages.ATTR_STACK_TRACE_BOTTOM_METHOD_DESC), UnitLookup.METHOD) {
				@Override
				public <U> IMemberAccessor<IMCMethod, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCFrame, U> accessor = STACK_TRACE_BOTTOM_FRAME.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<IMCMethod, U>() {
						@Override
						public IMCMethod getMember(U i) {
							IMCFrame frame = accessor.getMember(i);
							return frame == null ? null : frame.getMethod();
						}
					};
				}
			});
	public static final IAttribute<String> STACK_TRACE_BOTTOM_METHOD_STRING = Attribute
			.canonicalize(new Attribute<String>("(stackTrace).bottomMethodString", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_STACK_TRACE_BOTTOM_METHOD),
					Messages.getString(Messages.ATTR_STACK_TRACE_BOTTOM_METHOD_DESC), UnitLookup.PLAIN_TEXT) {
				@Override
				public <U> IMemberAccessor<String, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCMethod, U> accessor = STACK_TRACE_BOTTOM_METHOD.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<String, U>() {
						@Override
						public String getMember(U i) {
							IMCMethod method = accessor.getMember(i);
							return method == null ? null : FormatToolkit.getHumanReadable(method);
						}
					};
				}
			});

	public static final IAttribute<Boolean> STACK_TRACE_TRUNCATED = Attribute.canonicalize(
			new Attribute<Boolean>("(stackTrace).truncationState", Messages.getString(Messages.ATTR_STACK_TRACE_DEPTH), //$NON-NLS-1$
					Messages.getString(Messages.ATTR_STACK_TRACE_DEPTH_DESC), FLAG) {
				@Override
				public <U> IMemberAccessor<Boolean, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCStackTrace, U> accessor = JfrAttributes.EVENT_STACKTRACE.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<Boolean, U>() {
						@Override
						public Boolean getMember(U i) {
							IMCStackTrace trace = accessor.getMember(i);
							// FIXME: Fix train wreck to avoid NPEs and IndexOutOfBoundsException
							return trace == null ? null : trace.getTruncationState().isTruncated();
						}
					};
				}
			});

	public static final IAttribute<String> PID = attr("pid", Messages.getString(Messages.ATTR_PID), PLAIN_TEXT); //$NON-NLS-1$
	public static final IAttribute<String> COMMAND_LINE = attr("commandLine", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_COMMAND_LINE), PLAIN_TEXT);

	public static final IAttribute<IQuantity> JVM_SYSTEM = attr("jvmSystem", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_JVM_SYSTEM), Messages.getString(Messages.ATTR_JVM_SYSTEM_DESC),
			PERCENTAGE);
	public static final IAttribute<IQuantity> JVM_USER = attr("jvmUser", Messages.getString(Messages.ATTR_JVM_USER), //$NON-NLS-1$
			Messages.getString(Messages.ATTR_JVM_USER_DESC), PERCENTAGE);
	public static final IAttribute<IQuantity> JVM_TOTAL = Attribute.canonicalize(new Attribute<IQuantity>("jvmTotal", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_JVM_TOTAL), Messages.getString(Messages.ATTR_JVM_TOTAL_DESC), PERCENTAGE) {
		@Override
		public <U> IMemberAccessor<IQuantity, U> customAccessor(IType<U> type) {
			// Avoid possible future circularity by asking the type directly.
			final IMemberAccessor<IQuantity, U> jvmUserAccessor = type.getAccessor(JVM_USER.getKey());
			final IMemberAccessor<IQuantity, U> jvmSystemAccessor = type.getAccessor(JVM_SYSTEM.getKey());
			if ((jvmUserAccessor == null) || (jvmSystemAccessor == null)) {
				return null;
			}
			return new IMemberAccessor<IQuantity, U>() {
				@Override
				public IQuantity getMember(U i) {
					IQuantity jvmUser = jvmUserAccessor.getMember(i);
					IQuantity jvmSystem = jvmSystemAccessor.getMember(i);
					return jvmUser != null && jvmSystem != null ? jvmUser.add(jvmSystem) : null;
				}
			};
		}
	});
	public static final IAttribute<IQuantity> MACHINE_TOTAL = attr("machineTotal", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_MACHINE_TOTAL), Messages.getString(Messages.ATTR_MACHINE_TOTAL_DESC),
			PERCENTAGE);
	public static final IAttribute<IQuantity> OTHER_CPU = Attribute.canonicalize(new Attribute<IQuantity>("otherCpu", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_OTHER_CPU), Messages.getString(Messages.ATTR_OTHER_CPU_DESC), PERCENTAGE) {
		@Override
		public <U> IMemberAccessor<IQuantity, U> customAccessor(IType<U> type) {
			final IMemberAccessor<IQuantity, U> jvmTotalAccessor = JVM_TOTAL.getAccessor(type);
			// Avoid possible future circularity by asking the type directly.
			final IMemberAccessor<IQuantity, U> machineTotalAccessor = type.getAccessor(MACHINE_TOTAL.getKey());
			if ((jvmTotalAccessor == null) || (machineTotalAccessor == null)) {
				return null;
			}
			return new IMemberAccessor<IQuantity, U>() {
				@Override
				public IQuantity getMember(U i) {
					IQuantity jvmTotal = jvmTotalAccessor.getMember(i);
					IQuantity machineTotal = machineTotalAccessor.getMember(i);
					return jvmTotal != null && machineTotal != null ? machineTotal.subtract(jvmTotal) : null;
				}
			};
		}
	});

	public static final IAttribute<IQuantity> RECORDING_ID = attr("id", Messages.getString(Messages.ATTR_RECORDING_ID), //$NON-NLS-1$
			NUMBER);
	public static final IAttribute<String> RECORDING_NAME = attr("name", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_RECORDING_NAME), PLAIN_TEXT);
	public static final IAttribute<IQuantity> RECORDING_START = attr("recordingStart", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_RECORDING_START), TIMESTAMP);
	public static final IAttribute<IQuantity> RECORDING_DURATION = attr("recordingDuration", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_RECORDING_DURATION), TIMESPAN);
	public static final IAttribute<IQuantity> RECORDING_MAX_SIZE = attr("maxSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_RECORDING_MAX_SIZE), MEMORY);
	public static final IAttribute<IQuantity> RECORDING_MAX_AGE = attr("maxAge", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_RECORDING_MAX_AGE), TIMESPAN);
	public static final IAttribute<String> RECORDING_DESTINATION = attr("destination", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_RECORDING_DESTINATION), PLAIN_TEXT);

	public static final IAttribute<LabeledIdentifier> REC_SETTING_FOR = attr("settingFor", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_REC_SETTING_FOR), LABELED_IDENTIFIER);
	public static final IAttribute<String> REC_SETTING_NAME = attr("name", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_REC_SETTING_NAME), PLAIN_TEXT);
	public static final IAttribute<String> REC_SETTING_VALUE = attr("value", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_REC_SETTING_VALUE), PLAIN_TEXT);
	public static final IAttribute<String> REC_SETTING_FOR_NAME = Attribute.canonicalize(new Attribute<String>(
			"settingFor.string", Messages.getString(Messages.ATTR_REC_SETTING_FOR), null, PLAIN_TEXT) { //$NON-NLS-1$
		@Override
		public <U> IMemberAccessor<String, U> customAccessor(IType<U> type) {
			final IMemberAccessor<LabeledIdentifier, U> accessor = REC_SETTING_FOR.getAccessor(type);
			return accessor == null ? null : new IMemberAccessor<String, U>() {
				@Override
				public String getMember(U i) {
					LabeledIdentifier identifier = accessor.getMember(i);
					return identifier == null ? null : identifier.getName();
				}
			};
		}
	});
	public static final IAttribute<String> REC_SETTING_FOR_ID = Attribute.canonicalize(new Attribute<String>(
			"settingFor.id", Messages.getString(Messages.ATTR_REC_SETTING_FOR), null, PLAIN_TEXT) { //$NON-NLS-1$
		@Override
		public <U> IMemberAccessor<String, U> customAccessor(IType<U> type) {
			final IMemberAccessor<LabeledIdentifier, U> accessor = REC_SETTING_FOR.getAccessor(type);
			return accessor == null ? null : new IMemberAccessor<String, U>() {
				@Override
				public String getMember(U i) {
					LabeledIdentifier identifier = accessor.getMember(i);
					return identifier == null ? null : identifier.getInterfaceId();
				}
			};
		}
	});

	public static final IAttribute<IMCPackage> EXPORTED_PACKAGE = attr("exportedPackage", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_EXPORTED_PACKAGE), UnitLookup.PACKAGE);

	public static final IAttribute<IMCModule> EXPORTING_MODULE = Attribute.canonicalize(new Attribute<IMCModule>(
			"exportingModule", Messages.getString(Messages.ATTR_EXPORTING_MODULE), null, UnitLookup.MODULE) { //$NON-NLS-1$
		@Override
		public <U> IMemberAccessor<IMCModule, U> customAccessor(IType<U> type) {
			final IMemberAccessor<IMCPackage, U> accessor = EXPORTED_PACKAGE.getAccessor(type);
			return accessor == null ? null : new IMemberAccessor<IMCModule, U>() {
				@Override
				public IMCModule getMember(U i) {
					IMCPackage thePackage = accessor.getMember(i);
					return thePackage == null ? null : thePackage.getModule();
				}
			};
		}
	});

	public static final IAttribute<IQuantity> JVM_START_TIME = attr("jvmStartTime", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_JVM_START_TIME), TIMESTAMP);
	public static final IAttribute<String> JVM_NAME = attr("jvmName", Messages.getString(Messages.ATTR_JVM_NAME), //$NON-NLS-1$
			PLAIN_TEXT);
	public static final IAttribute<String> JVM_VERSION = attr("jvmVersion", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_JVM_VERSION), PLAIN_TEXT);
	public static final IAttribute<String> JVM_ARGUMENTS = attr("jvmArguments", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_JVM_ARGUMENTS), PLAIN_TEXT);
	public static final IAttribute<String> JAVA_ARGUMENTS = attr("javaArguments", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_JAVA_ARGUMENTS), PLAIN_TEXT);

	public static final IAttribute<String> IO_PATH = attr("path", Messages.getString(Messages.ATTR_IO_PATH), //$NON-NLS-1$
			Messages.getString(Messages.ATTR_IO_PATH_DESC), PLAIN_TEXT);
	public static final IAttribute<IQuantity> IO_FILE_BYTES_READ = attr("bytesRead", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_IO_FILE_BYTES_READ),
			Messages.getString(Messages.ATTR_IO_FILE_BYTES_READ_DESC), MEMORY);
	public static final IAttribute<Boolean> IO_FILE_READ_EOF = attr("endOfFile", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_IO_FILE_READ_EOF), Messages.getString(Messages.ATTR_IO_FILE_READ_EOF_DESC),
			FLAG);
	public static final IAttribute<IQuantity> IO_FILE_BYTES_WRITTEN = attr("bytesWritten", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_IO_FILE_BYTES_WRITTEN),
			Messages.getString(Messages.ATTR_IO_FILE_BYTES_WRITTEN_DESC), MEMORY);
	public static final IAttribute<IQuantity> IO_SOCKET_BYTES_READ = attr("bytesRead", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_IO_SOCKET_BYTES_READ),
			Messages.getString(Messages.ATTR_IO_SOCKET_BYTES_READ_DESC), MEMORY);
	public static final IAttribute<Boolean> IO_SOCKET_READ_EOS = attr("endOfStream", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_IO_SOCKET_READ_EOS),
			Messages.getString(Messages.ATTR_IO_SOCKET_READ_EOS_DESC), FLAG);
	public static final IAttribute<IQuantity> IO_SOCKET_BYTES_WRITTEN = attr("bytesWritten", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_IO_SOCKET_BYTES_WRITTEN),
			Messages.getString(Messages.ATTR_IO_SOCKET_BYTES_WRITTEN_DESC), MEMORY);
	public static final IAttribute<String> IO_ADDRESS = attr("address", Messages.getString(Messages.ATTR_IO_ADDRESS), //$NON-NLS-1$
			PLAIN_TEXT);
	public static final IAttribute<String> IO_HOST = attr("host", Messages.getString(Messages.ATTR_IO_HOST), //$NON-NLS-1$
			Messages.getString(Messages.ATTR_IO_HOST_DESC), PLAIN_TEXT);
	public static final IAttribute<IQuantity> IO_PORT = attr("port", Messages.getString(Messages.ATTR_IO_PORT), NUMBER); //$NON-NLS-1$
	public static final IAttribute<Object> IO_PORT_ON_ADDRESS = Attribute.canonicalize(new Attribute<Object>(
			"portOnAddress", Messages.getString(Messages.ATTR_IO_PORT_ON_ADDRESS), null, UNKNOWN) { //$NON-NLS-1$
		@Override
		public <U> IMemberAccessor<Object, U> customAccessor(IType<U> type) {
			class PortOnAddress implements IDisplayable {

				final String address;
				final IQuantity port;

				PortOnAddress(String address, IQuantity port) {
					this.address = address;
					this.port = port;
				}

				@Override
				public String displayUsing(String formatHint) {
					return address + " : " + port.displayUsing(formatHint); //$NON-NLS-1$
				}

				@Override
				public int hashCode() {
					return 31 * address.hashCode() + port.hashCode();
				};

				@Override
				public boolean equals(Object o) {
					return o instanceof PortOnAddress && ((PortOnAddress) o).address.equals(address)
							&& ((PortOnAddress) o).port.equals(port);
				};

			}
			// Avoid possible future circularity by asking the type directly.
			final IMemberAccessor<String, U> addressAccessor = type.getAccessor(IO_ADDRESS.getKey());
			final IMemberAccessor<IQuantity, U> portAccessor = type.getAccessor(IO_PORT.getKey());
			if ((addressAccessor == null) || (portAccessor == null)) {
				return null;
			}
			return new IMemberAccessor<Object, U>() {
				@Override
				public IDisplayable getMember(U i) {
					String address = addressAccessor.getMember(i);
					IQuantity port = portAccessor.getMember(i);
					if (address != null && port != null) {
						return new PortOnAddress(address, port);
					}
					return null;
				}
			};
		}
	});

	public static final IAttribute<IQuantity> IO_TIMEOUT = attr("timeout", Messages.getString(Messages.ATTR_IO_TIMEOUT), //$NON-NLS-1$
			TIMESPAN);

	public static final IAttribute<IQuantity> TLAB_SIZE = attr("tlabSize", Messages.getString(Messages.ATTR_TLAB_SIZE), //$NON-NLS-1$
			MEMORY);
	public static final IAttribute<IQuantity> ALLOCATION_SIZE = attr("allocationSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_ALLOCATION_SIZE), MEMORY);
	public static final IAttribute<IMCType> ALLOCATION_CLASS = attr("objectClass", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_ALLOCATION_CLASS), Messages.getString(Messages.ATTR_ALLOCATION_CLASS_DESC),
			CLASS);
	public static final IAttribute<IMCType> OBJECT_CLASS = attr("objectClass", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_OBJECT_CLASS), CLASS);
	public static final IAttribute<String> OBJECT_CLASS_FULLNAME = Attribute.canonicalize(new Attribute<String>(
			"objectClass.humanreadable", Messages.getString(Messages.ATTR_OBJECT_CLASS), null, PLAIN_TEXT) { //$NON-NLS-1$
		@Override
		public <U> IMemberAccessor<String, U> customAccessor(IType<U> type) {
			final IMemberAccessor<IMCType, U> accessor = OBJECT_CLASS.getAccessor(type);
			return accessor == null ? null : new IMemberAccessor<String, U>() {
				@Override
				public String getMember(U i) {
					IMCType type = accessor.getMember(i);
					return type == null ? null : type.getFullName();
				}
			};
		}
	});
	public static final IAttribute<IQuantity> COUNT = attr("count", Messages.getString(Messages.ATTR_COUNT), NUMBER); //$NON-NLS-1$

	public static final IAttribute<IQuantity> HW_THREADS = attr("hwThreads", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_HW_THREADS), Messages.getString(Messages.ATTR_HW_THREADS_DESC), NUMBER);

	public static final IAttribute<IQuantity> PARALLEL_GC_THREADS = attr("parallelGCThreads", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_PARALLEL_GC_THREADS),
			Messages.getString(Messages.ATTR_PARALLEL_GC_THREADS_DESC), NUMBER);
	public static final IAttribute<IQuantity> CONCURRENT_GC_THREADS = attr("concurrentGCThreads", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_CONCURRENT_GC_THREADS),
			Messages.getString(Messages.ATTR_CONCURRENT_GC_THREADS_DESC), NUMBER);
	public static final IAttribute<String> YOUNG_COLLECTOR = attr("youngCollector", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_YOUNG_COLLECTOR), Messages.getString(Messages.ATTR_YOUNG_COLLECTOR_DESC),
			PLAIN_TEXT);
	public static final IAttribute<String> OLD_COLLECTOR = attr("oldCollector", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_OLD_COLLECTOR), Messages.getString(Messages.ATTR_OLD_COLLECTOR_DESC),
			PLAIN_TEXT);
	public static final IAttribute<Boolean> EXPLICIT_GC_CONCURRENT = attr("isExplicitGCConcurrent", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_EXPLICIT_GC_CONCURRENT),
			Messages.getString(Messages.ATTR_EXPLICIT_GC_CONCURRENT_DESC), FLAG);
	public static final IAttribute<Boolean> EXPLICIT_GC_DISABLED = attr("isExplicitGCDisabled", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_EXPLICIT_GC_DISABLED),
			Messages.getString(Messages.ATTR_EXPLICIT_GC_DISABLED_DESC), FLAG);
	public static final IAttribute<Boolean> USE_DYNAMIC_GC_THREADS = attr("usesDynamicGCThreads", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_USE_DYNAMIC_GC_THREADS),
			Messages.getString(Messages.ATTR_USE_DYNAMIC_GC_THREADS_DESC), FLAG);
	public static final IAttribute<IQuantity> GC_TIME_RATIO = attr("gcTimeRatio", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_TIME_RATIO), Messages.getString(Messages.ATTR_GC_TIME_RATIO_DESC),
			NUMBER);

	public static final IAttribute<IQuantity> HEAP_MAX_SIZE = attr("maxSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_HEAP_MAX_SIZE), MEMORY);
	public static final IAttribute<IQuantity> HEAP_MIN_SIZE = attr("minSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_HEAP_MIN_SIZE), MEMORY);
	public static final IAttribute<IQuantity> HEAP_INITIAL_SIZE = attr("initialSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_HEAP_INITIAL_SIZE), MEMORY);
	public static final IAttribute<IQuantity> HEAP_OBJECT_ALIGNMENT = attr("objectAlignment", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_HEAP_OBJECT_ALIGNMENT),
			Messages.getString(Messages.ATTR_HEAP_OBJECT_ALIGNMENT_DESC), MEMORY);
	public static final IAttribute<IQuantity> HEAP_ADDRESS_SIZE = attr("heapAddressBits", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_HEAP_ADDRESS_SIZE),
			Messages.getString(Messages.ATTR_HEAP_ADDRESS_SIZE_DESC), NUMBER);
	public static final IAttribute<Boolean> HEAP_USE_COMPRESSED_OOPS = attr("usesCompressedOops", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_HEAP_USE_COMPRESSED_OOPS),
			Messages.getString(Messages.ATTR_HEAP_USE_COMPRESSED_OOPS_DESC), FLAG);
	public static final IAttribute<String> HEAP_COMPRESSED_OOPS_MODE = attr("compressedOopsMode", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_HEAP_COMPRESSED_OOPS_MODE),
			Messages.getString(Messages.ATTR_HEAP_COMPRESSED_OOPS_MODE_DESC), PLAIN_TEXT);

	public static final IAttribute<IQuantity> YOUNG_GENERATION_MIN_SIZE = attr("minSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_YOUNG_GENERATION_MIN_SIZE), MEMORY);
	public static final IAttribute<IQuantity> YOUNG_GENERATION_MAX_SIZE = attr("maxSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_YOUNG_GENERATION_MAX_SIZE), MEMORY);

	public static final IAttribute<IQuantity> NEW_RATIO = attr("newRatio", Messages.getString(Messages.ATTR_NEW_RATIO), //$NON-NLS-1$
			Messages.getString(Messages.ATTR_NEW_RATIO_DESC), NUMBER);
	public static final IAttribute<IQuantity> TENURING_THRESHOLD_INITIAL = attr("initialTenuringThreshold", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_TENURING_THRESHOLD_INITIAL),
			Messages.getString(Messages.ATTR_TENURING_THRESHOLD_INITIAL_DESC), NUMBER);
	public static final IAttribute<IQuantity> TENURING_THRESHOLD_MAXIMUM = attr("maxTenuringThreshold", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_TENURING_THRESHOLD_MAXIMUM),
			Messages.getString(Messages.ATTR_TENURING_THRESHOLD_MAXIMUM_DESC), NUMBER);

	public static final IAttribute<Boolean> USES_TLABS = attr("usesTLABs", Messages.getString(Messages.ATTR_USES_TLABS), //$NON-NLS-1$
			Messages.getString(Messages.ATTR_USES_TLABS_DESC), FLAG);
	public static final IAttribute<IQuantity> TLAB_MIN_SIZE = attr("minTLABSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_TLAB_MIN_SIZE), MEMORY);
	public static final IAttribute<IQuantity> TLAB_REFILL_WASTE_LIMIT = attr("tlabRefillWasteLimit", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_TLAB_REFILL_WASTE_LIMIT), MEMORY);

	public static final IAttribute<IQuantity> HEAP_TOTAL = attr("totalSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_HEAP_TOTAL), MEMORY);
	public static final IAttribute<IQuantity> HEAP_USED = attr("heapUsed", Messages.getString(Messages.ATTR_HEAP_USED), //$NON-NLS-1$
			Messages.getString(Messages.ATTR_HEAP_USED_DESC), MEMORY);
	public static final IAttribute<String> GC_WHEN = attr("when", Messages.getString(Messages.ATTR_GC_WHEN), //$NON-NLS-1$
			PLAIN_TEXT);
	public static final IAttribute<IQuantity> GC_ID = attr("gcId", Messages.getString(Messages.ATTR_GC_ID), //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_ID_DESC), NUMBER);
	public static final IAttribute<IQuantity> REFERENCE_COUNT = attr("count", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_REFERENCE_COUNT), NUMBER);
	public static final IAttribute<String> GC_PHASE_NAME = attr("name", Messages.getString(Messages.ATTR_GC_PHASE_NAME), //$NON-NLS-1$
			PLAIN_TEXT);

	public static final IAttribute<IQuantity> GC_HEAPSPACE_COMMITTED = attr("heapSpace:committedSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_HEAPSPACE_COMMITTED),
			Messages.getString(Messages.ATTR_GC_HEAPSPACE_COMMITTED_DESC), MEMORY);
	public static final IAttribute<IQuantity> GC_HEAPSPACE_RESERVED = attr("heapSpace:reservedSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_HEAPSPACE_RESERVED),
			Messages.getString(Messages.ATTR_GC_HEAPSPACE_RESERVED_DESC), MEMORY);

	public static final IAttribute<IQuantity> GC_METASPACE_CAPACITY = attr("metaspace:capacity", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_METASPACE_CAPACITY),
			Messages.getString(Messages.ATTR_GC_METASPACE_CAPACITY_DESC), MEMORY);
	public static final IAttribute<IQuantity> GC_METASPACE_COMMITTED = attr("metaspace:committed", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_METASPACE_COMMITTED),
			Messages.getString(Messages.ATTR_GC_METASPACE_COMMITTED_DESC), MEMORY);
	public static final IAttribute<IQuantity> GC_METASPACE_RESERVED = attr("metaspace:reserved", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_METASPACE_RESERVED),
			Messages.getString(Messages.ATTR_GC_METASPACE_RESERVED_DESC), MEMORY);
	public static final IAttribute<IQuantity> GC_METASPACE_USED = attr("metaspace:used", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_METASPACE_USED),
			Messages.getString(Messages.ATTR_GC_METASPACE_USED_DESC), MEMORY);
	public static final IAttribute<IQuantity> GC_DATASPACE_COMMITTED = attr("dataSpace:committed", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_METASPACE_DATA_COMMITTED),
			Messages.getString(Messages.ATTR_GC_METASPACE_DATA_COMMITTED_DESC), MEMORY);
	public static final IAttribute<IQuantity> GC_DATASPACE_RESERVED = attr("dataSpace:reserved", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_METASPACE_DATA_RESERVED),
			Messages.getString(Messages.ATTR_GC_METASPACE_DATA_RESERVED_DESC), MEMORY);
	public static final IAttribute<IQuantity> GC_DATASPACE_USED = attr("dataSpace:used", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_METASPACE_DATA_USED),
			Messages.getString(Messages.ATTR_GC_METASPACE_DATA_USED_DESC), MEMORY);
	public static final IAttribute<IQuantity> GC_CLASSSPACE_COMMITTED = attr("classSpace:committed", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_METASPACE_CLASS_COMMITTED),
			Messages.getString(Messages.ATTR_GC_METASPACE_CLASS_COMMITTED_DESC), MEMORY);
	public static final IAttribute<IQuantity> GC_CLASSSPACE_RESERVED = attr("classSpace:reserved", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_METASPACE_CLASS_RESERVED),
			Messages.getString(Messages.ATTR_GC_METASPACE_CLASS_RESERVED_DESC), MEMORY);
	public static final IAttribute<IQuantity> GC_CLASSSPACE_USED = attr("classSpace:used", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_METASPACE_CLASS_USED),
			Messages.getString(Messages.ATTR_GC_METASPACE_CLASS_USED_DESC), MEMORY);

	public static final IAttribute<IQuantity> GC_THRESHOLD = attr("gcThreshold", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_THRESHOLD), Messages.getString(Messages.ATTR_GC_THRESHOLD_DESC),
			MEMORY);

	public static final IAttribute<IQuantity> OS_MEMORY_TOTAL = attr("totalSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_OS_MEMORY_TOTAL), Messages.getString(Messages.ATTR_OS_MEMORY_TOTAL_DESC),
			MEMORY);
	public static final IAttribute<IQuantity> OS_MEMORY_USED = attr("usedSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_OS_MEMORY_USED), Messages.getString(Messages.ATTR_OS_MEMORY_USED_DESC),
			MEMORY);

	public static final IAttribute<String> FLAG_NAME = attr("name", Messages.getString(Messages.ATTR_FLAG_NAME), //$NON-NLS-1$
			PLAIN_TEXT);
	public static final IAttribute<String> FLAG_ORIGIN = attr("origin", Messages.getString(Messages.ATTR_FLAG_ORIGIN), //$NON-NLS-1$
			PLAIN_TEXT);
	public static final IAttribute<IQuantity> FLAG_VALUE_NUMBER = attr("value", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_FLAG_VALUE_NUMBER), NUMBER);
	public static final IAttribute<Boolean> FLAG_VALUE_BOOLEAN = attr("value", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_FLAG_VALUE_BOOLEAN), FLAG);
	public static final IAttribute<String> FLAG_VALUE_TEXT = attr("value", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_FLAG_VALUE_TEXT), PLAIN_TEXT);

	public static final IAttribute<String> THREAD_DUMP_RESULT = attr("result", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_THREAD_DUMP_RESULT), PLAIN_TEXT);
	public static final IAttribute<String> DUMP_REASON = attr("reason", Messages.getString(Messages.ATTR_DUMP_REASON), //$NON-NLS-1$
			Messages.getString(Messages.ATTR_DUMP_REASON_DESC), PLAIN_TEXT);
	public static final IAttribute<String> DUMP_REASON_RECORDING_ID = attr("recordingId", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_DUMP_REASON_RECORDING_ID),
			Messages.getString(Messages.ATTR_DUMP_REASON_RECORDING_ID_DESC), PLAIN_TEXT);
	
	public static final IAttribute<String> SHUTDOWN_REASON = attr("reason", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_SHUTDOWN_REASON),
			Messages.getString(Messages.ATTR_SHUTDOWN_REASON_DESC), PLAIN_TEXT);

	public static final IAttribute<IQuantity> CLASSLOADER_LOADED_COUNT = attr("loadedClassCount", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_CLASSLOADER_LOADED_COUNT),
			Messages.getString(Messages.ATTR_CLASSLOADER_LOADED_COUNT_DESC), NUMBER);
	public static final IAttribute<IQuantity> CLASSLOADER_UNLOADED_COUNT = attr("unloadedClassCount", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_CLASSLOADER_UNLOADED_COUNT),
			Messages.getString(Messages.ATTR_CLASSLOADER_UNLOADED_COUNT_DESC), NUMBER);

	private static final IAttribute<IMCType> CLASS_DEFINING_CLASSLOADER_V0 = attr("definingClassLoader", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_CLASS_DEFINING_CLASSLOADER), CLASS);
	private static final IAttribute<IMCType> CLASS_INITIATING_CLASSLOADER_V0 = attr("initiatingClassLoader", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_CLASS_INITIATING_CLASSLOADER), CLASS);
	private static final IAttribute<IMCType> PARENT_CLASSLOADER_V0 = attr("parentClassLoader", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_PARENT_CLASSLOADER), CLASS);
	private static final IAttribute<IMCType> CLASSLOADER_V0 = attr("classLoader", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_CLASSLOADER), CLASS);

	public static final IAttribute<IMCClassLoader> CLASS_DEFINING_CLASSLOADER = Attribute
			.canonicalize(new Attribute<IMCClassLoader>("definingClassLoader", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_CLASS_DEFINING_CLASSLOADER), null, CLASS_LOADER) {
				@Override
				public <U> IMemberAccessor<IMCClassLoader, U> customAccessor(IType<U> type) {
					// V1 is handled by the standard accessor
					final IMemberAccessor<IMCType, U> accessorV0 = CLASS_DEFINING_CLASSLOADER_V0.getAccessor(type);
					if (accessorV0 != null) {
						return new IMemberAccessor<IMCClassLoader, U>() {
							@Override
							public IMCClassLoader getMember(U i) {
								IMCType type = accessorV0.getMember(i);
								return new MCClassLoader(type, null);
							}
						};
					}
					return null;
				}
			});
	public static final IAttribute<IMCClassLoader> CLASS_INITIATING_CLASSLOADER = Attribute
			.canonicalize(new Attribute<IMCClassLoader>("initiatingClassLoader", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_CLASS_INITIATING_CLASSLOADER), null, CLASS_LOADER) {
				@Override
				public <U> IMemberAccessor<IMCClassLoader, U> customAccessor(IType<U> type) {
					// V1 is handled by the standard accessor
					final IMemberAccessor<IMCType, U> accessorV0 = CLASS_INITIATING_CLASSLOADER_V0.getAccessor(type);
					if (accessorV0 != null) {
						return new IMemberAccessor<IMCClassLoader, U>() {
							@Override
							public IMCClassLoader getMember(U i) {
								IMCType type = accessorV0.getMember(i);
								return new MCClassLoader(type, null);
							}
						};
					}
					return null;
				}
			});
	public static final IAttribute<IMCClassLoader> PARENT_CLASSLOADER = Attribute
			.canonicalize(new Attribute<IMCClassLoader>("parentClassLoader", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_PARENT_CLASSLOADER), null, CLASS_LOADER) {
				@Override
				public <U> IMemberAccessor<IMCClassLoader, U> customAccessor(IType<U> type) {
					// V1 is handled by the standard accessor
					final IMemberAccessor<IMCType, U> accessorV0 = PARENT_CLASSLOADER_V0.getAccessor(type);
					if (accessorV0 != null) {
						return new IMemberAccessor<IMCClassLoader, U>() {
							@Override
							public IMCClassLoader getMember(U i) {
								IMCType type = accessorV0.getMember(i);
								return new MCClassLoader(type, null);
							}
						};
					}
					return null;
				}
			});
	public static final IAttribute<IMCClassLoader> CLASSLOADER = Attribute
			.canonicalize(new Attribute<IMCClassLoader>("classLoader", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_CLASSLOADER), null, CLASS_LOADER) {
				@Override
				public <U> IMemberAccessor<IMCClassLoader, U> customAccessor(IType<U> type) {
					// V1 is handled by the standard accessor
					final IMemberAccessor<IMCType, U> accessorV0 = CLASSLOADER_V0.getAccessor(type);
					if (accessorV0 != null) {
						return new IMemberAccessor<IMCClassLoader, U>() {
							@Override
							public IMCClassLoader getMember(U i) {
								IMCType type = accessorV0.getMember(i);
								return new MCClassLoader(type, null);
							}
						};
					}
					return null;
				}
			});
	public static final IAttribute<String> CLASS_DEFINING_CLASSLOADER_STRING = Attribute
			.canonicalize(new Attribute<String>("defininingClassLoader.string", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_CLASS_DEFINING_CLASSLOADER), null, PLAIN_TEXT) {
				@Override
				public <U> IMemberAccessor<String, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCClassLoader, U> accessor = CLASS_DEFINING_CLASSLOADER.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<String, U>() {
						@Override
						public String getMember(U i) {
							IMCClassLoader cl = accessor.getMember(i);
							return cl == null ? null : FormatToolkit.getHumanReadable(cl);
						}
					};
				}
			});
	public static final IAttribute<String> CLASS_INITIATING_CLASSLOADER_STRING = Attribute
			.canonicalize(new Attribute<String>("initiatingClassLoader.string", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_CLASS_INITIATING_CLASSLOADER), null, PLAIN_TEXT) {
				@Override
				public <U> IMemberAccessor<String, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCClassLoader, U> accessor = CLASS_INITIATING_CLASSLOADER.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<String, U>() {
						@Override
						public String getMember(U i) {
							IMCClassLoader cl = accessor.getMember(i);
							return cl == null ? null : FormatToolkit.getHumanReadable(cl);
						}
					};
				}
			});
	public static final IAttribute<String> PARENT_CLASSLOADER_STRING = Attribute
			.canonicalize(new Attribute<String>("parentClassLoader.string", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_PARENT_CLASSLOADER), null, PLAIN_TEXT) {
				@Override
				public <U> IMemberAccessor<String, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCClassLoader, U> accessor = PARENT_CLASSLOADER.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<String, U>() {
						@Override
						public String getMember(U i) {
							IMCClassLoader cl = accessor.getMember(i);
							return cl == null ? null : FormatToolkit.getHumanReadable(cl);
						}
					};
				}
			});

	public static final IAttribute<String> CLASSLOADER_STRING = Attribute
			.canonicalize(new Attribute<String>("classLoader.string", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_CLASSLOADER), null, PLAIN_TEXT) {
				@Override
				public <U> IMemberAccessor<String, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCClassLoader, U> accessor = CLASSLOADER.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<String, U>() {
						@Override
						public String getMember(U i) {
							IMCClassLoader cl = accessor.getMember(i);
							return cl == null ? null : FormatToolkit.getHumanReadable(cl);
						}
					};
				}
			});

	public static final IAttribute<IMCType> CLASS_LOADED = attr("loadedClass", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_CLASS_LOADED), CLASS);
	public static final IAttribute<IMCType> CLASS_UNLOADED = attr("unloadedClass", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_CLASS_UNLOADED), CLASS);
	public static final IAttribute<IMCType> CLASS_DEFINED = attr("definedClass", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_CLASS_DEFINED), CLASS);
	public static final IAttribute<IQuantity> ANONYMOUS_BLOCK_SIZE = attr("anonymousBlockSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_ANONYMOUS_BLOCK_SIZE), MEMORY);
	public static final IAttribute<IQuantity> ANONYMOUS_CHUNK_SIZE = attr("anonymousChunkSize", //$NON-NLS-1$ 
			Messages.getString(Messages.ATTR_ANONYMOUS_CHUNK_SIZE), MEMORY);
	public static final IAttribute<IQuantity> ANONYMOUS_CLASS_COUNT = attr("anonymousClassCount", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_ANONYMOUS_CLASS_COUNT), NUMBER);
	public static final IAttribute<IQuantity> BLOCK_SIZE = attr("blockSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_BLOCK_SIZE), MEMORY);
	public static final IAttribute<IQuantity> CHUNK_SIZE = attr("chunkSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_CHUNK_SIZE), MEMORY);
	public static final IAttribute<IQuantity> CLASS_COUNT = attr("classCount", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_CLASS_COUNT), NUMBER);
	public static final IAttribute<IQuantity> CLASS_LOADER_DATA = attr("classLoaderData", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_CLASSLOADER_DATA), ADDRESS);	

	public static final IAttribute<IQuantity> COMPILER_COMPILATION_ID = attr("compileId", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_COMPILER_COMPILATION_ID), NUMBER);
	public static final IAttribute<IQuantity> COMPILER_CODE_SIZE = attr("codeSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_COMPILER_CODE_SIZE), MEMORY);
	public static final IAttribute<IQuantity> COMPILER_INLINED_SIZE = attr("inlinedBytes", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_COMPILER_INLINED_SIZE), MEMORY);
	public static final IAttribute<IMCMethod> COMPILER_METHOD = attr("method", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_COMPILER_METHOD), METHOD);
	public static final IAttribute<String> COMPILER_METHOD_STRING = Attribute.canonicalize(
			new Attribute<String>("method.humanreadable", Messages.getString(Messages.ATTR_COMPILER_METHOD_HUMAN), //$NON-NLS-1$
					Messages.getString(Messages.ATTR_COMPILER_METHOD_HUMAN_DESC), PLAIN_TEXT) {
				@Override
				public <U> IMemberAccessor<String, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCMethod, U> accessor = COMPILER_METHOD.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<String, U>() {
						@Override
						public String getMember(U i) {
							IMCMethod method = accessor.getMember(i);
							return method == null ? null : FormatToolkit.getHumanReadable(method);
						}
					};
				}
			});
	public static final IAttribute<String> COMPILER_FAILED_MESSAGE = attr("failureMessage", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_COMPILER_FAILED_MESSAGE), PLAIN_TEXT);
	public static final IAttribute<IQuantity> COMPILER_STANDARD_COUNT = attr("standardCompileCount", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_COMPILER_STANDARD_COUNT), NUMBER);
	public static final IAttribute<IQuantity> COMPILER_OSR_COUNT = attr("osrCompileCount", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_COMPILER_OSR_COUNT), NUMBER);
	public static final IAttribute<IQuantity> COMPILER_COMPILATION_LEVEL = attr("compileLevel", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_COMPILER_COMPILATION_LEVEL), NUMBER);
	public static final IAttribute<Boolean> COMPILER_COMPILATION_SUCCEEDED = attr("succeded", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_COMPILER_COMPILATION_SUCCEEDED), FLAG);
	public static final IAttribute<Boolean> COMPILER_IS_OSR = attr("isOsr", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_COMPILER_IS_OSR), FLAG);

	public static final IAttribute<IQuantity> START_ADDRESS = attr("startAddress", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_START_ADDRESS), ADDRESS);
	public static final IAttribute<IQuantity> COMMITTED_TOP = attr("commitedTopAddress", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_COMMITTED_TOP), ADDRESS);
	public static final IAttribute<IQuantity> RESERVED_TOP = attr("reservedTopAddress", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_RESERVED_TOP), ADDRESS);
	public static final IAttribute<IQuantity> ENTRIES = attr("entryCount", Messages.getString(Messages.ATTR_ENTRIES), //$NON-NLS-1$
			NUMBER);
	public static final IAttribute<IQuantity> METHODS = attr("methodCount", Messages.getString(Messages.ATTR_METHODS), //$NON-NLS-1$
			NUMBER);
	public static final IAttribute<IQuantity> ADAPTORS = attr("adaptorCount", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_ADAPTORS), NUMBER);
	public static final IAttribute<IQuantity> FULL_COUNT = attr("fullCount", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_FULL_COUNT), NUMBER);
	public static final IAttribute<IQuantity> UNALLOCATED = attr("unallocatedCapacity", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_UNALLOCATED), MEMORY);

	private static final String CODE_HEAP_NON_NMETHODS = "CodeHeap 'non-nmethods'"; //$NON-NLS-1$
	private static final String CODE_HEAP_NON_PROFILED_NMETHODS = "CodeHeap 'non-profiled nmethods'"; //$NON-NLS-1$
	private static final String CODE_HEAP_PROFILED_NMETHODS = "CodeHeap 'profiled nmethods'"; //$NON-NLS-1$

	public static final IAttribute<IQuantity> PROFILED_UNALLOCATED = Attribute
			.canonicalize(createCodeHeapAttribute(UNALLOCATED, CODE_HEAP_PROFILED_NMETHODS, "profiledUnallocated", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_PROFILED_NMETHODS_UNALLOCATED),
					Messages.getString(Messages.ATTR_PROFILED_NMETHODS_UNALLOCATED_DESCRIPTION), MEMORY));
	public static final IAttribute<IQuantity> PROFILED_ENTRIES = Attribute
			.canonicalize(createCodeHeapAttribute(ENTRIES, CODE_HEAP_PROFILED_NMETHODS, "profiledEntries", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_PROFILED_NMETHODS_ENTRIES),
					Messages.getString(Messages.ATTR_PROFILED_NMETHODS_ENTRIES_DESCRIPTION), NUMBER));
	public static final IAttribute<IQuantity> PROFILED_METHODS = Attribute
			.canonicalize(createCodeHeapAttribute(METHODS, CODE_HEAP_PROFILED_NMETHODS, "profiledMethods", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_PROFILED_NMETHODS_METHODS),
					Messages.getString(Messages.ATTR_PROFILED_NMETHODS_METHODS_DESCRIPTION), NUMBER));
	public static final IAttribute<IQuantity> NON_PROFILED_NMETHODS_UNALLOCATED = Attribute
			.canonicalize(createCodeHeapAttribute(UNALLOCATED, CODE_HEAP_NON_PROFILED_NMETHODS,
					"nonProfiledUnallocated", Messages.getString(Messages.ATTR_NON_PROFILED_NMETHODS_UNALLOCATED), //$NON-NLS-1$
					Messages.getString(Messages.ATTR_NON_PROFILED_NMETHODS_UNALLOCATED_DESCRIPTION), MEMORY));
	public static final IAttribute<IQuantity> NON_PROFILED_NMETHODS_ENTRIES = Attribute
			.canonicalize(createCodeHeapAttribute(ENTRIES, CODE_HEAP_NON_PROFILED_NMETHODS, "nonProfiledEntries", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_NON_PROFILED_NMETHODS_ENTRIES),
					Messages.getString(Messages.ATTR_NON_PROFILED_NMETHODS_ENTRIES_DESCRIPTION), NUMBER));
	public static final IAttribute<IQuantity> NON_PROFILED_METHODS = Attribute
			.canonicalize(createCodeHeapAttribute(METHODS, CODE_HEAP_NON_PROFILED_NMETHODS, "nonProfiledMethods", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_NON_PROFILED_NMETHODS_METHODS),
					Messages.getString(Messages.ATTR_NON_PROFILED_NMETHODS_METHODS_DESCRIPTION), NUMBER));
	public static final IAttribute<IQuantity> NON_NMETHODS_UNALLOCATED = Attribute
			.canonicalize(createCodeHeapAttribute(UNALLOCATED, CODE_HEAP_NON_NMETHODS, "nonNmethodsUnallocated", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_NON_NMETHODS_UNALLOCATED),
					Messages.getString(Messages.ATTR_NON_NMETHODS_UNALLOCATED_DESCRIPTION), MEMORY));
	public static final IAttribute<IQuantity> NON_NMETHODS_ENTRIES = Attribute
			.canonicalize(createCodeHeapAttribute(ENTRIES, CODE_HEAP_NON_NMETHODS, "nonNmethodsEntries", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_NON_NMETHODS_ENTRIES),
					Messages.getString(Messages.ATTR_NON_NMETHODS_ENTRIES_DESCRIPTION), NUMBER));
	public static final IAttribute<IQuantity> NON_NMETHODS_ADAPTORS = Attribute
			.canonicalize(createCodeHeapAttribute(ADAPTORS, CODE_HEAP_NON_NMETHODS, "nonNmethodsAdaptors", //$NON-NLS-1$
					Messages.getString(Messages.ATTR_NON_NMETHODS_ADAPTORS),
					Messages.getString(Messages.ATTR_NON_NMETHODS_ADAPTORS_DESCRIPTION), NUMBER));

	/**
	 * Workaround for badly constructed JDK 9 segmented code cache events. Creates a synthetic
	 * attributes for specific code heaps.
	 *
	 * @param attribute
	 *            the attribute to convert
	 * @param codeHeap
	 *            the code heap for the new attribute to be for
	 * @param identifier
	 *            the identifier of the new attribute
	 * @param name
	 *            the name of the new attribute
	 * @param description
	 *            the description of the new attribute
	 * @param contentType
	 *            the content type of the new attribute
	 * @return the wrapped attribute for the specified code heap and attribute
	 */
	private static Attribute<IQuantity> createCodeHeapAttribute(
		final IAttribute<IQuantity> attribute, final String codeHeap, String identifier, String name,
		String description, ContentType<IQuantity> contentType) {
		return new Attribute<IQuantity>(identifier, name, description, contentType) {
			@Override
			public <U> IMemberAccessor<IQuantity, U> customAccessor(IType<U> type) {
				final IMemberAccessor<IQuantity, U> attributeAccessor = attribute.getAccessor(type);
				final IMemberAccessor<String, U> codeHeapAccessor = CODE_HEAP.getAccessor(type);
				return attributeAccessor == null || codeHeapAccessor == null ? null
						: new IMemberAccessor<IQuantity, U>() {
							@Override
							public IQuantity getMember(U i) {
								return codeHeapAccessor.getMember(i).equals(codeHeap) ? attributeAccessor.getMember(i)
										: null;
							}
						};
			}
		};
	}

	public static final IAttribute<String> CODE_HEAP = attr("codeBlobType", Messages.getString(Messages.ATTR_CODE_HEAP), //$NON-NLS-1$
			PLAIN_TEXT);

	public static final IAttribute<IQuantity> SWEEP_INDEX = attr("sweepId", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_SWEEP_INDEX), NUMBER);
	public static final IAttribute<IQuantity> SWEEP_FRACTION_INDEX = attr("sweepFractionIndex", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_SWEEP_FRACTION_INDEX), NUMBER);
	public static final IAttribute<IQuantity> SWEEP_METHOD_SWEPT = attr("sweptCount", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_SWEEP_METHOD_SWEPT), NUMBER);
	public static final IAttribute<IQuantity> SWEEP_METHOD_FLUSHED = attr("flushedCount", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_SWEEP_METHOD_FLUSHED), NUMBER);
	public static final IAttribute<IQuantity> SWEEP_METHOD_RECLAIMED = attr("markedCount", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_SWEEP_METHOD_RECLAIMED), NUMBER);
	public static final IAttribute<IQuantity> SWEEP_METHOD_ZOMBIFIED = attr("zombifiedCount", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_SWEEP_METHOD_ZOMBIFIED), NUMBER);

	public static final IAttribute<IQuantity> INITIAL_SIZE = attr("initialSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_INITIAL_SIZE), MEMORY);
	public static final IAttribute<IQuantity> RESERVED_SIZE = attr("reservedSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_RESERVED_SIZE), MEMORY);
	public static final IAttribute<IQuantity> EXPANSION_SIZE = attr("expansionSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_EXPANSION_SIZE), MEMORY);
	public static final IAttribute<IQuantity> NON_PROFILED_SIZE = attr("nonProfiledSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_NON_PROFILED_SIZE), MEMORY);
	public static final IAttribute<IQuantity> PROFILED_SIZE = attr("profiledSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_PROFILED_SIZE), MEMORY);
	public static final IAttribute<IQuantity> NON_NMETHOD_SIZE = attr("nonNMethodSize", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_NON_NMETHOD_SIZE), MEMORY);

	public static final IAttribute<String> ENVIRONMENT_KEY = attr("key", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_ENVIRONMENT_KEY), PLAIN_TEXT);
	public static final IAttribute<String> ENVIRONMENT_VALUE = attr("value", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_ENVIRONMENT_VALUE), PLAIN_TEXT);

	public static final IAttribute<IQuantity> EXCEPTION_THROWABLES_COUNT = attr("throwables", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_EXCEPTION_THROWABLES_COUNT),
			Messages.getString(Messages.ATTR_EXCEPTION_THROWABLES_COUNT_DESC), NUMBER);
	public static final IAttribute<IMCType> EXCEPTION_THROWNCLASS = attr("thrownClass", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_EXCEPTION_THROWNCLASS), CLASS);
	public static final IAttribute<String> EXCEPTION_THROWNCLASS_NAME = Attribute.canonicalize(
			new Attribute<String>("thrownClassName", Messages.getString(Messages.ATTR_EXCEPTION_THROWNCLASS_NAME), //$NON-NLS-1$
					Messages.getString(Messages.ATTR_EXCEPTION_THROWNCLASS_NAME_DESC), PLAIN_TEXT) {
				@Override
				public <U> IMemberAccessor<String, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCType, U> accessor = EXCEPTION_THROWNCLASS.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<String, U>() {
						@Override
						public String getMember(U i) {
							IMCType type = accessor.getMember(i);
							return type == null ? null : type.getFullName();
						}
					};
				}
			});

	public static final IAttribute<String> EXCEPTION_MESSAGE = attr("message", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_EXCEPTION_MESSAGE), PLAIN_TEXT);

	public static final IAttribute<IQuantity> MONITOR_ADDRESS = attr("address", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_MONITOR_ADDRESS), ADDRESS);
	public static final IAttribute<IMCType> MONITOR_CLASS = attr("monitorClass", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_MONITOR_CLASS), CLASS);
	public static final IAttribute<IMCThread> MONITOR_PREVIOUS_OWNER = attr("previousOwner", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_MONITOR_PREVIOUS_OWNER), THREAD);

	public static final IAttribute<IQuantity> OS_SWITCH_RATE = attr("switchRate", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_OS_SWITCH_RATE), NUMBER);
	public static final IAttribute<String> REFERENCE_STATISTICS_TYPE = attr("type", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_REFERENCE_STATISTICS_TYPE), PLAIN_TEXT);
	public static final IAttribute<IQuantity> REFERENCE_STATISTICS_COUNT = attr("count", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_REFERENCE_STATISTICS_COUNT), NUMBER);
	public static final IAttribute<IQuantity> GC_SUM_OF_PAUSES = attr("sumOfPauses", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_SUM_OF_PAUSES), Messages.getString(Messages.ATTR_GC_SUM_OF_PAUSES_DESC),
			TIMESPAN);
	public static final IAttribute<IQuantity> GC_LONGEST_PAUSE = attr("longestPause", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_LONGEST_PAUSE), Messages.getString(Messages.ATTR_GC_LONGEST_PAUSE_DESC),
			TIMESPAN);
	public static final IAttribute<String> GC_NAME = attr("name", Messages.getString(Messages.ATTR_GC_NAME), //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_NAME_DESC), PLAIN_TEXT);
	public static final IAttribute<String> GC_CAUSE = attr("cause", Messages.getString(Messages.ATTR_GC_CAUSE), //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_CAUSE_DESC), PLAIN_TEXT);

	public static final IAttribute<IMCOldObject> OBJECT = attr("object", Messages.getString(Messages.ATTR_REFERRER), //$NON-NLS-1$
			Messages.getString(Messages.ATTR_REFERRER_DESC), OLD_OBJECT);
	public static final IAttribute<IQuantity> ALLOCATION_TIME = attr("allocationTime", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_REFERRER), Messages.getString(Messages.ATTR_REFERRER_DESC), TIMESTAMP);
	public static final IAttribute<IMCOldObjectGcRoot> GC_ROOT = attr("root", Messages.getString(Messages.ATTR_GC_ROOT), //$NON-NLS-1$
			Messages.getString(Messages.ATTR_GC_ROOT_DESC), UnitLookup.OLD_OBJECT_GC_ROOT);
	public static final IAttribute<IMCType> OLD_OBJECT_CLASS = Attribute
			.canonicalize(new Attribute<IMCType>("oldObjectClass", Messages.getString(Messages.ATTR_OLD_OBJECT_CLASS), //$NON-NLS-1$
					Messages.getString(Messages.ATTR_OLD_OBJECT_CLASS_DESC), CLASS) {
				@Override
				public <U> IMemberAccessor<IMCType, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCOldObject, U> accessor = OBJECT.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<IMCType, U>() {
						@Override
						public IMCType getMember(U i) {
							IMCOldObject object = accessor.getMember(i);
							return object == null ? null : object.getType();
						}
					};
				}
			});
	public static final IAttribute<String> OLD_OBJECT_DESCRIPTION = Attribute.canonicalize(
			new Attribute<String>("oldObjectDescription", Messages.getString(Messages.ATTR_OLD_OBJECT_DESCRIPTION), //$NON-NLS-1$
					Messages.getString(Messages.ATTR_OLD_OBJECT_DESCRIPTION_DESC), PLAIN_TEXT) {
				@Override
				public <U> IMemberAccessor<String, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCOldObject, U> accessor = OBJECT.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<String, U>() {
						@Override
						public String getMember(U i) {
							IMCOldObject object = accessor.getMember(i);
							return object == null ? null : object.getDescription();
						}
					};
				}
			});
	public static final IAttribute<Long> OLD_OBJECT_ARRAY_SIZE = Attribute.canonicalize(
			new Attribute<Long>("oldObjectArraySize", Messages.getString(Messages.ATTR_OLD_OBJECT_ARRAY_SIZE), //$NON-NLS-1$
					Messages.getString(Messages.ATTR_OLD_OBJECT_ARRAY_SIZE_DESC), UnitLookup.RAW_LONG) {
				@Override
				public <U> IMemberAccessor<Long, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCOldObject, U> accessor = OBJECT.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<Long, U>() {
						@Override
						public Long getMember(U i) {
							IMCOldObject object = accessor.getMember(i);
							return object != null && object.getReferrerArray() != null
									? object.getReferrerArray().getSize() : null;
						}
					};
				}
			});
	public static final IAttribute<IQuantity> OLD_OBJECT_ADDRESS = Attribute.canonicalize(
			new Attribute<IQuantity>("oldObjectAddress", Messages.getString(Messages.ATTR_OLD_OBJECT_ADDRESS), //$NON-NLS-1$
					Messages.getString(Messages.ATTR_OLD_OBJECT_ADDRESS_DESC), ADDRESS) {
				@Override
				public <U> IMemberAccessor<IQuantity, U> customAccessor(IType<U> type) {
					final IMemberAccessor<IMCOldObject, U> accessor = OBJECT.getAccessor(type);
					return accessor == null ? null : new IMemberAccessor<IQuantity, U>() {
						@Override
						public IQuantity getMember(U i) {
							IMCOldObject object = accessor.getMember(i);
							return object == null ? null : object.getAddress();
						}
					};
				}
			});

	public static final IAttribute<String> OS_VERSION = attr("osVersion", Messages.getString(Messages.ATTR_OS_VERSION), //$NON-NLS-1$
			PLAIN_TEXT);
	public static final IAttribute<IQuantity> NUMBER_OF_SOCKETS = attr("sockets", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_NUMBER_OF_SOCKETS),
			Messages.getString(Messages.ATTR_NUMBER_OF_SOCKETS_DESC), NUMBER);
	public static final IAttribute<String> CPU_DESCRIPTION = attr("description", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_CPU_DESCRIPTION), Messages.getString(Messages.ATTR_CPU_DESCRIPTION_DESC),
			PLAIN_TEXT);
	public static final IAttribute<String> CPU_TYPE = attr("cpu", Messages.getString(Messages.ATTR_CPU_TYPE), //$NON-NLS-1$
			PLAIN_TEXT);
	public static final IAttribute<IQuantity> NUMBER_OF_CORES = attr("cores", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_NUMBER_OF_CORES), Messages.getString(Messages.ATTR_NUMBER_OF_CORES_DESC),
			NUMBER);
	public static final IAttribute<Boolean> BLOCKING = attr("blocking", Messages.getString(Messages.ATTR_BLOCKING), //$NON-NLS-1$
			Messages.getString(Messages.ATTR_BLOCKING_DESC), FLAG);
	public static final IAttribute<Boolean> SAFEPOINT = attr("safepoint", Messages.getString(Messages.ATTR_SAFEPOINT), //$NON-NLS-1$
			Messages.getString(Messages.ATTR_SAFEPOINT_DESC), FLAG);
	public static final IAttribute<String> OPERATION = attr("operation", Messages.getString(Messages.ATTR_OPERATION), //$NON-NLS-1$
			Messages.getString(Messages.ATTR_OPERATION_DESC), PLAIN_TEXT);
	public static final IAttribute<IMCThread> CALLER = attr("caller", Messages.getString(Messages.ATTR_CALLER), //$NON-NLS-1$
			Messages.getString(Messages.ATTR_CALLER_DESC), THREAD);
	public static final IAttribute<IMCType> BIASED_REVOCATION_LOCK_CLASS = attr("lockClass", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_REVOKATION_LOCK_CLASS),
			Messages.getString(Messages.ATTR_REVOKATION_LOCK_CLASS_DESC), CLASS);
	public static final IAttribute<IMCType> BIASED_REVOCATION_CLASS = attr("revokedClass", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_REVOKED_CLASS), Messages.getString(Messages.ATTR_REVOKED_CLASS_DESC),
			CLASS);
	public static final IAttribute<Boolean> BIASED_REVOCATION_DISABLE_BIASING = attr("disableBiasing", //$NON-NLS-1$
			Messages.getString(Messages.ATTR_DISABLE_BIASING), FLAG);
}
