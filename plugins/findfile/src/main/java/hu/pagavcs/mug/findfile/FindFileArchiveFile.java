package hu.pagavcs.mug.findfile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.mucommander.file.AbstractFile;
import com.mucommander.file.AbstractRWArchiveFile;
import com.mucommander.file.ArchiveEntry;
import com.mucommander.file.ArchiveEntryIterator;
import com.mucommander.file.FileOperation;
import com.mucommander.file.UnsupportedFileOperationException;

public class FindFileArchiveFile extends AbstractRWArchiveFile {

	private final String findId;

	public FindFileArchiveFile(String findId, AbstractFile file) {
		this(findId, new FakeFindFile(file));
	}

	private FindFileArchiveFile(String findId, FakeFindFile file) {
		super(file);
		this.findId = findId;

		long totalSize = 0;
		for (AbstractFile child : FindManager.getInstance().getResults(findId)) {
			if (!child.isDirectory()) {
				totalSize += child.getSize();
			}
		}
		file.setSize(totalSize);
	}

	// public String getAbsolutePath() {
	// return super.getAbsolutePath() + "/FIND";
	// }

	// //////////////////////////////////////
	// AbstractArchiveFile implementation //
	// //////////////////////////////////////

	@Override
	public ArchiveEntryIterator getEntryIterator() throws IOException, UnsupportedFileOperationException {
		return new FindFileArchiveEntryIterator(findId);
	}

	@Override
	public InputStream getEntryInputStream(ArchiveEntry entry, ArchiveEntryIterator entryIterator) throws IOException, UnsupportedFileOperationException {
		// Will throw an IOException if the file designated by the entry doesn't
		// exist
		return ((FindFileArchiveEntry) entry).getRealFile().getInputStream();
		// return FileFactory.getFile(entry.getPath(), true).getInputStream();
	}

	public boolean isWritable() {
		return false;
	}

	public OutputStream addEntry(ArchiveEntry entry) throws IOException, UnsupportedFileOperationException {
		throw new UnsupportedFileOperationException(FileOperation.WRITE_FILE);
	}

	public void mkfile() throws IOException, UnsupportedFileOperationException {
		throw new UnsupportedFileOperationException(FileOperation.WRITE_FILE);
	}

	public void mkdir() throws IOException, UnsupportedFileOperationException {
		throw new UnsupportedFileOperationException(FileOperation.WRITE_FILE);
	}

	public void deleteEntry(ArchiveEntry entry) throws IOException, UnsupportedFileOperationException {
		((FindFileArchiveEntry) entry).getRealFile().delete();
		// Remove the entry from the entries tree
		removeFromEntriesTree(entry);
	}

	public void optimizeArchive() throws IOException, UnsupportedFileOperationException {}

	public void updateEntry(ArchiveEntry entry) throws IOException, UnsupportedFileOperationException {}
}
