package org.openjdk.jmc.agent.impl;

import org.openjdk.jmc.agent.Convertable;

public abstract class AbstractConvertable implements Convertable {
	private final String converterClassName;

	public AbstractConvertable(String converterClassName) {
		this.converterClassName = converterClassName;
	}

	@Override
	public String getConverterClassName() {
		return converterClassName;
	}
}
