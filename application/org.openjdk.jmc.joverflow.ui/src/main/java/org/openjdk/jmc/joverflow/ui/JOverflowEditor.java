/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.joverflow.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;

import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.ui.model.ModelLoader;
import org.openjdk.jmc.joverflow.ui.model.ModelLoaderListener;
import org.openjdk.jmc.joverflow.ui.model.ReferenceChain;
import org.openjdk.jmc.ui.misc.CompositeToolkit;
import org.openjdk.jmc.ui.misc.DialogToolkit;

public class JOverflowEditor extends EditorPart {
	private final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

	private FormToolkit mFormToolkit;

	private Composite mParentComposite;
	private ProgressIndicator mProgressIndicator;
	private JOverflowUi mJOverflowUi;

	private ModelLoader mLoader;
	private Snapshot mSnapshot;
	private Collection<ReferenceChain> mModel;
	private Future<?> mBackground;

	private final List<UiLoadedListener> mUiLoadedListeners = new ArrayList<>();

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);

		IPathEditorInput ipei;
		if (input instanceof IPathEditorInput) {
			ipei = (IPathEditorInput) input;
		} else {
			ipei = input.getAdapter(IPathEditorInput.class);
		}

		if (ipei == null) {
			// Not likely to be null, but guard just in case
			throw new PartInitException("The JOverflow editor cannot handle the provided editor input"); //$NON-NLS-1$
		}

		loadModel(ipei);
	}

	private void loadModel(final IPathEditorInput input) {
		if (mLoader != null) {
			mLoader.cancel();
			mLoader = null;
		}

		if (mBackground != null && !mBackground.isDone()) {
			mBackground.cancel(true);
		}

		if (mSnapshot != null) {
			mSnapshot.discard();
			mSnapshot = null;
		}

		setPartName(input.getName());

		String inputPath = input.getPath().toOSString();
		mLoader = new ModelLoader(inputPath, new ModelLoaderListener() {
			private double worked = 0; // the amount of work already done

			@Override
			public void onProgressUpdate(final double progress) {
				updateUi(progress);
			}

			@Override
			public void onModelLoaded(Snapshot snapshot, final Collection<ReferenceChain> model) {
				mSnapshot = snapshot;
				mModel = model;

				updateUi(1);
				getSite().getShell().getDisplay().asyncExec(() -> {
					for (Control child : mParentComposite.getChildren()) {
						child.dispose();
					}

					createJoverflowUi(mParentComposite);
					mJOverflowUi.setModel(mModel);
				});
			}

			@Override
			public void onModelLoadFailed(final Throwable failure) {
				getSite().getShell().getDisplay().asyncExec(() -> {
					String message = failure.getLocalizedMessage();
					DialogToolkit.showException(getSite().getShell(), "Could not open " + inputPath, message, failure);
					getSite().getPage().closeEditor(JOverflowEditor.this, false);
				});
			}

			private void updateUi(final double progress) {
				getSite().getShell().getDisplay().asyncExec(() -> {
					if (mProgressIndicator == null || mProgressIndicator.isDisposed()) {
						return;
					}

					// in case of overflow
					if (progress < worked) {
						mProgressIndicator.beginTask(1);
						worked = 0;
					}

					mProgressIndicator.worked(progress - worked);
					worked = progress;
				});
			}
		});

		mBackground = EXECUTOR_SERVICE.submit(mLoader);
	}

	@Override
	public void createPartControl(Composite parent) {
		mParentComposite = parent;

		mFormToolkit = new FormToolkit(FlightRecorderUI.getDefault().getFormColors(Display.getCurrent()));
		mFormToolkit.setBorderStyle(SWT.NULL);

		createProgressIndicator(parent);
	}

	private void createProgressIndicator(Composite parent) {
		mProgressIndicator = CompositeToolkit.createWaitIndicator(mFormToolkit.createComposite(parent), mFormToolkit);
		mProgressIndicator.beginTask(1);
	}

	private void createJoverflowUi(Composite parent) {
		Form mForm = mFormToolkit.createForm(parent);
		mForm.setText("JOverflow");
		mForm.setImage(getTitleImage());
		mFormToolkit.decorateFormHeading(mForm);

		IToolBarManager manager = mForm.getToolBarManager();
		manager.add((new Action("Reset") {
			{
				setImageDescriptor(JOverflowPlugin.getDefault().getMCImageDescriptor(JOverflowPlugin.ICON_UNDO_EDIT));
			}

			@Override
			public void run() {
				mJOverflowUi.reset();
			}
		}));
		mForm.updateToolBar();

		Composite body = mForm.getBody();
		body.setLayout(new FillLayout());

		mJOverflowUi = new JOverflowUi(body, SWT.NONE);

		for (UiLoadedListener l : mUiLoadedListeners) {
			l.uiLoaded(mJOverflowUi);
		}

		// FIXME: a hack for Eclipse Photon. Remove when we don't build against Photon anymore.
		parent.layout();
	}

	@Override
	public void dispose() {
		super.dispose();

		if (mLoader != null) {
			mLoader.cancel();
		}

		if (mSnapshot != null) {
			mSnapshot.discard();
		}
	}

	@Override
	public void setFocus() {
		if (mJOverflowUi != null) {
			mJOverflowUi.setFocus();
			return;
		}

		if (mProgressIndicator != null) {
			mProgressIndicator.setFocus();
			return;
		}

		mParentComposite.setFocus();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// no op
	}

	@Override
	public void doSaveAs() {
		// no op
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	JOverflowUi getJOverflowUi() {
		return mJOverflowUi;
	}

	Snapshot getSnapshot() {
		return mSnapshot;
	}

	void addUiLoadedListener(UiLoadedListener listener) {
		mUiLoadedListeners.add(listener);
		if (mJOverflowUi != null) {
			listener.uiLoaded(mJOverflowUi);
		}
	}

	void removeUiLoadedListener(UiLoadedListener listener) {
		mUiLoadedListeners.remove(listener);
	}

	interface UiLoadedListener {
		void uiLoaded(JOverflowUi ui);
	}
}
