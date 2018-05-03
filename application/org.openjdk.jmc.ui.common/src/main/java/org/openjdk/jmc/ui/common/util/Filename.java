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
package org.openjdk.jmc.ui.common.util;

import java.text.SimpleDateFormat;
import java.util.Random;

/**
 * Class for handling file names. Primarily used for creating unique file names.
 */
public class Filename {

	private static Random RAND = new Random();
	private final String name;
	private final String ext;

	public Filename(String name, String ext) {
		this.name = name;
		this.ext = ext;
	}

	public String getName() {
		return name;
	}

	public String getExtension() {
		return ext;
	}

	public Filename asRandomFilename() {
		int nextInt = RAND.nextInt();
		SimpleDateFormat sf = new SimpleDateFormat("_yyyy-MM-dd_HH-mm-ss_"); //$NON-NLS-1$
		return new Filename(name + sf.format(System.currentTimeMillis()) + Integer.toHexString(nextInt), ext);
	}

	@Override
	public String toString() {
		return ext.isEmpty() ? name : name + "." + ext; //$NON-NLS-1$
	}

	public static Filename splitFilename(String name) {
		int lastPeriod = name.lastIndexOf('.');
		if (lastPeriod < 0) {
			return new Filename(name, ""); //$NON-NLS-1$
		} else {
			return new Filename(name.substring(0, lastPeriod), name.substring(lastPeriod + 1));
		}
	}
}
