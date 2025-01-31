/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.api.Recording;
import org.openjdk.jmc.flightrecorder.writer.api.RecordingSettings;
import org.openjdk.jmc.flightrecorder.writer.api.Type;
import org.openjdk.jmc.flightrecorder.writer.api.TypeStructureBuilder;
import org.openjdk.jmc.flightrecorder.writer.api.TypedFieldBuilder;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValue;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValueBuilder;
import org.openjdk.jmc.flightrecorder.writer.api.Types;

import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;
import jdk.jfr.Timestamp;

/**
 * The main entry point to JFR recording functionality. Allows to define custom types and initiate
 * {@link Chunk chunks} for writing user events.
 */
public final class RecordingImpl extends Recording {
	private static final Logger LOGGER = Logger.getLogger(RecordingImpl.class.getName());

	private static final byte[] MAGIC = new byte[] {'F', 'L', 'R', '\0'};
	private static final short MAJOR_VERSION = 2;
	private static final short MINOR_VERSION = 0;

	private static final long SIZE_OFFSET = 8;
	private static final long CONSTANT_OFFSET_OFFSET = 16;
	private static final long METADATA_OFFSET_OFFSET = 24;
	private static final long DURATION_NANOS_OFFSET = 40;

	private final Set<Chunk> activeChunks = new CopyOnWriteArraySet<>();
	private final LEB128Writer globalWriter = LEB128Writer.getInstance();
	private final InheritableThreadLocal<WeakReference<Chunk>> threadChunk = new InheritableThreadLocal<WeakReference<Chunk>>() {
		@Override
		protected WeakReference<Chunk> initialValue() {
			Chunk chunk = new Chunk();
			activeChunks.add(chunk);
			/*
			 * Use weak reference to minimize the damage caused by thread-local leaks. The chunk
			 * value is strongly held by activeChunks set and as such will not be released until it
			 * is removed from that set eg. in the close() method.
			 */
			return new WeakReference<>(chunk);
		}
	};

	private final long startTicks;
	private final long startNanos;
	private final long duration;

	private final OutputStream outputStream;

	private final AtomicBoolean closed = new AtomicBoolean();

	private final BlockingDeque<LEB128Writer> chunkDataQueue = new LinkedBlockingDeque<>();
	private final ExecutorService chunkDataMergingService = Executors.newSingleThreadExecutor();

	private final ConstantPools constantPools = new ConstantPools();
	private final MetadataImpl metadata = new MetadataImpl(constantPools);
	private final TypesImpl types;

	// a cache to hold already computed stack frames
	private final Map<StackTraceElement, TypedValue> frameCache = new HashMap<>(16000);
	// a cache to hold already resolved class loaders
	private final Map<String, TypedValue> classLoaderCache = new HashMap<>(128);
	// a cache to hold already resolved modules
	private final Map<String, TypedValue> moduleCache = new HashMap<>(4096);

