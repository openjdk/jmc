/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
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
import org.openjdk.jmc.joverflow.dominator.DominatorTree;
import org.openjdk.jmc.joverflow.dominator.DominatorTreeBuilder;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;
import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;
import org.openjdk.jmc.joverflow.ui.model.ModelLoader;
import org.openjdk.jmc.joverflow.ui.model.ModelLoaderListener;
import org.openjdk.jmc.joverflow.ui.model.ReferenceChain;
import org.openjdk.jmc.ui.misc.CompositeToolkit;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

public class JOverflowEditor extends EditorPart {
	private static final int MIN_READ_BUFFER_SIZE = 16 * 1024 * 1024;
	private static final int MAX_READ_BUFFER_SIZE = 256 * 1024 * 1024;
	private final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

	private FormToolkit mFormToolkit;

	private Composite mParentComposite;
	private ProgressIndicator mProgressIndicator;
	private JOverflowUi mJOverflowUi;
	private DominatorTreePage mDominatorPage;
	private Composite mModeContainer;
	private StackLayout mModeLayout;
	private Action mResetAction;
	private Action mSpaceHuntingAction;
	private Action mDominatorTreeAction;
	private boolean mDominatorMode;
	private volatile boolean mDisposed;

	private ModelLoader mLoader;
	private String mInputPath;
	private Snapshot mSnapshot;
	private Collection<ReferenceChain> mModel;
	private DominatorTree mDominatorTree;
	private DominatorTreeBuilder mDominatorTreeBuilder;
	private Job mDominatorJob;
	private Future<?> mBackground;

	private final List<UiLoadedListener> mUiLoadedListeners = new ArrayList<>();
	private final List<Runnable> mDominatorTreeListeners = new ArrayList<>();

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
		cancelDominatorTreeCalculation();
		mDominatorTreeBuilder = null;
		mDominatorJob = null;
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
		mDominatorTree = null;

		setPartName(input.getName());

