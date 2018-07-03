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
package org.openjdk.jmc.flightrecorder.rules.jdk.util;

import java.util.Map;

import org.openjdk.jmc.common.item.IItemQuery;

/**
 * Provides a simple ResultSet abstraction for item queries.
 */
// FIXME: We may want to clean all of this up and move it to common, so that it can be used more generally.
public interface IItemResultSet {
	/**
	 * Returns the query used to create the result set.
	 *
	 * @return the query used to create the result set.
	 */
	IItemQuery getQuery();

	/**
	 * Returns the value for the specified column, at the current row.
	 *
	 * @param column
	 *            the column for which to return the value.
	 * @return value for the specified column, at the current row.
	 * @throws ItemResultSetException
	 *             if there was a problem reading the value, such as the cursor not being at a
	 *             value, the column not existing etc.
	 */
	Object getValue(int column) throws ItemResultSetException;

	/**
	 * Returns the column metadata for the result set. Useful for finding out what column an
	 * attribute or an aggregator is mapping to.
	 *
	 * @return the column metadata.
	 */
	Map<String, ColumnInfo> getColumnMetadata();

	/**
	 * Advances the cursor to the next row in the result set. Returns true if there is still more
	 * rows, and false if the end has been reached.
	 *
	 * @return true if there is still more rows, and false if the end has been reached.
	 */
	boolean next();
}
