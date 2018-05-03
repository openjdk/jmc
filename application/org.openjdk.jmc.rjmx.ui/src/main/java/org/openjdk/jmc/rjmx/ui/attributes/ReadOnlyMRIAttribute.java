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
package org.openjdk.jmc.rjmx.ui.attributes;

import java.rmi.UnmarshalException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.RuntimeErrorException;
import javax.management.RuntimeMBeanException;
import javax.management.openmbean.CompositeData;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.util.ExceptionToolkit;
import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.services.IReadOnlyAttribute;
import org.openjdk.jmc.rjmx.services.IUpdateInterval;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;
import org.openjdk.jmc.rjmx.subscription.IUpdatePolicy;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIMetadataToolkit;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.rjmx.subscription.internal.AttributeValueToolkit;
import org.openjdk.jmc.rjmx.subscription.internal.DefaultUpdatePolicy;
import org.openjdk.jmc.rjmx.subscription.internal.ExtendedMRIMetadataToolkit;
import org.openjdk.jmc.rjmx.subscription.internal.OneShotUpdatePolicy;
import org.openjdk.jmc.rjmx.subscription.internal.SimpleUpdatePolicy;
import org.openjdk.jmc.rjmx.subscription.internal.UpdatePolicyToolkit;
import org.openjdk.jmc.rjmx.ui.RJMXUIPlugin;
import org.openjdk.jmc.rjmx.util.internal.AbstractReadOnlyAttribute;
import org.openjdk.jmc.rjmx.util.internal.PartitionedList;
import org.openjdk.jmc.rjmx.util.internal.SimpleAttributeInfo;
import org.openjdk.jmc.ui.common.util.Environment;

public class ReadOnlyMRIAttribute extends AbstractReadOnlyAttribute implements IUpdateInterval, IAdaptable {

	private static final int MAX_TRACE_FRAMES_IN_MESSAGE = 5;

	private final IConnectionHandle m_handle;
	private final MRI m_mri;
	private Object m_value;
	private String m_refreshProblem = null;

	protected ReadOnlyMRIAttribute(IConnectionHandle handle, MRI mri) {
		this(handle, mri, null);
	}

	public ReadOnlyMRIAttribute(IConnectionHandle handle, MRI mri, Object value) {
		super(new SimpleAttributeInfo(extractName(mri.getDataPath()),
				MRIMetadataToolkit.getMRIMetadata(handle, mri).getValueType()));
		m_handle = handle;
		m_mri = mri;
		m_value = value;
	}

	private static String extractName(String dataPath) {
		if (dataPath == null) {
			return "[null]"; //$NON-NLS-1$
		}
		int index = dataPath.lastIndexOf(MRI.VALUE_COMPOSITE_DELIMITER);
		if (index >= 0) {
			return '#' + dataPath.substring(index + 1);
		}
		return dataPath;
	}

	@Override
	public boolean hasChildren() {
		if (getValue() instanceof CompositeData) {
			return !((CompositeData) getValue()).getCompositeType().keySet().isEmpty();
		}
		return super.hasChildren();
	}

	@Override
	public Collection<?> getChildren() {
		if (getValue() instanceof CompositeData) {
			return getMRICompositeChildren((CompositeData) getValue());
		}
		return super.getChildren();
	}

	private Collection<?> getMRICompositeChildren(CompositeData compositeDataValue) {
		IConnectionHandle handle = getHandle();
		MRI baseMRI = getMRI();
		List<IReadOnlyAttribute> elements = new ArrayList<>();
		for (String key : compositeDataValue.getCompositeType().keySet()) {
			elements.add(new ReadOnlyMRIAttribute(handle, new MRI(baseMRI, key), compositeDataValue.get(key)));
		}
		return PartitionedList.create(elements);
	}

	public boolean refresh() {
		try {
			Object newValue = getMBeanHelperService().getAttributeValue(m_mri);
			if (m_value == null ? newValue == null : m_value.equals(newValue)) {
				return false;
			}
			m_refreshProblem = null;
			m_value = newValue;
			return true;
		} catch (ConnectionException e) {
			m_refreshProblem = Messages.ReadOnlyMRIAttribute_PROBLEM_CONNECTION_CLOSED;
			RJMXUIPlugin.getDefault().getLogger().log(Level.INFO,
					"Could not refresh ReadOnlyMRIAttribute since the connection was closed"); //$NON-NLS-1$
		} catch (UnmarshalException e) {
			m_refreshProblem = NLS.bind(Messages.ReadOnlyMRIAttribute_PROBLEM_UNMARSHAL, getMRI().getDataPath()) + "\n" //$NON-NLS-1$
					+ e.getMessage();
			if (Environment.isDebug()) {
				RJMXUIPlugin.getDefault().getLogger().log(Level.WARNING,
						MessageFormat.format("Could not unmarshal the value for attribute {0}: {1}", getMRI() //$NON-NLS-1$
								.getDataPath(), e.getMessage()),
						e);
			}
		} catch (MBeanException e) {
			Throwable cause = unmarshalMBeanException(e);
			m_refreshProblem = NLS.bind(Messages.ReadOnlyMRIAttribute_PROBLEM_SERVER, getMRI().getDataPath()) + "\n" //$NON-NLS-1$
					+ ExceptionToolkit.toString(cause, MAX_TRACE_FRAMES_IN_MESSAGE);
			if (Environment.isDebug()) {
				RJMXUIPlugin.getDefault().getLogger().log(Level.WARNING,
						MessageFormat.format("Server error getting attribute {0}: {1}", getMRI().getDataPath(), cause), //$NON-NLS-1$
						cause);
			}
		} catch (AttributeNotFoundException e) {
			// This can happen if we start a console before the first GC has happened (LastGcInfo is null).
			// In this case we should not warn.
			m_refreshProblem = NLS.bind(Messages.ReadOnlyMRIAttribute_PROBLEM_ATTRIBUTE_NOT_FOUND,
					getMRI().getDataPath());
			RJMXUIPlugin.getDefault().getLogger().log(Level.INFO,
					MessageFormat.format("Error getting attribute {0}: {1}", getMRI().getDataPath(), e.getMessage())); //$NON-NLS-1$
		} catch (Exception e) {
			m_refreshProblem = NLS.bind(Messages.ReadOnlyMRIAttribute_PROBLEM_EXCEPTION, getMRI().getDataPath(),
					e.getMessage()) + "\n" + Messages.ReadOnlyMRIAttribute_STACK_TRACE_IN_LOG; //$NON-NLS-1$
			RJMXUIPlugin.getDefault().getLogger().log(Level.WARNING,
					MessageFormat.format("Error getting attribute {0}: {1}", getMRI().getDataPath(), e.getMessage()), //$NON-NLS-1$
					e);
		}
		m_value = MRIValueEvent.UNAVAILABLE_VALUE;
		return false;
	}

