package org.openjdk.jmc.agent.impl;

import java.lang.reflect.Method;

import org.openjdk.jmc.agent.Agent;
import org.openjdk.jmc.agent.Convertable;

public class ResolvedConvertable extends AbstractConvertable implements Convertable {
	private final static String CONVERTER_METHOD = "convert";
	private final transient Class<?> converterClass;
	private final transient Method converterMethod;

	public ResolvedConvertable(String converterClassName) {
		super(converterClassName);
		Class<?> tmpClass = null;
		try {
			if (converterClassName != null) {
				tmpClass = Class.forName(converterClassName);
			}
		} catch (ClassNotFoundException e) {
			Agent.getLogger().severe("Failed to load specified converter class " + converterClassName
					+ " - will not use that converter!");
		}
		this.converterClass = tmpClass;
		this.converterMethod = getConvertMethod(tmpClass);
	}

	public Class<?> getConverterClass() {
		return converterClass;
	}

	public Method getConverterMethod() {
		return converterMethod;
	}

	private static Method getConvertMethod(Class<?> converterClass) {
		if (converterClass == null) {
			return null;
		}
		for (Method m : converterClass.getDeclaredMethods()) {
			if (CONVERTER_METHOD.equals(m.getName())) {
				return m;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return "Resolved " + getConverterClassName() + ":\nClass: " + converterClass.getCanonicalName() + "\nMethod: "
				+ getConverterMethod();
	}

}
