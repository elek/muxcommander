/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2010 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mucommander.ui.viewer.text;

import com.mucommander.AppLogger;
import com.mucommander.commons.file.AbstractFile;
import com.mucommander.commons.file.FileOperation;
import com.mucommander.commons.io.bom.BOM;
import com.mucommander.commons.io.bom.BOMInputStream;
import com.mucommander.commons.io.bom.BOMWriter;
import com.mucommander.conf.MuConfiguration;
import com.mucommander.text.Translator;
import com.mucommander.ui.dialog.DialogOwner;
import com.mucommander.ui.dialog.InformationDialog;
import com.mucommander.ui.encoding.EncodingListener;
import com.mucommander.ui.encoding.EncodingMenu;
import com.mucommander.ui.helper.MenuToolkit;
import com.mucommander.ui.helper.MnemonicHelper;
import com.mucommander.ui.viewer.FileEditor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.io.*;


/**
 * A simple text editor.
 *
 * @author Maxence Bernard, Nicolas Rinaudo, Arik Hadas
 */
class TextEditor extends FileEditor implements DocumentListener, EncodingListener {

    /** Menu bar */
    // Menus //
    private JMenu editMenu;
    private JMenu viewMenu;
    // Items //
    private JMenuItem copyItem;
    private JMenuItem cutItem;
    private JMenuItem pasteItem;
    private JMenuItem selectAllItem;
    private JMenuItem findItem;
    private JMenuItem findNextItem;
    private JMenuItem findPreviousItem;
    private JMenuItem toggleWordWrapItem;
    private JMenuItem toggleLineNumbersItem;

    private BOM bom;
    
    private TextEditorImpl textEditorImpl;
    private TextViewer textViewerDelegate;
    
