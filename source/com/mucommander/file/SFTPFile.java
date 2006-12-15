
package com.mucommander.file;

import com.mucommander.io.FileTransferException;
import com.mucommander.io.RandomAccessInputStream;
import com.mucommander.auth.AuthException;
import com.mucommander.auth.Credentials;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.io.UnsignedInteger32;
import com.sshtools.j2ssh.sftp.*;
import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;


/**
 * SFTPFile represents a file on an SSH/SFTP server.
 *
 * @author Maxence Bernard
 */
public class SFTPFile extends AbstractFile {

    private SftpFile file;
    private SshClient sshClient;
    SftpSubsystemClient sftpSubsystem;

    //	private FileURL fileURL;

    protected String absPath;

    private AbstractFile parent;
    private boolean parentValSet;
    
    private final static String SEPARATOR = DEFAULT_SEPARATOR;

	
    static {
        // Disables J2SSH logging on standard output
        System.getProperties().setProperty(org.apache.commons.logging.Log.class.getName(), org.apache.commons.logging.impl.NoOpLog.class.getName());
    }
		

	
    /**
     * Creates a new instance of SFTPFile and initializes the SSH/SFTP connection to the server.
     */
    public SFTPFile(FileURL fileURL) throws IOException {
        this(fileURL, true, null, null);
    }

	
    /**
     * Creates a new instance of SFTPFile and reuses the given SSH/SFTP active connection.
     */
    private SFTPFile(FileURL fileURL, boolean addAuthInfo, SshClient sshClient, SftpSubsystemClient sftpChannel) throws IOException {
        super(fileURL);

        this.absPath = fileURL.getPath();
		
        if(sshClient==null) {
            // Initialize connection
//            initConnection(this.fileURL, addAuthInfo);
            initConnection(this.fileURL);
        }
        else {
            this.sshClient = sshClient;
            this.sftpSubsystem = sftpChannel;
        }
		
        if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("fileURL="+fileURL+" sftpChannel="+this.sftpSubsystem);

        try {
            if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("Retrieving file "+fileURL.getPath());
            this.file = new SftpFile(fileURL.getPath(), this.sftpSubsystem.getAttributes(fileURL.getPath()));
        }
        catch(IOException e) {
            if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("Cannot retrieve file "+fileURL.getPath()+": "+e);
            // File is null (doesn't exist on the remote server), it's OK
        }
    }

	
    private SFTPFile(FileURL fileURL, SftpFile file, SshClient sshClient, SftpSubsystemClient sftpChannel) throws IOException {
        super(fileURL);
        this.absPath = this.fileURL.getPath();
        this.file = file;
        this.sshClient = sshClient;
        this.sftpSubsystem = sftpChannel;
        //		this.fileExists = true;
    }
	
	
//    private void initConnection(FileURL fileURL, boolean addAuthInfo) throws IOException {
private void initConnection(FileURL fileURL) throws IOException {
        if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("connecting to "+fileURL.getHost());
        try {
            // Init SSH client
            this.sshClient = new SshClient();

            // Override default port (22) if a custom port was specified in the URL
            int port = fileURL.getPort();
            if(port==-1)
                port = 22;

            // Connect
            sshClient.connect(fileURL.getHost(), port, new IgnoreHostKeyVerification());

            // Find auth info for this URL
//            CredentialsManager.authenticate(fileURL, addAuthInfo);
            Credentials credentials = fileURL.getCredentials();

            // Throw an AuthException if no auth information
            if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("fileURL="+fileURL.getStringRep(true)+" credentials="+ credentials);
            if(credentials ==null)
                throw new AuthException(fileURL, "Login and password required");
			
            // Authenticate
            PasswordAuthenticationClient pwd = new PasswordAuthenticationClient();
            pwd.setUsername(credentials.getLogin());
            pwd.setPassword(credentials.getPassword());
            int authResult = sshClient.authenticate(pwd);

            // Throw an AuthException if authentication failed
            if(authResult!=AuthenticationProtocolState.COMPLETE)
                throw new AuthException(fileURL, "Login or password rejected");
			
            if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("creating sftpclient ");

            // Init SFTP connection
            this.sftpSubsystem = sshClient.openSftpChannel();

            if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("sftpclient = "+this.sftpSubsystem);
        }
        catch(IOException e) {
            if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("ioexception thrown = "+e);
            // Disconnect if something went wrong
            if(sshClient!=null && sshClient.isConnected())
                sshClient.disconnect();

            this.sshClient = null;
            this.sftpSubsystem = null;

            // Re-throw exception
            throw e;
        }
    }


