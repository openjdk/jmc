/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
 */

package org.openjdk.jmc.utils.releasenotes;

import com.sun.org.apache.xalan.internal.xslt.Process;

public class Transform {
	// Transform -IN notes.xml -XSL stylesheet.xsl -OUT new_and_noteworthy.html
	public static void main(String[] args) {
		Process._main(args);
	}
}
