package hu.pagavcs.mug.findfile;

import java.io.IOException;
import java.util.Iterator;

import com.mucommander.file.AbstractFile;
import com.mucommander.file.ArchiveEntry;
import com.mucommander.file.ArchiveEntryIterator;

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
