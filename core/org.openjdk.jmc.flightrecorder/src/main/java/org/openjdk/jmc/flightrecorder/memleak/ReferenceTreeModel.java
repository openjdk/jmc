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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openjdk.jmc.common.IMCOldObject;
import org.openjdk.jmc.common.IMCOldObjectGcRoot;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;

/**
 * The ReferenceTreeModel is used to build a representation of Old Object Sample JFR events which
 * consists of one tree per GC root. Each object in the model is a ReferenceTreeObject instance,
 * which is a subclass of IMCOldObject. The objects referred to as "Leak Objects" here are
 * specifically those that were sampled by the JFR implementation and exist as the top-level Old
 * Objects in the Old Object Sample event.
 */
public class ReferenceTreeModel {

	private final Map<IQuantity, ReferenceTreeObject> map = new HashMap<>();
	private final List<ReferenceTreeObject> rootObjects = new ArrayList<>();
	private final List<ReferenceTreeObject> leakObjects = new ArrayList<>();
	private final Map<IItem, ReferenceTreeObject> rootObjectsByLeakItems = new HashMap<>();

	private ReferenceTreeModel() {
	}

	/**
	 * Creates and returns a ReferenceTreeModel object that represents the total reference tree
	 * created by Old Object Samples and their aggregated reference chains.
	 *
	 * @param items
	 *            should be a filtered item collection containing only the Old Object Sample events
	 * @return a ReferenceTreeModel object to use when querying the reference tree
	 */
	public static ReferenceTreeModel buildReferenceTree(IItemCollection items) {
		ReferenceTreeModel model = new ReferenceTreeModel();
		for (IItemIterable itemIterable : items) {
			IType<IItem> type = itemIterable.getType();
			for (IItem item : itemIterable) {
				model.add(item, JdkAttributes.OBJECT.getAccessor(type), JdkAttributes.ALLOCATION_TIME.getAccessor(type),
						JdkAttributes.GC_ROOT.getAccessor(type), type);
			}
		}
		return model;
	}

	/**
	 * @return a list of all objects that are Roots in the reference tree
	 */
	public List<ReferenceTreeObject> getRootObjects() {
		return rootObjects;
	}

	/**
	 * Performs a leak relevance calculation on every object in the reference tree and sets this
	 * value to the respective objects.
	 *
	 * @param relevanceThreshold
	 *            the threshold used to determine which objects to return as interesting memory leak
	 *            candidates
	 * @return a list of ReferenceTreeObject instances that have been deemed to be memory leak
	 *         candidates
	 */
	public List<ReferenceTreeObject> getLeakCandidates(double relevanceThreshold) {
		List<ReferenceTreeObject> candidates = new ArrayList<>();
		for (ReferenceTreeObject root : rootObjects) {
			int distanceFromRoot = 1;
			ReferenceTreeObject leakCandidate = null;
			for (ReferenceTreeObject child : root.getChildren()) {
				leakCandidate = setLeakRelevance(child, root, distanceFromRoot, leakCandidate);
				leakCandidate = getLeakCandidates(child, root, distanceFromRoot + 1, leakCandidate);
			}
			if (leakCandidate != null) {
				root.setLeakRelevance(leakCandidate.getLeakRelevance());
				if (leakCandidate.getLeakRelevance() > relevanceThreshold) {
					candidates.add(leakCandidate);
				}
			}
		}
		return candidates;
	}

	/**
	 * A helper method to traverse the tree with a recursive depth-first search.
	 * <p>
	 * Every touched node gets a calculated distance to the root node to assist with calculating how
	 * likely that particular node is to be a leak candidate.
	 * 
	 * @param object
	 *            the node to begin/continue the search from
	 * @param root
	 *            the original root node, needed for candidate evaluation
	 * @param distanceFromRoot
	 *            the distance from the root node to the current object node
	 * @param leakCandidate
	 *            the most promising leak candidate found so far
	 * @return the most promising leak candidate
	 */
	private ReferenceTreeObject getLeakCandidates(
		ReferenceTreeObject object, ReferenceTreeObject root, int distanceFromRoot, ReferenceTreeObject leakCandidate) {
		ReferenceTreeObject candidate = leakCandidate;
		for (ReferenceTreeObject child : object.getChildren()) {
			int distance = distanceFromRoot + child.getReferrerSkip();
			candidate = setLeakRelevance(child, root, distance, candidate);
			candidate = getLeakCandidates(child, root, distance + 1, candidate);
		}
		return candidate;
	}

