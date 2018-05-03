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
package org.openjdk.jmc.joverflow.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmc.common.io.IOToolkit;

/**
 * Simple file-related utilities.
 */
public class FileUtils {

	public static ArrayList<String> readTextFile(String fileName) throws IOException {
		return readTextFile(new File(fileName));
	}

	public static ArrayList<String> readTextFile(File file) throws IOException {
		FileReader reader = new FileReader(file);
		BufferedReader br = new BufferedReader(reader);

		ArrayList<String> lines = new ArrayList<>();
		String s;
		try {
			while ((s = br.readLine()) != null) {
				lines.add(s);
			}
		} finally {
			br.close();
		}

		return lines;
	}

	public static byte[] readBytesFromFile(String fileName) throws IOException {
		return readBytesFromFile(new File(fileName));
	}

	public static byte[] readBytesFromFile(File file) throws IOException {
		long longSize = file.length();
		if (longSize > Integer.MAX_VALUE) {
			throw new IOException(
					"File length is " + longSize + ". Cannot read files longer than " + Integer.MAX_VALUE);
		}
		int size = (int) longSize;
		byte[] result = new byte[size];

		BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
		try {
			int readBytes = 0;
			while (readBytes < size) {
				readBytes += in.read(result, readBytes, size - readBytes);
			}
		} finally {
			in.close();
		}

		return result;
	}

	public static void writeTextToFile(File file, List<String> lines) throws IOException {
		PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)));
		for (String line : lines) {
			out.println(line);
		}
		out.close();
	}

	public static void writeBytesToFile(File file, byte[] bytes) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		try {
			out.write(bytes);
		} finally {
			IOToolkit.closeSilently(out);
		}
	}

	public static File fileExistsAndReadableOrExit(String fileName) {
		try {
			return checkFileExistsAndReadable(fileName, false);
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
			System.exit(-1);
		}
		return null; // Never reached; just makes the compiler happy
	}

	public static File dirExistsAndReadableOrExit(String dirName) {
		try {
			return checkFileExistsAndReadable(dirName, true);
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
			System.exit(-1);
		}
		return null; // Never reached; just makes the compiler happy
	}

	public static File checkFileExistsAndReadable(String fileName, boolean isDirectory) throws IOException {
		File file = new File(fileName);
		if (!file.exists()) {
			throw new IOException("File " + fileName + " does not exist");
		}

		if (isDirectory) {
			if (!file.isDirectory()) {
				throw new IOException(fileName + " is not a directory");
			}
		} else if (!file.isFile()) {
			throw new IOException("File " + fileName + " is not a normal file");
		}

		if (!file.canRead()) {
			throw new IOException("File " + fileName + " cannot be read");
		}
		return file;
	}

	public static File dirExistsAndWritableOrExit(String dirName) {
		File dir = new File(dirName);
		if (!dir.exists()) {
			System.err.println("Directory " + dirName + " does not exist");
			System.exit(-1);
		}

		if (!dir.isDirectory()) {
			System.err.println("File " + dirName + " is not a directory");
			System.exit(-1);
		}

		if (!dir.canWrite()) {
			System.err.println("Directory " + dirName + " is not writable");
			System.exit(-1);
		}

		return dir;
	}

	public static File fileWritableOrExit(String fileName) {
		File f = new File(fileName);

		if (f.exists() && !f.isFile()) {
			System.err.println("File " + fileName + " is not a normal file");
			System.exit(-1);
		}

		if (f.exists() && !f.canWrite()) {
			System.err.println("File " + fileName + " is not writable");
			System.exit(-1);
		}

		try {
			FileOutputStream fo = new FileOutputStream(f);
			fo.close();
		} catch (IOException ex) {
			System.err.println("Cannot write to file " + fileName + ": " + ex.getMessage());
			System.exit(-1);
		}

		return f;
	}
}
