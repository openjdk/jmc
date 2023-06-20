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
package org.openjdk.jmc.rjmx.subscription.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import javax.management.Descriptor;
import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;

import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;
import org.openjdk.jmc.rjmx.subscription.IMBeanServerChangeListener;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataProvider;
import org.openjdk.jmc.rjmx.subscription.IMRIService;
import org.openjdk.jmc.rjmx.subscription.IMRITransformation;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;

/**
 * Responsible for querying the existing MBean server for metadata. Will cache metadata for
 * everything that is not directly available through the MBean information of the MBean server.
 */
public final class MBeanMRIMetadataDB implements IMRIService, IMBeanServerChangeListener {
	// FIXME: extend possibility to record MBean metadata about notifications
	private final IMBeanHelperService mbeanService;
	private final Map<ObjectName, Map<MRI, Map<String, Object>>> cachedMRIMetadata = new HashMap<>();
	private final Set<ObjectName> introspectedMBeans = new HashSet<>();

	public MBeanMRIMetadataDB(IMBeanHelperService mbeanService) {
		this.mbeanService = mbeanService;
	}

	@Override
	public Set<MRI> getMRIs() {
		Set<MRI> allMRIOnServer = new HashSet<>();
		try {
			for (ObjectName mbean : mbeanService.getMBeanNames()) {
				allMRIOnServer.addAll(getMBeanData(mbean).keySet());
			}
		} catch (Exception e) {
			RJMXPlugin.getDefault().getLogger().log(Level.SEVERE, "Unable to retrieve MBean names from server!", e); //$NON-NLS-1$
		}
		return allMRIOnServer;
	}

	@Override
	public boolean isMRIAvailable(MRI mri) {
		if (mri.getType() == Type.TRANSFORMATION) {
			return isTransformationAvailable(mri);
		}
		return getMBeanData(mri.getObjectName()).keySet().contains(mri);
	}

