package hu.pagavcs.mug;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;

public class MugHelper {

	private static Preferences prefs = Preferences.userNodeForPackage(MugHelper.class);

	public static final String KEY_FIND_NAME_HISTORY = "find-name-history";

	public static List<String> loadList(String listName) {
		try {
		Map<String, String> result = new HashMap<String, String>();
		List<String> resultList = new ArrayList<String>();
		Preferences node = prefs.node(listName);
		for (String key : node.keys()) {
			result.put(key, node.get(key, null));
		}
		Object[] keys = result.keySet().toArray();
		Arrays.sort(keys);
		for (Object key : keys) {
			resultList.add(result.get(key));
		}
		return resultList;
		} catch (BackingStoreException ex) {
			// TODO log
			return Collections.EMPTY_LIST;
		}
	}

	public static void storeList(String mapName, List<String> data) {
		try {
		Preferences node = prefs.node(mapName);
		node.clear();
		int index = 0;
		for (String li : data) {
			node.put(String.format("%05d", index), li);
			index++;
		}
		} catch (BackingStoreException ex) {
			// TODO log
		}
	}

	public static void addPopupMenu(JTextComponent component) {
		JPopupMenu contextMenu = new JPopupMenu();
		contextMenu.add(new CutAction(component));
		contextMenu.add(new CopyAction(component));
		contextMenu.add(new PasteAction(component));
		addPopupMenu(component, contextMenu);
	}

	public static void addPopupMenu(JComponent component, final JPopupMenu contextMenu) {

		component.addMouseListener(new MouseAdapter() {

			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}

			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}

			private void maybeShowPopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					contextMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
	}

	private static class CopyAction extends AbstractAction {

		private final JTextComponent target;

		public CopyAction(JTextComponent target) {
			super("Copy");
			this.target = target;
		}

		public void actionPerformed(ActionEvent e) {
			boolean deselectAfterCopy = false;
			if ((target.getSelectedText() == null)) {
				target.selectAll();
				deselectAfterCopy = true;
			}
			target.copy();
			if (deselectAfterCopy) {
				target.select(0, 0);
			}
		}
	}

	private static class CutAction extends AbstractAction {

		private final JTextComponent target;

		public CutAction(JTextComponent target) {
			super("Cut");
			this.target = target;
		}

		public void actionPerformed(ActionEvent e) {
			boolean deselectAfterCopy = false;
			if (target.getSelectedText() == null) {
				target.selectAll();
				deselectAfterCopy = true;
			}
			if (target.isEditable()) {
				target.cut();
			} else {
				target.copy();
			}

			if (deselectAfterCopy) {
				target.select(0, 0);
			}
		}
	}

	private static class PasteAction extends AbstractAction {

		private final JTextComponent target;

		public PasteAction(JTextComponent target) {
			super("Paste");
			this.target = target;
		}

		public void actionPerformed(ActionEvent e) {
			target.paste();
		}
	}
}
