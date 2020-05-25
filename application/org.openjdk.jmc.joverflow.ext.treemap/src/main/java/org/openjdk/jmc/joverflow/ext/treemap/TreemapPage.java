package org.openjdk.jmc.joverflow.ext.treemap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.Page;
import org.openjdk.jmc.joverflow.ext.treemap.swt.Treemap;
import org.openjdk.jmc.joverflow.ext.treemap.swt.TreemapItem;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.ui.JOverflowEditor;
import org.openjdk.jmc.joverflow.ui.model.ModelListener;
import org.openjdk.jmc.joverflow.ui.model.ObjectCluster;

import java.util.HashMap;
import java.util.Map;

public class TreemapPage extends Page implements ModelListener {
	public static final Color[] COLORS = {new Color(Display.getCurrent(), 250, 206, 210), // red
			new Color(Display.getCurrent(), 185, 214, 255), // blue
			new Color(Display.getCurrent(), 229, 229, 229), // grey
			new Color(Display.getCurrent(), 255, 231, 199), // orange
			new Color(Display.getCurrent(), 171, 235, 238), // aqua
			new Color(Display.getCurrent(), 228, 209, 252), // purple
			new Color(Display.getCurrent(), 255, 255, 255), // white
			new Color(Display.getCurrent(), 205, 249, 212), // green
	};
	
	private final JOverflowEditor editor;

	private Treemap treemap;

	private HashMap<String, Double> classes = new HashMap<>();

	TreemapPage(JOverflowEditor editor) {
		this.editor = editor;
	}

	@Override
	public void createControl(Composite parent) {
		treemap = new Treemap(parent, SWT.NONE);
		treemap.setText("[ROOT]");
		updateInput();
	}

	@Override
	public Control getControl() {
		return treemap;
	}

	@Override
	public void setFocus() {
		treemap.setFocus();
	}

	@Override
	public void include(
			ObjectCluster cluster, RefChainElement referenceChain) {
		if (cluster.getObjectCount() == 0) {
			return;
		}

		JavaClass clazz = getObjectAtPosition(cluster.getGlobalObjectIndex(0)).getClazz();
		String className = clazz.getName();
		classes.putIfAbsent(className, 0.0);
		double size = classes.get(className);
		size += cluster.getMemory();
		classes.put(className, size);
	}

	@Override
	public void allIncluded() {
		updateInput();
		classes.clear();
	}

	private void updateInput() {
		if (treemap == null) {
			return;
		}

		treemap.removeAll();
		if (treemap.getItemCount() > 0) {
			throw new RuntimeException("wtf?");
		}
		HashMap<String, TreemapItem> items = new HashMap<>();
		for (Map.Entry<String, Double> entry : classes.entrySet()) {
			addTreemapItem(treemap, items, entry.getKey(), entry.getValue());
		}
		
		TreemapItem rootItem = treemap.getRootItem();
		rootItem.setMessage("[ROOT]");
		setColorAndMessage(rootItem, 0);
		treemap.setTopItem(rootItem);
		treemap.setSelection(null);
		treemap.redraw();
	}

	private void addTreemapItem(Treemap parent, Map<String, TreemapItem> items, String fullName, double size) {
		if (items.containsKey(fullName) && size != 0) {
			TreemapItem item = items.get(fullName);
			double bytes = item.getWeight() + size;
			item.setWeight(bytes);
			item.setMessage(fullName);
			return;
		}

		if (fullName.indexOf('.') == -1) {
			TreemapItem item = new TreemapItem(parent, SWT.NONE);
			item.setText(fullName);
			if (size != 0) {
				item.setWeight(size);
			}
			item.setMessage(fullName);
			items.put(fullName, item);
			return;
		}

		String parentName = fullName.substring(0, fullName.lastIndexOf('.'));
		if (!items.containsKey(parentName)) {
			addTreemapItem(parent, items, parentName, 0);
		}

		TreemapItem parentItem = items.get(parentName);
		TreemapItem item = new TreemapItem(parentItem, SWT.NONE);
		item.setText(fullName.substring(parentName.length() + 1));
		item.setMessage(fullName);
		if (size != 0) {
			item.setWeight(size);
		}
		items.put(fullName, item);
	}

	private void setColorAndMessage(TreemapItem item, int depth) {
		item.setMessage(item.getMessage() + "\n" + getHumanReadableSize(item.getWeight()));
		item.setBackground(COLORS[depth % COLORS.length]);

		for (TreemapItem child : item.getItems()) {
			setColorAndMessage(child, depth + 1);
		}
	}
	
	private String getHumanReadableSize(double bytes) {
		String unit = "B";
		double quantity = bytes;
		if (quantity > 1024) {
			quantity /= 1024;
			unit = "KiB";
		}
		if (quantity > 1024) {
			quantity /= 1024;
			unit = "MiB";
		}
		if (quantity > 1024) {
			quantity /= 1024;
			unit = "GiB";
		}
		if (quantity > 1024) {
			quantity /= 1024;
			unit = "TiB";
		}

		return String.format("%.2f %s", quantity, unit);
	}

	private JavaHeapObject getObjectAtPosition(int globalObjectPos) {
		return editor.getSnapshot().getObjectAtGlobalIndex(globalObjectPos);
	}
}
