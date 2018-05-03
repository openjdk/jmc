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
package org.openjdk.jmc.flightrecorder.internal.parser.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.internal.InvalidJfrFileException;
import org.openjdk.jmc.flightrecorder.internal.util.ParserToolkit;

class ChunkMetadata {
	final static long METADATA_EVENT_TYPE = 0;
	private static final String VALUE = "value"; //$NON-NLS-1$

	private static interface IElement {
		public void setAttribute(String attribute, String value);

		public IElement appendChild(String childName);
	}

	static class RootElement implements IElement {
		MetadataElement metadata;
		RegionElement region;

		@Override
		public void setAttribute(String attribute, String value) {
		}

		@Override
		public IElement appendChild(String childName) {
			switch (childName) {
			case "metadata": //$NON-NLS-1$
				metadata = new MetadataElement();
				return metadata;
			case "region": //$NON-NLS-1$
				region = new RegionElement();
				return region;
			default:
				return null;
			}
		}
	}

	static class MetadataElement implements IElement {
		List<ClassElement> classes;

		@Override
		public void setAttribute(String attribute, String value) {
		}

		@Override
		public IElement appendChild(String childName) {
			switch (childName) {
			case "class": //$NON-NLS-1$
				if (classes == null) {
					classes = new ArrayList<>();
				}
				ClassElement child = new ClassElement();
				classes.add(child);
				return child;
			default:
				return null;
			}
		}
	}

	static class AnnotatedElement implements IElement {
		private static final String ANNOTATION_DESCRIPTION = "jdk.jfr.Description"; //$NON-NLS-1$
		private static final String ANNOTATION_LABEL = "jdk.jfr.Label"; //$NON-NLS-1$
		private static final String ANNOTATION_EXPERIMENTAL = "jdk.jfr.Experimental"; //$NON-NLS-1$

		List<AnnotationElement> annotations;
		String label;
		String description;
		boolean experimental;

		void resolveAnnotation(String typeIdentifier, Map<String, String> values) throws InvalidJfrFileException {
			switch (typeIdentifier) {
			case ANNOTATION_LABEL:
				label = values.get(VALUE);
				return;
			case ANNOTATION_DESCRIPTION:
				description = values.get(VALUE);
				return;
			case ANNOTATION_EXPERIMENTAL:
				experimental = true;
				return;
			}
		}

		@Override
		public void setAttribute(String attribute, String value) {
		}

		@Override
		public IElement appendChild(String childName) {
			switch (childName) {
			case "annotation": //$NON-NLS-1$
				if (annotations == null) {
					annotations = new ArrayList<>();
				}
				AnnotationElement child = new AnnotationElement();
				annotations.add(child);
				return child;
			default:
				return null;
			}
		}
	}

	static class ClassElement extends AnnotatedElement {
		private static final String ANNOTATION_CATEGORY = "jdk.jfr.Category"; //$NON-NLS-1$
		private static final String SUPER_TYPE_EVENT = "jdk.jfr.Event"; //$NON-NLS-1$

		long classId;
		String typeIdentifier;
		String superType;
		Boolean simpleType;
		List<FieldElement> fields;
		private SettingElement ignored;

		String[] category;

		@Override
		public void setAttribute(String attribute, String value) {
			switch (attribute) {
			case "id": //$NON-NLS-1$
				classId = Long.parseLong(value);
				break;
			case "name": //$NON-NLS-1$
				typeIdentifier = value;
				break;
			case "superType": //$NON-NLS-1$
				superType = value;
				break;
			case "simpleType": //$NON-NLS-1$
				simpleType = Boolean.parseBoolean(value);
				break;
			default:
				break;
			}
		}

		@Override
		public IElement appendChild(String childName) {
			switch (childName) {
			case "field": //$NON-NLS-1$
				if (fields == null) {
					fields = new ArrayList<>();
				}
				FieldElement field = new FieldElement();
				fields.add(field);
				return field;
			case "setting": //$NON-NLS-1$
				/*
				 * NOTE: Several setting children will be appended, but since they are not actually
				 * used, just returning the same ignored one. Still need to return something so that
				 * the attributes for the child are read from input.
				 */
				if (ignored == null) {
					ignored = new SettingElement();
				}
				return ignored;
			default:
				return super.appendChild(childName);
			}
		}

