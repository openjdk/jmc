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
package org.openjdk.jmc.rjmx.ui.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import javax.management.ObjectName;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.ui.RJMXUIPlugin;

/**
 * Utility class that reorders a MBeans properties in an consistent order in accordance with user
 * preferences.
 */
public final class MBeanPropertiesOrderer {

	/**
	 * A listener used to get information about when the preferences for MBean properties sort order
	 * has been changed.
	 */
	public static interface IMBeanPropertiesOrderChangedListener {
		/**
		 * Called when some MBean property order preference has been updated.
		 *
		 * @param e
		 *            the event holding knowledge about which preference was updated.
		 */
		void propertiesOrderChanged(PropertyChangeEvent e);
	}

	/**
	 * List that overrides the prefix order of the property keys of object names.
	 */
	private static ArrayList<String> propertyKeyPrefixOrderList;

	/**
	 * List that overrides the suffix order of the property keys of object names.
	 */
	private static ArrayList<String> propertyKeySuffixOrderList;

	/**
	 * Whether properties not found on order list should be sorted alphabetical or remain on the
	 * object name order.
	 */
	private static boolean propertiesInAlphabeticOrder;

	/**
	 * Whether property keys should be compared case insensitive or not.
	 */
	private static boolean caseInsensitivePropertyKeys;

	/**
	 * Whether to show compressed MBean paths (skipping property keys) or not.
	 */
	private static boolean showCompressedPaths;

	/**
	 * The set of listeners interested in knowledge about when the properties orderer has been
	 * updated.
	 */
	private static HashSet<IMBeanPropertiesOrderChangedListener> propertiesOrderChangeListeners;

	/**
	 * Newer instantiated.
	 */
	private MBeanPropertiesOrderer() {
	}

