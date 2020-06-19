package org.openjdk.jmc.flightrecorder.writer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ConstantPoolsTest {
	private ConstantPools instance;
	private TypeImpl type1;
	private TypeImpl type2;

	@BeforeEach
	void setUp() {
		instance = new ConstantPools();
		type1 = Mockito.mock(TypeImpl.class);
		type2 = Mockito.mock(TypeImpl.class);
	}

	@Test
	void forTypeWithCP() {
		Mockito.when(type1.hasConstantPool()).thenReturn(true);
		assertNotNull(instance.forType(type1));
	}

	@Test
	void forTypeWithoutCP() {
		Mockito.when(type1.hasConstantPool()).thenReturn(false);
		assertThrows(IllegalArgumentException.class, () -> instance.forType(type1));
	}

	@Test
	void size() {
		assertEquals(0, instance.size());
		addConstantPool();
		assertEquals(2, instance.size());
	}

	@Test
	void iteratorEmpty() {
		Iterator<ConstantPool> iterator = instance.iterator();
		assertNotNull(iterator);
		assertFalse(iterator.hasNext());
	}

	@Test
	void iterator() {
		addConstantPool();
		Iterator<ConstantPool> iterator = instance.iterator();
		assertNotNull(iterator);
		assertTrue(iterator.hasNext());
		assertNotNull(iterator.next());
	}

	@Test
	void spliteratorEmpty() {
		Spliterator<ConstantPool> iterator = instance.spliterator();
		assertNotNull(iterator);
		AtomicReference<ConstantPool> pool = new AtomicReference<>();
		assertFalse(iterator.tryAdvance(pool::set));
		assertNull(pool.get());
	}

	@Test
	void spliterator() {
		addConstantPool();
		Spliterator<ConstantPool> iterator = instance.spliterator();
		assertNotNull(iterator);
		AtomicReference<ConstantPool> pool = new AtomicReference<>();
		assertTrue(iterator.tryAdvance(pool::set));
		assertNotNull(pool.get());
	}

	@Test
	void forEachEmpty() {
		AtomicInteger cntr = new AtomicInteger();
		instance.forEach(cp -> cntr.incrementAndGet());
		assertEquals(0, cntr.get());
	}

	@Test
	void forEach() {
		addConstantPool();
		AtomicInteger cntr = new AtomicInteger();
		instance.forEach(cp -> cntr.incrementAndGet());
		assertNotEquals(0, cntr.get());
	}

	private void addConstantPool() {
		Mockito.when(type1.hasConstantPool()).thenReturn(true);
		Mockito.when(type2.hasConstantPool()).thenReturn(true);
		instance.forType(type1);
		instance.forType(type2);
	}
}
