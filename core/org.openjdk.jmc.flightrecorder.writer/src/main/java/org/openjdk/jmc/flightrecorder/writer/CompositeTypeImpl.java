package org.openjdk.jmc.flightrecorder.writer;

import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** A composite JFR type */
final class CompositeTypeImpl extends BaseType {
	private int hashCode = 0;

	private final Map<String, TypedFieldImpl> fieldMap;
	private final List<TypedFieldImpl> fields;
	private final List<Annotation> annotations;

	CompositeTypeImpl(long id, String name, String supertype, TypeStructureImpl typeStructure,
			ConstantPools constantPools, TypesImpl types) {
		super(id, name, supertype, constantPools, types);
		this.fields = collectFields(typeStructure);
		this.annotations = typeStructure == null ? Collections.emptyList()
				: Collections.unmodifiableList(typeStructure.getAnnotations());
		this.fieldMap = fields.stream().collect(Collectors.toMap(TypedFieldImpl::getName, f -> f));
	}

	private List<TypedFieldImpl> collectFields(TypeStructureImpl typeStructure) {
		if (typeStructure == null) {
			return Collections.emptyList();
		}
		List<TypedFieldImpl> fields = new ArrayList<>();
		for (TypedFieldImpl field : typeStructure.fields) {
			if (field.getType() == SelfType.INSTANCE) {
				fields.add(new TypedFieldImpl(this, field.getName(), field.isArray()));
			} else {
				fields.add(field);
			}
		}
		return fields;
	}

	@Override
	public boolean isBuiltin() {
		return false;
	}

	@Override
	public List<TypedFieldImpl> getFields() {
		return fields;
	}

	@Override
	public TypedFieldImpl getField(String name) {
		return fieldMap.get(name);
	}

	@Override
	public List<Annotation> getAnnotations() {
		return annotations;
	}

	@Override
	public boolean canAccept(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof TypedValueImpl) {
			return ((TypedValueImpl) value).getType().equals(this);
		}
		return value instanceof Map;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		CompositeTypeImpl that = (CompositeTypeImpl) o;
		return fields.equals(that.fields) && annotations.equals(that.annotations);
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			List<TypedFieldImpl> nonRecursiveFields = new ArrayList<>(fields.size());
			for (TypedFieldImpl typedField : fields) {
				if (typedField.getType() != this) {
					nonRecursiveFields.add(typedField);
				}
			}
			hashCode = NonZeroHashCode.hash(super.hashCode(), nonRecursiveFields, annotations);
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return "CompositeType(" + getTypeName() + ")";
	}
}
