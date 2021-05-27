/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.utils.releasenotes;

import static java.nio.file.Files.*;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Arrays;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Wrapper for Xalan to do an XSLT. Used to generate the release notes HTML.
 * <p>
 * Example: Transform -IN notes.xml -XSL stylesheet.xsl -OUT new_and_noteworthy.html
 */
public class Transform {

	public static void main(String[] args) {
		final ArrayDeque<String> deque = new ArrayDeque<>(Arrays.asList(args));
		Path inputFile = null;
		Path outputFile = null;
		Path sylesheetFile = null;
		for (; !deque.isEmpty();) {
			switch (deque.poll()) {
			case "-IN":
				inputFile = checkedToPath(deque.poll(), "input file", true);
				break;
			case "-OUT":
				outputFile = checkedToPath(deque.poll(), "output file", false);
				break;
			case "-XSL":
				sylesheetFile = checkedToPath(deque.poll(), "stylesheet file", true);
				break;
			default:
				break;
			}
		}
		if (inputFile != null && outputFile != null && sylesheetFile != null) {
			try {
				transform(inputFile, outputFile, sylesheetFile);
				System.exit(0);
			} catch (IOException | TransformerException e) {
				e.printStackTrace();
				System.exit(1);
			}
		} else {
			usage();
			System.exit(1);
		}
	}

	private static void usage() {
		System.out.println("Usage: Transform -IN <input file> -XSL <stylesheet file> -OUT <output file>");
	}

	private static Path checkedToPath(String arg, String fileDescription, boolean isInputFile) {
		if (arg == null) {
			System.err.format("%s not given", fileDescription).println();
		} else {
			Path file = Paths.get(arg);
			if (isInputFile && exists(file)) {
				return file;
			} else {
				System.err.format("%s '%s' not found", fileDescription, file).println();
			}
		}
		return null;
	}

	private static void transform(Path inputFile, Path outputFile, Path sylesheetFile)
			throws IOException, TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		try (InputStream xslIn = newInputStream(sylesheetFile);
				InputStream in = newInputStream(inputFile);
				OutputStream out = newOutputStream(outputFile)) {
			transformerFactory.newTransformer(new StreamSource(xslIn)).transform(new StreamSource(in),
					new StreamResult(out));
		}
	}
}
