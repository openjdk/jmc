package org.openjdk.jmc.joverflow.ext.treemap;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openjdk.jmc.ui.CoreImages;

class TreemapAction extends Action {
	private final TreemapActionType actionType;
	private Runnable runnable;
	
	TreemapAction(TreemapActionType actionType) {
		super(actionType.message, actionType.action);
		this.actionType = actionType;
		setToolTipText(actionType.message);
		setImageDescriptor(actionType.imageDescriptor);
	}

	@Override
	public void run() {
		runnable.run();
	}

	public void setRunnable(Runnable callback) {
		runnable = callback;
	}

	public TreemapActionType getType() {
		return actionType;
	}

	enum TreemapActionType {
		ZOOM_IN("Zoom-in to the selected node", IAction.AS_PUSH_BUTTON, CoreImages.ZOOM_IN),
		ZOOM_OUT("Zoom-out to the parent node", IAction.AS_PUSH_BUTTON, CoreImages.ZOOM_OUT),
		ZOOM_OFF("Display the root node", IAction.AS_PUSH_BUTTON, CoreImages.ZOOM_OFF);

		private final String message;
		private final int action;
		private final ImageDescriptor imageDescriptor;

		TreemapActionType(String message, int action, ImageDescriptor imageDescriptor) {
			this.message = message;
			this.action = action;
			this.imageDescriptor = imageDescriptor;
		}
	}
}
