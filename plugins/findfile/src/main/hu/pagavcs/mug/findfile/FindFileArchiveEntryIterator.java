package hu.pagavcs.mug.findfile;

import java.io.IOException;
import java.util.Iterator;

import com.mucommander.commons.file.AbstractFile;
import com.mucommander.commons.file.ArchiveEntry;
import com.mucommander.commons.file.ArchiveEntryIterator;

class FindFileArchiveEntryIterator implements ArchiveEntryIterator {

	private Iterator<AbstractFile> iterator;

	FindFileArchiveEntryIterator(String findId) throws IOException {
		iterator = FindManager.getInstance().getResults(findId).iterator();
	}

	public ArchiveEntry nextEntry() throws IOException {
		if (iterator.hasNext()) {
			AbstractFile file = iterator.next();
			return new FindFileArchiveEntry(file.getName(), file.isDirectory(), file.getDate(), file.getSize(), file);
		} else {
			return null;
		}
	}

	public void close() throws IOException {}
}
