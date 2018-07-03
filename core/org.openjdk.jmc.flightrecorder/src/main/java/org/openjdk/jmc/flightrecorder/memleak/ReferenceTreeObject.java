/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmc.flightrecorder.memleak;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openjdk.jmc.common.IMCOldObject;
import org.openjdk.jmc.common.IMCOldObjectArray;
import org.openjdk.jmc.common.IMCOldObjectField;
import org.openjdk.jmc.common.IMCOldObjectGcRoot;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.unit.IQuantity;

/**
 * A data type representing alive objects in a Java heap used in a {@link ReferenceTreeModel}.
 */
public class ReferenceTreeObject implements IMCOldObject {

	public enum ReferenceTreeObjectType {
		Array, InstanceField, JavaObject, LeakObject
	}

	private final List<ReferenceTreeObject> children = new ArrayList<>();
	private final Set<IItem> items = new HashSet<>();

	private String rootDescription;
	private IMCOldObject object;
	private int objectsKeptAliveCount;
	private ReferenceTreeObject parent;
	private IQuantity timestamp;
	private double leakRelevance;
	private int distanceFromRoot;
	public static final int FORMAT_PACKAGE = 0b00001;
	public static final int FORMAT_FIELD = 0b00010;
	public static final int FORMAT_STATIC_MODIFIER = 0b00100;
	public static final int FORMAT_OTHER_MODIFIERS = 0b01000;
	public static final int FORMAT_ARRAY_INFO = 0b10000;

	/**
	 * @param timestamp
	 *            a timestamp representing when the object was allocated
	 * @param object
	 *            the object itself
	 */
	ReferenceTreeObject(IQuantity timestamp, IMCOldObject object) {
		this.timestamp = timestamp;
		this.object = object;
		leakRelevance = -1;
		distanceFromRoot = 0;
	}

	/**
	 * @param distance
	 *            the distance from the root object
	 */
	void setDistanceFromRoot(int distance) {
		this.distanceFromRoot = distance;
	}

	/**
	 * @return the number of steps in a referral chain from the root object
	 */
	public int getDistanceFromRoot() {
		return distanceFromRoot;
	}

	/**
	 * @param relevance
	 *            how relevant this object is as a leak candidate
	 */
	void setLeakRelevance(double relevance) {
		this.leakRelevance = relevance;
	}

	/**
	 * @return the relevance of this object for memory leak detection
	 */
	public double getLeakRelevance() {
		return this.leakRelevance;
	}

	/**
	 * @param node
	 *            a child to be added to this object
	 */
	void addChild(ReferenceTreeObject node) {
		if (!children.contains(node)) {
			children.add(node);
			node.setParent(this);
		}
	}

	/**
	 * @param item
	 *            an item this object keeps alive
	 */
	void addItem(IItem item) {
		items.add(item);
	}

	/**
	 * This method is used when it is necessary to get information about all objects this object
	 * keeps alive in the {@link ReferenceTreeModel}. E.g. the Mission Control GUI uses this when a
	 * user selects a row in the tree to show everything below it as well in the properties view.
	 *
	 * @return a set representing all {@link IItem} objects this object keeps alive, including
	 *         itself
	 */
	public Set<IItem> getItems() {
		return items;
	}

	/**
	 * @param root
	 *            a GC root description
	 */
	void addRoot(IMCOldObjectGcRoot root) {
		if (root != null) {
			rootDescription = root.toString();
		}
	}

	/**
	 * @return the GC root description
	 */
	public String getRootDescription() {
		return rootDescription;
	}

	@Override
	public IQuantity getAddress() {
		return object.getAddress();
	}

	@Override
	public IMCOldObjectArray getReferrerArray() {
		return object.getReferrerArray();
	}

	/**
	 * @return if this object is an array, gets information representing that array, {@code null}
	 *         otherwise
	 */
	public IMCOldObjectArray getArray() {
		if (getChildren().size() > 0) {
			return getChildren().get(0).getReferrerArray();
		}
		return null;
	}

	/**
	 * @return the children of this object
	 */
	public List<ReferenceTreeObject> getChildren() {
		return children;
	}

	@Override
	public IMCOldObjectField getReferrerField() {
		return object.getReferrerField();
	}

