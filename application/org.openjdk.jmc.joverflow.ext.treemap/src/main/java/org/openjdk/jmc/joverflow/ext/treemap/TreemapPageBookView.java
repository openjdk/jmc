package org.openjdk.jmc.joverflow.ext.treemap;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;
import org.openjdk.jmc.joverflow.ui.JOverflowEditor;
import org.openjdk.jmc.joverflow.ui.JOverflowUi;

public class TreemapPageBookView extends PageBookView {

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
		TreemapPage page = new TreemapPage(editor);

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
}
