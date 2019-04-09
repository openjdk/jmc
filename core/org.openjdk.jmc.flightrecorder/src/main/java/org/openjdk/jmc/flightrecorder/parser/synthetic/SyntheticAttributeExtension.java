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
package org.openjdk.jmc.flightrecorder.parser.synthetic;

import static org.openjdk.jmc.common.item.Attribute.attr;
import static org.openjdk.jmc.flightrecorder.JfrAttributes.EVENT_STACKTRACE;
import static org.openjdk.jmc.flightrecorder.JfrAttributes.EVENT_THREAD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCModule;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.LabeledIdentifier;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.internal.util.JfrInternalConstants;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.parser.IEventSink;
import org.openjdk.jmc.flightrecorder.parser.IEventSinkFactory;
import org.openjdk.jmc.flightrecorder.parser.IParserExtension;
import org.openjdk.jmc.flightrecorder.parser.ValueField;

public class SyntheticAttributeExtension implements IParserExtension {

	private static final IAttribute<IMCStackTrace> EXECUTION_SAMPLES_STACKTRACE = attr("stackTrace", //$NON-NLS-1$
			Messages.getString(Messages.SyntheticAttributeExtension_EXECUTION_SAMPLES_STACKTRACE),
			EVENT_STACKTRACE.getContentType());
	private static final IAttribute<IMCThread> EXECUTION_SAMPLES_THREAD = attr("sampledThread", //$NON-NLS-1$
			Messages.getString(Messages.SyntheticAttributeExtension_EXECUTION_SAMPLES_THREAD),
			EVENT_THREAD.getContentType());
	private static final IAttribute<IMCThread> ALLOC_STATISTICS_THREAD = attr("thread", //$NON-NLS-1$
			Messages.getString(Messages.SyntheticAttributeExtension_ALLOC_STATISTICS_THREAD),
			EVENT_THREAD.getContentType());
	// FIXME: Break out constant
	static final IAttribute<LabeledIdentifier> REC_SETTING_EVENT_ID_ATTRIBUTE = attr("id", //$NON-NLS-1$
			Messages.getString(Messages.SyntheticAttributeExtension_REC_SETTING_EVENT_ID_ATTRIBUTE),
			UnitLookup.LABELED_IDENTIFIER);

