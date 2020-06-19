package org.openjdk.jmc.flightrecorder.writer;

import lombok.NonNull;
import lombok.ToString;
import org.openjdk.jmc.flightrecorder.writer.api.TypedFieldValue;
import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

final class SingleFieldMap implements Map<String, TypedFieldValue> {
	private int hashCode = 0;
	private final ImmutableMapEntry<String, TypedFieldValue> entry;

	SingleFieldMap(@NonNull String name, @NonNull TypedFieldValue value) {
		this.entry = new ImmutableMapEntry<>(name, value);
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public boolean containsKey(Object o) {
		return entry.getKey().equals(o);
	}

	@Override
	public boolean containsValue(Object o) {
		return entry.getValue().equals(o);
	}

	@Override
	public TypedFieldValue get(Object o) {
		return entry.getKey().equals(o) ? entry.getValue() : null;
	}

	@Override
	public TypedFieldValue put(String s, TypedFieldValue typedFieldValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedFieldValue remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends String, ? extends TypedFieldValue> map) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> keySet() {
		return Collections.singleton(entry.getKey());
	}

	@Override
	public Collection<TypedFieldValue> values() {
		return Collections.singleton(entry.getValue());
	}

	@Override
	public Set<Entry<String, TypedFieldValue>> entrySet() {
		return Collections.singleton(entry);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SingleFieldMap that = (SingleFieldMap) o;
		return entry.equals(that.entry);
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = NonZeroHashCode.hash(entry);
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return "SingleFieldMap{" + entry.getKey() + "=" + entry.getValue() + '}';
	}
}