		@Override
		void resolveAnnotation(String typeIdentifier, Map<String, String> values) throws InvalidJfrFileException {
			switch (typeIdentifier) {
			case ANNOTATION_CATEGORY:
				List<String> list = new ArrayList<>();
				int index = 0;
				while (true) {
					String s = values.get("value-" + index++); //$NON-NLS-1$
					if (s == null) {
						break;
					}
					list.add(s);
				}
				category = list.toArray(new String[list.size()]);
				return;
			default:
				super.resolveAnnotation(typeIdentifier, values);
			}
		}

		int getFieldCount() {
			return fields == null ? 0 : fields.size();
		}

		boolean isSimpleType() {
			return simpleType != null && simpleType;
		}

		boolean isEventType() {
			return SUPER_TYPE_EVENT.equals(superType);
		}
	}

	static class AnnotationElement implements IElement {
		long classId;
		Map<String, String> values;

		@Override
		public void setAttribute(String attribute, String value) {
			switch (attribute) {
			case "class": //$NON-NLS-1$
				classId = Long.parseLong(value);
				break;
			default:
				if (values == null) {
					values = new HashMap<>();
				}
				values.put(attribute, value);
				break;
			}
		}

		@Override
		public IElement appendChild(String child) {
			return null;
		}
	}

	static class SettingElement extends AnnotatedElement {
		// NOTE: Fields that exist in the JFR metadata, but currently not used by JMC
//		String name;
//		long classId; // Named "class", but can't use as field name

		@Override
		public void setAttribute(String attribute, String value) {
		}
	}

	static class FieldElement extends AnnotatedElement {
		private static final String ANNOTATION_TIMESTAMP = "jdk.jfr.Timestamp"; //$NON-NLS-1$
		private static final String ANNOTATION_TIMESPAN = "jdk.jfr.Timespan"; //$NON-NLS-1$
		private static final String ANNOTATION_MEMORY_ADDRESS = "jdk.jfr.MemoryAddress"; //$NON-NLS-1$
		private static final String ANNOTATION_PERCENTAGE = "jdk.jfr.Percentage"; //$NON-NLS-1$
		private static final String ANNOTATION_MEMORY_AMOUNT = "jdk.jfr.MemoryAmount"; //$NON-NLS-1$ //backward compatibility for pre-release JDK 9
		private static final String ANNOTATION_DATA_AMOUNT = "jdk.jfr.DataAmount"; //$NON-NLS-1$
		private static final String ANNOTATION_UNSIGNED = "jdk.jfr.Unsigned"; //$NON-NLS-1$
		private static final String UNIT_S = "SECONDS"; //$NON-NLS-1$
		private static final String UNIT_MS = "MILLISECONDS"; //$NON-NLS-1$
		private static final String UNIT_NS = "NANOSECONDS"; //$NON-NLS-1$
		private static final String UNIT_TICKS = "TICKS"; //$NON-NLS-1$
		private static final String UNIT_S_SINCE_EPOCH = "SECONDS_SINCE_EPOCH"; //$NON-NLS-1$
		private static final String UNIT_MS_SINCE_EPOCH = "MILLISECONDS_SINCE_EPOCH"; //$NON-NLS-1$
		private static final String UNIT_NS_SINCE_EPOCH = "NANOSECONDS_SINCE_EPOCH"; //$NON-NLS-1$

		String fieldIdentifier;
		long classId;
		Boolean constantPool;
		Integer dimension;

		IUnit unit;
		KindOfQuantity<?> ticksUnitKind;
		boolean unsigned;

		@Override
		public void setAttribute(String attribute, String value) {
			switch (attribute) {
			case "name": //$NON-NLS-1$
				fieldIdentifier = value;
				break;
			case "class": //$NON-NLS-1$
				classId = Long.parseLong(value);
				break;
			case "constantPool": //$NON-NLS-1$
				constantPool = Boolean.parseBoolean(value);
				break;
			case "dimension": //$NON-NLS-1$
				dimension = Integer.parseInt(value);
				break;
			default:
				break;
			}
		}

		@Override
		public IElement appendChild(String childName) {
			return super.appendChild(childName);
		}

