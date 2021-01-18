/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.api.Recording;
import org.openjdk.jmc.flightrecorder.writer.api.TypeStructureBuilder;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValue;

/**
 * The main entry point to JFR recording functionality. Allows to define custom types and initiate
 * {@link Chunk chunks} for writing user events.
 */
public final class RecordingImpl extends Recording {
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

	private final OutputStream outputStream;

	private final AtomicBoolean closed = new AtomicBoolean(false);

	private final BlockingDeque<LEB128Writer> chunkDataQueue = new LinkedBlockingDeque<>();
	private final ExecutorService chunkDataMergingService = Executors.newSingleThreadExecutor();

	private final ConstantPools constantPools = new ConstantPools();
	private final MetadataImpl metadata = new MetadataImpl(constantPools);
	private final TypesImpl types = new TypesImpl(metadata);

	public RecordingImpl(OutputStream output) {
		this.startTicks = System.nanoTime();
		this.startNanos = System.currentTimeMillis() * 1_000_000L;
		this.outputStream = output;
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
		System.err.println("=== rotate chunk");
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
							field -> field.addAnnotation(TypesImpl.JDK.ANNOTATION_TIMESTAMP, "TICKS"));
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

	private void writeFileHeader() {
		globalWriter.writeBytes(MAGIC).writeShortRaw(MAJOR_VERSION).writeShortRaw(MINOR_VERSION).writeLongRaw(0L) // size placeholder
				.writeLongRaw(0L) // CP event offset
				.writeLongRaw(0L) // meta event offset
				.writeLongRaw(startNanos) // start timestamp
				.writeLongRaw(0L) // duration placeholder
				.writeLongRaw(startTicks).writeLongRaw(1_000_000_000L) // 1 tick = 1 ns
				.writeIntRaw(1); // use compressed integers
	}

	private void finalizeRecording() {
		long duration = System.nanoTime() - startTicks;
		types.resolveAll();

		long checkpointOffset = globalWriter.position();
		writeCheckpointEvent();
		long metadataOffset = globalWriter.position();
		writeMetadataEvent(duration);

		globalWriter.writeLongRaw(DURATION_NANOS_OFFSET, duration);
		globalWriter.writeLongRaw(SIZE_OFFSET, globalWriter.position());
		globalWriter.writeLongRaw(CONSTANT_OFFSET_OFFSET, checkpointOffset);
		globalWriter.writeLongRaw(METADATA_OFFSET_OFFSET, metadataOffset);
	}

	private void writeCheckpointEvent() {
		LEB128Writer cpWriter = LEB128Writer.getInstance();

		cpWriter.writeLong(1L) // checkpoint event ID
				.writeLong(startNanos) // start timestamp
				.writeLong(System.nanoTime() - startTicks) // duration till now
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