    private void checkConnection() throws IOException {
        if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("isConnected= "+sshClient.isConnected());
        // Reconnect if disconnected
        if(sshClient==null || !sshClient.isConnected()) {
            // Connect again
//            initConnection(this.fileURL, false);
            initConnection(this.fileURL);
            return;
        }
    }

	
    /////////////////////////////////////////
    // AbstractFile methods implementation //
    /////////////////////////////////////////

    public boolean isSymlink() {
        return file==null?false:file.isLink();
    }

    public long getDate() {
        return file==null?0:file.getAttributes().getModifiedTime().longValue()*1000;
    }

    public boolean changeDate(long lastModified) {
        try {
            SftpFile sftpFile = sftpSubsystem.openFile(absPath, SftpSubsystemClient.OPEN_WRITE);
            FileAttributes attributes = sftpFile.getAttributes();
            attributes.setTimes(attributes.getAccessedTime(), new UnsignedInteger32(lastModified/1000));
            sftpSubsystem.setAttributes(sftpFile, attributes);
            if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("return true");
            return true;
        }
        catch(IOException e) {
            if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("return false "+e);
            return false;
        }
    }
	
    public long getSize() {
        return file==null?0:file.getAttributes().getSize().longValue();
    }
	
	
    public AbstractFile getParent() {
        if(!parentValSet) {
            FileURL parentFileURL = this.fileURL.getParent();
            if(parentFileURL!=null) {
                if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("getParent, parentURL="+parentFileURL.getStringRep(true)+" sig="+com.mucommander.Debug.getCallerSignature(1));
                try { this.parent = new SFTPFile(parentFileURL, false, this.sshClient, this.sftpSubsystem); }
                catch(IOException e) {}
            }

            this.parentValSet = true;
            return this.parent;
        }
		
        return this.parent;
    }
	
	
    public void setParent(AbstractFile parent) {
        this.parent = parent;
        this.parentValSet = true;
    }
	
	
    public boolean exists() {
        return file!=null;
    }
	
    public boolean canRead() {
        return file!=null && file.canRead();
    }
	
    public boolean canWrite() {
        return file!=null && file.canWrite();
    }

    public boolean canExecute() {
        return file!=null && (file.getAttributes().getPermissions().intValue()&FileAttributes.S_IXUSR)!=0;
    }

    public boolean setReadable(boolean readable) {
        if(file==null)
            return false;

        int perms = getFilePermissions();
        setPermissionBit(perms, FileAttributes.S_IRUSR, readable);
        return changeFilePermissions(perms);
    }

    public boolean setWritable(boolean writable) {
        if(file==null)
            return false;

        int perms = getFilePermissions();
        setPermissionBit(perms, FileAttributes.S_IWUSR, writable);
        return changeFilePermissions(perms);
    }

    public boolean setExecutable(boolean executable) {
        if(file==null)
            return false;

        int perms = getFilePermissions();
        setPermissionBit(perms, FileAttributes.S_IXUSR, executable);
        return changeFilePermissions(perms);
    }

