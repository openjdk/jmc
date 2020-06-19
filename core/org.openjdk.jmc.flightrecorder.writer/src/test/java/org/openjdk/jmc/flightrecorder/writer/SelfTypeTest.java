package org.openjdk.jmc.flightrecorder.writer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SelfTypeTest {
	@Test
	void isBuiltin() {
		assertThrows(UnsupportedOperationException.class, () -> SelfType.INSTANCE.isBuiltin());
	}

	@Test
	void getFields() {
		assertThrows(UnsupportedOperationException.class, () -> SelfType.INSTANCE.getFields());
	}

	@Test
	void getField() {
		assertThrows(UnsupportedOperationException.class, () -> SelfType.INSTANCE.getField("field"));
	}

	@Test
	void getAnnotations() {
		assertThrows(UnsupportedOperationException.class, () -> SelfType.INSTANCE.getAnnotations());
	}

	@Test
	void canAccept() {
		assertThrows(UnsupportedOperationException.class, () -> SelfType.INSTANCE.canAccept("value"));
	}
}
