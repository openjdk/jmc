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

import java.awt.Color;

import javax.management.Descriptor;
import javax.management.openmbean.CompositeData;

import org.openjdk.jmc.common.util.ColorToolkit;
import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.rjmx.IConnectionHandle;

/**
 * Helper class for extracting some commonly used metadata from the {@link IMRIMetadataService}.
 */
public final class MRIMetadataToolkit {

	private MRIMetadataToolkit() {
		throw new AssertionError("This is a toolkit! Do not instantiate!"); //$NON-NLS-1$
	}

	/**
	 * Returns a more user friendly display name for the attribute. If available, the display name
	 * will be retrieved from the metadata available in the meta data service, otherwise the
	 * attribute name will be massaged into a more user friendly form.
	 *
	 * @param handle
	 *            the connection handle for which to check.
	 * @param mri
	 *            the descriptor for which to retrieve the display name.
	 * @return the defined display name for this attribute. Can be overridden by metadata.
	 */
	public static String getDisplayName(IConnectionHandle handle, MRI mri) {
		return getDisplayName(handle.getServiceOrNull(IMRIMetadataService.class), mri);
	}

	/**
	 * Returns a more user friendly display name for the attribute. If available, the display name
	 * will be retrieved from the metadata available in the meta data service, otherwise the
	 * attribute name will be massaged into a more user friendly form.
	 *
	 * @param service
	 *            the metadata service to use.
	 * @param mri
	 *            the descriptor for which to retrieve the display name.
	 * @return the defined display name for this attribute. Can be overridden by metadata.
	 */
	public static String getDisplayName(IMRIMetadataService service, MRI mri) {
		if (service != null) {
			String displayName = (String) service.getMetadata(mri, IMRIMetadataProvider.KEY_DISPLAY_NAME);
			if (displayName != null && displayName.trim().length() > 0) {
				return displayName;
			}
		}

		StringBuilder builder = new StringBuilder();
		String attributeDisplayName = mri.getDataPath();
		if (attributeDisplayName == null) {
			return ""; //$NON-NLS-1$
		}
		boolean lastCharLowerCase = false;
		for (int n = 0; n < attributeDisplayName.length(); n++) {
			char c = attributeDisplayName.charAt(n);
			if (lastCharLowerCase && Character.isUpperCase(c)) {
				builder.append(' ');
				builder.append(Character.toUpperCase(c));
			} else {
				builder.append(c);
			}
			lastCharLowerCase = Character.isLowerCase(c);
		}
		return builder.toString();
	}

	private static Object getMetadata(IConnectionHandle handle, MRI descriptor, String property) {
		IMRIMetadataService infoService = handle.getServiceOrNull(IMRIMetadataService.class);
		if (infoService != null) {
			return infoService.getMetadata(descriptor, property);
		}
		return null;
	}

	/**
	 * Returns the description for the specified MRI.
	 *
	 * @param handle
	 *            the connection handle for which to check.
	 * @param mri
	 *            the descriptor for which to retrieve the description.
	 * @return the description for this attribute. Can be overridden by metadata.
	 */
	public static String getDescription(IConnectionHandle handle, MRI mri) {
		String str = (String) getMetadata(handle, mri, IMRIMetadataProvider.KEY_DESCRIPTION);
		if (str != null) {
			return str;
		} else {
			return ""; //$NON-NLS-1$
		}
	}

	/**
	 * Returns the metadata for given MRI.
	 *
	 * @param handle
	 *            the connection to use
	 * @param descriptor
	 *            the MRI
	 * @return the metadata info
	 */
	public static IMRIMetadata getMRIMetadata(IConnectionHandle handle, MRI descriptor) {
		IMRIMetadataService infoService = handle.getServiceOrNull(IMRIMetadataService.class);
		if (infoService != null) {
			return infoService.getMetadata(descriptor);
		}
		return null;
	}

	/**
	 * Answers whether the type of given {@link IMRIMetadata} is of numerical type or not.
	 *
	 * @param info
	 *            the {@link IMRIMetadata} to inspect type.
	 * @return true if type is numerical type, false otherwise
	 */
	public static boolean isNumerical(IMRIMetadata info) {
		return isNumerical(info.getValueType());
	}

