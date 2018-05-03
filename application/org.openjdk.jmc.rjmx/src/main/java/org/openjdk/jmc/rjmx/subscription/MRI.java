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
package org.openjdk.jmc.rjmx.subscription;

import java.io.Serializable;

import javax.management.ObjectName;

import org.openjdk.jmc.rjmx.ConnectionToolkit;

/**
 * The MBean Resource Identifier is a simple data class representing the fully qualified name for a
 * JMX based value to be retrieved by the subscription engine.
 * <p>
 * Note that that subscription engine handles both attribute based subscriptions as well as
 * notification based. Transformation data sources might both based on all kinds value sources, even
 * other transformations.
 * <p>
 * The value descriptor consists of:
 * <ul>
 * <li>A type representing the type of data source.</li>
 * <li>An {@link ObjectName} for the MBean from which to retrieve the value.</li>
 * <li>A string for the value path, describing how to reach the value.</li>
 * </ul>
 * The string form for a value descriptor is a String using URI form:
 *
 * <pre>
 * &lt;type&gt;://&lt;ObjectName&gt;/&lt;value path&gt;
 * </pre>
 *
 * Note that the FQN for a value descriptor cannot be relied upon for persistence, as ObjectNames
 * are allowed to contain slashes.
 */
public final class MRI implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String MRI_DELIMITER = "://"; //$NON-NLS-1$
	/**
	 * The delimiter used to separate the object name from the attribute name in the fully qualified
	 * form.
	 */
	private static final char ATTRIBUTE_DELIMITER = '/';

	/**
	 * The delimiter used to separate the attribute name from the key name in composite attributes.
	 */
	public static final char VALUE_COMPOSITE_DELIMITER = '/';
	public static final String VALUE_COMPOSITE_DELIMITER_STRING = "/"; //$NON-NLS-1$

	/**
	 * The kind of value source that the attribute descriptor points out. Is either
	 * {@link Type#ATTRIBUTE} for a standard JMX attribute or {@link Type#NOTIFICATION} if the value
	 * is derived from a JMX notification. {@link Type#TRANSFORMATION} denotes that the type is a
	 * transformation of some other type (possible a transformation).
	 */
	public enum Type {
		ATTRIBUTE("attribute"), NOTIFICATION("notification"), TRANSFORMATION("transformation"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		private String typeName;

		private Type(String typeName) {
			this.typeName = typeName;
		}

		/**
		 * The string representation of the type.
		 *
		 * @return the string representation of the type.
		 */
		public String getTypeName() {
			return typeName;
		}

		/**
		 * The type instance given its string representation.
		 *
		 * @param typeName
		 *            the string to look up
		 * @return the type instance or {@code null} if unknown
		 */
		public static Type fromString(String typeName) {
			for (Type type : values()) {
				if (typeName.equals(type.getTypeName())) {
					return type;
				}
			}
			return null;
		}
	}

	private final ObjectName m_objectName;
	private final Type m_type;
	private final String m_dataPath;
	private final String m_qualifiedName;

	/**
	 * Constructor.
	 *
	 * @param type
	 *            the type of the MRI.
	 * @param objectName
	 *            the object name of the MBean.
	 * @param valuePath
	 *            the path to the data.
	 * @throws NullPointerException
	 *             if objectName is null.
	 */
	public MRI(Type type, ObjectName objectName, String valuePath) {
		if (objectName == null) {
			throw new NullPointerException("objectName may not be null!"); //$NON-NLS-1$
		}
		if (valuePath == null) {
			throw new NullPointerException("valuePath may not be null!"); //$NON-NLS-1$
		}
		if (type == null) {
			throw new NullPointerException("type may not be null!"); //$NON-NLS-1$
		}
		m_type = type;
		m_objectName = objectName;
		m_dataPath = valuePath;
		m_qualifiedName = generateQualifiedName(type, objectName, valuePath).intern();

	}

	/**
	 * Constructor.
	 *
	 * @param type
	 *            the type of the MRI.
	 * @param beanName
	 *            the canonical name of the MBean.
	 * @param dataPath
	 *            the path to the data.
	 * @throws NullPointerException
	 *             if beanName is null.
	 */
	public MRI(Type type, String beanName, String dataPath) {
		this(type, ConnectionToolkit.createObjectName(beanName), dataPath);
	}

	/**
	 * Constructor.
	 *
	 * @param mri
	 *            the MRI being specialized
	 * @param child
	 *            the child extension of the MRIs data path
	 */
	public MRI(MRI mri, String child) {
		this(mri.getType(), mri.getObjectName(), mri.getDataPath() + VALUE_COMPOSITE_DELIMITER_STRING + child);
	}

	/**
	 * Returns the fully qualified name (FQN) of the attribute descriptor.
	 *
	 * @return the fully qualified name of the attribute descriptor.
	 */
	public String getQualifiedName() {
		return m_qualifiedName;
	}

	@Override
	public boolean equals(Object obj) {
		// Quick equals operator
		if (obj != null && obj instanceof MRI) {
			return m_qualifiedName.equals(((MRI) obj).getQualifiedName());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return m_qualifiedName.hashCode();
	}

	/**
	 * Returns the object name (MBean) part of the descriptor.
	 *
	 * @return the object name (MBean) part of the descriptor.
	 */
	public ObjectName getObjectName() {
		return m_objectName;
	}

	/**
	 * Returns the data path. For attributes this is normally simply the attribute name.
	 *
	 * @return the data path.
	 */
	public String getDataPath() {
		return m_dataPath;
	}

	/**
	 * Returns the type of this MRI.
	 *
	 * @see Type
	 * @return the type of this MRI.
	 */
	public Type getType() {
		return m_type;
	}

	@Override
	public String toString() {
		return m_qualifiedName;
	}

	/**
	 * Factory method for creating an MRI from a qualified name.
	 *
	 * @param qualName
	 *            the qualified name to create the MRI from.
	 * @return the created {@link MRI}.
	 */
	public static MRI createFromQualifiedName(String qualName) {
		int index = qualName.indexOf(MRI_DELIMITER);
		if (index < 0) {
			throw new IllegalArgumentException(
					String.format("Malformed FQN. Could not find %s in %s", MRI_DELIMITER, qualName)); //$NON-NLS-1$
		}
		Type type = Type.fromString(qualName.substring(0, index));
		if (type == null) {
			throw new IllegalArgumentException(String.format("%s is not a recognized type.", qualName.substring(0, //$NON-NLS-1$
					index)));
		}
		int mbeanStart = index + MRI_DELIMITER.length();
		index = qualName.indexOf(ATTRIBUTE_DELIMITER, mbeanStart);
		if (index >= 0) {
			String beanName = qualName.substring(mbeanStart, index);
			String attributeName = qualName.substring(index + 1);
			return new MRI(type, beanName, attributeName);
		} else {
			throw new IllegalArgumentException(String.format(
					"Malformed FQN. Could not find attribute name delimiter '%s' in %s", ATTRIBUTE_DELIMITER, //$NON-NLS-1$
					qualName));
		}

	}

	private static String generateQualifiedName(Type type, ObjectName objectName, String valuePath) {
		return type.getTypeName() + MRI_DELIMITER + objectName.getCanonicalName() + ATTRIBUTE_DELIMITER + valuePath;
	}

	/**
	 * Whether given MRI is a child to this MRI.
	 *
	 * @param childCandidate
	 *            the candidate MRI to test
	 * @return {@code true} if given MRI is a child, {@code false} otherwise
	 */
	public boolean isChild(MRI childCandidate) {
		if (!m_objectName.equals(childCandidate.getObjectName()) || childCandidate.getType() != m_type) {
			return false;
		}
		String childMRIDataPath = childCandidate.getDataPath();
		if (childMRIDataPath.startsWith(m_dataPath)) {
			String childDataPath = childMRIDataPath.substring(m_dataPath.length());
			return childDataPath.indexOf(VALUE_COMPOSITE_DELIMITER) == 0
					&& childDataPath.lastIndexOf(VALUE_COMPOSITE_DELIMITER) == 0;
		}
		return false;
	}

	/**
	 * Creates an array of all ancestor MRI for this object including itself.
	 *
	 * @return all MRI from "topmost" ancestor down to this instance
	 */
	public MRI[] getParentMRIs() {
		String[] dataParts = m_dataPath.split(VALUE_COMPOSITE_DELIMITER_STRING);
		MRI[] parentMris = new MRI[dataParts.length - 1];
		StringBuilder dataPath = new StringBuilder();
		for (int i = 0; i < parentMris.length; i++) {
			dataPath.append(dataParts[i]);
			parentMris[i] = new MRI(m_type, m_objectName, dataPath.toString());
			dataPath.append(VALUE_COMPOSITE_DELIMITER);
		}
		return parentMris;
	}

}
