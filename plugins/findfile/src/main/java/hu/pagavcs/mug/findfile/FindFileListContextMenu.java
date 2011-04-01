package hu.pagavcs.mug.findfile;


import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JSeparator;

import com.mucommander.extensions.ContextMenuExtension;
import com.mucommander.extensions.ContextMenuExtensionPositions;
import com.mucommander.file.AbstractFile;
import com.mucommander.file.util.FileSet;
import com.mucommander.ui.main.MainFrame;
import com.mucommander.ui.main.menu.TablePopupMenu;

public class FindFileListContextMenu implements ContextMenuExtension {

	public int getPriority() {
		return 0;
	}

	public ContextMenuExtensionPositions getPosition() {
		return ContextMenuExtensionPositions.BOTTOM;
	}

	public void addMenu(TablePopupMenu tablePopupMenu, MainFrame mainFrame, AbstractFile currentFolder, AbstractFile clickedFile, boolean parentFolderClicked,
	        FileSet markedFiles) {
		if (clickedFile != null) {
			AbstractFile realFile = FindManager.getRealFile(clickedFile);
			if (!clickedFile.getAbsolutePath().equals(realFile.getAbsolutePath())) {
				tablePopupMenu.add(new JSeparator());
				tablePopupMenu.add(new JumpToRealFileAction(mainFrame, realFile));
			}
		}
	}

	private static class JumpToRealFileAction extends AbstractAction {

		private final AbstractFile realFile;
		private final MainFrame    mainFrame;

		public JumpToRealFileAction(MainFrame mainFrame, AbstractFile realFile) {
			super("Jump to real file");
			this.mainFrame = mainFrame;
			this.realFile = realFile;
		}

		public void actionPerformed(ActionEvent e) {
			try {
				mainFrame.getActivePanel().tryChangeCurrentFolder(realFile.getParent(), realFile, true);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

	}

}
