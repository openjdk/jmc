package org.openjdk.jmc.flightrecorder.writer.api;

import java.util.List;

public interface TypeStructure {
	List<? extends TypedField> getFields();

	List<Annotation> getAnnotations();
}