	@Override
	public IEventSinkFactory getEventSinkFactory(final IEventSinkFactory sf) {
		return SettingsTransformer.wrapSinkFactory(new IEventSinkFactory() {

			@Override
			public IEventSink create(
				String identifier, String label, String[] category, String description,
				List<ValueField> dataStructure) {
				if (JdkTypeIDs.EXECUTION_SAMPLE.equals(identifier)) {
					ValueField[] struct = new ValueField[dataStructure.size()];
					for (int i = 0; i < struct.length; i++) {
						ValueField vf = dataStructure.get(i);
						if (vf.matches(EXECUTION_SAMPLES_STACKTRACE)) {
							vf = new ValueField(EVENT_STACKTRACE);
						} else if (vf.matches(EXECUTION_SAMPLES_THREAD)) {
							vf = new ValueField(EVENT_THREAD);
						}
						struct[i] = vf;
					}
					dataStructure = Arrays.asList(struct);
				} else if (JdkTypeIDs.NATIVE_METHOD_SAMPLE.equals(identifier)) {
					ValueField[] struct = new ValueField[dataStructure.size()];
					for (int i = 0; i < struct.length; i++) {
						ValueField vf = dataStructure.get(i);
						if (vf.matches(EXECUTION_SAMPLES_THREAD)) {
							vf = new ValueField(EVENT_THREAD);
						}
						struct[i] = vf;
					}
					dataStructure = Arrays.asList(struct);
				} else if (JdkTypeIDs.THREAD_ALLOCATION_STATISTICS.equals(identifier)) {
					ValueField[] struct = new ValueField[dataStructure.size()];
					for (int i = 0; i < struct.length; i++) {
						ValueField vf = dataStructure.get(i);
						if (vf.matches(ALLOC_STATISTICS_THREAD)) {
							vf = new ValueField(EVENT_THREAD);
						}
						struct[i] = vf;
					}
					dataStructure = Arrays.asList(struct);
				} else if (JdkTypeIDs.EXCEPTIONS_THROWN.equals(identifier)) {
					for (int i = 0; i < dataStructure.size(); i++) {
						final int stacktraceIndex = i;
						if (dataStructure.get(i).matches(JfrAttributes.EVENT_STACKTRACE)) {
							final IEventSink subSink = sf.create(identifier, label, category, description,
									dataStructure);
							return new IEventSink() {

								@Override
								public void addEvent(Object[] values) {
									IMCStackTrace st = (IMCStackTrace) values[stacktraceIndex];
									/*
									 * NOTE: Filters out JavaExceptionThrow events created from the
									 * Error constructor to avoid constructed errors being
									 * represented both with a JavaErrorThrow and two
									 * JavaExceptionThrow.
									 */
									if (st == null || st.getFrames().size() < 2
											|| !isError(st.getFrames().get(0)) && !isError(st.getFrames().get(1))) {
										subSink.addEvent(values);
									}
								}

								private boolean isError(IMCFrame frame) {
									return frame.getMethod().getType().getFullName().equals("java.lang.Error"); //$NON-NLS-1$
								}
							};
						}
					}
				} else if (JdkTypeIDs.RECORDING_SETTING.equals(identifier)) {
					ValueField[] struct = new ValueField[dataStructure.size()];
					for (int i = 0; i < struct.length; i++) {
						ValueField vf = dataStructure.get(i);
						if (vf.matches(REC_SETTING_EVENT_ID_ATTRIBUTE)) {
							vf = new ValueField(JdkAttributes.REC_SETTING_FOR);
						}
						struct[i] = vf;
					}
					dataStructure = Arrays.asList(struct);
				} else if (JdkTypeIDs.MODULE_EXPORT.equals(identifier)) {
					// Unwrapping the exporting module field from the exported package as a separate attribute in the event type.
					int packageIndex = -1;
					for (int i = 0; i < dataStructure.size(); i++) {
						ValueField vf = dataStructure.get(i);
						if (vf.matches(JdkAttributes.EXPORTED_PACKAGE)) {
							packageIndex = i;
							break;
						}
					}
					if (packageIndex != -1) {
						List<ValueField> newDataStructure = new ArrayList<>(dataStructure);
						newDataStructure.add(new ValueField(JdkAttributes.EXPORTING_MODULE));
						IEventSink subSink = sf.create(identifier, label, category, description, newDataStructure);
						IEventSink moduleExportSink = new ModuleExportSink(subSink, packageIndex);
						return moduleExportSink;
					}
				}
				return sf.create(identifier, label, category, description, dataStructure);
			}

			@Override
			public void flush() {
				sf.flush();
			}
		});
	}

	@Override
	public String getValueInterpretation(String eventTypeId, String fieldId) {
		if (REC_SETTING_EVENT_ID_ATTRIBUTE.getIdentifier().equals(fieldId)
				&& (JdkTypeIDsPreJdk11.RECORDING_SETTING.equals(eventTypeId)
						|| JdkTypeIDsPreJdk11.JDK9_RECORDING_SETTING.equals(eventTypeId)
						|| JdkTypeIDs.RECORDING_SETTING.equals(eventTypeId))) {
			return JfrInternalConstants.TYPE_IDENTIFIER_VALUE_INTERPRETATION;
		}
		return null;
	}

	private static class ModuleExportSink implements IEventSink {
		private final IEventSink subSink;
		private final int packageFieldIndex;

		public ModuleExportSink(IEventSink subSink, int packageFieldIndex) {
			this.subSink = subSink;
			this.packageFieldIndex = packageFieldIndex;
		}

		@Override
		public void addEvent(Object[] values) {
			IMCPackage thePackage = (IMCPackage) values[packageFieldIndex];
			IMCModule exportingModule = thePackage.getModule();
			Object[] newValues = new Object[values.length + 1];
			System.arraycopy(values, 0, newValues, 0, values.length);
			newValues[values.length] = exportingModule;
			subSink.addEvent(newValues);
		}
	}
}
