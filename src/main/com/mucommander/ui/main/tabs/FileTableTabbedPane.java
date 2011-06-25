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

package com.mucommander.ui.main.tabs;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;

import com.mucommander.commons.runtime.JavaVersions;
import com.mucommander.desktop.DesktopManager;
import com.mucommander.ui.main.FolderPanel;
import com.mucommander.ui.main.MainFrame;
import com.mucommander.ui.tabs.TabbedPane;

/**
 * TabbedPane that present the FileTable tabs.
 * 
 * This TabbedPane doesn't contain different FileTable for each tab, instead
 * it use one FileTable instance as a shared object for all tabs. when switching between
 * tabs, the FileTable instance is updated as needed according to the selected tab state.
 * 
 * @author Arik Hadas
 */
public class FileTableTabbedPane extends TabbedPane<FileTableTab> {

	/** The FileTable instance presented in each tab */
	private JComponent fileTableComponent;
	
	private MainFrame mainFrame;
	private FolderPanel folderPanel;
	
	public FileTableTabbedPane(MainFrame mainFrame, FolderPanel folderPanel, JComponent fileTableComponent) {
		this.fileTableComponent = fileTableComponent;
		this.mainFrame = mainFrame;
		this.folderPanel = folderPanel;

		setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		addMouseListener(new MouseAdapter() {
			
			public void mouseClicked(MouseEvent e) {
				final Point clickedPoint = e.getPoint();
				int selectedTabIndex = indexAtLocation(clickedPoint.x, clickedPoint.y);
				if (selectedTabIndex != -1) {
					if (DesktopManager.isRightMouseButton(e)) {
						setSelectedIndex(selectedTabIndex);
						new FileTableTabPopupMenu(FileTableTabbedPane.this.mainFrame).show(FileTableTabbedPane.this, clickedPoint.x, clickedPoint.y);
					}
				}
			}
		});
	}
	
	@Override
	public boolean requestFocusInWindow() {
		return fileTableComponent.requestFocusInWindow();
	}

	@Override
	public void removeTabAt(int index) {
		super.removeTabAt(index);
		
		if (index == 0 && getTabCount() > 0)
			setComponentAt(0, fileTableComponent); 
	}
	
	/**
	 * Not in use yet
	 * 
	 * @param index
	 * @param component
	 */
	public void setTabHeader(int index, FileTableTabHeader component) {
		super.setTabComponentAt(index, component);
	}
	
	/**
	 * Not in use yet
	 * 
	 * @param index
	 * @return
	 */
	private FileTableTabHeader getTabHeader(int index) {
		return (FileTableTabHeader) getTabComponentAt(index);
	}
	
	@Override
	public void add(FileTableTab tab) {
		add(tab, getTabCount());
	}

	@Override
	public void add(FileTableTab tab, int index) {
		set(tab, index);
		setSelectedIndex(getTabCount()-1); // ?
	}
	
	@Override
	public void set(FileTableTab tab, int index) {
		add(getTabCount() == 0 ? fileTableComponent : new JLabel(), index);

		setTitleAt(index, tab.getLocation().getName());
		setToolTipTextAt(index, tab.getLocation().getAbsolutePath());
	}
	
	@Override
	public void setTitleAt(int index, String title) {
		if (JavaVersions.JAVA_1_5.isCurrentOrLower())
			super.setTitleAt(index, title);
		else {
			FileTableTabHeader header = getTabHeader(index);
			if (header == null) {
				header = new FileTableTabHeader(folderPanel);
				setTabHeader(index, header);
			}
				
			header.setTitle(title);
		}
	}
	
	@Override
	public String getTitleAt(int index) {
		if (JavaVersions.JAVA_1_5.isCurrentOrLower())
			return super.getTitleAt(index);
		else {
			FileTableTabHeader header = getTabHeader(index);
			return header!=null ? header.getTitle() : "";
		}
	}
	
	@Override
	public void setSelectedIndex(int index) {
        super.setSelectedIndex(index);
        requestFocusInWindow();
    }
	
	@Override
	public void update(FileTableTab tab, int index) {
		setTitleAt(index, tab.getLocation().getName());
		setToolTipTextAt(index, tab.getLocation().getAbsolutePath());
		
		doLayout();
	}

	@Override
	public void show(FileTableTab t) {
		folderPanel.tryChangeCurrentFolder(t.getLocation());
		validate();
	}
}
