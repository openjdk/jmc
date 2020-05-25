package org.openjdk.jmc.joverflow.ext.treemap;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;
import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.ui.JOverflowEditor;
import org.openjdk.jmc.joverflow.ui.JOverflowUi;
import org.openjdk.jmc.joverflow.ui.model.ModelListener;
import org.openjdk.jmc.joverflow.ui.model.ObjectCluster;

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
		MessagePage page = new MessagePage();
		page.setMessage("Joverflow editor selected");

		if (part instanceof JOverflowEditor) {
//			editor.addUiLoadedListener((ui) -> ui.addModelListener(new ModelListener() {
//				private int mTotalInstancesCount = 0;
//				
//				@Override
//				public void include(ObjectCluster cluster, RefChainElement referenceChain) {
//					// TODO Auto-generated method stub
//					mTotalInstancesCount += cluster.getObjectCount();
//				}
//
//				@Override
//				public void allIncluded() {
//					// TODO Auto-generated method stub
//					page.setMessage(String.format("%d objects added", mTotalInstancesCount));
//					
//					mTotalInstancesCount = 0;
//				}
//			}));

			initPage(page);
			page.createControl(getPageBook());
			return new PageRec(part, page);
		}
		return null;
	}

	@Override
	protected void doDestroyPage(IWorkbenchPart part, PageRec pageRecord) {
		if (part instanceof JOverflowEditor) {
//			final JOverflowUi ui = ((JOverflowEditor) part).getJOverflowUi();
//			if (ui != null) {
//				// TODO: remove listener
//			}
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
