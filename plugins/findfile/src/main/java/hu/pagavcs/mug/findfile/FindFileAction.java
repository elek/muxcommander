package hu.pagavcs.mug.findfile;

import java.awt.event.KeyEvent;
import java.util.Hashtable;

import javax.swing.KeyStroke;

import com.mucommander.ui.action.AbstractActionDescriptor;
import com.mucommander.ui.action.ActionCategories;
import com.mucommander.ui.action.ActionCategory;
import com.mucommander.ui.action.ActionFactory;
import com.mucommander.ui.action.InvokesDialog;
import com.mucommander.ui.action.MuAction;
import com.mucommander.ui.main.MainFrame;

public class FindFileAction extends MuAction implements InvokesDialog {

	public FindFileAction(MainFrame mainFrame, Hashtable<String, Object> properties) {
		super(mainFrame, properties);
	}

	@Override
	public void performAction() {
		new FindFileDialog(mainFrame);

	}

	public static class Factory implements ActionFactory {

		public MuAction createAction(MainFrame mainFrame, Hashtable<String, Object> properties) {
			return new FindFileAction(mainFrame, properties);
		}
	}

	public static class Descriptor extends AbstractActionDescriptor {

		public static final String ACTION_ID = "FindFile";

		public String getId() {
			return ACTION_ID;
		}

		public ActionCategory getCategory() {
			return ActionCategories.FILES;
		}

		public KeyStroke getDefaultAltKeyStroke() {
			return null;
		}

		public KeyStroke getDefaultKeyStroke() {
			return KeyStroke.getKeyStroke(KeyEvent.VK_F7, KeyEvent.ALT_DOWN_MASK);
		}
	}
}
