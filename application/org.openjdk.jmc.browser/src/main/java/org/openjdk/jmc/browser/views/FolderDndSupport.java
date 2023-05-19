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
package org.openjdk.jmc.browser.views;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.openjdk.jmc.ui.common.util.ICopyable;

public class FolderDndSupport implements DropTargetListener, DragSourceListener {

	private int currentOperation;
	private final TreeViewer viewer;
	private final Folder root;

	public FolderDndSupport(TreeViewer treeViewer, Folder rootFolder) {
		viewer = treeViewer;
		root = rootFolder;
		Transfer[] transfer = new Transfer[] {LocalSelectionTransfer.getTransfer()};
		int oper = DND.DROP_MOVE | DND.DROP_COPY;
		viewer.addDropSupport(oper, transfer, this);
		viewer.addDragSupport(oper, transfer, this);
	}

	// DropTargetListener

	@Override
	public void dragOver(DropTargetEvent event) {
		event.detail = getDragOverAction(event);
	}

	@Override
	public void dragEnter(DropTargetEvent event) {
		currentOperation = event.detail == DND.DROP_DEFAULT ? DND.DROP_MOVE : event.detail;
		event.detail = getDragOverAction(event);
	}

	@Override
	public void dragOperationChanged(DropTargetEvent event) {
		currentOperation = event.detail == DND.DROP_DEFAULT ? DND.DROP_MOVE : event.detail;
		event.detail = getDragOverAction(event);
	}

	@Override
	public void drop(DropTargetEvent event) {
		boolean copy = event.detail == DND.DROP_COPY;
		Object element = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
		if (event.item == null) {
			doDrop(element, root, copy);
		} else if (event.item.getData() instanceof Folder) {
			doDrop(element, (Folder) event.item.getData(), copy);
			viewer.setExpandedState(event.item.getData(), true);
		}
	}

	@Override
	public void dragLeave(DropTargetEvent event) {
	}

	@Override
	public void dropAccept(DropTargetEvent event) {
	}

	protected void doDrop(Object element, Folder inFolder, boolean copy) {
		if (copy) {
			if (element instanceof ICopyable) {
				ICopyable copyable = ((ICopyable) element);
				if (copyable.isCopyable()) {
					inFolder.insert(copyable.copy());
				}
			}
		} else {
			inFolder.insert(element);
		}
	}

	protected int getDragOverAction(DropTargetEvent event) {
		Object selected = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
		if (event.item == null) {
			return getDragOverAction(root, selected);
		} else if (event.item.getData() instanceof Folder) {
			return getDragOverAction((Folder) event.item.getData(), selected);
		} else {
			return DND.DROP_NONE;
		}
	}

	protected int getDragOverAction(Folder target, Object item) {
		if (target.isModifiable()) {
			if (isMoveable(item, target)) {
				return getDragOverAction(target, item, isCopyable(item) ? currentOperation : DND.DROP_MOVE);
			} else if (isCopyable(item)) {
				return getDragOverAction(target, item, DND.DROP_COPY);
			}
		}
		return DND.DROP_NONE;
	}

	protected int getDragOverAction(Folder target, Object item, int opertation) {
		if (item instanceof Folder) {
			Folder draggedFolder = (Folder) item;
			if (target.hasSubFolder(draggedFolder.getName())) {
				return DND.DROP_NONE;
			} else if (target.isDescendentForm(draggedFolder)) {
				return DND.DROP_NONE;
			}
		}
		return opertation;
	}

	// DragSourceListener

	@Override
	public void dragStart(DragSourceEvent event) {
		if (((IStructuredSelection) viewer.getSelection()).size() == 1) {
			Object el = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
			event.doit = isCopyable(el) || isMoveable(el, null);
		} else {
			event.doit = false;
		}
	}

	@Override
	public void dragSetData(DragSourceEvent event) {
		LocalSelectionTransfer.getTransfer().setSelection(viewer.getSelection());
	}

	@Override
	public void dragFinished(DragSourceEvent event) {
	}

	protected boolean isCopyable(Object item) {
		return item instanceof ICopyable && ((ICopyable) item).isCopyable();
	}

	protected boolean isMoveable(Object item, Object target) {
		if (item instanceof Folder) {
			Folder f = (Folder) item;
			return f.isModifiable();
		}
		return true;
	}

}
