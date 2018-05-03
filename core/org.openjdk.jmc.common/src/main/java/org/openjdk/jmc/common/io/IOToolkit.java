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

/**
 * Common functionality you might want when you're working with I/O.
 */
public final class IOToolkit {
	private static final int ZIP_MAGIC[] = new int[] {80, 75, 3, 4};
	private static final int GZ_MAGIC[] = new int[] {31, 139};

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
	 * Get an input stream for a optionally compressed file. If the file is compressed using either
	 * GZip or ZIP then an appropriate unpacking will be done.
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
			if (hasMagic(file, GZ_MAGIC)) {
				return new GZIPInputStream(in);
			} else if (hasMagic(file, ZIP_MAGIC)) {
				ZipInputStream zin = new ZipInputStream(in);
				zin.getNextEntry();
				return zin;
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
	 * Get an input stream for a optionally compressed input stream. If the input stream is
	 * compressed using either GZip or ZIP then an appropriate unpacking will be done.
	 * 
	 * @param stream
	 *            input stream to read from
	 * @return input stream for the unpacked content
	 * @throws IOException
	 *             on I/O error
	 */
	public static InputStream openUncompressedStream(InputStream stream) throws IOException {
		InputStream in = stream;
		if (!in.markSupported()) {
			in = new BufferedInputStream(stream);
		}
		in.mark(GZ_MAGIC.length + 1);
		if (hasMagic(in, GZ_MAGIC)) {
			in.reset();
			return new GZIPInputStream(in);
		}
		in.reset();
		in.mark(ZIP_MAGIC.length + 1);
		if (hasMagic(in, ZIP_MAGIC)) {
			in.reset();
			ZipInputStream zin = new ZipInputStream(in);
			zin.getNextEntry();
			return zin;
		}
		in.reset();
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
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			return hasMagic(fis, magic);
		} finally {
			closeSilently(fis);
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
		return hasMagic(file, GZ_MAGIC);
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
		return hasMagic(file, ZIP_MAGIC);
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
		BufferedInputStream is = null;
		try {
			is = new BufferedInputStream(new FileInputStream(file), ZIP_MAGIC.length + 1);
			is.mark(ZIP_MAGIC.length + 1);
			if (hasMagic(is, GZ_MAGIC)) {
				return true;
			}
			is.reset();
			return hasMagic(is, ZIP_MAGIC);
		} finally {
			closeSilently(is);
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
		FileReader fr = new FileReader(file);
		try {
			return loadFromReader(fr);
		} catch (IOException e) {
			throw e;
		} finally {
			closeSilently(fr);
		}
	}

	private static List<String> loadFromReader(Reader reader) throws IOException {
		List<String> lines = new ArrayList<>();
		BufferedReader br = new BufferedReader(reader);
		while (br.ready()) {
			lines.add(br.readLine());
		}
		return lines;
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
		PrintWriter pr = null;
		try {
			pr = new PrintWriter(new FileWriter(file));
			for (String line : lines) {
				pr.println(line);
			}
		} finally {
			closeSilently(pr);
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
		try {
			List<String> lines = new ArrayList<>();
			BufferedInputStream bis = new BufferedInputStream(is);
			BufferedReader r = new BufferedReader(new InputStreamReader(bis));
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
		FileOutputStream fos = new FileOutputStream(toOutput, append);
		BufferedOutputStream os = null;
		try {
			os = new BufferedOutputStream(fos);
			copy(in, os);
		} finally {
			closeSilently(os);
			closeSilently(fos);
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
		RandomAccessFile raf = new RandomAccessFile(file, "r"); //$NON-NLS-1$
		try {
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
		} finally {
			closeSilently(raf);
		}
	}
}
