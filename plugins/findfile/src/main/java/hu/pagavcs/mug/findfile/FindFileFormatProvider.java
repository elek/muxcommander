package hu.pagavcs.mug.findfile;

import java.io.IOException;

import com.mucommander.file.AbstractArchiveFile;
import com.mucommander.file.AbstractFile;
import com.mucommander.file.ArchiveFormatProvider;
import com.mucommander.file.filter.ExtensionFilenameFilter;
import com.mucommander.file.filter.FilenameFilter;

public class FindFileFormatProvider implements ArchiveFormatProvider {

	private final static ExtensionFilenameFilter filenameFilter = new ExtensionFilenameFilter(".mugFileFound");

	public AbstractArchiveFile getFile(AbstractFile file) throws IOException {
		return new FindFileArchiveFile(null, file);
	}

	public FilenameFilter getFilenameFilter() {
		return filenameFilter;
	}
}