		@Override
		void resolveAnnotation(String typeIdentifier, Map<String, String> values) throws InvalidJfrFileException {
			switch (typeIdentifier) {
			case ANNOTATION_UNSIGNED:
				unsigned = true;
				return;
			case ANNOTATION_MEMORY_AMOUNT:
			case ANNOTATION_DATA_AMOUNT:
				unit = UnitLookup.BYTE;
				return;
			case ANNOTATION_PERCENTAGE:
				unit = UnitLookup.PERCENT_UNITY;
				return;
			case ANNOTATION_MEMORY_ADDRESS:
				unit = UnitLookup.ADDRESS_UNITY;
				return;
			case ANNOTATION_TIMESPAN: {
				String unitId = values.get(VALUE);
				switch (unitId) {
				case UNIT_TICKS:
					ticksUnitKind = UnitLookup.TIMESPAN;
					return;
				case UNIT_NS:
					unit = UnitLookup.NANOSECOND;
					return;
				case UNIT_MS:
					unit = UnitLookup.MILLISECOND;
					return;
				case UNIT_S:
					unit = UnitLookup.SECOND;
					return;
				}
				return;
			}
			case ANNOTATION_TIMESTAMP: {
				String unitId = values.get(VALUE);
				switch (unitId) {
				case UNIT_TICKS:
					ticksUnitKind = UnitLookup.TIMESTAMP;
					return;
				case UNIT_NS_SINCE_EPOCH:
					unit = UnitLookup.EPOCH_NS;
					return;
				case UNIT_MS_SINCE_EPOCH:
					unit = UnitLookup.EPOCH_MS;
					return;
				case UNIT_S_SINCE_EPOCH:
					unit = UnitLookup.EPOCH_S;
					return;
				}
				return;
			}
			default:
				super.resolveAnnotation(typeIdentifier, values);
				return;
			}
		}

		boolean isStoredInPool() {
			return (constantPool != null) && constantPool;
		}

		boolean isArray() throws InvalidJfrFileException {
			if (dimension == null || dimension == 0) {
				return false;
			} else if (dimension == 1) {
				return true;
			} else {
				throw new InvalidJfrFileException("Array dimension " + dimension + " is not supported"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	static class RegionElement implements IElement {
		// NOTE: Fields that exist in the JFR metadata, but currently not used by JMC
//		String locale;
//		String gmtOffset;
//		String ticksToMillis;

		@Override
		public void setAttribute(String attribute, String value) {
		}

		@Override
		public IElement appendChild(String child) {
			return null;
		}
	}

	static RootElement readMetadata(IDataInput input) throws IOException, InvalidJfrFileException {
		input.readInt(); // size
		ParserToolkit.assertValue(input.readLong(), ChunkMetadata.METADATA_EVENT_TYPE); // type;
		input.readLong(); // event start
		input.readLong(); // event duration
		input.readLong(); // metadataId
		int stringCount = input.readInt();
		String[] strings = new String[stringCount];
		for (int i = 0; i < strings.length; i++) {
			strings[i] = input.readRawString(input.readByte());
		}
		int nameIndex = input.readInt(); // nameIndex
		if (!"root".equals(strings[nameIndex])) { //$NON-NLS-1$
			Logger.getLogger(ChunkMetadata.class.getName()).log(Level.WARNING,
					"Expected metadata root element to be named root, but it was named " + strings[nameIndex] //$NON-NLS-1$
							+ ". If the metadata parsing fails in a later stage, this might be the cause."); //$NON-NLS-1$
		}
		RootElement rootElement = new RootElement();
		loadElement(input, strings, rootElement);
		return rootElement;
	}

	private static void loadElement(IDataInput in, String[] s, IElement element)
			throws IOException, InvalidJfrFileException {
		int attributeCount = in.readInt();
		for (int i = 0; i < attributeCount; i++) {
			int key = in.readInt();
			int value = in.readInt();
			element.setAttribute(s[key], s[value]);
		}
		int elementCount = in.readInt();
		for (int i = 0; i < elementCount; i++) {
			int nameIndex = in.readInt();

			IElement child = element.appendChild(s[nameIndex]);
			if (child != null) {
				loadElement(in, s, child);
			} else {
				throw new InvalidJfrFileException(
						"Did not expect a child of " //$NON-NLS-1$
							+ element.getClass().getSimpleName() + " with name '" + s[nameIndex] + "'."); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
}