		String inputPath = input.getPath().toOSString();
		mInputPath = inputPath;
		mLoader = new ModelLoader(inputPath, new ModelLoaderListener() {
			private double worked = 0; // the amount of work already done

			@Override
			public void onProgressUpdate(final double progress) {
				updateUi(progress);
			}

			@Override
			public void onModelLoaded(Snapshot snapshot, final Collection<ReferenceChain> model) {
				if (mDisposed) {
					snapshot.discard();
					return;
				}
				mSnapshot = snapshot;
				mModel = model;

				updateUi(1);
				getSite().getShell().getDisplay().asyncExec(() -> {
					if (mDisposed || mParentComposite == null || mParentComposite.isDisposed()) {
						snapshot.discard();
						return;
					}
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
					if (mDisposed) {
						return;
					}
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
		mResetAction = new Action(Messages.JOverflowEditor_RESET) {
			{
				setImageDescriptor(JOverflowPlugin.getDefault().getMCImageDescriptor(JOverflowPlugin.ICON_UNDO_EDIT));
				setToolTipText(Messages.JOverflowEditor_RESET_TOOLTIP);
			}

			@Override
			public void run() {
				mJOverflowUi.reset();
			}
		};
		manager.add(mResetAction);
		manager.add(new Separator());
		mSpaceHuntingAction = createModeAction(Messages.JOverflowEditor_MODE_SPACE_HUNTING,
				Messages.JOverflowEditor_MODE_SPACE_HUNTING_TOOLTIP, "joverflow.png", false); //$NON-NLS-1$
		mDominatorTreeAction = createModeAction(Messages.JOverflowEditor_MODE_DOMINATOR_TREE,
				Messages.JOverflowEditor_MODE_DOMINATOR_TREE_TOOLTIP, "dominator_tree.png", true); //$NON-NLS-1$
		for (Action action : new Action[] {mSpaceHuntingAction, mDominatorTreeAction}) {
			ActionContributionItem item = new ActionContributionItem(action);
			item.setMode(ActionContributionItem.MODE_FORCE_TEXT);
			manager.add(item);
		}
		mForm.updateToolBar();

		mModeContainer = mForm.getBody();
		mModeLayout = new StackLayout();
		mModeContainer.setLayout(mModeLayout);
		mJOverflowUi = new JOverflowUi(mModeContainer, SWT.NONE);
		mDominatorPage = new DominatorTreePage(this);
		mDominatorPage.createControl(mModeContainer);
		showMode(mDominatorMode);

		for (UiLoadedListener l : mUiLoadedListeners) {
			l.uiLoaded(mJOverflowUi);
		}

		// FIXME: a hack for Eclipse Photon. Remove when we don't build against Photon anymore.
		parent.layout();
	}

	private Action createModeAction(String text, String toolTip, String icon, boolean dominatorMode) {
		Action action = new Action(text, IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				if (isChecked()) {
					showMode(dominatorMode);
				}
			}
		};
		action.setToolTipText(toolTip);
		action.setImageDescriptor(JOverflowPlugin.getDefault().getMCImageDescriptor(icon));
		return action;
	}

	private void showMode(boolean dominatorMode) {
		mDominatorMode = dominatorMode;
		mModeLayout.topControl = dominatorMode ? mDominatorPage.getControl() : mJOverflowUi;
		mSpaceHuntingAction.setChecked(!dominatorMode);
		mDominatorTreeAction.setChecked(dominatorMode);
		mResetAction.setEnabled(!dominatorMode);
		mModeContainer.layout();
	}

	@Override
	public void dispose() {
		mDisposed = true;
		super.dispose();
		cancelDominatorTreeCalculation();
		mDominatorTreeBuilder = null;
		mDominatorJob = null;
		if (mDominatorPage != null) {
			mDominatorPage.dispose();
		}
		mDominatorTreeListeners.clear();

		if (mLoader != null) {
			mLoader.cancel();
		}

		if (mSnapshot != null) {
			mSnapshot.discard();
		}
		mDominatorTree = null;
	}

	@Override
	public void setFocus() {
		if (mDominatorMode && mDominatorPage != null) {
			mDominatorPage.setFocus();
			return;
		}
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

	String getInputPath() {
		return mInputPath;
	}

	/**
	 * Returns the cached tree, or null before calculation.
	 */
	DominatorTree getDominatorTree() {
		return mDominatorTree;
	}

	DominatorTreeBuilder getDominatorTreeBuilder() {
		return mDominatorTreeBuilder;
	}

	void addDominatorTreeListener(Runnable listener) {
		mDominatorTreeListeners.add(listener);
		listener.run();
	}

	void removeDominatorTreeListener(Runnable listener) {
		mDominatorTreeListeners.remove(listener);
	}

	void startDominatorTreeCalculation() {
		if (mDisposed || mSnapshot == null || mInputPath == null || mDominatorTree != null
				|| mDominatorTreeBuilder != null) {
			return;
		}
		ReadBuffer.Factory factory = new ReadBuffer.CachedReadBufferFactory(mInputPath, calculateReadBufferSize());
		DominatorTreeBuilder builder = new DominatorTreeBuilder(mSnapshot, factory);
		mDominatorTreeBuilder = builder;
		notifyDominatorTreeListeners();
		Job job = new Job(NLS.bind(Messages.DominatorTreePage_JOB_NAME, getPartName())) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					DominatorTree tree = builder.build();
					DisplayToolkit.safeAsyncExec(mParentComposite,
							() -> finishDominatorTreeCalculation(builder, tree, null));
					return Status.OK_STATUS;
				} catch (HprofParsingCancelledException e) {
					return Status.CANCEL_STATUS;
				} catch (IOException | RuntimeException | OutOfMemoryError e) {
					DisplayToolkit.safeAsyncExec(mParentComposite,
							() -> finishDominatorTreeCalculation(builder, null, e));
					return Status.OK_STATUS;
				}
			}

			@Override
			protected void canceling() {
				builder.cancel();
			}
		};
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getResult().matches(IStatus.CANCEL)) {
					DisplayToolkit.safeAsyncExec(mParentComposite,
							() -> finishDominatorTreeCalculation(builder, null, null));
				}
			}
		});
		job.setUser(false);
		job.setSystem(false);
		mDominatorJob = job;
		job.schedule();
	}

	void cancelDominatorTreeCalculation() {
		if (mDominatorTreeBuilder != null) {
			mDominatorTreeBuilder.cancel();
		}
		if (mDominatorJob != null) {
			mDominatorJob.cancel();
		}
	}

	private void finishDominatorTreeCalculation(DominatorTreeBuilder builder, DominatorTree tree, Throwable failure) {
		if (mDisposed || builder != mDominatorTreeBuilder) {
			return;
		}
		mDominatorTreeBuilder = null;
		mDominatorJob = null;
		if (tree != null && tree.getSnapshot() == mSnapshot) {
			mDominatorTree = tree;
		}
		notifyDominatorTreeListeners();
		if (failure instanceof OutOfMemoryError) {
			DialogToolkit.showWarning(getSite().getShell(), Messages.DominatorTreePage_FAILED_TITLE,
					Messages.DominatorTreePage_OUT_OF_MEMORY);
		} else if (failure != null) {
			DialogToolkit.showException(getSite().getShell(), Messages.DominatorTreePage_FAILED_TITLE,
					failure.getLocalizedMessage(), failure);
		}
	}

	private void notifyDominatorTreeListeners() {
		for (Runnable listener : new ArrayList<>(mDominatorTreeListeners)) {
			listener.run();
		}
	}

	private static int calculateReadBufferSize() {
		Runtime runtime = Runtime.getRuntime();
		long available = runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory();
		return (int) Math.max(MIN_READ_BUFFER_SIZE, Math.min(MAX_READ_BUFFER_SIZE, available / 5));
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
