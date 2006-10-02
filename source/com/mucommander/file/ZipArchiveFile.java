package com.mucommander.file;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
// do not import java.util.zip.ZipEntry !

/**
 * 
 *
 * @author Maxence Bernard
 */
public class ZipArchiveFile extends AbstractArchiveFile {

    /**
     * Creates a new ZipArchiveFile.
     */
    public ZipArchiveFile(AbstractFile file) {
        super(file);
        if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("called");
    }


    ////////////////////////////////////////
    // AbstractArchiveFile implementation //
    ////////////////////////////////////////
	
    protected Vector getEntries() throws IOException {
        if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("called");

        // Load all zip entries
        Vector entries = new Vector();
		
        // If the underlying file is a local file, use the ZipFile.getEntries() method as it 
        // is *way* faster than using ZipInputStream to iterate over the entries.
        // Note: under Mac OS X at least, ZipFile.getEntries() method is native
        if(file instanceof FSFile) {
            Enumeration entriesEnum = new ZipFile(getAbsolutePath()).entries();
            while(entriesEnum.hasMoreElements())
                entries.add(new ZipEntry((java.util.zip.ZipEntry)entriesEnum.nextElement()));
        }
        else {
            // works but it is *way* slower
            ZipInputStream zin = new ZipInputStream(file.getInputStream());
            java.util.zip.ZipEntry entry;
            while ((entry=zin.getNextEntry())!=null) {
                entries.add(new ZipEntry(entry));
            }
            zin.close();
        }

        return entries;
    }


    InputStream getEntryInputStream(ArchiveEntry entry) throws IOException {
        if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("called");

        // If the underlying file is a local file, use the ZipFile.getInputStream() method as it
        // is *way* faster than using ZipInputStream and looking for the entry
        if (file instanceof FSFile) {
            return new ZipFile(getAbsolutePath()).getInputStream((java.util.zip.ZipEntry)entry.getEntry());
        }
        // works but it is *way* slower
        else {
            ZipInputStream zin = new ZipInputStream(file.getInputStream());
            java.util.zip.ZipEntry tempEntry;
            String entryPath = entry.getPath();
            // Iterate until we find the entry we're looking for
            while ((tempEntry=zin.getNextEntry())!=null)
                if (tempEntry.getName().equals(entryPath)) // That's the one, return it
                    return zin;
            return null;
        }
    }
}
