package org.openjdk.jmc.joverflow.ext.treemap;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;
import org.openjdk.jmc.joverflow.ui.JOverflowEditor;
import org.openjdk.jmc.joverflow.ui.JOverflowUi;

import java.util.stream.Stream;

public class TreemapPageBookView extends PageBookView {

	private TreemapAction[] treemapActions;

	@Override
	protected IPage createDefaultPage(PageBook book) {
		MessagePage page = new MessagePage();
		initPage(page);
		page.createControl(book);
		page.setMessage("No JOverflow editor selected");
		return page;
	}

	@Override
	protected PageRec doCreatePage(IWorkbenchPart part) {
		if (!(part instanceof JOverflowEditor)) {
			return null;
		}

		final JOverflowEditor editor = ((JOverflowEditor) part);
		TreemapPage page = new TreemapPage(editor, treemapActions);

		editor.addUiLoadedListener((ui) -> ui.addModelListener(page));

		initPage(page);
		page.createControl(getPageBook());
		return new PageRec(part, page);
	}

	@Override
	protected void doDestroyPage(IWorkbenchPart part, PageRec pageRecord) {
		if (part instanceof JOverflowEditor) {
			final JOverflowUi ui = ((JOverflowEditor) part).getJOverflowUi();
			if (ui != null) {
				ui.removeModelListener((TreemapPage) pageRecord.page);
			}
		}

		pageRecord.page.dispose();
		pageRecord.dispose();

	}

	@Override
	protected IWorkbenchPart getBootstrapPart() {
		IWorkbenchPage page = getSite().getPage();
		if (page != null) {
			return page.getActiveEditor();
		}
		return null;
	}

	@Override
	protected boolean isImportant(IWorkbenchPart part) {
		// We only care about JOverflowEditor
		return (part instanceof JOverflowEditor);
	}

	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);

		treemapActions = new TreemapAction[] {new TreemapAction(TreemapAction.TreemapActionType.ZOOM_IN), //
				new TreemapAction(TreemapAction.TreemapActionType.ZOOM_OUT), //
				new TreemapAction(TreemapAction.TreemapActionType.ZOOM_OFF), //
		};
		Stream.of(treemapActions).forEach((action) -> action.setEnabled(false));

		IToolBarManager toolBar = site.getActionBars().getToolBarManager();
		Stream.of(treemapActions).forEach(toolBar::add);
	}

	@Override
	protected void showPageRec(PageRec pageRec) {
		super.showPageRec(pageRec);

		if (pageRec.page instanceof TreemapPage) {
			((TreemapPage) pageRec.page).bindTreemapActions();
		} else {
			Stream.of(treemapActions).forEach((action) -> action.setEnabled(false));
		}
	}
}
