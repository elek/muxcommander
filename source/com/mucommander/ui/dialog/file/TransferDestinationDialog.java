/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2009 Maxence Bernard
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


package com.mucommander.ui.dialog.file;

import com.mucommander.file.util.FileSet;
import com.mucommander.file.util.PathUtils;
import com.mucommander.job.TransferFileJob;
import com.mucommander.text.Translator;
import com.mucommander.ui.dialog.DialogToolkit;
import com.mucommander.ui.layout.YBoxPanel;
import com.mucommander.ui.main.MainFrame;
import com.mucommander.ui.text.FilePathField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * This class is an abstract dialog which allows the user to specify in a text field the destination of a transfer
 * and control some options such as the default action to perform when a file already exists in the destination, or
 * if the files should be checked for integrity.
 *
 * <p>The {@link #createTransferFileJob(ProgressDialog, PathUtils.ResolvedDestination, int)} method is called to create
 * and return a {@link TransferFileJob} when the user has confirmed the operation, either by pressing the OK button or
 * by pressing the Enter key.</p>
 *
 * @author Maxence Bernard
 */
public abstract class TransferDestinationDialog extends JobDialog implements ActionListener {

    protected JTextField pathField;
    protected JComboBox fileExistsActionComboBox;
    protected JCheckBox skipErrorsCheckBox;
    protected JCheckBox verifyIntegrityCheckBox;
    protected JButton okButton;
    protected JButton cancelButton;

    protected String errorDialogTitle = Translator.get("error");
	
    // Dialog size constraints
    protected final static Dimension MINIMUM_DIALOG_DIMENSION = new Dimension(320,0);	
    // Dialog width should not exceed 360, height is not an issue (always the same)
    protected final static Dimension MAXIMUM_DIALOG_DIMENSION = new Dimension(400,10000);	

	
    private final static int DEFAULT_ACTIONS[] = {
        FileCollisionDialog.CANCEL_ACTION,
        FileCollisionDialog.SKIP_ACTION,
        FileCollisionDialog.OVERWRITE_ACTION,
        FileCollisionDialog.OVERWRITE_IF_OLDER_ACTION,
        FileCollisionDialog.RESUME_ACTION,
        FileCollisionDialog.RENAME_ACTION
    };

    private final static String DEFAULT_ACTIONS_TEXT[] = {
        FileCollisionDialog.CANCEL_TEXT,
        FileCollisionDialog.SKIP_TEXT,
        FileCollisionDialog.OVERWRITE_TEXT,
        FileCollisionDialog.OVERWRITE_IF_OLDER_TEXT,
        FileCollisionDialog.RESUME_TEXT,
        FileCollisionDialog.RENAME_TEXT
    };
	

    public TransferDestinationDialog(MainFrame mainFrame, FileSet files) {
        super(mainFrame, null, files);
    }
	
    public TransferDestinationDialog(MainFrame mainFrame, FileSet files, String title, String labelText, String okText, String errorDialogTitle) {
        this(mainFrame, files);
		
        init(title, labelText, okText, errorDialogTitle);
    }
	
	
    protected void init(String title, String labelText, String okText, String errorDialogTitle) {
        this.errorDialogTitle = errorDialogTitle;

        setTitle(title);
		
        YBoxPanel mainPanel = new YBoxPanel();
		
        JLabel label = new JLabel(labelText+" :");
        mainPanel.add(label);

        // Create a path field with auto-completion capabilities
        pathField = new FilePathField();

        pathField.addActionListener(this);
        mainPanel.add(pathField);
        mainPanel.addSpace(10);

        // Path field will receive initial focus
        setInitialFocusComponent(pathField);		

        // Combo box that allows the user to choose the default action when a file already exists in destination
        mainPanel.add(new JLabel(Translator.get("destination_dialog.file_exists_action")+" :"));
        fileExistsActionComboBox = new JComboBox();
        fileExistsActionComboBox.addItem(Translator.get("ask"));
        int nbChoices = DEFAULT_ACTIONS_TEXT.length;
        for(int i=0; i<nbChoices; i++)
            fileExistsActionComboBox.addItem(DEFAULT_ACTIONS_TEXT[i]);
        mainPanel.add(fileExistsActionComboBox);

//        mainPanel.addSpace(5);

        skipErrorsCheckBox = new JCheckBox(Translator.get("destination_dialog.skip_errors"));
        mainPanel.add(skipErrorsCheckBox);

//        mainPanel.addSpace(5);

        verifyIntegrityCheckBox = new JCheckBox(Translator.get("destination_dialog.verify_integrity"));
        mainPanel.add(verifyIntegrityCheckBox);

        mainPanel.addSpace(10);

        // Create file details button and OK/cancel buttons and lay them out a single row
        JPanel fileDetailsPanel = createFileDetailsPanel();

        okButton = new JButton(okText);
        cancelButton = new JButton(Translator.get("cancel"));

        mainPanel.add(createButtonsPanel(createFileDetailsButton(fileDetailsPanel),
                DialogToolkit.createOKCancelPanel(okButton, cancelButton, getRootPane(), this)));
        mainPanel.add(fileDetailsPanel);

        getContentPane().add(mainPanel, BorderLayout.NORTH);
		
        // Set minimum/maximum dimension
        setMinimumSize(MINIMUM_DIALOG_DIMENSION);
        setMaximumSize(MAXIMUM_DIALOG_DIMENSION);
    }


    protected void setTextField(String text) {
        pathField.setText(text);
        // Text is selected so that user can directly type and replace path
        pathField.setSelectionStart(0);
        pathField.setSelectionEnd(text.length());
    }


    protected void setTextField(String text, int selStart, int selEnd) {
        pathField.setText(text);
        // Text is selected so that user can directly type and replace path
        pathField.setSelectionStart(selStart);
        pathField.setSelectionEnd(selEnd);
    }
	
	protected boolean verifyPath(PathUtils.ResolvedDestination resolvedDest, String destPath) {
        // The path entered doesn't correspond to any existing folder
        if (resolvedDest==null || (files.size()>1 && resolvedDest.getDestinationType()!=PathUtils.ResolvedDestination.EXISTING_FOLDER)) {
            showErrorDialog(Translator.get("invalid_path", destPath), errorDialogTitle);
            return false;
        }
        return true;
	}
	
    /**
     * This method is invoked when the OK button is pressed.
     */
    private void okPressed() {
        String destPath = pathField.getText();

        // Resolves destination folder
        // TODO: move those I/O bound calls to job as they can lock the main thread
        PathUtils.ResolvedDestination resolvedDest = PathUtils.resolveDestination(destPath, mainFrame.getActiveTable().getCurrentFolder());
        if (!verifyPath(resolvedDest, destPath))
        	return;

        // Retrieve default action when a file exists in destination, default choice
        // (if not specified by the user) is 'Ask'
        int defaultFileExistsAction = fileExistsActionComboBox.getSelectedIndex();
        if(defaultFileExistsAction==0)
            defaultFileExistsAction = FileCollisionDialog.ASK_ACTION;
        else
            defaultFileExistsAction = DEFAULT_ACTIONS[defaultFileExistsAction-1];
        // Note: we don't remember default action on purpose: we want the user to specify it each time,
        // it would be too dangerous otherwise.

        ProgressDialog progressDialog = new ProgressDialog(mainFrame, getProgressDialogTitle());
        TransferFileJob job = createTransferFileJob(progressDialog, resolvedDest, defaultFileExistsAction);

        if(job!=null) {
            job.setAutoSkipErrors(skipErrorsCheckBox.isSelected());
            job.setIntegrityCheckEnabled(verifyIntegrityCheckBox.isSelected());
            progressDialog.start(job);
        }
    }


    //////////////////////
    // Abstract methods //
    //////////////////////

    protected abstract TransferFileJob createTransferFileJob(ProgressDialog progressDialog, PathUtils.ResolvedDestination resolvedDest, int defaultFileExistsAction);

    protected abstract String getProgressDialogTitle();


    ////////////////////////
    // Overridden methods //
    ////////////////////////

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        dispose();

        // OK action
        if(source == okButton || source == pathField) {
            okPressed();
        }
    }
}
