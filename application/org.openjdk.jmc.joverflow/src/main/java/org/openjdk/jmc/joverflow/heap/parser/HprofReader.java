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
package org.openjdk.jmc.joverflow.heap.parser;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.openjdk.jmc.joverflow.heap.model.ArrayTypeCodes;
import org.openjdk.jmc.joverflow.heap.model.JavaBoolean;
import org.openjdk.jmc.joverflow.heap.model.JavaByte;
import org.openjdk.jmc.joverflow.heap.model.JavaChar;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaDouble;
import org.openjdk.jmc.joverflow.heap.model.JavaField;
import org.openjdk.jmc.joverflow.heap.model.JavaFloat;
import org.openjdk.jmc.joverflow.heap.model.JavaInt;
import org.openjdk.jmc.joverflow.heap.model.JavaLong;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectRef;
import org.openjdk.jmc.joverflow.heap.model.JavaShort;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.heap.model.Root;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.model.StackFrame;
import org.openjdk.jmc.joverflow.heap.model.StackTrace;
import org.openjdk.jmc.joverflow.util.FileUtils;
import org.openjdk.jmc.joverflow.util.LongToObjectMap;
import org.openjdk.jmc.joverflow.util.MiscUtils;
import org.openjdk.jmc.joverflow.util.VerboseOutputCollector;

/**
 * Functionality for reading a hprof file.
 */
class HprofReader extends HeapDumpReader /* imports */ implements ArrayTypeCodes {
	final static int MAGIC_NUMBER = 0x4a415641;
	// That's "JAVA", the first part of "JAVA PROFILE ..."
	private final static String[] VERSIONS = {" PROFILE 1.0\0", " PROFILE 1.0.1\0", " PROFILE 1.0.2\0",};

	// The following version numbers are indices into VERSIONS. The instance
	// data member version is set to one of these, and it drives decisions when
	// reading the file.
	//
	// Version 1.0.1 added HPROF_GC_PRIM_ARRAY_DUMP, which requires no
	// version-sensitive parsing.
	//
	// Version 1.0.1 changed the type of a constant pool entry from a signature
	// to a typecode.
	//
	// Version 1.0.2 added HPROF_HEAP_DUMP_SEGMENT and HPROF_HEAP_DUMP_END
	// to allow a large heap to be dumped as a sequence of heap dump segments.
	//
	// The HPROF agent in J2SE 1.2 through to 5.0 generate a version 1.0.1
	// file. In Java SE 6.0 the version is either 1.0.1 or 1.0.2 depending on
	// the size of the heap (normally it will be 1.0.1 but for multi-GB
	// heaps the heap dump will not fit in a HPROF_HEAP_DUMP record so the
	// dump is generated as version 1.0.2).
	@SuppressWarnings("unused")
	private final static int VERSION_JDK12BETA3 = 0;
	private final static int VERSION_JDK12BETA4 = 1;
	private final static int VERSION_JDK6 = 2;

	// Record types
	static final int HPROF_UTF8 = 0x01;
	static final int HPROF_LOAD_CLASS = 0x02;
	static final int HPROF_UNLOAD_CLASS = 0x03;
	static final int HPROF_FRAME = 0x04;
	static final int HPROF_TRACE = 0x05;
	static final int HPROF_ALLOC_SITES = 0x06;
	static final int HPROF_HEAP_SUMMARY = 0x07;

	static final int HPROF_START_THREAD = 0x0a;
	static final int HPROF_END_THREAD = 0x0b;

	static final int HPROF_HEAP_DUMP = 0x0c;

	static final int HPROF_CPU_SAMPLES = 0x0d;
	static final int HPROF_CONTROL_SETTINGS = 0x0e;
	static final int HPROF_LOCKSTATS_WAIT_TIME = 0x10;
	static final int HPROF_LOCKSTATS_HOLD_TIME = 0x11;

	static final int HPROF_GC_ROOT_UNKNOWN = 0xff;
	static final int HPROF_GC_ROOT_JNI_GLOBAL = 0x01;
	static final int HPROF_GC_ROOT_JNI_LOCAL = 0x02;
	static final int HPROF_GC_ROOT_JAVA_FRAME = 0x03;
	static final int HPROF_GC_ROOT_NATIVE_STACK = 0x04;
	static final int HPROF_GC_ROOT_STICKY_CLASS = 0x05;
	static final int HPROF_GC_ROOT_THREAD_BLOCK = 0x06;
	static final int HPROF_GC_ROOT_MONITOR_USED = 0x07;
	static final int HPROF_GC_ROOT_THREAD_OBJ = 0x08;

