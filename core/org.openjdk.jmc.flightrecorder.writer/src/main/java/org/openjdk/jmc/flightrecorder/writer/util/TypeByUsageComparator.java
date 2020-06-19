package org.openjdk.jmc.flightrecorder.writer.util;

import org.openjdk.jmc.flightrecorder.writer.api.Type;

import java.util.Comparator;

/**
 * A {@linkplain Type} comparator based on the 'is-used' relationship.
 * <ol>
 * <li>Given two types T1 and T2, T1 will be considered 'less-than' T2 if T1 is used by T2
 * transitively.
 * <li>Type T1 is considered to be 'used' by type T2 if any field of T2 is of type T1 or T1 is used
 * by any T2 field type TF.
 * </ol>
 */
public final class TypeByUsageComparator implements Comparator<Type> {
	public static final TypeByUsageComparator INSTANCE = new TypeByUsageComparator();

	@Override
	public int compare(Type t1, Type t2) {
		if (t1 == t2) {
			return 0;
		}
		if (t1 == null) {
			return -1;
		}
		if (t2 == null) {
			return 1;
		}
		return t1.isUsedBy(t2) ? -1 : 1;
	}
}
