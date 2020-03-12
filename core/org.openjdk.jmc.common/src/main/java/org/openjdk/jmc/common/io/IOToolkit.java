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
package org.openjdk.jmc.common.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import net.jpountz.lz4.LZ4FrameInputStream;

/**
 * Common functionality you might want when you're working with I/O.
 */
public final class IOToolkit {
	/**
	 * Magic bytes for recognizing Zip.
	 */
	private static final int MAGIC_ZIP[] = new int[] {80, 75, 3, 4};

	/**
	 * Magic bytes for recognizing GZip.
	 */
	private static final int MAGIC_GZ[] = new int[] {31, 139};

	/**
	 * Magic bytes for recognizing LZ4.
	 */
	private static final int MAGIC_LZ4[] = new int[] {4, 34, 77, 24};

	private IOToolkit() {
		throw new Error("Don't"); //$NON-NLS-1$
	}

	/**
	 * Closes a closeable. Typically you call this in a final statement so the method also ignores
	 * if the closeable is null.
	 *
	 * @param closeable
	 *            object to close, may be null
	 */
	public static void closeSilently(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				// keep your mouth shut
			}
		}
	}

	/**
	 * Get an input stream for a optionally compressed file. If the file is compressed using GZip,
	 * ZIP or LZ4, then an appropriate unpacking will be done.
	 *
	 * @param file
	 *            file to read from
	 * @return input stream for the unpacked file content
	 * @throws IOException
	 *             on I/O error
	 */
	public static InputStream openUncompressedStream(File file) throws IOException {
		FileInputStream fin = new FileInputStream(file);
		try {
			InputStream in = new BufferedInputStream(fin);
			if (hasMagic(file, MAGIC_GZ)) {
				return new GZIPInputStream(in);
			} else if (hasMagic(file, MAGIC_ZIP)) {
				ZipInputStream zin = new ZipInputStream(in);
				zin.getNextEntry();
				return zin;
			} else if (hasMagic(file, MAGIC_LZ4)) {
				return new LZ4FrameInputStream(in);
			}
			return in;
		} catch (RuntimeException e) {
			closeSilently(fin);
			throw e;
		} catch (IOException e) {
			closeSilently(fin);
			throw e;
		} catch (Error e) {
			closeSilently(fin);
			throw e;
		}
	}

	/**
	 * Get an input stream for a optionally compressed input stream. If the file is compressed using
	 * GZip, ZIP or LZ4, then an appropriate unpacking will be done.
	 *
	 * @param stream
	 *            input stream to read from
	 * @return input stream for the unpacked content
	 * @throws IOException
	 *             on I/O error
	 */
	public static InputStream openUncompressedStream(InputStream stream) throws IOException {
		InputStream in = stream;
		if (in.markSupported()) {
			in.mark(MAGIC_GZ.length + 1);
			if (hasMagic(in, MAGIC_GZ)) {
				in.reset();
				return new GZIPInputStream(in);
			}
			in.reset();
			in.mark(MAGIC_ZIP.length + 1);
			if (hasMagic(in, MAGIC_ZIP)) {
				in.reset();
				ZipInputStream zin = new ZipInputStream(in);
				zin.getNextEntry();
				return zin;
			}
			in.reset();
			in.mark(MAGIC_LZ4.length + 1);
			if (hasMagic(in, MAGIC_LZ4)) {
				in.reset();
				return new LZ4FrameInputStream(in);
			}
			in.reset();
		}
		in = new BufferedInputStream(stream);
		return in;
	}

	/**
	 * Checks if a file begins with a specified array of bytes.
	 *
	 * @param file
	 *            the file to examine
	 * @param magic
	 *            the magic data, an array with values between 0 and 255
	 * @return {@code true} if the file begins with the magic, {@code false} otherwise
	 * @throws IOException
	 *             if an error occurred when trying to read from the file
	 */
	public static boolean hasMagic(File file, int[] magic) throws IOException {
		try (FileInputStream fis = new FileInputStream(file)) {
			return hasMagic(fis, magic);
		}
	}

	/**
	 * Checks if an input stream begins with a specified array of bytes. The input stream will be
	 * positioned at the first byte after the magic data after this call.
	 *
	 * @param is
	 *            the input stream to examine
	 * @param magic
	 *            the magic data, an array with values between 0 and 255
	 * @return {@code true} if the input stream begins with the magic, {@code false} otherwise
	 * @throws IOException
	 *             if an error occurred when trying to read from the stream
	 */
	public static boolean hasMagic(InputStream is, int[] magic) throws IOException {
		for (int element : magic) {
			int b = is.read();
			if (b != element) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns true if the file is GZip file.
	 *
	 * @param file
	 *            the file to examine
	 * @return {@code true} if it is a GZip file, {@code false} otherwise
	 * @throws IOException
	 *             if an error occurred when trying to read from the file
	 */
	public static boolean isGZipFile(File file) throws IOException {
		return hasMagic(file, MAGIC_GZ);
	}

	/**
	 * Returns true if the file is LZ4 compressed.
	 *
	 * @param file
	 *            the file to examine
	 * @return {@code true} if it is an LZ4 compressed file, {@code false} otherwise
	 * @throws IOException
	 *             if an error occurred when trying to read from the file
	 */
	public static boolean isLZ4File(File file) throws IOException {
		return hasMagic(file, MAGIC_LZ4);
	}

	/**
	 * Checks if the file is a ZIP archive.
	 *
	 * @param file
	 *            the file to examine
	 * @return {@code true} if it's a ZIP archive, {@code false} otherwise
	 * @throws IOException
	 *             if an error occurred when trying to read from the file
	 */
	public static boolean isZipFile(File file) throws IOException {
		return hasMagic(file, MAGIC_ZIP);
	}

	/**
	 * Returns the magic bytes for identifying Gzip. This is a defensive copy. It's up to the user
	 * to cache this to avoid excessive allocations.
	 * 
	 * @return a copy of the magic bytes for Gzip.
	 */
	public static int[] getGzipMagic() {
		return MAGIC_GZ.clone();
	}

	/**
	 * Returns the magic bytes for identifying Zip. This is a defensive copy. It's up to the user to
	 * cache this to avoid excessive allocations.
	 * 
	 * @return a copy of the magic bytes for Zip.
	 */
	public static int[] getZipMagic() {
		return MAGIC_ZIP.clone();
	}

	/**
	 * Returns the magic bytes for identifying LZ4. This is a defensive copy. It's up to the user to
	 * cache this to avoid excessive allocations.
	 * 
	 * @return a copy of the magic bytes for LZ4.
	 */
	public static int[] getLz4Magic() {
		return MAGIC_LZ4.clone();
	}

	/**
	 * Checks if the file is compressed in a way compatible with
	 * {@link #openUncompressedStream(File)}.
	 *
	 * @param file
	 *            the file to examine
	 * @return {@code true} if the file is compressed in a manner which can be uncompressed by
	 *         {@link #openUncompressedStream(File)}, {@code false} otherwise
	 * @throws IOException
	 *             if an error occurred when trying to read from the file
	 */
	public static boolean isCompressedFile(File file) throws IOException {
		try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(file), MAGIC_ZIP.length + 1)) {
			is.mark(MAGIC_ZIP.length + 1);
			if (hasMagic(is, MAGIC_GZ)) {
				return true;
			}
			is.reset();
			if (hasMagic(is, MAGIC_ZIP)) {
				return true;
			}
			;
			is.reset();
			return hasMagic(is, MAGIC_LZ4);
		}
	}

	/**
	 * Read lines from a text file.
	 *
	 * @see #saveToFile(File, List)
	 * @param file
	 *            file to read lines from
	 * @return a list of strings, one for each line in the file
	 * @throws IOException
	 *             on I/O error
	 */
	public static List<String> loadFromFile(File file) throws IOException {
		try (FileReader fr = new FileReader(file)) {
			return loadFromReader(fr);
		}
	}

	private static List<String> loadFromReader(Reader reader) throws IOException {
		List<String> lines = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(reader)) {
			while (br.ready()) {
				lines.add(br.readLine());
			}
			return lines;
		}
	}

	/**
	 * Write lines to a text file. If the file already exists, it will be overwritten.
	 *
	 * @see #loadFromFile(File)
	 * @param file
	 *            file to write lines to
	 * @param lines
	 *            a list of strings that will be written on one line each
	 * @throws IOException
	 *             on I/O error
	 */
	public static void saveToFile(File file, List<String> lines) throws IOException {
		try (PrintWriter pr = new PrintWriter(new FileWriter(file))) {
			for (String line : lines) {
				pr.println(line);
			}
		}
	}

	/**
	 * Read lines from an input stream.
	 *
	 * @see #saveToFile(File, List)
	 * @param is
	 *            input stream to read lines from
	 * @return a list of strings, one for each line in the stream
	 * @throws IOException
	 *             on I/O error
	 */
	public static List<String> loadFromStream(InputStream is) throws IOException {
		try (BufferedInputStream bis = new BufferedInputStream(is);
				BufferedReader r = new BufferedReader(new InputStreamReader(bis))) {
			List<String> lines = new ArrayList<>();
			while (r.ready()) {
				lines.add(r.readLine());
			}
			return lines;
		} finally {
			closeSilently(is);
		}
	}

	/**
	 * Copy all data from an input stream to a file.
	 *
	 * @param in
	 *            input stream to read from
	 * @param toOutput
	 *            file to write to
	 * @param append
	 *            {@code true} if the file should be appended to, {@code false} if it should be
	 *            overwritten
	 * @throws IOException
	 *             on I/O error
	 */
	public static void write(InputStream in, File toOutput, boolean append) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(toOutput, append);
				BufferedOutputStream os = new BufferedOutputStream(fos)) {
			copy(in, os);
		}
	}

	/**
	 * Copy all data from an input stream to an output stream.
	 *
	 * @param is
	 *            input stream to read from
	 * @param os
	 *            output stream to write to
	 * @throws IOException
	 *             on I/O error
	 */
	public static void copy(InputStream is, OutputStream os) throws IOException {
		copy(is, os, 1024);
	}

	/**
	 * Copy all data from an input stream to an output stream.
	 *
	 * @param is
	 *            input stream to read from
	 * @param os
	 *            output stream to write to
	 * @param bufferSize
	 *            size of the buffer used when copying data
	 * @throws IOException
	 *             on I/O error
	 */
	public static void copy(InputStream is, OutputStream os, int bufferSize) throws IOException {
		int length;
		byte[] buffer = new byte[bufferSize];
		while ((length = is.read(buffer)) > 0) {
			os.write(buffer, 0, length);
		}
		is.close();
	}

	/**
	 * Copies srcFile to targetFile. Will do nothing if srcFile and targetFile are the same file.
	 * Will copy file attributes.
	 *
	 * @param srcFile
	 *            source file to copy data from
	 * @param targetFile
	 *            target file to copy data to
	 * @throws IOException
	 *             if something goes wrong during the copy
	 */
	public static void copyFile(File srcFile, File targetFile) throws IOException {
		Files.copy(srcFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING,
				StandardCopyOption.COPY_ATTRIBUTES);
	}

	/**
	 * Calculates an MD5 hash on ten evenly distributed 1kB blocks from the file.
	 *
	 * @param file
	 *            file to calculate hash for
	 * @return MD5 hash string
	 * @throws IOException
	 *             if something goes wrong when reading file data
	 */
	public static String calculateFileHash(File file) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) { //$NON-NLS-1$
			long seek = raf.length() / 10;
			byte[] buffer = new byte[1024];
			MessageDigest hash = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
			int read;
			while ((read = raf.read(buffer)) > 0) {
				hash.update(buffer, 0, read);
				raf.seek(raf.getFilePointer() + seek);
			}
			return new BigInteger(1, hash.digest()).toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}
