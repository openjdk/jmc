package org.openjdk.jmc.common.test.unit;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.UnitLookup;

public class QuantityRangeTest {

	@Test
	public void intersection_overlap() {
		IRange<IQuantity> r1 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		IRange<IQuantity> r2 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(5),
				UnitLookup.SECOND.quantity(15));
		IRange<IQuantity> intersection = QuantityRange.intersection(r1, r2);
		IRange<IQuantity> expected = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(5),
				UnitLookup.SECOND.quantity(10));
		Assert.assertEquals(expected, intersection);
	}

	@Test
	public void intersection_full_overlap() {
		IRange<IQuantity> r1 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		IRange<IQuantity> r2 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		IRange<IQuantity> intersection = QuantityRange.intersection(r1, r2);
		IRange<IQuantity> expected = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		Assert.assertEquals(expected, intersection);
	}

	@Test
	public void intersection_limit() {
		IRange<IQuantity> r1 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		IRange<IQuantity> r2 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(10),
				UnitLookup.SECOND.quantity(20));
		IRange<IQuantity> intersection = QuantityRange.intersection(r1, r2);
		IRange<IQuantity> expected = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(10),
				UnitLookup.SECOND.quantity(10));
		Assert.assertEquals(expected, intersection);
	}

	@Test
	public void intersection_disjoint() {
		IRange<IQuantity> r1 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(5));
		IRange<IQuantity> r2 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(10),
				UnitLookup.SECOND.quantity(15));
		IRange<IQuantity> intersection = QuantityRange.intersection(r1, r2);
		Assert.assertNull(intersection);
	}

	@Test
	public void union_overlap() {
		IRange<IQuantity> r1 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		IRange<IQuantity> r2 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(5),
				UnitLookup.SECOND.quantity(15));
		IRange<IQuantity> union = QuantityRange.union(r1, r2);
		IRange<IQuantity> expected = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(15));
		Assert.assertEquals(expected, union);
	}

	@Test
	public void union_full_overlap() {
		IRange<IQuantity> r1 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		IRange<IQuantity> r2 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		IRange<IQuantity> union = QuantityRange.union(r1, r2);
		IRange<IQuantity> expected = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		Assert.assertEquals(expected, union);
	}

	@Test
	public void union_limit() {
		IRange<IQuantity> r1 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		IRange<IQuantity> r2 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(10),
				UnitLookup.SECOND.quantity(20));
		IRange<IQuantity> union = QuantityRange.union(r1, r2);
		IRange<IQuantity> expected = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(20));
		Assert.assertEquals(expected, union);
	}

	@Test
	public void union_disjoint() {
		IRange<IQuantity> r1 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(5));
		IRange<IQuantity> r2 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(10),
				UnitLookup.SECOND.quantity(15));
		IRange<IQuantity> union = QuantityRange.union(r1, r2);
		Assert.assertNull(union);
	}
}
