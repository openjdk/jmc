package org.openjdk.jmc.flightrecorder.writer;

import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.api.TypeStructure;
import org.openjdk.jmc.flightrecorder.writer.api.TypedField;

import java.util.Collections;
import java.util.List;

/** A structure-like holder class for the type's fields and annotations */
final class TypeStructureImpl implements TypeStructure {
	static final TypeStructureImpl EMPTY = new TypeStructureImpl(Collections.emptyList(), Collections.emptyList());

	final List<TypedFieldImpl> fields;
	final List<Annotation> annotations;

	TypeStructureImpl(List<TypedFieldImpl> fields, List<Annotation> annotations) {
		this.fields = fields;
		this.annotations = annotations;
	}

	@Override
	public List<? extends TypedField> getFields() {
		return fields;
	}

	@Override
	public List<Annotation> getAnnotations() {
		return annotations;
	}
}