	public RecordingImpl(OutputStream output, RecordingSettings settings) {
		this.startTicks = settings.getStartTicks() != -1 ? settings.getStartTicks() : System.nanoTime();
		this.startNanos = settings.getStartTimestamp() != -1 ? settings.getStartTimestamp()
				: System.currentTimeMillis() * 1_000_000L;
		this.duration = settings.getDuration();
		this.outputStream = output;
		this.types = new TypesImpl(metadata, settings.shouldInitializeJDKTypes());
		writeFileHeader();

		chunkDataMergingService.submit(() -> {
			try {
				while (!chunkDataMergingService.isShutdown()) {
					processChunkDataQueue(500, TimeUnit.MILLISECONDS);
				}
				// process any outstanding elements in the queue
				processChunkDataQueue(1, TimeUnit.NANOSECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
	}

	private void processChunkDataQueue(long pollTimeout, TimeUnit timeUnit) throws InterruptedException {
		LEB128Writer writer = chunkDataQueue.poll(pollTimeout, timeUnit);
		if (writer != null) {
			List<LEB128Writer> writers = new ArrayList<>();
			writers.add(writer);
			chunkDataQueue.drainTo(writers);

			for (LEB128Writer w : writers) {
				globalWriter.writeBytes(w.export());
			}
		}
	}

	@Override
	public RecordingImpl rotateChunk() {
		Chunk chunk = getChunk();
		activeChunks.remove(chunk);
		threadChunk.remove();

		chunk.finish(writer -> {
			try {
				chunkDataQueue.put(writer);
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}
		});
		return this;
	}

	@Override
	public void close() throws IOException {
		if (closed.compareAndSet(false, true)) {
			try {
				/*
				 * All active chunks are stable here - no new data will be added there so we can get
				 * away with slightly racy code ....
				 */
				for (Chunk chunk : activeChunks) {
					chunk.finish(writer -> {
						try {
							chunkDataQueue.put(writer);
						} catch (InterruptedException ignored) {
							Thread.currentThread().interrupt();
						}
					});
				}
				activeChunks.clear();

				chunkDataMergingService.shutdown();
				boolean flushed = false;
				try {
					flushed = chunkDataMergingService.awaitTermination(5, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				if (!flushed) {
					throw new RuntimeException("Unable to flush dangling JFR chunks");
				}
				finalizeRecording();

				outputStream.write(globalWriter.export());
			} finally {
				outputStream.close();
			}
		}
	}

	private Chunk getChunk() {
		if (closed.get()) {
			throw new IllegalStateException("Recording is already closed. Can not add more data.");
		}
		return threadChunk.get().get();
	}

	@Override
	public RecordingImpl writeEvent(TypedValue event) {
		getChunk().writeEvent((TypedValueImpl) event);
		return this;
	}

	@Override
	public TypeImpl registerEventType(String name) {
		return registerEventType(name, builder -> {
		});
	}

	@Override
	public TypeImpl registerEventType(String name, Consumer<TypeStructureBuilder> builderCallback) {
		if (name == null || builderCallback == null) {
			throw new IllegalArgumentException();
		}
		return registerType(name, "jdk.jfr.Event", builder -> {
			builder.addField("stackTrace", TypesImpl.JDK.STACK_TRACE).addField("eventThread", TypesImpl.JDK.THREAD)
					.addField("startTime", TypesImpl.Builtin.LONG,
							field -> field.addAnnotation(Types.JDK.ANNOTATION_TIMESTAMP, "TICKS"));
			builderCallback.accept(builder);
		});
	}

	@Override
	public TypeImpl registerAnnotationType(String name) {
		return registerAnnotationType(name, builder -> {
		});
	}

	@Override
	public TypeImpl registerAnnotationType(String name, Consumer<TypeStructureBuilder> builderCallback) {
		return registerType(name, Annotation.ANNOTATION_SUPER_TYPE_NAME, builderCallback);
	}

	@Override
	public TypeImpl registerType(String name, Consumer<TypeStructureBuilder> builderCallback) {
		return registerType(name, null, builderCallback);
	}

	@Override
	public TypeImpl registerType(String name, String supertype, Consumer<TypeStructureBuilder> builderCallback) {
		if (builderCallback == null || name == null) {
			throw new IllegalArgumentException();
		}
		return types.getOrAdd(name, supertype, builderCallback);
	}

	@Override
	public TypeImpl getType(TypesImpl.JDK type) {
		if (type == null) {
			throw new IllegalArgumentException();
		}
		return getType(type.getTypeName());
	}

	@Override
	public TypeImpl getType(String typeName) {
		if (typeName == null) {
			throw new IllegalArgumentException();
		}
		TypeImpl type = types.getType(typeName);
		if (type == null) {
			throw new IllegalArgumentException();
		}
		return type;
	}

	@Override
	public TypesImpl getTypes() {
		return types;
	}

	@Override
	public TypeImpl registerEventType(Class<? extends Event> eventType) {
		/*
		 * JMC implementation is slightly mishandling some event types - not using the special call
		 * and rather registering all implicit fields by hand.
		 */
		return registerType(getEventName(eventType), "jdk.jfr.Event", b -> {
			Field[] fields = eventType.getDeclaredFields();

			boolean eventThredOverride = false;
			boolean stackTraceOverride = false;
			boolean startTimeOverride = false;
			for (Field f : fields) {
				if (Modifier.isTransient(f.getModifiers()) || Modifier.isStatic(f.getModifiers())) {
					// skip static and transient fields
					continue;
				}
				// Add field definition
				TypeImpl fieldType = types.getType(f.getType().getName());
				if (fieldType != null) {
					java.lang.annotation.Annotation[] as = f.getAnnotations();
					String fieldName = getFieldName(f);
					if (fieldName.equals("startTime")) {
						startTimeOverride = true;
					} else if (fieldName.equals("eventThread")) {
						eventThredOverride = true;
					} else if (fieldName.equals("stackTrace")) {
						stackTraceOverride = true;
					}
					TypedFieldBuilder fieldTypeBuilder = types.fieldBuilder(fieldName, fieldType);

					boolean foundTimestampAnnotation = false;
					for (java.lang.annotation.Annotation a : as) {
						if (a.getClass().equals(Timestamp.class)) {
							foundTimestampAnnotation = true;
						}
						AnnotationValueObject val = processAnnotation(types, a);
						if (val != null) {
							fieldTypeBuilder = val.annotationValue != null
									? fieldTypeBuilder.addAnnotation(val.annotationType, val.annotationValue)
									: fieldTypeBuilder.addAnnotation(val.annotationType);
						}
					}
					if (fieldName.equals("startTime") && !foundTimestampAnnotation) {
						// make sure that even the overridden startTime field has a @Timestamp annotation
						fieldTypeBuilder.addAnnotation(Types.JDK.ANNOTATION_TIMESTAMP, "NANOSECONDS_SINCE_EPOCH");
					}
					b.addField(fieldTypeBuilder.build());
				}
			}
			if (!startTimeOverride) {
				// force 'startTime' field
				b.addField("startTime", Types.Builtin.LONG,
						field -> field.addAnnotation(Types.JDK.ANNOTATION_TIMESTAMP, "NANOSECONDS_SINCE_EPOCH"));
			}
			if (!eventThredOverride) {
				// force 'eventThread' field
				b.addField("eventThread", Types.JDK.THREAD);
			}
			if (!stackTraceOverride) {
				// force 'stackTrace' field if the event is collecting stacktraces
				if (hasStackTrace(eventType)) {
					b.addField("stackTrace", Types.JDK.STACK_TRACE);
				}
			}
			for (java.lang.annotation.Annotation a : eventType.getAnnotations()) {
				AnnotationValueObject val = processAnnotation(types, a);
				if (val != null) {
					b = val.annotationValue != null ? b.addAnnotation(val.annotationType, val.annotationValue)
							: b.addAnnotation(val.annotationType);
				}
			}
		});
	}

	@Override
	public RecordingImpl writeEvent(Event event) {
		registerEventType(event.getClass());
		writeEvent(createEventValue(event));
		return this;
	}

	private String getEventName(Class<? extends Event> eventType) {
		Name nameAnnotation = eventType.getAnnotation(Name.class);
		if (nameAnnotation != null) {
			return nameAnnotation.value();
		}
		return eventType.getSimpleName();
	}

	private String getFieldName(Field fld) {
		Name nameAnnotation = fld.getAnnotation(Name.class);
		if (nameAnnotation != null) {
			return nameAnnotation.value();
		}
		return fld.getName();
	}

	private boolean hasStackTrace(Class<? extends Event> eventType) {
		StackTrace stAnnotation = eventType.getAnnotation(StackTrace.class);
		if (stAnnotation != null) {
			return stAnnotation.value();
		}
		return false;
	}

	private TypedValue createEventValue(Event event) {
		Type eventType = getType(getEventName(event.getClass()));
		Field[] fields = event.getClass().getDeclaredFields();

		TypedValue typedValue = eventType.asValue(access -> {
			boolean startTimeWritten = false;
			boolean eventThreadWritten = false;
			boolean stackTraceWritten = false;
			for (Field f : fields) {
				f.setAccessible(true);

				/*
				 * From jdk.jfr.Event.java: Supported field types are the Java primitives: {@code
				 * boolean}, {@code char}, {@code byte}, {@code short}, {@code int}, {@code long},
				 * {@code float}, and {@code double}. Supported reference types are: {@code String},
				 * {@code Thread} and {@code Class}. Arrays, enums, and other reference types are
				 * silently ignored and not included. Fields that are of the supported types can be
				 * excluded by using the transient modifier. Static fields, even of the supported
				 * types, are not included.
				 */
				// Transient and static fields are excluded
				if (Modifier.isTransient(f.getModifiers()) || Modifier.isStatic(f.getModifiers())) {
					continue;
				}

				String fldName = getFieldName(f);
				if (fldName.equals("startTime")) {
					startTimeWritten = true;
				} else if (fldName.equals("eventThread")) {
					eventThreadWritten = true;
				} else if (fldName.equals("stackTrace")) {
					stackTraceWritten = true;
				}
				try {
					switch (f.getType().getName()) {
					case "byte": {
						byte byteValue = f.getByte(event);
						access.putField(fldName, byteValue);
						break;
					}
					case "char": {
						char charValue = f.getChar(event);
						access.putField(fldName, charValue);
						break;
					}
					case "short": {
						short shortValue = f.getShort(event);
						access.putField(fldName, shortValue);
						break;
					}
					case "int": {
						int intValue = f.getInt(event);
						access.putField(fldName, intValue);
						break;
					}
					case "long": {
						long longValue = f.getLong(event);
						access.putField(fldName, longValue);
						break;
					}
					case "float": {
						float floatValue = f.getFloat(event);
						access.putField(fldName, floatValue);
						break;
					}
					case "double": {
						double doubleValue = f.getDouble(event);
						access.putField(fldName, doubleValue);
						break;
					}
					case "boolean": {
						boolean booleanValue = f.getBoolean(event);
						access.putField(fldName, booleanValue);
						break;
					}
					case "java.lang.String": {
						String stringValue = (String) f.get(event);
						access.putField(fldName, stringValue);
						break;
					}
					case "java.lang.Class": {
						Class<?> clz = (Class<?>) f.get(event);
						access.putField(fldName, fldAccess -> {
							fldAccess.putField("name", nameAccess -> {
								nameAccess.putField("string", clz.getSimpleName());
							}).putField("package", clz.getPackage().getName()).putField("modifiers",
									clz.getModifiers());
						});
						break;
					}
					case "java.lang.Thread": {
						Thread thrd = (Thread) f.get(event);
						putThreadField(access, fldName, thrd);
						break;
					}
					case "java.lang.StackTraceElement[]": {
						StackTraceElement[] stackTrace = (StackTraceElement[]) f.get(event);
						putStackTraceField(access, fldName, stackTrace);
						break;
					}
					default: {
						LOGGER.log(Level.WARNING, "Cannot write type:" + f.getType().getName());
					}
					}
				} catch (IllegalAccessException e) {
					throw new RuntimeException();
				}
			}
			if (!startTimeWritten) {
				// default to 0
				access.putField("startTime", 0L);
			}
			if (!eventThreadWritten) {
				// default to current thread
				putThreadField(access, "eventThread", Thread.currentThread());
			}
			if (!stackTraceWritten && hasStackTrace(event.getClass())) {
				putStackTraceField(access, "stackTrace", Thread.currentThread().getStackTrace());
			}
		});

		return typedValue;
	}

	private void putThreadField(TypedValueBuilder access, String fldName, Thread thread) {
		access.putField(fldName, fldAccess -> {
			fldAccess.putField("javaThreadId", thread.getId()).putField("osThreadId", thread.getId())
					.putField("javaName", thread.getName());
		});
	}

	private void putStackTraceField(TypedValueBuilder access, String fldName, StackTraceElement[] stackTrace) {
		Types types = access.getType().getTypes();
		TypedValue[] frames = new TypedValue[stackTrace.length];
		boolean[] truncated = new boolean[] {false};
		for (int i = 0; i < stackTrace.length; i++) {
			frames[i] = asStackFrame(types, stackTrace[i]);
			if (i >= 8192) {
				truncated[0] = true;
				break;
			}
		}
		access.putField(fldName, p -> {
			p.putField("frames", frames).putField("truncated", truncated[0]);
		});
	}

	private TypedValue asStackFrame(Types types, StackTraceElement element) {
		return frameCache.computeIfAbsent(element, k -> types.getType(Types.JDK.STACK_FRAME).asValue(p -> {
			p.putField("method", methodValue(types, k)).putField("lineNumber", k.getLineNumber())
					.putField("bytecodeIndex", -1).putField("type", k.isNativeMethod() ? "native" : "java");
		}));
	}

	private TypedValue methodValue(Types types, StackTraceElement element) {
		return types.getType(Types.JDK.METHOD).asValue(p -> {
			p.putField("type", classValue(types, element)).putField("name", element.getMethodName());
		});
	}

	private TypedValue classValue(Types types, StackTraceElement element) {
		return types.getType(Types.JDK.CLASS).asValue(p -> {
			p.putField("classLoader", classLoaderValue(types, element.getClassLoaderName()))
					.putField("name", getSimpleName(element.getClassName())).putField("package",
							packageValue(types, getPackageName(element.getClassName()), element.getModuleName()));
		});
	}

	private TypedValue classLoaderValue(Types types, String classLoaderName) {
		return classLoaderCache.computeIfAbsent(classLoaderName,
				k -> types.getType(Types.JDK.CLASS_LOADER).asValue(p -> {
					p.putField("name", k);
				}));
	}

	private TypedValue packageValue(Types types, String packageName, String module) {
		return types.getType(Types.JDK.PACKAGE).asValue(p -> {
			p.putField("name", packageName).putField("module", moduleValue(types, module));
		});
	}

	private TypedValue moduleValue(Types types, String module) {
		return moduleCache.computeIfAbsent(module, k -> types.getType(Types.JDK.MODULE).asValue(p -> {
			p.putField("name", k);
		}));
	}

	private String getSimpleName(String className) {
		return className.substring(className.lastIndexOf('.') + 1);
	}

	private String getPackageName(String className) {
		int idx = className.lastIndexOf('.');
		if (idx > -1) {
			return className.substring(0, idx);
		}
		return "";
	}

	private AnnotationValueObject processAnnotation(Types types, java.lang.annotation.Annotation annotation) {
		// skip non-JFR related annotations
		if (!isJfrAnnotation(annotation)) {
			return null;
		}
		if (annotation instanceof Name) {
			// skip @Name annotation
			return null;
		}

		String value = null;
		try {
			Method m = annotation.getClass().getMethod("value");
			if (!String.class.isAssignableFrom(m.getReturnType())) {
				// wrong value type
				return null;
			}
			value = (String) m.invoke(annotation);
		} catch (NoSuchMethodException ignored) {
			// no-value annotations are also permitted
		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// error retrieving value attribute
			return null;
		}
		String annotationValue = value;
		String annotationTypeName = annotation.annotationType().getTypeName();
		Type annotationType = types.getOrAdd(annotationTypeName, Annotation.ANNOTATION_SUPER_TYPE_NAME, builder -> {
			if (annotationValue != null) {
				builder.addField("value", Types.Builtin.STRING);
			}
		});
		return new AnnotationValueObject(annotationType, annotationValue);
	}

	private boolean isJfrAnnotation(java.lang.annotation.Annotation target) {
		String typeName = target.annotationType().getName();
		if (typeName.startsWith("jdk.jfr.")) {
			return true;
		}
		for (java.lang.annotation.Annotation annotation : target.annotationType().getAnnotations()) {
			if (isJfrAnnotation(annotation)) {
				return true;
			}
		}
		return false;
	}

	private void writeFileHeader() {
		globalWriter.writeBytes(MAGIC).writeShortRaw(MAJOR_VERSION).writeShortRaw(MINOR_VERSION).writeLongRaw(0L) // size placeholder
				.writeLongRaw(0L) // CP event offset
				.writeLongRaw(0L) // meta event offset
				.writeLongRaw(startNanos) // start time in nanoseconds
				.writeLongRaw(0L) // duration placeholder
				.writeLongRaw(startTicks) // start time in ticks
				.writeLongRaw(1_000_000_000L) // 1 tick = 1 ns
				.writeIntRaw(1); // use compressed integers
	}

	private void finalizeRecording() {
		long recDuration = duration > 0 ? duration : System.nanoTime() - startTicks;
		types.resolveAll();

		long checkpointOffset = globalWriter.position();
		writeCheckpointEvent(recDuration);
		long metadataOffset = globalWriter.position();
		writeMetadataEvent(recDuration);

		globalWriter.writeLongRaw(DURATION_NANOS_OFFSET, recDuration);
		globalWriter.writeLongRaw(SIZE_OFFSET, globalWriter.position());
		globalWriter.writeLongRaw(CONSTANT_OFFSET_OFFSET, checkpointOffset);
		globalWriter.writeLongRaw(METADATA_OFFSET_OFFSET, metadataOffset);
	}

	private void writeCheckpointEvent(long duration) {
		LEB128Writer cpWriter = LEB128Writer.getInstance();

		cpWriter.writeLong(1L) // checkpoint event ID
				.writeLong(startNanos) // start timestamp
				.writeLong(duration) // duration till now
				.writeLong(0L) // fake delta-to-next
				.writeInt(1) // all checkpoints are flush for now
				.writeInt(metadata.getConstantPools().size()); // start writing constant pools array

		for (ConstantPool cp : metadata.getConstantPools()) {
			cp.writeTo(cpWriter);
		}

		globalWriter.writeInt(cpWriter.length()); // write event size
		globalWriter.writeBytes(cpWriter.export());
	}

	private void writeMetadataEvent(long duration) {
		metadata.writeMetaEvent(globalWriter, startTicks, duration);
	}
}
