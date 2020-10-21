package org.openjdk.jmc.flightrecorder.rules.jdk.util;

import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IPersister;
import org.openjdk.jmc.common.unit.QuantityConversionException;

public class ClassEntryPersister implements IPersister<ClassEntry> {

	@Override
	public IConstraint<ClassEntry> combine(IConstraint<?> other) {
		return null;
	}

	@Override
	public ClassEntry parsePersisted(String persistedValue) throws QuantityConversionException {
		return null;
	}

	@Override
	public ClassEntry parseInteractive(String interactiveValue) throws QuantityConversionException {
		return null;
	}

	@Override
	public boolean validate(ClassEntry value) {
		return false;
	}

	@Override
	public String persistableString(ClassEntry value) {
		return null;
	}

	@Override
	public String interactiveFormat(ClassEntry value) {
		return null;
	}

}
