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
package org.openjdk.jmc.flightrecorder.internal.parser.v0;

import org.openjdk.jmc.common.collection.FastAccessNumberMap;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.DecimalPrefix;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.LabeledIdentifier;
import org.openjdk.jmc.flightrecorder.internal.InvalidJfrFileException;
import org.openjdk.jmc.flightrecorder.internal.parser.LoaderContext;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.factories.GlobalObjectPool;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.factories.IPoolFactory;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.ContentTypeDescriptor;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.DataType;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.EventTypeDescriptor;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.ProducerDescriptor;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.ValueDescriptor;
import org.openjdk.jmc.flightrecorder.internal.util.DataInputToolkit;
import org.openjdk.jmc.flightrecorder.internal.util.JfrInternalConstants;

class ReaderFactory {

	private final FastAccessNumberMap<ConstantMap> constants = new FastAccessNumberMap<>(100, 5);
	private final ChunkMetadata metadata;
	private final FastAccessNumberMap<LabeledIdentifier> types = new FastAccessNumberMap<>();

	ReaderFactory(ChunkMetadata metadata, byte[] chunkData, LoaderContext context) throws InvalidJfrFileException {
		this.metadata = metadata;
		for (ProducerDescriptor pd : metadata.getProducers()) {
			for (ContentTypeDescriptor ct : pd.getContentTypes()) {
				IValueReader reader = createReader(ct.getDataStructure());
				IPoolFactory<?> factory = GlobalObjectPool.getFactory(ct, context);
				getConstantMap(ct.getContentTypeId()).init(reader, ct.getDataType(), factory);
			}
			for (EventTypeDescriptor etd : pd.getEventTypeDescriptors()) {
				types.put(etd.getIdentifier(), new LabeledIdentifier(pd.getURIString() + etd.getPath(),
						etd.getIdentifier(), etd.getLabel(), etd.getDescription()));
			}
		}
		int prevCpOffset = metadata.getPreviousCheckPoint();
		while (prevCpOffset != 0) {
			Offset offset = new Offset(chunkData, prevCpOffset);
			offset.increase(DataInputToolkit.INTEGER_SIZE); // skip event type
			long timestamp = readTicksTimestamp(chunkData, offset);
			prevCpOffset = (int) NumberReaders.readLong(chunkData, offset);
			while (offset.get() < offset.getEnd()) {
				int contentTypeId = NumberReaders.readInt(chunkData, offset);
				int entries = NumberReaders.readInt(chunkData, offset);
				ConstantMap entry = getConstantMap(contentTypeId);
				for (int n = 0; n < entries; n++) {
					entry.readValue(chunkData, offset, timestamp);
				}
			}
		}
		for (ConstantMap cp : constants) {
			cp.setLoadDone();
		}
		// Look up all Java Thread Id to force JavaThreadFactory to inject Java thread id and group name into FLRThread
		ConstantMap threadPool = constants.get(EventParserManager.CONTENT_TYPE_JAVATHREADID);
		if (threadPool != null) {
			threadPool.touchAll();
		}
	}

	long readTicksTimestamp(byte[] data, Offset offset) throws InvalidJfrFileException {
		return metadata.asNanoTimestamp(NumberReaders.readLong(data, offset));
	}

	private ConstantMap getConstantMap(int contentTypeId) {
		ConstantMap constantMap = constants.get(contentTypeId);
		if (constantMap == null) {
			constantMap = new ConstantMap();
			constants.put(contentTypeId, constantMap);
		}
		return constantMap;
	}

	private IValueReader createReader(ValueDescriptor[] vds) throws InvalidJfrFileException {
		if (vds.length == 1) {
			return createReader(vds[0], null);
		} else {
			IValueReader[] readers = new IValueReader[vds.length];
			for (int i = 0; i < readers.length; i++) {
				readers[i] = createReader(vds[i], null);
			}
			return new CompositeReader(readers);
		}
	}

	IValueReader createReader(ValueDescriptor vd, String valueType) throws InvalidJfrFileException {
		if (vd.getDataType().isPrimitive()) {
			return createPrimitiveReader(vd.getDataType(), vd.getContentType(), valueType);
		} else if (vd.getDataType() == DataType.ARRAY) {
			return new ArrayReader(createPrimitiveReader(vd.getInnerDataType(), vd.getContentType(), valueType));
		} else if (vd.getDataType() == DataType.STRUCTARRAY) {
			return new ArrayReader(createReader(vd.getChildren()));
		} else if (vd.getDataType() == DataType.STRUCT) {
			return createReader(vd.getChildren());
		} else {
			throw new InvalidJfrFileException();
		}
	}

	private IValueReader createPrimitiveReader(DataType dataType, int contentType, String valueType)
			throws InvalidJfrFileException {
		if (dataType.isNumeric()) {
			switch (contentType) {
			case EventParserManager.CONTENT_TYPE_EPOCHMILLIS:
				return new QuantityReader(dataType, UnitLookup.EPOCH_MS);
			case EventParserManager.CONTENT_TYPE_MILLIS:
				return new QuantityReader(dataType, UnitLookup.MILLISECOND);
			case EventParserManager.CONTENT_TYPE_NANOS:
				return new QuantityReader(dataType, UnitLookup.TIMESPAN.getUnit(DecimalPrefix.NANO));
			case EventParserManager.CONTENT_TYPE_TICKS:
				return new QuantityReader(dataType, metadata.getTicksUnit());
			case EventParserManager.CONTENT_TYPE_PERCENTAGE:
				return new QuantityReader(dataType, UnitLookup.PERCENT_UNITY);
			case EventParserManager.CONTENT_TYPE_MEMORY:
				return new QuantityReader(dataType, UnitLookup.BYTE);
			case EventParserManager.CONTENT_TYPE_ADDRESS:
				return new QuantityReader(dataType, UnitLookup.ADDRESS_UNITY);
			case EventParserManager.CONTENT_TYPE_POOL_NONE:
				if (JfrInternalConstants.TYPE_IDENTIFIER_VALUE_INTERPRETATION.equals(valueType)) {
					return new TypeIdentifierReader(dataType);
				} else {
					IUnit unit = UnitLookup.getUnitOrNull(valueType);
					if (unit != null) {
						return new QuantityReader(dataType, unit);
					}
				}
				return new QuantityReader(dataType, UnitLookup.NUMBER_UNITY);
			default:
				return createConstantReader(dataType, contentType);
			}
		} else if (dataType == DataType.BOOLEAN) {
			return BooleanReader.INSTANCE;
		} else if (dataType == DataType.STRING) {
			return StringReader.INSTANCE;
		} else if (dataType == DataType.UTF8) {
			return UTFStringParser.INSTANCE;
		}
		throw new InvalidJfrFileException();
	}

	IValueReader createConstantReader(DataType dataType, int contentType) throws InvalidJfrFileException {
		return new ConstantReader(getConstantMap(contentType), dataType);
	}

	private class TypeIdentifierReader implements IValueReader {

		private final DataType dataType;

		TypeIdentifierReader(DataType dataType) {
			this.dataType = dataType;
		}

		@Override
		public Object readValue(byte[] bytes, Offset offset, long timestamp) throws InvalidJfrFileException {
			long typeId = NumberReaders.readKey(bytes, offset, dataType);
			return types.get(typeId);
		}

		@Override
		public ContentType<?> getValueType() {
			return UnitLookup.LABELED_IDENTIFIER;
		}

	}

}