	static final int HPROF_GC_CLASS_DUMP = 0x20;
	static final int HPROF_GC_INSTANCE_DUMP = 0x21;
	static final int HPROF_GC_OBJ_ARRAY_DUMP = 0x22;
	static final int HPROF_GC_PRIM_ARRAY_DUMP = 0x23;

	static final int HPROF_HEAP_DUMP_SEGMENT = 0x1c;
	static final int HPROF_HEAP_DUMP_END = 0x2c;

	private final static int T_CLASS = 2;

	private final ReadBuffer.Factory bufFactory;

	private final File hprofFile; // Non-null if we use a real disk file
	private final byte[] fileImageBytes; // Non-null if we use a byte[] array with file image
	private PositionDataInputStream in;
	private final long fileSize;

	private int version; // The version of .hprof being read

	private int dumpsToSkip;

	private int identifierSize; // Size, in bytes, of identifiers in HPROF file.
	private final LongToObjectMap<String> names;

	// HashMap<Integer, ThreadObject>, used to map the thread sequence number
	// (aka "serial number") to the thread object ID for
	// HPROF_GC_ROOT_THREAD_OBJ.  ThreadObject is a trivial inner class,
	// at the end of this file.
	private HashMap<Integer, ThreadObject> threadObjects;

	/** Maps class object ID to class name (in dotted format) */
	private LongToObjectMap<String> classNameFromObjectID;

	// Maps stack frame ID to StackFrame.
	// Null if we are not tracking call stacks
	private HashMap<Long, StackFrame> stackFrames;

	// HashMap<Integer, StackTrace> maps stack frame ID to StackTrace
	// Null if we are not tracking call stacks
	private HashMap<Integer, StackTrace> stackTraces;

	// Maps class serial # to class object ID
	// Null if we are not tracking call stacks
	private HashMap<Integer, String> classNameFromSerialNo;

	private Snapshot.Builder snpBuilder;

	// Maximum size of a mapped byte buffer used for lazy reads of object contents.
	// If the heap dump file is longer than this, we need to use more than one BB for it.
	private static final int MAX_BB_SIZE = Integer.MAX_VALUE;

	// True if heap dump is longer than 2GB - in that case we'll have to create multiple
	// mapped byte buffers to perform random reads efficiently
	private final boolean longFile;

	private final ArrayList<Long> mappedBBEndOfs;
	private long currentBBMaxOfs;
	private long prevObjStartOfs;

	// If > 0, use this instead of the value that we half-read/half-guess from the snapshot
	private final int explicitPointerSize;

	// Diagnostics and progress tracking
	private final VerboseOutputCollector vc;
	private volatile boolean cancelled;

	HprofReader(ReadBuffer.Factory bufFactory, boolean callStack, int explicitPointerSize, VerboseOutputCollector vc)
			throws DumpCorruptedException {
		this.bufFactory = bufFactory;
		String fileName = bufFactory.getFileName();
		int dumpNumber = 1;
		if (fileName != null) {
			int pos = fileName.lastIndexOf('#');
			if (pos > -1) {
				String num = fileName.substring(pos + 1, fileName.length());
				try {
					dumpNumber = Integer.parseInt(num, 10);
				} catch (NumberFormatException ex) {
					String msg = "in file name \"" + fileName + "\", a dump number was "
							+ "expected after the :, but \"" + num + "\" was found instead.";
					throw new DumpCorruptedException(msg);
				}
				fileName = fileName.substring(0, pos);
			}
		}

		fileImageBytes = bufFactory.getFileImageBytes();
		if (fileImageBytes == null) { // .hprof file will be read from disk
			try {
				hprofFile = FileUtils.checkFileExistsAndReadable(fileName, false);
				this.fileSize = hprofFile.length();
				if (fileSize == 0) {
					throw new DumpCorruptedException("file size is 0");
				}
			} catch (IOException ex) {
				throw new DumpCorruptedException(ex.getMessage());
			}
		} else { // We have the .hprof file bytes in Java heap - typically in tests
			hprofFile = null;
			this.fileSize = fileImageBytes.length;
		}

		this.vc = vc;

		this.dumpsToSkip = dumpNumber - 1;
		this.explicitPointerSize = explicitPointerSize;
		names = new LongToObjectMap<>((int) (fileSize / 100000), false);
		threadObjects = new HashMap<>(43);
		classNameFromObjectID = new LongToObjectMap<>(1000, false);
		if (callStack) {
			stackFrames = new HashMap<>(43);
			stackTraces = new HashMap<>(43);
			classNameFromSerialNo = new HashMap<>();
		}

		longFile = fileSize > MAX_BB_SIZE;
		if (longFile) {
			mappedBBEndOfs = new ArrayList<>();
			currentBBMaxOfs = MAX_BB_SIZE - 1;
		} else {
			mappedBBEndOfs = null;
		}
	}