	@Override
	public Object getValue() {
		return m_value;
	}

	protected IMBeanHelperService getMBeanHelperService() throws ConnectionException, ServiceNotAvailableException {
		return m_handle.getServiceOrThrow(IMBeanHelperService.class);
	}

	public void updateValue(Object value) {
		m_value = value;
	}

	public MRI getMRI() {
		return m_mri;
	}

	public IUnit getUnit() {
		return ExtendedMRIMetadataToolkit.getUnit(m_handle, m_mri);
	}

	protected IConnectionHandle getHandle() {
		return m_handle;
	}

	@Override
	public int getUpdateInterval() {
		IUpdatePolicy updatePolicy = UpdatePolicyToolkit.getUpdatePolicy(m_handle, m_mri);
		if (updatePolicy instanceof OneShotUpdatePolicy) {
			return IUpdateInterval.ONCE;
		} else if (updatePolicy instanceof SimpleUpdatePolicy) {
			return ((SimpleUpdatePolicy) updatePolicy).getIntervalTime();
		}
		return IUpdateInterval.DEFAULT;
	}

	private static IUpdatePolicy convertToUpdatePolicy(int interval) {
		switch (interval) {
		case IUpdateInterval.ONCE:
			return OneShotUpdatePolicy.newPolicy();
		case IUpdateInterval.DEFAULT:
			return DefaultUpdatePolicy.newPolicy();
		default:
			return SimpleUpdatePolicy.newPolicy(interval);
		}
	}

	@Override
	public void setUpdateInterval(int interval) {
		UpdatePolicyToolkit.setUpdatePolicy(m_handle, m_mri, convertToUpdatePolicy(interval));
	}

	@Override
	public boolean equals(Object that) {
		return (that instanceof ReadOnlyMRIAttribute) ? getMRI().equals(((ReadOnlyMRIAttribute) that).getMRI()) : false;
	}

	@Override
	public int hashCode() {
		return getMRI().hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + '{' + getMRI() + ',' + getValue() + '}';
	}

	/**
	 * Try to get all attribute values at once. Try to retrieve the attributes that did not receive
	 * a value one by one afterwards to get error messages on what is wrong.
	 *
	 * @param connection
	 *            the connection to use
	 * @param attributes
	 *            the attributes to refresh
	 */
	public static void refresh(IConnectionHandle connection, Map<MRI, ReadOnlyMRIAttribute> attributeMap) {
		try {
			MBeanServerConnection server = connection.getServiceOrThrow(MBeanServerConnection.class);
			Map<MRI, Object> attributes = AttributeValueToolkit.getAttributes(server, attributeMap.keySet());
			for (Entry<MRI, Object> value : attributes.entrySet()) {
				attributeMap.remove(value.getKey()).updateValue(value.getValue());
			}
		} catch (ConnectionException e) {
			RJMXUIPlugin.getDefault().getLogger().log(Level.INFO,
					"Could not refresh attributes since the connection was closed"); //$NON-NLS-1$
			return;
		} catch (Exception e) {
			RJMXUIPlugin.getDefault().getLogger().log(Level.WARNING, "Error getting attributes: " + e.getMessage()); //$NON-NLS-1$
		}
		for (ReadOnlyMRIAttribute attribute : attributeMap.values()) {
			attribute.refresh();
		}
	}

	private static Throwable unmarshalMBeanException(MBeanException e) {
		Throwable cause = e.getTargetException();
		if (cause instanceof RuntimeErrorException) {
			cause = ((RuntimeErrorException) cause).getTargetError();
		} else if (cause instanceof RuntimeMBeanException) {
			cause = ((RuntimeMBeanException) cause).getTargetException();
		}
		return cause;
	}

	public String getRefreshProblem() {
		return m_refreshProblem;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return MRI.class.equals(adapter) ? adapter.cast(m_mri) : null;
	}
}