	/**
	 * Evaluates a ReferenceTreeObject as a candidate for a memory leak.
	 * <p>
	 * The calculation of each objects relevance as a candidate is a factor of the following:
	 * <ul>
	 * <li>distance from the root object (higher distance means that it is more likely to be a
	 * candidate)</li>
	 * <li>the ratio of how many other objects this particular object keeps alive to how many
	 * objects its root object keeps alive (a high ratio here together with the distance is a good
	 * indicator that this could be a leak)</li>
	 * <li>the ratio of how many objects this object keeps alive to how many objects are alive
	 * globally (this is a good indicator of the severity of this particular leak)</li>
	 * </ul>
	 * These factors together represent a simple number that is used to gather the most promising
	 * leak candidates in the tree.
	 * 
	 * @param object
	 *            the object to evaluate as a candidate
	 * @param root
	 *            the root object
	 * @param distanceFromRoot
	 *            how many steps from the root the object is
	 * @param leakCandidate
	 *            the prior best leak candidate
	 * @return either leakCandidate or object, depending on whether or not the relevance is higher
	 *         for object
	 */
	private ReferenceTreeObject setLeakRelevance(
		ReferenceTreeObject object, ReferenceTreeObject root, int distanceFromRoot, ReferenceTreeObject leakCandidate) {
		int keptAlive = object.getObjectsKeptAliveCount();
		double localRatio = ((double) keptAlive) / root.getObjectsKeptAliveCount();
		double globalRatio = ((double) keptAlive) / leakObjects.size();
		double relevance = localRatio * distanceFromRoot * globalRatio;
		object.setLeakRelevance(relevance);
		object.setDistanceFromRoot(distanceFromRoot);
		if (leakCandidate == null || leakCandidate.getLeakRelevance() < relevance) {
			return object;
		}
		return leakCandidate;
	}

	/**
	 * @return a map between classes and the corresponding reference tree objects
	 */
	public Map<IMCType, List<ReferenceTreeObject>> getObjectsByType() {
		Map<IMCType, List<ReferenceTreeObject>> map = new HashMap<>();
		for (ReferenceTreeObject referenceTreeObject : leakObjects) {
			IMCType asType = referenceTreeObject.getType();
			List<ReferenceTreeObject> list = map.get(asType);
			if (list == null) {
				list = new ArrayList<>();
				map.put(asType, list);
			}
			list.add(referenceTreeObject);
		}
		return map;
	}

	/**
	 * @param timerange
	 *            a range of time that specifies which root objects to retrieve
	 * @return a list of all objects that are Roots in the reference tree during the specified time
	 *         range
	 */
	public Collection<ReferenceTreeObject> getRootObjects(IRange<IQuantity> timerange) {
		List<ReferenceTreeObject> objects = new ArrayList<>();
		for (ReferenceTreeObject referenceTreeObject : rootObjects) {
			IQuantity itemTime = referenceTreeObject.getTimestamp();
			if (timerange.getStart().compareTo(itemTime) <= 0 && timerange.getEnd().compareTo(itemTime) >= 0) {
				objects.add(referenceTreeObject);
			}
		}
		return objects;
	}

	/**
	 * @return a list of the actual objects sampled by the Old Object Sample event
	 */
	public List<ReferenceTreeObject> getLeakObjects() {
		return leakObjects;
	}

	/**
	 * @param address
	 *            the address of a specific object to retrieve from the reference tree
	 * @return the specified object
	 */
	public ReferenceTreeObject getObject(IQuantity address) {
		return map.get(address);
	}

	/**
	 * Adds an {@link IItem} to the tree model as a {@link ReferenceTreeObject}
	 * 
	 * @param item
	 *            the item to add
	 * @param objectAccessor
	 *            an accessor for {@link IMCOldObject}
	 * @param allocationTimeAccessor
	 *            an accessor for the allocation time
	 * @param gcRootAccessor
	 *            an accessor for {@link IMCOldObjectGcRoot}
	 * @param type
	 *            the type of the item
	 */
	private void add(
		IItem item, IMemberAccessor<IMCOldObject, IItem> objectAccessor,
		IMemberAccessor<IQuantity, IItem> allocationTimeAccessor,
		IMemberAccessor<IMCOldObjectGcRoot, IItem> gcRootAccessor, IType<IItem> type) {
		Set<IQuantity> addresses = new HashSet<>();

		IQuantity timestamp = allocationTimeAccessor.getMember(item);
		IMCOldObject object = objectAccessor.getMember(item);

		addresses.add(object.getAddress());
		ReferenceTreeObject last = map.get(object.getAddress());
		if (last == null) {
			// initializing new leak object
			last = new ReferenceTreeObject(timestamp, object);
			last.addItem(item);
			leakObjects.add(last);
			map.put(object.getAddress(), last);
		}
		ReferenceTreeObject node = null;
		boolean root = true;
		object = object.getReferrer();
		IQuantity address;
		while (object != null) {
			address = object.getAddress();
			if (address.longValue() == 0) {
				Logger.getLogger(ReferenceTreeModel.class.getName()).log(Level.WARNING,
						"Found object without address, breaking parsing of referrer chain."); //$NON-NLS-1$
				break;
			}
			if (addresses.contains(address)) {
				Logger.getLogger(ReferenceTreeModel.class.getName()).log(Level.WARNING,
						"Same addresses multiple times in same chain " + address); //$NON-NLS-1$
				break;
			} else {
				addresses.add(address);
			}
			node = map.get(address);
			if (node == null) {
				node = new ReferenceTreeObject(timestamp, object);
				node.addRoot(gcRootAccessor.getMember(item));
				map.put(address, node);
				object = object.getReferrer();
			} else {
				if (last != null) {
					node.addChild(last);
				}
				root = false;
				break;
			}
			if (last != null) {
				node.addChild(last);
			}
			last = node;
		}
		if (last != null) {
			if (root) {
				rootObjects.add(last);
				rootObjectsByLeakItems.put(item, last);
			}
		}
		object = objectAccessor.getMember(item);
		if (object != null) {
			address = object.getAddress();
			node = map.get(address);
			while (node != null) {
				node.incrementObjectsKeptAliveCount();
				node.addItem(item);
				node = node.getParent();
			}
		}
	}
}
