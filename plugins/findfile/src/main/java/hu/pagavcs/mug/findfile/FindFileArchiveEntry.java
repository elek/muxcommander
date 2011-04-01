package hu.pagavcs.mug.findfile;


import com.mucommander.file.AbstractFile;
import com.mucommander.file.ArchiveEntry;

public class FindFileArchiveEntry extends ArchiveEntry implements RealFileProvider {

	protected AbstractFile realFile;

	public FindFileArchiveEntry(String path, boolean directory, long date, long size, AbstractFile realFile) {
		super(path, directory, date, size, true);
		this.realFile = realFile;
	}

	public AbstractFile getRealFile() {
		return realFile;
	}
}