	@Override
	public Snapshot read() throws DumpCorruptedException, HprofParsingCancelledException {
		String dumpCorruptedExMsg = "";
		ReadBuffer readBuf = null;
		try {
			if (hprofFile != null) {
				in = new PositionDataInputStream(new BufferedInputStream(new FileInputStream(hprofFile)));
			} else {
				in = new PositionDataInputStream(new ByteArrayInputStream(fileImageBytes));
			}

			doRead();

			// Some very simple/obvious sanity checks
			if (snpBuilder.getNumAllObjects() == 0) {
				throw new DumpCorruptedException("did not read any objects");
			}
			if (snpBuilder.getNumClasses() == 0) {
				throw new DumpCorruptedException("did not read any classes");
			}

			snpBuilder.onFinishReadObjects();

			long[] mappedBBEndOfsArray = null;
			if (mappedBBEndOfs != null) {
				mappedBBEndOfsArray = new long[mappedBBEndOfs.size() + 1];
				for (int i = 0; i < mappedBBEndOfs.size(); i++) {
					mappedBBEndOfsArray[i] = mappedBBEndOfs.get(i);
				}
				mappedBBEndOfsArray[mappedBBEndOfsArray.length - 1] = fileSize - 1;
			}
			readBuf = bufFactory.create(mappedBBEndOfsArray);
		} catch (IOException ex) {
			dumpCorruptedExMsg = "caught exception " + ex + ". Details:\n";
			StringWriter exWriterBuf = new StringWriter(200);
			ex.printStackTrace(new PrintWriter(exWriterBuf));
			dumpCorruptedExMsg += exWriterBuf.toString();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ex) {
					dumpCorruptedExMsg += "\nAlso, could not close the stream properly: caught exception " + ex;
					if (readBuf != null) {
						readBuf.close();
					}
				}
			}
		}

		if (dumpCorruptedExMsg.length() > 0) {
			throw new DumpCorruptedException(dumpCorruptedExMsg);
		}

		try {
			return snpBuilder.buildSnapshot(readBuf);
		} catch (RuntimeException ex) {
			if (readBuf != null) {
				readBuf.close();
			}
			throw ex;
		} catch (Error er) { // Most likely an OOM
			if (readBuf != null) {
				readBuf.close();
			}
			throw er;
		}
	}

	@Override
	public synchronized int getProgressPercentage() {
		if (in != null) {
			return (int) (in.position() * 100 / fileSize);
		} else {
			return 0;
		}
	}

	@Override
	public void cancelReading() {
		cancelled = true;
	}

	private void doRead() throws DumpCorruptedException, IOException, HprofParsingCancelledException {
		int magicNumber = in.readInt();
		if (magicNumber != MAGIC_NUMBER) {
			throw new DumpCorruptedException("unrecognized magic number: " + magicNumber);
		}

		version = readVersionHeader();
		identifierSize = in.readInt();
		if (identifierSize != 4 && identifierSize != 8) {
			throw new DumpCorruptedException("unsupported format: " + "specifies pointer size of " + identifierSize
					+ ". JOverflow supports only size 4 and 8.");
		}

		snpBuilder = new Snapshot.Builder(fileSize, identifierSize, explicitPointerSize, vc);

		skipBytes(8); // long creationDate = in.readLong();
//		System.out.println("Dump file created " + (new Date(creationDate)));

		while (true) {
			int type;
			try {
				type = in.readUnsignedByte();
			} catch (EOFException ignored) {
				break;
			}
			in.readInt(); // Timestamp of this record
			// Length of record: readInt() will return negative value for record length >2GB. So store 32bit value in long to keep it unsigned.
			long length = in.readInt() & 0xffffffffL;
//			System.out.println("Read record type " + type + ", length " + length + " at position " + toHex(currPos));
			if (length < 0) {
				throw new DumpCorruptedException(
						"bad record length of " + length + " at byte " + (in.position() - 4) + " of file.");
			}
			switch (type) {
			case HPROF_UTF8: {
				long id = readID();
				byte[] chars = new byte[(int) length - identifierSize];
				in.readFully(chars);
				names.put(id, new String(chars));
				break;
			}

			case HPROF_LOAD_CLASS: {
				int serialNo = in.readInt(); // Not used
				long classID = readID();
				in.readInt(); // int stackTraceSerialNo, unused
				long classNameID = readID();
				String nm = getNameFromID(classNameID).replace('/', '.');
				classNameFromObjectID.put(classID, nm);
				if (classNameFromSerialNo != null) {
					classNameFromSerialNo.put(serialNo, nm);
				}
				break;
			}

			case HPROF_HEAP_DUMP: {
				if (dumpsToSkip <= 0) {
					try {
						vc.debug("Sub-dump of length " + length + " starts at position " + in.position());
						readHeapDump(length);
					} catch (EOFException exp) {
						handleEOF(exp);
					}
//					System.out.println("Finished processing instances in heap dump.");
					return;
				} else {
					dumpsToSkip--;
					skipBytes(length);
				}
				break;
			}

			case HPROF_HEAP_DUMP_END: {
				if (version >= VERSION_JDK6) {
					if (dumpsToSkip <= 0) {
						skipBytes(length); // should be no-op
						return;
					} else {
						// skip this dump (of the end record for a sequence of dump segments)
						dumpsToSkip--;
					}
				} else {
					// HPROF_HEAP_DUMP_END only recognized in >= 1.0.2
					vc.addWarning("Ignoring unrecognized record type", Integer.toString(type));
				}
				skipBytes(length); // should be no-op
				break;
			}

			case HPROF_HEAP_DUMP_SEGMENT: {
				if (version >= VERSION_JDK6) {
					if (dumpsToSkip <= 0) {
						try {
							vc.debug("Segment of length " + length + " starts at position " + in.position());
							// read the dump segment
							readHeapDump(length);
						} catch (EOFException exp) {
							handleEOF(exp);
						}
					} else {
						// all segments comprising the heap dump will be skipped
						skipBytes(length);
					}
				} else {
					// HPROF_HEAP_DUMP_SEGMENT only recognized in >= 1.0.2
					vc.addWarning("Ignoring unrecognized record type", Integer.toString(type));
					skipBytes(length);
				}
				break;
			}

			case HPROF_FRAME: {
				if (stackFrames == null) {
					skipBytes(length);
				} else {
					long id = readID();
					String methodName = getNameFromID(readID());
					String methodSig = getNameFromID(readID());
					String sourceFile = getNameFromID(readID());
					int classSer = in.readInt();
					String className = classNameFromSerialNo.get(Integer.valueOf(classSer));
					int lineNumber = in.readInt();
					if (lineNumber < StackFrame.LINE_NUMBER_NATIVE) {
						vc.addWarning("Weird stack frame line number", Integer.toString(lineNumber));
						lineNumber = StackFrame.LINE_NUMBER_UNKNOWN;
					}
					stackFrames.put(id, new StackFrame(methodName, methodSig, className, sourceFile, lineNumber));
				}
				break;
			}

			case HPROF_TRACE: {
				if (stackTraces == null) {
					skipBytes(length);
				} else {
					int serialNo = in.readInt();
					in.readInt(); // int threadSeq, not used
					StackFrame[] frames = new StackFrame[in.readInt()];
					for (int i = 0; i < frames.length; i++) {
						long fid = readID();
						frames[i] = stackFrames.get(Long.valueOf(fid));
						if (frames[i] == null) {
							throw new DumpCorruptedException("stack frame " + toHex(fid) + " not found");
						}
					}
					stackTraces.put(serialNo, new StackTrace(frames));
				}
				break;
			}

			case HPROF_HEAP_SUMMARY:
			case HPROF_UNLOAD_CLASS:
			case HPROF_ALLOC_SITES:
			case HPROF_START_THREAD:
			case HPROF_END_THREAD:
			case HPROF_CPU_SAMPLES:
			case HPROF_CONTROL_SETTINGS:
			case HPROF_LOCKSTATS_WAIT_TIME:
			case HPROF_LOCKSTATS_HOLD_TIME: {
				// Ignore these record types
				skipBytes(length);
				break;
			}

			default: {
				skipBytes(length);
				vc.addWarning("Ignoring unrecognized record type", Integer.toString(type));
			}
			}
		}
	}

	private void skipBytes(long length) throws IOException, DumpCorruptedException {
		long remainingBytes = length;
		do {
			int skippedBytes = in.skipBytes((int) length);
			remainingBytes -= skippedBytes;
			if (remainingBytes > 0) {
				if (in.position() >= fileSize) {
					throw new DumpCorruptedException("Reached end of file while trying to skip " + length + " bytes");

				}
			}
		} while (remainingBytes > 0);
	}

	private int readVersionHeader() throws IOException, DumpCorruptedException {
		int candidatesLeft = VERSIONS.length;
		boolean[] matched = new boolean[VERSIONS.length];
		for (int i = 0; i < candidatesLeft; i++) {
			matched[i] = true;
		}

		int pos = 0;
		while (candidatesLeft > 0) {
			char c = (char) in.readByte();
			for (int i = 0; i < VERSIONS.length; i++) {
				if (matched[i]) {
					if (c != VERSIONS[i].charAt(pos)) { // Not matched
						matched[i] = false;
						--candidatesLeft;
					} else if (pos == VERSIONS[i].length() - 1) { // Full match
						vc.debug("Hprof file version: " + VERSIONS[i]);
						return i;
					}
				}
			}
			++pos;
		}
		throw new DumpCorruptedException("version string not recognized at byte " + (pos + 3));
	}

	private void readHeapDump(long dumpLength)
			throws DumpCorruptedException, IOException, HprofParsingCancelledException {
		long startPos = in.position();
		long endPos = startPos + dumpLength;
		// "Chunks" below are used to check for cancellation periodically
		int curChunk = (int) (in.position() >> 19); // Check every 512K

		long id, pos;
		while ((pos = in.position()) < endPos) {
			int recordType = in.readUnsignedByte();

			int newCurChunk = (int) (pos >> 19);
			if (newCurChunk > curChunk) {
				curChunk = newCurChunk;
				checkForCancellation();
			}

			switch (recordType) {
			case HPROF_GC_INSTANCE_DUMP: {
				readInstance();
				break;
			}
			case HPROF_GC_OBJ_ARRAY_DUMP: {
				readArray(false);
				break;
			}
			case HPROF_GC_PRIM_ARRAY_DUMP: {
				readArray(true);
				break;
			}

			case HPROF_GC_ROOT_UNKNOWN: {
				id = readID();
				snpBuilder.addRoot(new Root(id, 0, Root.UNKNOWN, ""));
				break;
			}
			case HPROF_GC_ROOT_THREAD_OBJ: {
				id = readID();
				int threadSeq = in.readInt();
				int stackSeq = in.readInt();
				threadObjects.put(threadSeq, new ThreadObject(id, stackSeq));
				break;
			}
			case HPROF_GC_ROOT_JNI_GLOBAL: {
				id = readID();
				readID(); // long globalRefId, ignored for now
				snpBuilder.addRoot(new Root(id, 0, Root.JNI_GLOBAL, ""));
				break;
			}
			case HPROF_GC_ROOT_JNI_LOCAL: {
				id = readID();
				int threadSeq = in.readInt();
				int depth = in.readInt();
				ThreadObject to = getThreadObjectFromSequence(threadSeq);
				StackTrace st = getStackTraceFromSerial(to.stackSeq);
				if (st != null) {
					st = st.traceForDepth(depth + 1);
				}
				snpBuilder.addRoot(new Root(id, to.threadId, Root.JNI_LOCAL, "", st));
				break;
			}
			case HPROF_GC_ROOT_JAVA_FRAME: {
				id = readID();
				int threadSeq = in.readInt();
				int depth = in.readInt();
				ThreadObject to = getThreadObjectFromSequence(threadSeq);
				StackTrace st = getStackTraceFromSerial(to.stackSeq);
				if (st != null) {
					st = st.traceForDepth(depth + 1);
				}
				snpBuilder.addRoot(new Root(id, to.threadId, Root.JAVA_LOCAL, "", st));
				break;
			}
			case HPROF_GC_ROOT_NATIVE_STACK: {
				id = readID();
				int threadSeq = in.readInt();
				ThreadObject to = getThreadObjectFromSequence(threadSeq);
				StackTrace st = getStackTraceFromSerial(to.stackSeq);
				snpBuilder.addRoot(new Root(id, to.threadId, Root.NATIVE_STACK, "", st));
				break;
			}
			case HPROF_GC_ROOT_STICKY_CLASS: {
				id = readID();
				snpBuilder.addRoot(new Root(id, 0, Root.SYSTEM_CLASS, ""));
				break;
			}
			case HPROF_GC_ROOT_THREAD_BLOCK: {
				id = readID();
				int threadSeq = in.readInt();
				ThreadObject to = getThreadObjectFromSequence(threadSeq);
				StackTrace st = getStackTraceFromSerial(to.stackSeq);
				snpBuilder.addRoot(new Root(id, to.threadId, Root.THREAD_BLOCK, "", st));
				break;
			}
			case HPROF_GC_ROOT_MONITOR_USED: {
				id = readID();
				snpBuilder.addRoot(new Root(id, 0, Root.BUSY_MONITOR, ""));
				break;
			}
			case HPROF_GC_CLASS_DUMP: {
				readClass();
				break;
			}
			default: {
				throw new DumpCorruptedException("unrecognized heap dump sub-record type:  " + recordType
						+ ". Technical info: position = " + pos + ", bytes left = " + (endPos - pos));
			}
			}
		}

		if (pos != endPos) {
			vc.addWarning("Error reading heap dump or heap dump segment",
					"Byte count is " + pos + " instead of " + endPos + ". Difference is " + (endPos - pos));
			skipBytes(endPos - pos);
		}
	}

	private long readID() throws IOException {
		return (identifierSize == 4) ? (Snapshot.SMALL_ID_MASK & in.readInt()) : in.readLong();
	}

	/**
	 * Read a java value. If result is non-null, it's expected to be an array of one element. We use
	 * it to fake multiple return values. Returns the number of bytes read.
	 */
	private int readValue(JavaThing[] resultArr) throws DumpCorruptedException, IOException {
		byte type = in.readByte();
		return 1 + readValueForType(type, resultArr);
	}

	private int readValueForType(byte type, JavaThing[] resultArr) throws DumpCorruptedException, IOException {
		if (version >= VERSION_JDK12BETA4) {
			type = signatureFromTypeId(type);
		}
		return readValueForTypeSignature(type, resultArr);
	}

	private int readValueForTypeSignature(byte type, JavaThing[] resultArr) throws DumpCorruptedException, IOException {
		switch (type) {
		case '[':
		case 'L': {
			long id = readID();
			if (resultArr != null) {
				resultArr[0] = new JavaObjectRef(id);
			}
			return identifierSize;
		}
		case 'Z': {
			int b = in.readByte();
			if (b != 0 && b != 1) {
				vc.addWarning("Illegal boolean value read", Integer.toString(b));
			}
			if (resultArr != null) {
				resultArr[0] = new JavaBoolean(b != 0);
			}
			return 1;
		}
		case 'B': {
			byte b = in.readByte();
			if (resultArr != null) {
				resultArr[0] = new JavaByte(b);
			}
			return 1;
		}
		case 'S': {
			short s = in.readShort();
			if (resultArr != null) {
				resultArr[0] = new JavaShort(s);
			}
			return 2;
		}
		case 'C': {
			char ch = in.readChar();
			if (resultArr != null) {
				resultArr[0] = new JavaChar(ch);
			}
			return 2;
		}
		case 'I': {
			int val = in.readInt();
			if (resultArr != null) {
				resultArr[0] = new JavaInt(val);
			}
			return 4;
		}
		case 'J': {
			long val = in.readLong();
			if (resultArr != null) {
				resultArr[0] = new JavaLong(val);
			}
			return 8;
		}
		case 'F': {
			float val = in.readFloat();
			if (resultArr != null) {
				resultArr[0] = new JavaFloat(val);
			}
			return 4;
		}
		case 'D': {
			double val = in.readDouble();
			if (resultArr != null) {
				resultArr[0] = new JavaDouble(val);
			}
			return 8;
		}
		default: {
			throw new DumpCorruptedException("Bad value signature:  " + type);
		}
		}
	}

	private ThreadObject getThreadObjectFromSequence(int threadSeq) throws DumpCorruptedException, IOException {
		ThreadObject to = threadObjects.get(Integer.valueOf(threadSeq));
		if (to == null) {
			throw new DumpCorruptedException("thread " + threadSeq + " not found for JNI local ref");
		}
		return to;
	}

	private String getNameFromID(long id) throws IOException {
		if (id == 0L) {
			return "";
		}
		String result = names.get(id);
		if (result == null) {
			vc.addWarning("name not found", "at " + toHex(id));
			return "unresolved name " + toHex(id);
		}
		return result;
	}

	private StackTrace getStackTraceFromSerial(int ser) throws IOException {
		if (stackTraces == null) {
			return null;
		}
		StackTrace result = stackTraces.get(Integer.valueOf(ser));
		if (result == null) {
			vc.addWarning("Stack trace not found", "for serial # " + ser);
		}
		return result;
	}

	/** Handles a HPROF_GC_CLASS_DUMP. Returns the number of bytes read. */
	private int readClass() throws DumpCorruptedException, IOException {
		long id = readID();
		skipBytes(4); // StackTrace stackTrace = getStackTraceFromSerial(in.readInt());
		long superId = readID();
		long classLoaderId = readID();
		long signersId = readID();
		long protDomainId = readID();
		readID(); // long reserved1, unused
		readID(); // long reserved2, unused
		int fieldsSize = in.readInt();
		int bytesRead = 7 * identifierSize + 8;

		int numConstPoolEntries = in.readUnsignedShort();
		bytesRead += 2;
		for (int i = 0; i < numConstPoolEntries; i++) {
			in.readUnsignedShort(); // int index, unused
			bytesRead += 2;
			bytesRead += readValue(null); // We ignore the values
		}

		int numStatics = in.readUnsignedShort();
		bytesRead += 2;
		// We may need additional quasi-fields for signers and protection domain
		int numQuasiFields = (signersId != 0 || protDomainId != 0) ? 2 : 0;
		int nAllStatics = numStatics + numQuasiFields;
		JavaField[] staticFields = nAllStatics > 0 ? new JavaField[nAllStatics] : JavaClass.NO_FIELDS;
		JavaThing[] staticValues = nAllStatics > 0 ? new JavaThing[nAllStatics] : JavaClass.NO_VALUES;
		if (numStatics > 0) {
			JavaThing[] valueBin = new JavaThing[1];
			for (int i = 0; i < numStatics; i++) {
				long nameId = readID();
				bytesRead += identifierSize;
				byte type = in.readByte();
				bytesRead++;
				bytesRead += readValueForType(type, valueBin);
				String fieldName = getNameFromID(nameId);
				if (version >= VERSION_JDK12BETA4) {
					type = signatureFromTypeId(type);
				}
				staticFields[i] = JavaField.newInstance(fieldName, (char) type, snpBuilder.getPointerSize());
				staticValues[i] = valueBin[0];
			}
		}
		if (numQuasiFields > 0) {
			JavaField.addStaticQuaziFields(staticFields);
		}

		int numFields = in.readUnsignedShort();
		bytesRead += 2;
		JavaField[] fields = numFields > 0 ? new JavaField[numFields] : JavaClass.NO_FIELDS;
		for (int i = 0; i < numFields; i++) {
			long nameId = readID();
			bytesRead += identifierSize;
			byte type = in.readByte();
			bytesRead++;
			String fieldName = getNameFromID(nameId);
			if (version >= VERSION_JDK12BETA4) {
				type = signatureFromTypeId(type);
			}
			fields[i] = JavaField.newInstance(fieldName, (char) type, snpBuilder.getPointerSize());
		}

		String name = classNameFromObjectID.get(id);
		if (name == null) {
			vc.addWarning("Class name not found", "for " + toHex(id));
			name = "unknown-name@" + toHex(id);
		}

		JavaClass c = new JavaClass(id, name, superId, classLoaderId, signersId, protDomainId, fields, staticFields,
				staticValues, fieldsSize, snpBuilder.getInMemoryInstanceSize(fieldsSize));
		snpBuilder.addClass(c);

		return bytesRead;
	}

	private String toHex(long addr) {
		return MiscUtils.toHex(addr);
	}

	/**
	 * Handles a HPROF_GC_INSTANCE_DUMP Return number of bytes read
	 */
	private int readInstance() throws DumpCorruptedException, IOException {
		long objOfsInFile = in.position();
		long id = readID();
		skipBytes(4); // StackTrace stackTrace = getStackTraceFromSerial(in.readInt());
		long classID = readID();
		int objDataSize = in.readInt();
		int bytesRead = (2 * identifierSize) + 8 + objDataSize;
		skipBytes(objDataSize);
		snpBuilder.addJavaObject(id, classID, objOfsInFile, objDataSize);
		if (longFile) {
			handlePossibleBBBorder(objOfsInFile);
		}
		return bytesRead;
	}

	/**
	 * Handles a HPROF_GC_OBJ_ARRAY_DUMP or HPROF_GC_PRIM_ARRAY_DUMP. Returns number of bytes read.
	 */
	private int readArray(boolean isPrimitive) throws DumpCorruptedException, IOException {
		long objOfsInFile = in.position();
		long id = readID();
		skipBytes(4); // StackTrace stackTrace = getStackTraceFromSerial(in.readInt());
		int num = in.readInt();
		int bytesRead = identifierSize + 8;
		long arrayClassID;
		if (isPrimitive) {
			arrayClassID = in.readByte();
			bytesRead++;
		} else {
			arrayClassID = readID();
			bytesRead += identifierSize;
		}

		// Check for primitive arrays:
		char primitiveSignature = 0x00;
		int elSize = 0;
		if (isPrimitive || version < VERSION_JDK12BETA4) {
			switch ((int) arrayClassID) {
			case T_BOOLEAN: {
				primitiveSignature = 'Z';
				elSize = 1;
				break;
			}
			case T_CHAR: {
				primitiveSignature = 'C';
				elSize = 2;
				break;
			}
			case T_FLOAT: {
				primitiveSignature = 'F';
				elSize = 4;
				break;
			}
			case T_DOUBLE: {
				primitiveSignature = 'D';
				elSize = 8;
				break;
			}
			case T_BYTE: {
				primitiveSignature = 'B';
				elSize = 1;
				break;
			}
			case T_SHORT: {
				primitiveSignature = 'S';
				elSize = 2;
				break;
			}
			case T_INT: {
				primitiveSignature = 'I';
				elSize = 4;
				break;
			}
			case T_LONG: {
				primitiveSignature = 'J';
				elSize = 8;
				break;
			}
			}
			if (version >= VERSION_JDK12BETA4 && primitiveSignature == 0x00) {
				throw new DumpCorruptedException("unrecognized typecode: " + arrayClassID);
			}
		}

		int dataSize = isPrimitive ? elSize * num : identifierSize * num;
		if (in.position() + dataSize > fileSize) {
			throw new DumpCorruptedException((isPrimitive ? "Primitive" : "Object") + " array at position "
					+ in.position() + " is " + dataSize + " bytes long, that does not fit into the dump file");
		}

		bytesRead += dataSize;
		skipBytes(dataSize);

		if (isPrimitive) {
			snpBuilder.addJavaValueArray(id, primitiveSignature, objOfsInFile, num, dataSize);
		} else {
			snpBuilder.addJavaObjectArray(id, arrayClassID, objOfsInFile, num, dataSize);
		}
		if (longFile) {
			handlePossibleBBBorder(objOfsInFile);
		}

		return bytesRead;
	}

	private byte signatureFromTypeId(byte typeId) throws DumpCorruptedException, IOException {
		switch (typeId) {
		case T_CLASS:
			return (byte) 'L';
		case T_BOOLEAN:
			return (byte) 'Z';
		case T_CHAR:
			return (byte) 'C';
		case T_FLOAT:
			return (byte) 'F';
		case T_DOUBLE:
			return (byte) 'D';
		case T_BYTE:
			return (byte) 'B';
		case T_SHORT:
			return (byte) 'S';
		case T_INT:
			return (byte) 'I';
		case T_LONG:
			return (byte) 'J';
		default:
			throw new DumpCorruptedException("invalid type id of " + typeId);
		}
	}

	private void handlePossibleBBBorder(long thisObjStartOfs) {
		if (thisObjStartOfs >= currentBBMaxOfs) {
			if (prevObjStartOfs > 0) {
				// Normal case
				mappedBBEndOfs.add(prevObjStartOfs - 1);
			} else {
				// Seems to happen only in tests, when maxBBSize is small
				mappedBBEndOfs.add(Long.valueOf(MAX_BB_SIZE));
			}
			currentBBMaxOfs = mappedBBEndOfs.get(mappedBBEndOfs.size() - 1) + MAX_BB_SIZE;
		}
		prevObjStartOfs = thisObjStartOfs;
	}

	private void handleEOF(EOFException exp) {
		vc.addWarning("Unexpected EOF", "Will miss information");
		// we have EOF, we have to tolerate missing references
		snpBuilder.setUnresolvedObjectsOk(true);
	}

	private void checkForCancellation() throws HprofParsingCancelledException {
		if (cancelled) {
			throw new HprofParsingCancelledException();
		}
	}

	/**
	 * A trivial data-holder class for HPROF_GC_ROOT_THREAD_OBJ.
	 */
	private static class ThreadObject {

		long threadId;
		int stackSeq;

		ThreadObject(long threadId, int stackSeq) {
			this.threadId = threadId;
			this.stackSeq = stackSeq;
		}
	}
}