	/**
	 * Initializes the properties to decide in which order MBean key properties should be presented,
	 * and sets up a property listener on the preference store.
	 */
	static {
		updatePrefixOrderString();
		updateSuffixOrderString();
		updatePropertiesInAlpabeticOrder();
		updateCaseInsensitivePropertyKeys();
		updateShowCompressedPaths();
		propertiesOrderChangeListeners = new HashSet<>();

		RJMXUIPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				String property = event.getProperty();
				if (property.equals(RJMXUIConstants.PROPERTY_MBEAN_PROPERTY_KEY_ORDER)) {
					updatePrefixOrderString();
					firePropertiesOrderChangedEvent(event);
				} else if (property.equals(RJMXUIConstants.PROPERTY_MBEAN_SUFFIX_PROPERTY_KEY_ORDER)) {
					updateSuffixOrderString();
					firePropertiesOrderChangedEvent(event);
				} else if (property.equals(RJMXUIConstants.PROPERTY_MBEAN_PROPERTIES_IN_ALPHABETIC_ORDER)) {
					updatePropertiesInAlpabeticOrder();
					firePropertiesOrderChangedEvent(event);
				} else if (property.equals(RJMXUIConstants.PROPERTY_MBEAN_CASE_INSENSITIVE_PROPERTY_ORDER)) {
					updateCaseInsensitivePropertyKeys();
					firePropertiesOrderChangedEvent(event);
				} else if (property.equals(RJMXUIConstants.PROPERTY_MBEAN_SHOW_COMPRESSED_PATHS)) {
					updateShowCompressedPaths();
					firePropertiesOrderChangedEvent(event);
				}
			}
		});
	}

	/**
	 * Updates and rebuilds the list of prefix properties.
	 */
	private static synchronized void updatePrefixOrderString() {
		String prefixOrderString = RJMXUIPlugin.getDefault().getPreferenceStore()
				.getString(RJMXUIConstants.PROPERTY_MBEAN_PROPERTY_KEY_ORDER);
		propertyKeyPrefixOrderList = new ArrayList<>();
		splitCommaSeparatedKeyString(propertyKeyPrefixOrderList, prefixOrderString);
	}

	/**
	 * Updates and rebuilds the list of suffix properties.
	 */
	private static synchronized void updateSuffixOrderString() {
		String suffixOrderString = RJMXUIPlugin.getDefault().getPreferenceStore()
				.getString(RJMXUIConstants.PROPERTY_MBEAN_SUFFIX_PROPERTY_KEY_ORDER);
		propertyKeySuffixOrderList = new ArrayList<>();
		splitCommaSeparatedKeyString(propertyKeySuffixOrderList, suffixOrderString);
	}

	/**
	 * Updates whether to sort remaining properties alphabetic or not.
	 */
	private static synchronized void updatePropertiesInAlpabeticOrder() {
		propertiesInAlphabeticOrder = RJMXUIPlugin.getDefault().getPreferenceStore()
				.getBoolean(RJMXUIConstants.PROPERTY_MBEAN_PROPERTIES_IN_ALPHABETIC_ORDER);
	}

	/**
	 * Updates whether to handle the remaining properties case insensitive or not.
	 */
	private static synchronized void updateCaseInsensitivePropertyKeys() {
		caseInsensitivePropertyKeys = RJMXUIPlugin.getDefault().getPreferenceStore()
				.getBoolean(RJMXUIConstants.PROPERTY_MBEAN_CASE_INSENSITIVE_PROPERTY_ORDER);
	}

	/**
	 * Updates whether to show compressed paths (without property keys) or not.
	 */
	private static synchronized void updateShowCompressedPaths() {
		showCompressedPaths = RJMXUIPlugin.getDefault().getPreferenceStore()
				.getBoolean(RJMXUIConstants.PROPERTY_MBEAN_SHOW_COMPRESSED_PATHS);
	}

	/**
	 * Splits the string of keys on commas (',') and inserts the substrings in the list. Zero-length
	 * substrings are not inserted (i.e. the string "type,, name" gives the substrings "type" and "
	 * name"). Will honor whether keys are to be case insensitive or not.
	 *
	 * @param list
	 *            the list to add keys to
	 * @param str
	 *            the string with keys to split on commas
	 */
	private static void splitCommaSeparatedKeyString(List<String> list, String str) {
		int last = 0;
		int next;
		while ((next = str.indexOf(',', last)) >= 0) {
			if (next > last) {
				list.add(comparableKey(str.substring(last, next)));
			}
			last = next + 1;
		}
		if (last + 1 < str.length()) {
			list.add(comparableKey(str.substring(last)));
		}
	}

	/**
	 * Returns given key in the current used comparison mode (case sensitive / insensitive).
	 *
	 * @param key
	 *            the key to possible convert
	 * @return the key in correct comparison mode
	 */
	private static String comparableKey(String key) {
		return (caseInsensitivePropertyKeys) ? key.toLowerCase(Locale.ENGLISH) : key;
	}

	/**
	 * Adds the given listener to the set of listeners to inform about property order updates.
	 *
	 * @param listener
	 *            the listener to add.
	 */
	public static void addPropertiesOrderChangedListener(IMBeanPropertiesOrderChangedListener listener) {
		synchronized (propertiesOrderChangeListeners) {
			propertiesOrderChangeListeners.add(listener);
		}
	}

	/**
	 * Removes the given listener from the set of listeners to inform about property order updates.
	 *
	 * @param listener
	 *            the listener to add.
	 */
	public static void removePropertiesOrderChangedListener(IMBeanPropertiesOrderChangedListener listener) {
		synchronized (propertiesOrderChangeListeners) {
			propertiesOrderChangeListeners.remove(listener);
		}
	}

	/**
	 * Calls all registered listeners with the given event.
	 *
	 * @param e
	 *            the event to pass to listener's
	 *            {@link IMBeanPropertiesOrderChangedListener#propertiesOrderChanged(PropertyChangeEvent)}
	 *            method
	 */
	private static void firePropertiesOrderChangedEvent(PropertyChangeEvent e) {
		synchronized (propertiesOrderChangeListeners) {
			for (IMBeanPropertiesOrderChangedListener imBeanPropertiesOrderChangedListener : propertiesOrderChangeListeners) {
				imBeanPropertiesOrderChangedListener.propertiesOrderChanged(e);
			}
		}
	}

	/**
	 * Creates a domain and properties string using to settings sort order that is in use by
	 * attribute selector dialogs and MBean browser.
	 *
	 * @param bean
	 *            the MBean to create path for.
	 * @return a string giving the path to the MBean.
	 */
	public static String getMBeanPath(ObjectName bean) {
		StringBuffer path = new StringBuffer();
		path.append(bean.getDomain());
		Property[] properties = getOrderedProperties(bean);
		for (int i = 0; i < properties.length; i += 1) {
			path.append((i == 0) ? ':' : ',');
			if (!showCompressedPaths) {
				path.append(properties[i].getKey());
				path.append('=');
			}
			path.append(properties[i].getValue());
		}
		return path.toString();
	}

	/**
	 * Creates a string describing the MRI for use in a tooltip.
	 *
	 * @param attributeDescriptor
	 *            the MRI
	 * @return a string for use in a tooltip
	 */
	public static String mriAsTooltip(MRI attributeDescriptor) {
		StringBuffer path = new StringBuffer(getMBeanPath(attributeDescriptor.getObjectName()));
		path.append('/');
		path.append(attributeDescriptor.getDataPath());
		return path.toString();
	}

	/**
	 * Creates an array containing all bean properties (excluding the domain) in order based on
	 * order properties.
	 *
	 * @param bean
	 *            the bean to order properties for
	 * @return an array with the {@link Property} of the bean
	 */
	public static Property[] getOrderedProperties(ObjectName bean) {
		List<Property> propertiesList = getProperties(bean);
		Property[] properties = new Property[propertiesList.size()];
		int insertPos = 0;
		// retrieve all prefix keys in given order
		for (int pre = 0; pre < propertyKeyPrefixOrderList.size(); pre += 1) {
			String prefixKey = propertyKeyPrefixOrderList.get(pre);
			for (int prop = 0; prop < propertiesList.size(); prop += 1) {
				Property property = propertiesList.get(prop);
				if (property != null && comparableKey(property.getKey()).equals(prefixKey)) {
					properties[insertPos] = property;
					insertPos += 1;
					propertiesList.set(prop, null);
				}
			}
		}
		// retrieve all suffix keys in given order from the end
		int lastInsertPos = properties.length; // exclusive
		if (insertPos < lastInsertPos) {
			for (int suf = propertyKeySuffixOrderList.size() - 1; suf >= 0; suf -= 1) {
				String suffixKey = propertyKeySuffixOrderList.get(suf);
				for (int prop = propertiesList.size() - 1; prop >= 0; prop -= 1) {
					Property property = propertiesList.get(prop);
					if (property != null && comparableKey(property.getKey()).equals(suffixKey)) {
						lastInsertPos -= 1;
						properties[lastInsertPos] = property;
						propertiesList.set(prop, null);
					}
				}
			}
		}
		// insert the remaining properties
		if (insertPos < lastInsertPos) {
			int startInsertPos = insertPos;
			for (int i = 0; i < propertiesList.size(); i += 1) {
				Property property = propertiesList.get(i);
				if (property != null) {
					properties[insertPos] = property;
					insertPos += 1;
				}
			}
			if (propertiesInAlphabeticOrder && (insertPos - startInsertPos) > 1) {
				Arrays.sort(properties, startInsertPos, lastInsertPos, new Comparator<Property>() {
					@Override
					public int compare(Property first, Property second) {
						return comparableKey(first.getKey()).compareTo(comparableKey(second.getKey()));
					}
				});
			}
		}
		// insert the actual bean on the last property
		Property property = properties[properties.length - 1];
		properties[properties.length - 1] = new PropertyWithMBean(property.getKey(), property.getValue(), bean);
		return properties;
	}

	/**
	 * Creates a list of properties (excluding the domain) for given bean
	 *
	 * @param bean
	 *            the bean to extract properties from
	 * @return a {@link List} of {@link Property}
	 */
	private static List<Property> getProperties(ObjectName bean) {
		List<Property> propertiesList = new ArrayList<>();
		String propertiesString = bean.getKeyPropertyListString();
		Hashtable<String, String> bar = bean.getKeyPropertyList();
		Hashtable<String, String> propertiesTable = new Hashtable<>(bar);
		int index = propertiesString.indexOf('=');
		while (index != -1) {
			String key = propertiesString.substring(0, index);
			String value = propertiesTable.get(key);
			propertiesTable.remove(key);
			int endValueIndex = index + 1 + value.length();
			int nextIndex = propertiesString.indexOf(',', endValueIndex);
			if (nextIndex != -1) {
				propertiesString = propertiesString.substring(nextIndex + 1);
			} else {
				propertiesString = propertiesString.substring(propertiesString.length());
			}
			index = propertiesString.indexOf('=');
			propertiesList.add(new Property(key, value));
		}
		return propertiesList;
	}

	/**
	 * Container of key=value property for a MBean.
	 */
	public static class Property {
		private final String key;
		private final String value;

		/**
		 * Creates a new {@link Property}.
		 *
		 * @param key
		 *            the key for the {@link Property}
		 * @param value
		 *            the value for the {@link Property}
		 */
		public Property(String key, String value) {
			this.key = key;
			this.value = value;
		}

		/**
		 * Returns the key of this {@link Property}.
		 *
		 * @return the key
		 */
		public String getKey() {
			return key;
		}

		/**
		 * Returns the value of this {@link Property}.
		 *
		 * @return the value
		 */
		public String getValue() {
			return value;
		}

		public String getStringRepresentation() {
			return key + '=' + value;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Property) {
				Property that = (Property) obj;
				return key.equals(that.key) && value.equals(that.value);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return getStringRepresentation().hashCode();
		}

		@Override
		public String toString() {
			return super.toString() + '[' + getStringRepresentation() + ']';
		}
	}

	/**
	 * A "leaf" property with the actual MBean information. Can be adapted to an {@link ObjectName}.
	 */
	public static class PropertyWithMBean extends Property implements IAdaptable {
		private final ObjectName bean;

		/**
		 * Creates a new {@link PropertyWithMBean}.
		 *
		 * @param key
		 *            the key for the {@link PropertyWithMBean}
		 * @param value
		 *            the value for the {@link PropertyWithMBean}
		 * @param bean
		 *            the actual bean for the {@link PropertyWithMBean}
		 */
		public PropertyWithMBean(String key, String value, ObjectName bean) {
			super(key, value);
			this.bean = bean;
		}

		/**
		 * Returns the bean information of this {@link PropertyWithMBean}.
		 *
		 * @return the bean
		 */
		public ObjectName getBean() {
			return bean;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof PropertyWithMBean) {
				PropertyWithMBean that = (PropertyWithMBean) obj;
				return super.equals(that) && bean.equals(that.bean);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return super.hashCode() + bean.hashCode();
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			if (adapter == ObjectName.class) {
				return adapter.cast(getBean());
			} else {
				return null;
			}
		}
	}
}