    public TextEditor() {
    	textViewerDelegate = new TextViewer(textEditorImpl = new TextEditorImpl(true)) {
    		
    		@Override
    		protected void setComponentToPresent(JComponent component) {
    			TextEditor.this.setComponentToPresent(component);
    		}
    		
    		@Override
    		protected void showLineNumbers(boolean show) {
    			TextEditor.this.setRowHeaderView(show ? new TextLineNumbersPanel(textEditorImpl.getTextArea()) : null);
    	    }
    		
    		@Override
    		protected void initMenuBarItems() {
    			// Edit menu
    	        editMenu = new JMenu(Translator.get("text_editor.edit"));
    	        MnemonicHelper menuItemMnemonicHelper = new MnemonicHelper();

    	        copyItem = MenuToolkit.addMenuItem(editMenu, Translator.get("text_editor.copy"), menuItemMnemonicHelper, null, TextEditor.this);

    	        cutItem = MenuToolkit.addMenuItem(editMenu, Translator.get("text_editor.cut"), menuItemMnemonicHelper, null, TextEditor.this);
    	        pasteItem = MenuToolkit.addMenuItem(editMenu, Translator.get("text_editor.paste"), menuItemMnemonicHelper, null, TextEditor.this);

    	        selectAllItem = MenuToolkit.addMenuItem(editMenu, Translator.get("text_editor.select_all"), menuItemMnemonicHelper, null, TextEditor.this);
    	        editMenu.addSeparator();

    	        findItem = MenuToolkit.addMenuItem(editMenu, Translator.get("text_editor.find"), menuItemMnemonicHelper, KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), TextEditor.this);
    	        findNextItem = MenuToolkit.addMenuItem(editMenu, Translator.get("text_editor.find_next"), menuItemMnemonicHelper, KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), TextEditor.this);
    	        findPreviousItem = MenuToolkit.addMenuItem(editMenu, Translator.get("text_editor.find_previous"), menuItemMnemonicHelper, KeyStroke.getKeyStroke(KeyEvent.VK_F3, KeyEvent.SHIFT_DOWN_MASK), TextEditor.this);
    	        
    	        viewMenu = new JMenu(Translator.get("text_editor.view"));
    	        
    	        toggleWordWrapItem = MenuToolkit.addCheckBoxMenuItem(viewMenu, Translator.get("text_editor.word_wrap"), menuItemMnemonicHelper, null, TextEditor.this);
    	        toggleWordWrapItem.setSelected(textEditorImpl.isWrap());
    	        toggleLineNumbersItem = MenuToolkit.addCheckBoxMenuItem(viewMenu, Translator.get("text_editor.line_numbers"), menuItemMnemonicHelper, null, TextEditor.this);
    	        toggleLineNumbersItem.setSelected(TextEditor.this.getRowHeader().getView() != null);
    		}
    	};
    	
    	setComponentToPresent(textEditorImpl.getTextArea());
    }
    
    protected void setComponentToPresent(JComponent component) {
		getViewport().add(component);
	}
    
    void loadDocument(InputStream in, String encoding, DocumentListener documentListener) throws IOException {
    	textViewerDelegate.loadDocument(in, encoding, documentListener);
    	
    	if(encoding.toLowerCase().startsWith("utf")) {
    		bom = ((BOMInputStream)in).getBOM();
    	}
    }
    
    private void write(OutputStream out) throws IOException {
        Writer writer;

        // If there was a BOM originally, preserve it when writing the file.
        if(bom==null)
            writer = new OutputStreamWriter(out, textViewerDelegate.getEncoding());
        else
            writer = new BOMWriter(out, bom);

        textEditorImpl.write(writer);
    }

    @Override
    public JMenuBar getMenuBar() {
    	JMenuBar menuBar = super.getMenuBar();

    	// Encoding menu
         EncodingMenu encodingMenu = new EncodingMenu(new DialogOwner(getFrame()), textViewerDelegate.getEncoding());
         encodingMenu.addEncodingListener(this);

         menuBar.add(editMenu);
         menuBar.add(viewMenu);
         menuBar.add(encodingMenu);
         
    	return menuBar;
    }
    
    @Override
    public void beforeCloseHook() {
    	MuConfiguration.setVariable(MuConfiguration.WORD_WRAP, textEditorImpl.isWrap());
    	MuConfiguration.setVariable(MuConfiguration.LINE_NUMBERS, getRowHeader().getView() != null);
    }

    ///////////////////////////////
    // FileEditor implementation //
    ///////////////////////////////

    @Override
    protected void saveAs(AbstractFile destFile) throws IOException {
        OutputStream out;

        out = null;

        try {
            out = destFile.getOutputStream();
            write(out);

            setSaveNeeded(false);

            // Change the parent folder's date to now, so that changes are picked up by folder auto-refresh (see ticket #258)
            if(destFile.isFileOperationSupported(FileOperation.CHANGE_DATE)) {
                try {
                    destFile.getParent().changeDate(System.currentTimeMillis());
                }
                catch (IOException e) {
                    AppLogger.fine("failed to change the date of "+destFile, e);
                    // Fail silently
                }
            }
        }
        finally {
            if(out != null) {
                try {out.close();}
                catch(IOException e) {
                    // Ignored
                }
            }
        }
    }

    @Override
    public void show(AbstractFile file) throws IOException {
    	textViewerDelegate.startEditing(file, this);
    }
    
    /////////////////////////////////////
    // DocumentListener implementation //
    /////////////////////////////////////
	
    public void changedUpdate(DocumentEvent e) {
        setSaveNeeded(true);
    }
	
    public void insertUpdate(DocumentEvent e) {
        setSaveNeeded(true);
    }

    public void removeUpdate(DocumentEvent e) {
        setSaveNeeded(true);
    }
    
    ///////////////////////////////////
    // ActionListener implementation //
    ///////////////////////////////////

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if(source == copyItem)
        	textEditorImpl.copy();
        else if(source == cutItem)
        	textEditorImpl.cut();
        else if(source == pasteItem)
        	textEditorImpl.paste();
        else if(source == selectAllItem)
        	textEditorImpl.selectAll();
        else if(source == findItem)
        	textEditorImpl.find();
        else if(source == findNextItem)
        	textEditorImpl.findNext();
        else if(source == findPreviousItem)
        	textEditorImpl.findPrevious();
        else if(source == toggleWordWrapItem)
        	textEditorImpl.wrap(toggleWordWrapItem.isSelected());
        else if(source == toggleLineNumbersItem)
        	setRowHeaderView(toggleLineNumbersItem.isSelected() ? new TextLineNumbersPanel(textEditorImpl.getTextArea()) : null);
        else
        	super.actionPerformed(e);
    }
    
    /////////////////////////////////////
    // EncodingListener implementation //
    /////////////////////////////////////

    public void encodingChanged(Object source, String oldEncoding, String newEncoding) {
    	if(!askSave())
    		return;         // Abort if the file could not be saved

    	try {
    		// Reload the file using the new encoding
    		// Note: loadDocument closes the InputStream
    		loadDocument(getCurrentFile().getInputStream(), newEncoding, null);
    	}
    	catch(IOException ex) {
    		InformationDialog.showErrorDialog(getFrame(), Translator.get("read_error"), Translator.get("file_editor.cannot_read_file", getCurrentFile().getName()));
    	}
    }
}