	private boolean isTransformationAvailable(MRI mri) {
		IMRITransformation transformation = MRITransformationToolkit.createTransformation(mri);
		for (MRI attribute : transformation.getAttributes()) {
			if (!isMRIAvailable(attribute)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void mbeanUnregistered(ObjectName mbean) {
		synchronized (cachedMRIMetadata) {
			introspectedMBeans.remove(mbean);
			cachedMRIMetadata.remove(mbean);
		}
	}

	@Override
	public void mbeanRegistered(ObjectName mbean) {
	}

	public Map<MRI, Map<String, Object>> getMBeanData(ObjectName mbean) {
		if (hasIntrospectedMBean(mbean)) {
			synchronized (cachedMRIMetadata) {
				return getNonNullMap(cachedMRIMetadata, mbean);
			}
		}
		return introspectMBean(mbean);
	}

	private boolean hasIntrospectedMBean(ObjectName mbean) {
		return introspectedMBeans.contains(mbean);
	}

	private <K, V, C> Map<V, C> getNonNullMap(Map<K, Map<V, C>> map, K key) {
		Map<V, C> keyMap = map.get(key);
		if (keyMap == null) {
			return Collections.emptyMap();
		}
		return keyMap;
	}

	private Map<MRI, Map<String, Object>> lookupMBeanMRIData(ObjectName mbean) {
		MBeanInfo info = lookupMBeanInfo(mbean);
		if (info != null) {
			Map<MRI, Map<String, Object>> mbeanMetadata = new HashMap<>();
			for (MBeanAttributeInfo attribute : info.getAttributes()) {
				if (attribute.getName() == null) {
					RJMXPlugin.getDefault().getLogger()
							.warning("Omitting attribute with name==null in MBean + " + mbean.toString()); //$NON-NLS-1$
				} else {
					if (attribute.getType() == null) {
						RJMXPlugin.getDefault().getLogger().warning(
								"Found MBean attribute with invalid type for " + mbean + "/" + attribute.getName()); //$NON-NLS-1$ //$NON-NLS-2$
					}
					mbeanMetadata.put(new MRI(Type.ATTRIBUTE, mbean, attribute.getName()), createMetadata(attribute));
				}
			}
			for (MBeanNotificationInfo notification : info.getNotifications()) {
				for (String type : notification.getNotifTypes()) {
					mbeanMetadata.put(new MRI(Type.NOTIFICATION, mbean, type), createMetadata(notification, type));
				}
			}
			return mbeanMetadata;
		}
		return null;
	}

	private MBeanInfo lookupMBeanInfo(ObjectName mbean) {
		try {
			return mbeanService.getMBeanInfo(mbean);
		} catch (InstanceNotFoundException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.INFO, "MBean " + mbean + " does not exist on the server"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (Exception e) {
			RJMXPlugin.getDefault().getLogger().log(Level.SEVERE, "Unable to retrieve MBean information from server!", //$NON-NLS-1$
					e);
		}
		return null;
	}

	private Map<String, Object> createMetadata(MBeanAttributeInfo attribute) {
		String typeName = attribute.getType();
		Object originalType = attribute.getDescriptor().getFieldValue("originalType"); //$NON-NLS-1$
		if (originalType instanceof String) {
			typeName = (String) originalType;
		}
		Map<String, Object> metadata = new HashMap<>();
		metadata.put(IMRIMetadataProvider.KEY_DISPLAY_NAME, attribute.getName());
		metadata.put(IMRIMetadataProvider.KEY_DESCRIPTION, attribute.getDescription());
		metadata.put(IMRIMetadataProvider.KEY_VALUE_TYPE, typeName);
		metadata.put(IMRIMetadataProvider.KEY_COMPOSITE, isCompositeType(attribute.getType()));
		metadata.put(IMRIMetadataProvider.KEY_READABLE, attribute.isReadable());
		metadata.put(IMRIMetadataProvider.KEY_WRITABLE, attribute.isWritable());
		metadata.put(IMRIMetadataProvider.KEY_DESCRIPTOR, attribute.getDescriptor());
		return metadata;
	}

	private Map<String, Object> createMetadata(MBeanNotificationInfo notification, String type) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put(IMRIMetadataProvider.KEY_DISPLAY_NAME, type);
		metadata.put(IMRIMetadataProvider.KEY_DESCRIPTION, notification.getDescription());
		metadata.put(IMRIMetadataProvider.KEY_VALUE_TYPE, notification.getName());
		metadata.put(IMRIMetadataProvider.KEY_DESCRIPTOR, notification.getDescriptor());
		return metadata;
	}

	private boolean isCompositeType(String className) {
		if (className == null) {
			return false;
		}
		try {
			return CompositeData.class.isAssignableFrom(TypeHandling.getClassWithName(className));
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	private Map<MRI, Map<String, Object>> introspectMBean(ObjectName mbean) {
		Map<MRI, Map<String, Object>> cachedMBeanInfo;
		synchronized (cachedMRIMetadata) {
			Map<MRI, Map<String, Object>> mbeanMetadata = cachedMRIMetadata.get(mbean);
			if (mbeanMetadata == null) {
				mbeanMetadata = lookupMBeanMRIData(mbean);
				if (mbeanMetadata == null) {
					return Collections.emptyMap();
				}
			}
			cachedMBeanInfo = new HashMap<>(mbeanMetadata);
			boolean hasIntrospected = true;
			for (Entry<MRI, Map<String, Object>> entry : mbeanMetadata.entrySet()) {
				CompositeType type = getCompositeType(entry.getValue());
				if (type == null && isComposite(entry.getValue())) {
					// FIXME: We ought to retrieve all composites at once but we will not need this for well behaved MBeans
					CompositeData compositeData = getCompositeData(entry.getKey());
					if (compositeData != null) {
						type = compositeData.getCompositeType();
					} else {
						hasIntrospected = false;
					}
				}
				if (type != null) {
					Object readable = entry.getValue().get(IMRIMetadataProvider.KEY_READABLE);
					Object writable = entry.getValue().get(IMRIMetadataProvider.KEY_WRITABLE);
					entry.getValue().put(IMRIMetadataProvider.KEY_COMPOSITE, true);
					cachedMBeanInfo.putAll(introspectChildren(entry.getKey(), type, readable, writable));
				}
			}
			cachedMRIMetadata.put(mbean, cachedMBeanInfo);
			if (hasIntrospected) {
				introspectedMBeans.add(mbean);
			}
		}
		return new HashMap<>(cachedMBeanInfo);
	}

	private boolean isComposite(Map<String, Object> metadata) {
		Object object = metadata.get(IMRIMetadataProvider.KEY_COMPOSITE);
		if (object != null && object instanceof Boolean) {
			return ((Boolean) object).booleanValue();
		}
		return false;
	}

	private CompositeType getCompositeType(Map<String, Object> metadata) {
		Object descriptor = metadata.get(IMRIMetadataProvider.KEY_DESCRIPTOR);
		if (descriptor instanceof Descriptor) {
			Object type = ((Descriptor) descriptor).getFieldValue(JMX.OPEN_TYPE_FIELD);
			if (type instanceof CompositeType) {
				return (CompositeType) type;
			}
		}
		return null;
	}

	private CompositeData getCompositeData(MRI mri) {
		try {
			Object attributeValue = mbeanService.getAttributeValue(mri);
			if (attributeValue instanceof CompositeData) {
				return (CompositeData) attributeValue;
			}
			return null;
		} catch (Exception e) {
			RJMXPlugin.getDefault().getLogger().log(Level.SEVERE, "Could not retrieve attribute: " + mri, e); //$NON-NLS-1$
			return null;
		}
	}

	private Map<MRI, Map<String, Object>> introspectChildren(
		MRI parentMRI, CompositeType parentType, Object readable, Object writable) {
		Map<MRI, Map<String, Object>> children = new HashMap<>();
		for (Object key : parentType.keySet()) {
			String childAttribute = String.valueOf(key);
			MRI childMRI = new MRI(parentMRI, childAttribute);
			Map<String, Object> childMetadata = new HashMap<>();
			children.put(childMRI, childMetadata);
			childMetadata.put(IMRIMetadataProvider.KEY_DISPLAY_NAME, childAttribute);
			childMetadata.put(IMRIMetadataProvider.KEY_DESCRIPTION, parentType.getDescription(childAttribute));
			childMetadata.put(IMRIMetadataProvider.KEY_VALUE_TYPE, parentType.getType(childAttribute).getClassName());
			childMetadata.put(IMRIMetadataProvider.KEY_READABLE, readable);
			childMetadata.put(IMRIMetadataProvider.KEY_WRITABLE, writable);
			OpenType<?> childType = parentType.getType(childAttribute);
			if (childType instanceof CompositeType) {
				childMetadata.put(IMRIMetadataProvider.KEY_COMPOSITE, true);
				children.putAll(introspectChildren(childMRI, (CompositeType) childType, readable, writable));
			}
		}
		return children;
	}
}
