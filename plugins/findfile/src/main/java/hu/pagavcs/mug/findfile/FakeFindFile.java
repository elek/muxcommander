/**
 * 
 */
package hu.pagavcs.mug.findfile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import com.mucommander.file.AbstractFile;
import com.mucommander.file.FilePermissions;
import com.mucommander.file.PermissionBits;
import com.mucommander.file.UnsupportedFileOperationException;
import com.mucommander.io.RandomAccessInputStream;
import com.mucommander.io.RandomAccessOutputStream;

class FakeFindFile extends AbstractFile {

	private final static String FAKE_PATH = "#SEARCH RESULTS#";

	private final AbstractFile  parent;

	private long                date;
	private long                size;

	protected FakeFindFile(AbstractFile parent) {
		super(parent.getURL());
		String path = getURL().getPath();
		if (!path.endsWith(FAKE_PATH)) {
			getURL().setPath(path + FAKE_PATH);
		}
		this.parent = parent;
		date = new Date().getTime();
	}

	public void setSize(long size) {
		this.size = size;
	}

	public AbstractFile getParent() {
		return parent;
	}

	public boolean canGetGroup() {
		return false;
	}

	public boolean canGetOwner() {
		return false;
	}

	public void changeDate(long lastModified) throws IOException, UnsupportedFileOperationException {}

	public void changePermission(int access, int permission, boolean enabled) throws IOException, UnsupportedFileOperationException {}

	public void copyRemotelyTo(AbstractFile destFile) throws IOException, UnsupportedFileOperationException {}

	public void delete() throws IOException, UnsupportedFileOperationException {}

	public boolean exists() {
		return true;
	}

	public OutputStream getAppendOutputStream() throws IOException, UnsupportedFileOperationException {
		return null;
	}

	public PermissionBits getChangeablePermissions() {
		return null;
	}

	public long getDate() {
		return date;
	}

	public long getFreeSpace() throws IOException, UnsupportedFileOperationException {
		return 0;
	}

	public String getGroup() {
		return null;
	}

	public InputStream getInputStream() throws IOException, UnsupportedFileOperationException {
		return null;
	}

	public OutputStream getOutputStream() throws IOException, UnsupportedFileOperationException {
		return null;
	}

	public String getOwner() {
		return null;
	}

	public FilePermissions getPermissions() {
		return null;
	}

	public RandomAccessInputStream getRandomAccessInputStream() throws IOException, UnsupportedFileOperationException {
		return null;
	}

	public RandomAccessOutputStream getRandomAccessOutputStream() throws IOException, UnsupportedFileOperationException {
		return null;
	}

	public long getSize() {
		return size;
	}

	public long getTotalSpace() throws IOException, UnsupportedFileOperationException {
		return size;
	}

	public Object getUnderlyingFileObject() {
		return null;
	}

	public boolean isArchive() {
		return true;
	}

	public boolean isDirectory() {
		return false;
	}

	public boolean isSymlink() {
		return false;
	}

	public AbstractFile[] ls() throws IOException, UnsupportedFileOperationException {
		return null;
	}

	public void mkdir() throws IOException, UnsupportedFileOperationException {}

	public void renameTo(AbstractFile destFile) throws IOException, UnsupportedFileOperationException {}

	public void setParent(AbstractFile parent) {}

}