	/**
	 * Answers whether the given type is of a numerical type or not.
	 *
	 * @param attributeType
	 *            the type to inspect
	 * @return true if type is numerical type, false otherwise
	 */
	public static boolean isNumerical(String attributeType) {
		// is not numerical until proven otherwise. Note that it should be
		// enough to connect once to get this data properly sorted out.
		if (attributeType == null) {
			return false;
		}

		if (attributeType.equals(int.class.getName())) {
			return true;
		}
		if (attributeType.equals(float.class.getName())) {
			return true;
		}
		if (attributeType.equals(double.class.getName())) {
			return true;
		}
		if (attributeType.equals(long.class.getName())) {
			return true;
		}
		if (attributeType.equals(short.class.getName())) {
			return true;
		}
		if (attributeType.equals(byte.class.getName())) {
			return true;
		}
		try {
			return Number.class.isAssignableFrom(Class.forName(attributeType));
		} catch (ClassNotFoundException fallTrough) {
		}
		return false;
	}

	/**
	 * Convenience method that returns true if the value type of the supplied metadata is certain to
	 * be of {@link CompositeData}
	 *
	 * @param info
	 *            the metadata to check.
	 * @return <tt>true</tt> if the supplied info value type is {@link CompositeData},
	 *         <tt>false</tt> if not or undecided.
	 */
	public static boolean isComposite(IMRIMetadataProvider info) {
		return getBooleanValue(info, IMRIMetadataProvider.KEY_COMPOSITE);
	}

	/**
	 * Checks whether the string specifies a class name that is a descendant of the CompositeData
	 * interface.
	 *
	 * @param className
	 *            the class to check
	 * @return true if it a descendant of the CompositeData interface.
	 */
	public static boolean isCompositeType(String className) {
		if (className == null || className.length() == 0) {
			return false;
		}
		try {
			if (TypeHandling.isPrimitive(className)) {
				return false;
			}
			return CompositeData.class.isAssignableFrom(Class.forName(className));
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	/**
	 * Convenience method that returns true if the value type of the supplied metadata is certain to
	 * be readable.
	 *
	 * @param info
	 *            the metadata to check.
	 * @return <tt>true</tt> if the supplied info value type is readable, <tt>false</tt> otherwise.
	 */
	public static boolean isReadable(IMRIMetadataProvider info) {
		return getBooleanValue(info, IMRIMetadataProvider.KEY_READABLE);
	}

	/**
	 * Convenience method that returns true if the value type of the supplied metadata is certain to
	 * be writable.
	 *
	 * @param info
	 *            the metadata to check.
	 * @return <tt>true</tt> if the supplied info value type is writable, <tt>false</tt> otherwise.
	 */
	public static boolean isWritable(IMRIMetadataProvider info) {
		return getBooleanValue(info, IMRIMetadataProvider.KEY_WRITABLE);
	}

	/**
	 * Returns the possible descriptor for given metadata.
	 *
	 * @param info
	 *            the metadata to use
	 * @return the descriptor object, or {@code null} if not available.
	 */
	public static Descriptor getDescriptor(IMRIMetadataProvider info) {
		return (Descriptor) info.getMetadata(IMRIMetadataProvider.KEY_DESCRIPTOR);
	}

	/**
	 * Returns the metadata color. Either the color stored in the metadata or an assigned
	 * distinguishable color.
	 *
	 * @param info
	 *            the metadata to use
	 * @return a color to use for given metadata
	 */
	public static Color getColor(IMRIMetadataProvider info) {
		Object colorStr = info.getMetadata(IMRIMetadataProvider.KEY_COLOR);
		if (colorStr != null) {
			return ColorToolkit.decode(colorStr.toString());
		}
		return ColorToolkit.getDistinguishableColor(info.getMRI());
	}

	/**
	 * Whether a metadata property is {@code true} or not.
	 *
	 * @param info
	 *            the metadata to use
	 * @param key
	 *            the property key to use for look up
	 * @return {@code true} if the property is present and set to {@code true}, {@code false}
	 *         otherwise
	 */
	public static boolean getBooleanValue(IMRIMetadataProvider info, String key) {
		Boolean b = (Boolean) info.getMetadata(key);
		if (b != null) {
			return b.booleanValue();
		}
		return false; // Default value
	}
}