    public boolean setPermissions(int permissions) {
        if(file==null)
            return false;

        int perms = getFilePermissions();

        setPermissionBit(perms, FileAttributes.S_IRUSR, (permissions&READ_MASK)!=0);
        setPermissionBit(perms, FileAttributes.S_IWUSR, (permissions&WRITE_MASK)!=0);
        setPermissionBit(perms, FileAttributes.S_IXUSR, (permissions&EXECUTE_MASK)!=0);

        return changeFilePermissions(perms);
    }

    /**
     * Returns the SFTP file permissions.
     */
    private int getFilePermissions() {
        return file.getAttributes().getPermissions().intValue();
    }

    /**
     * Changes the SFTP file permissions to the given permissions int.
     */
    private boolean changeFilePermissions(int permissions) {
        try {
            sftpSubsystem.changePermissions(file, permissions);
            return true;
        }
        catch(IOException e) {
            return false;
        }
    }

    /**
     * Enables/disables the given bit in the permissions int.
     */
    private void setPermissionBit(int permissions, int bit, boolean enabled) {
        if(enabled)
            permissions &= ~bit;
        else
            permissions |= bit;
    }

    public boolean isDirectory() {
        return file==null?false:file.isDirectory();
    }
	
    public InputStream getInputStream() throws IOException {
        return getInputStream(0);
    }

    public RandomAccessInputStream getRandomAccessInputStream() throws IOException {
        // No random access for SFTP files unfortunately
        throw new IOException();
    }

    public OutputStream getOutputStream(boolean append) throws IOException {
        // Check connection and reconnect if connection timed out
        checkConnection();

        if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("file="+getAbsolutePath()+" append="+append+" exists="+exists());

        boolean fileExists = exists();
        //		FileAttributes attributes = new FileAttributes();
        //		attributes.setPermissions("rw-------");
        //		SftpFile sftpFile = sftpChannel.openFile(absPath, fileExists?(append?SftpSubsystemClient.OPEN_WRITE|SftpSubsystemClient.OPEN_APPEND:SftpSubsystemClient.OPEN_WRITE):SftpSubsystemClient.OPEN_WRITE|SftpSubsystemClient.OPEN_CREATE, attributes);
        SftpFile sftpFile = sftpSubsystem.openFile(absPath,
                                                 fileExists?(append?SftpSubsystemClient.OPEN_WRITE|SftpSubsystemClient.OPEN_APPEND:SftpSubsystemClient.OPEN_WRITE|SftpSubsystemClient.OPEN_TRUNCATE)
                                                 :SftpSubsystemClient.OPEN_WRITE|SftpSubsystemClient.OPEN_CREATE);
		
        // If file was just created, change permissions to 600: read+write for owner only (default is 0)
        if(!fileExists)
            sftpSubsystem.changePermissions(sftpFile, "rw-------");

        // Custom made constructor, not part of the official J2SSH API
        return new SftpFileOutputStream(sftpFile, append?getSize():0);
    }
	

    public void delete() throws IOException {
        // Check connection and reconnect if connection timed out
        checkConnection();

        if(isDirectory())
            sftpSubsystem.removeDirectory(absPath);
        else
            sftpSubsystem.removeFile(absPath);
    }


