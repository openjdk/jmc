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
package org.openjdk.jmc.flightrecorder.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Program for splitting a JFR file. Run without arguments to list usage.
 */
public class SplitRecording {
	private final static int MIB = 1024 * 1024;

	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.out.println("Usage:"); //$NON-NLS-1$
			System.out.println("java SplitRecording filename [targetSizePerFile in MiB]"); //$NON-NLS-1$
			System.out.println();
			System.out.println("Note that files will be aligned on chunk boundaries. The target size per file "); //$NON-NLS-1$
			System.out.println("is a rough estimate. Files can be both larger and smaller than the set value. "); //$NON-NLS-1$
			System.out.println("If no target size is specified, the file will be split into chunks."); //$NON-NLS-1$
			System.exit(2);
		}

		File file = new File(args[0]);
		if (!file.exists()) {
			System.out.println("The specified file does not exist: " + args[0]); //$NON-NLS-1$
			System.exit(3);
		}

		int targetSize = -1;
		if (args.length > 1) {
			targetSize = Integer.parseInt(args[1]) * MIB;
		}

		split(file, targetSize);
	}

	private static void split(File file, int targetSize) throws IOException {
		System.out.println("Splitting " + file + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		String namePattern = getNamePatternFromFile(file);
		Iterator<byte[]> chunks = ChunkReader.readChunks(file);
		if (targetSize == -1) {
			System.out.println("No target size - will split along chunk borders"); //$NON-NLS-1$
			writeAllChunks(namePattern, chunks);
		} else {
			System.out.println("Target size is " + targetSize / MIB + " MiB"); //$NON-NLS-1$ //$NON-NLS-2$
			writecCollatedChunks(targetSize, namePattern, chunks);
		}
		System.out.println("All done!"); //$NON-NLS-1$
	}

	private static void writecCollatedChunks(int targetSize, String namePattern, Iterator<byte[]> chunks)
			throws IOException {
		List<byte[]> writeList = new ArrayList<>();
		int writeCount = 0;
		int sum = 0;
		while (chunks.hasNext()) {
			byte[] nextChunk = chunks.next();

			if (sum != 0) {
				if (distance(sum, targetSize) <= distance(sum + nextChunk.length, targetSize)) {
					writeChunks(writeCount++, writeList, namePattern);
					writeList.clear();
					sum = 0;
				}
			}
			writeList.add(nextChunk);
			sum += nextChunk.length;
		}
	}

	private static void writeChunks(int i, List<byte[]> writeList, String namePattern) throws IOException {
		writeChunks(getFile(i, namePattern), writeList);
	}

	private static int distance(int sum, int targetSize) {
		return Math.abs(sum - targetSize);
	}

	private static void writeAllChunks(String namePattern, Iterator<byte[]> chunks) throws IOException {
		for (int i = 0; chunks.hasNext(); i++) {
			writeChunk(getFile(i, namePattern), chunks.next());
		}
	}

	private static File getFile(int i, String namePattern) {
		return new File(String.format(namePattern, i));
	}

	private static String getNamePatternFromFile(File file) {
		String absolutePath = file.getAbsolutePath();
		String extension = getExtension(file);
		absolutePath = absolutePath.substring(0, absolutePath.length() - extension.length());
		return absolutePath + "_%d" + extension; //$NON-NLS-1$
	}

	private static void writeChunk(File file, byte[] chunk) throws IOException {
		System.out.print("Writing " + file.getName() + "... "); //$NON-NLS-1$ //$NON-NLS-2$
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(chunk);
		}
		System.out.println("finished!"); //$NON-NLS-1$
	}

	private static void writeChunks(File file, List<byte[]> chunks) throws IOException {
		System.out.print(String.format("Writing %d chunk(s) to %s...", chunks.size(), file.getName())); //$NON-NLS-1$
		try (FileOutputStream fos = new FileOutputStream(file)) {
			for (byte[] chunk : chunks) {
				fos.write(chunk);
			}
		}
		System.out.println("finished!"); //$NON-NLS-1$
	}

	private static String getExtension(File file) {
		int index = file.getName().lastIndexOf('.');
		if (index == -1) {
			return ""; //$NON-NLS-1$
		} else {
			return file.getName().substring(index);
		}
	}
}
