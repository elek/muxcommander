
package com.mucommander.ui.viewer;

import com.mucommander.file.AbstractFile;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import java.io.*;

import com.mucommander.ui.comp.MnemonicHelper;
import com.mucommander.ui.comp.FocusRequester;
import com.mucommander.ui.comp.menu.MenuToolkit;

import com.mucommander.conf.ConfigurationManager;

import com.mucommander.text.Translator;


//public class TextViewer extends FileViewer implements ActionListener, WindowListener {
public class TextViewer extends FileViewer implements ActionListener {

//	private String encoding;

	private JMenuItem copyItem;
	private JMenuItem selectAllItem;
	
	private JTextArea textArea;
	
	
	public TextViewer(ViewerFrame frame) {
		super(frame);
//		this.encoding = ConfigurationManager.getVariable("prefs.text_viewer.last_encoding", "utf-8");

		setLayout(new BorderLayout());

		// Create default menu
		MnemonicHelper menuMnemonicHelper = new MnemonicHelper();
		MnemonicHelper menuItemMnemonicHelper = new MnemonicHelper();

		// Edit menu
		JMenuBar menuBar = frame.getJMenuBar();
		JMenu editMenu = MenuToolkit.addMenu(Translator.get("text_viewer.edit"), menuMnemonicHelper, null);
		copyItem = MenuToolkit.addMenuItem(editMenu, Translator.get("text_viewer.copy"), menuItemMnemonicHelper, null, this);
		selectAllItem = MenuToolkit.addMenuItem(editMenu, Translator.get("text_viewer.select_all"), menuItemMnemonicHelper, null, this);
		menuBar.add(editMenu);
		
		textArea = new JTextArea();
		textArea.setEditable(false);

		add(new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

		// Request focus on text area when visible
		FocusRequester.requestFocus(textArea);
//		frame.addWindowListener(this);
	}

	
	public void startViewing(AbstractFile fileToView, boolean isSeparateWindow) throws IOException {
		textArea.read(new InputStreamReader(fileToView.getInputStream()), null);
		textArea.setCaretPosition(0);
//		pack();
	}


	public Dimension getPreferredSize() {
//		return new Dimension(Math.min(480, image.getWidth(null)), Math.min(360,image.getHeight(null)));
		Dimension d = super.getPreferredSize();
System.out.println("TextViewer.getPreferrredSize()="+d);		
System.out.println("TextViewer.getPreferrredSize, textArea.getPrefferredSize()="+textArea.getPreferredSize());		
		return d;
	}

	
	public static boolean canViewFile(AbstractFile file) {
		String name = file.getName();
		String nameLowerCase = name.toLowerCase();
		return nameLowerCase.endsWith(".txt")
			||nameLowerCase.endsWith(".conf")
			||nameLowerCase.endsWith(".xml")
			||nameLowerCase.endsWith(".ini")
			||nameLowerCase.endsWith(".nfo")
			||nameLowerCase.endsWith(".diz")
			||nameLowerCase.endsWith(".sh")
			||nameLowerCase.equals("readme")
			||nameLowerCase.equals("read.me")
			||nameLowerCase.equals("license");
	}

	public long getMaxRecommendedSize() {
		return 131072;
	}


	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		
		if(source == copyItem)
			textArea.copy();
		else if(source == selectAllItem)
			textArea.selectAll();
	}


    /**************************
     * WindowListener methods *
     **************************/	
/*
    public void windowClosing(WindowEvent e) {
	}

    public void windowActivated(WindowEvent e) {
		textArea.requestFocus();
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }
*/
}