	/**
	 * @return if this object is a field, gets information representing that field, {@code null}
	 *         otherwise
	 */
	public IMCOldObjectField getField() {
		if (getChildren().size() > 0) {
			return getChildren().get(0).getReferrerField();
		}
		return null;
	}

	@Override
	public String getDescription() {
		return object.getDescription();
	}

	/**
	 * @return the number of objects this object keeps alive
	 */
	public int getObjectsKeptAliveCount() {
		return objectsKeptAliveCount;
	}

	/**
	 * This method returns an object that is keeping this object alive.
	 *
	 * @return the object linking to this object from the direction of the gc root
	 */
	public ReferenceTreeObject getParent() {
		return parent;
	}

	@Override
	public IMCOldObject getReferrer() {
		return object.getReferrer();
	}

	/**
	 * @return the timestamp this object was allocated
	 */
	public IQuantity getTimestamp() {
		return timestamp;
	}

	@Override
	public IMCType getType() {
		return object.getType();
	}

	@Override
	public int hashCode() {
		return object.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ReferenceTreeObject) {
			ReferenceTreeObject that = (ReferenceTreeObject) o;
			return that.getAddress().equals(this.getAddress());
		}
		return false;
	}

	/**
	 * Increments the number of objects this object keeps alive.
	 */
	void incrementObjectsKeptAliveCount() {
		objectsKeptAliveCount++;
	}

	/**
	 * @param parent
	 *            the parent of this object
	 */
	public void setParent(ReferenceTreeObject parent) {
		this.parent = parent;
	}

	@Override
	public int getReferrerSkip() {
		return object.getReferrerSkip();
	}

	/**
	 * Returns a string representation of this object.
	 *
	 * @param displayFormatting
	 *            an int describing how this object is to be represented, using bitwise masking of
	 *            constants defined in {@link ReferenceTreeObject}
	 * @return a human readable string representation of this object
	 */
	public String toString(int displayFormatting) {
		String text = getType().getTypeName();
		if ((displayFormatting & ReferenceTreeObject.FORMAT_PACKAGE) != 0) {
			text = getType().getFullName();
		}
		if (this.getChildren().size() > 0 && getField() != null) {
			if ((displayFormatting & ReferenceTreeObject.FORMAT_FIELD) != 0) {
				text = text + "." + getField().getName(); //$NON-NLS-1$
			}
			if (getField().getModifier() != null) {
				String modifiers = Modifier.toString(getField().getModifier());
				if ((displayFormatting & ReferenceTreeObject.FORMAT_STATIC_MODIFIER) != 0
						&& (displayFormatting & ReferenceTreeObject.FORMAT_OTHER_MODIFIERS) == 0) {
					if (modifiers.contains("static")) { //$NON-NLS-1$
						text = "static " + text; //$NON-NLS-1$
					}
				} else if ((displayFormatting & ReferenceTreeObject.FORMAT_OTHER_MODIFIERS) != 0
						&& (displayFormatting & ReferenceTreeObject.FORMAT_STATIC_MODIFIER) == 0) {
					String nonStaticModifiers = modifiers.replaceAll("static ", ""); //$NON-NLS-1$ //$NON-NLS-2$
					if (!"".equals(nonStaticModifiers)) { //$NON-NLS-1$
						text = nonStaticModifiers + " " + text; //$NON-NLS-1$
					}
				} else if ((displayFormatting & ReferenceTreeObject.FORMAT_STATIC_MODIFIER) != 0
						&& (displayFormatting & ReferenceTreeObject.FORMAT_OTHER_MODIFIERS) != 0) {
					if (!"".equals(modifiers)) { //$NON-NLS-1$
						text = modifiers + " " + text; //$NON-NLS-1$
					}
				}
			}
		}
		if ((displayFormatting & ReferenceTreeObject.FORMAT_ARRAY_INFO) != 0 && this.getArray() != null) {
			if (text.endsWith("[]")) { //$NON-NLS-1$
				text = text.substring(0, text.length() - 1) + getArray().getIndex() + "/" + getArray().getSize() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				text = text + getArray().getIndex() + "/" + getArray().getSize(); //$NON-NLS-1$
			}
		}
		return text.trim();
	}

}