    public AbstractFile[] ls() throws IOException {
        if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("starts");

        // Check connection and reconnect if connection timed out
        checkConnection();

        Vector files = new Vector();

        // Modified J2SSH method to remove the 100 files limitation
        sftpSubsystem.listChildren(file, files);

        // File doesn't exists
        int nbFiles = files.size();
        if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("nbFiles="+nbFiles);
        if(nbFiles==0)
            return new AbstractFile[] {};
	
//        String parentURL = fileURL.getStringRep(false);
        String parentURL = fileURL.getStringRep(true);
        if(!parentURL.endsWith(SEPARATOR))
            parentURL += SEPARATOR;

        AbstractFile children[] = new AbstractFile[nbFiles];
        AbstractFile child;
        FileURL childURL;
        SftpFile sftpFile;
        String filename;
        int fileCount = 0;
        // Fill AbstractFile array and discard '.' and '..' files
        for(int i=0; i<nbFiles; i++) {
            sftpFile = (SftpFile)files.elementAt(i);
            filename = sftpFile.getFilename();
            // Discard '.' and '..' files
            if(filename.equals(".") || filename.equals(".."))
                continue;
            childURL = new FileURL(parentURL+filename, fileURL);
            // if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("sftpFile="+sftpFile);
            child = FileFactory.wrapArchive(new SFTPFile(childURL, sftpFile, sshClient, sftpSubsystem));
            child.setParent(this);

            children[fileCount++] = child;
        }
		
        // Create new array of the exact file count
        if(fileCount<nbFiles) {
            AbstractFile newChildren[] = new AbstractFile[fileCount];
            System.arraycopy(children, 0, newChildren, 0, fileCount);
            return newChildren;
        }
		
        return children;
    }

	
    public void mkdir(String name) throws IOException {
        String dirPath = absPath+(absPath.endsWith(SEPARATOR)?"":SEPARATOR)+name;
        sftpSubsystem.makeDirectory(dirPath);
        // Set new directory permissions to 700: read+write+execute for owner only
        //		sftpChannel.changePermissions(dirPath, 700);
        sftpSubsystem.changePermissions(dirPath, "rwx------");
    }


    public long getFreeSpace() {
        // No way to retrieve this information with J2SSH, return -1 (not available)
        return -1;
    }

    public long getTotalSpace() {
        // No way to retrieve this information with J2SSH, return -1 (not available)
        return -1;
    }


    ////////////////////////
    // Overridden methods //
    ////////////////////////


//    public String getName() {
//        return file==null?fileURL.getFilename():file.getFilename();
//    }


    /**
     * Overrides {@link AbstractFile#moveTo(AbstractFile)} to support server-to-server move if the destination file
     * uses SFTP and is located on the same host.
     */
    public void moveTo(AbstractFile destFile) throws FileTransferException {
        // If destination file is an SFTP file located on the same server, tells the server to rename the file.

        // Use the default moveTo() implementation if the destination file doesn't use FTP
        // or is not on the same host
        if(!destFile.fileURL.getProtocol().equals(FileProtocols.SFTP) || !destFile.fileURL.getHost().equals(this.fileURL.getHost())) {
            super.moveTo(destFile);
            return;
        }

        // If file is an archive file, retrieve the enclosed file, which is likely to be an SFTPFile but not necessarily
        // (may be an ArchiveEntryFile)
        if(destFile instanceof AbstractArchiveFile)
            destFile = ((AbstractArchiveFile)destFile).getProxiedFile();

        // If destination file is not an SFTPFile (for instance an archive entry), server renaming won't work
        // so use default moveTo() implementation instead
        if(!(destFile instanceof SFTPFile)) {
            super.moveTo(destFile);
            return;
        }

        try {
            // Check connection and reconnect if connection timed out
            checkConnection();

            sftpSubsystem.renameFile(absPath, destFile.getURL().getPath());
        }
        catch(IOException e) {
            throw new FileTransferException(FileTransferException.UNKNOWN_REASON);    // Report that move failed
        }
    }


    public InputStream getInputStream(long skipBytes) throws IOException {
        // Check connection and reconnect if connection timed out
        checkConnection();

        if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("skipBytes="+skipBytes, -1);

        SftpFile sftpFile = sftpSubsystem.openFile(absPath, SftpSubsystemClient.OPEN_READ);
        // Custom made constructor, not part of the official J2SSH API
        return new SftpFileInputStream(sftpFile, skipBytes);
    }


    public boolean equals(Object f) {
        if(!(f instanceof SFTPFile))
            return super.equals(f);		// could be equal to a ZipArchiveFile

        return fileURL.equals(((SFTPFile)f).fileURL);
    }
}
