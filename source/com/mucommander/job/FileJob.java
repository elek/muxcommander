
package com.mucommander.job;

import com.mucommander.file.AbstractFile;
import com.mucommander.file.FileSet;
import com.mucommander.text.SizeFormat;
import com.mucommander.text.Translator;
import com.mucommander.ui.FileExistsDialog;
import com.mucommander.ui.MainFrame;
import com.mucommander.ui.ProgressDialog;
import com.mucommander.ui.comp.dialog.QuestionDialog;
import com.mucommander.ui.table.FileTable;


/**
 * FileJob is a container for a 'file task' : basically an operation that involves files and bytes.
 * The class extending FileJob is required to give some information about the status of the job that
 * will be used to display visual indications of the job's progress.
 *
 * <p>The actual file operations are performed in a separate thread.</p>
 *
 * @author Maxence Bernard
 */
public abstract class FileJob implements Runnable {

    /** Thread in which the file job is performed */
    private Thread jobThread;

    /** Serves to differenciate between the 'stopped' and 'not started yet' states */
    private boolean hasStarted;
	
    /** Is this job paused ? */
    private boolean isPaused;

    /** Lock used when job is being paused */
    private Object pauseLock = new Object();

    /** Timestamp in milliseconds when job started */
    private long startDate;

    /** Number of milliseconds during which this job has been paused (been waiting for some user response).
     * Used to compute stats like average speed. */
    private long pausedTime;

    /** Contains the timestamp when this job has been put in pause (if in pause) */
    private long pauseStartDate;

    /** Associated dialog showing job progression */
    protected ProgressDialog progressDialog;

    /** Main frame on which the job is to be performed */ 
    protected MainFrame mainFrame;
	
    /** Base source folder */
    protected AbstractFile baseSourceFolder;
	
    /** Files which are going to be processed */
    protected FileSet files;

    /** Number of files that this job contains */
    protected int nbFiles;

    /** Index of file currently being processed, see {@link #getCurrentFileIndex()} */
    protected int currentFileIndex = -1;

    /** File currently being processed */
    protected AbstractFile currentFile;

    /** If set to true, processed files will be unmarked from current table */
    private boolean autoUnmark = true;
	
    /** File to be selected after job has finished (can be null if not set) */
    private AbstractFile fileToSelect;


//    private int nbFilesProcessed;
//    private int nbFilesDiscovered;

    protected final static int SKIP_ACTION = 0;
    protected final static int RETRY_ACTION = 1;
    protected final static int CANCEL_ACTION = 2;
    protected final static int APPEND_ACTION = 3;

    protected final static String SKIP_TEXT = Translator.get("skip");
    protected final static String RETRY_TEXT = Translator.get("retry");
    protected final static String CANCEL_TEXT = Translator.get("cancel");
    protected final static String APPEND_TEXT = Translator.get("resume");
	
	
    /**
     * Creates a new FileJob without starting it.
     *
     * @param progressDialog dialog which shows this job's progress
     * @param mainFrame mainFrame this job has been triggered by
     * @param files files which are going to be processed
     */
    public FileJob(ProgressDialog progressDialog, MainFrame mainFrame, FileSet files) {
        this(mainFrame, files);
        this.progressDialog = progressDialog;
    }

	
    /**
     * Creates a new FileJob without starting it, and with no associated ProgressDialog.
     *
     * @param mainFrame mainFrame this job has been triggered by
     * @param files files which are going to be processed
     */
    public FileJob(MainFrame mainFrame, FileSet files) {
        this.mainFrame = mainFrame;
        this.files = files;
		
        this.nbFiles = files.size();
        this.baseSourceFolder = files.getBaseFolder();
    }
	
	
    /**
     * Specifies whether or not files that have been processed should be unmarked from current table (enabled by default).
     */
    public void setAutoUnmark(boolean autoUnmark) {
        this.autoUnmark = autoUnmark;
    }
	
	
    /**
     * Sets the given file to be selected in the active table after this job has finished.
     * The file will only be selected if it exists in the active table's folder and if this job hasn't
     * been cancelled. The selection will occur after the tables have been refreshed (if they are refreshed).
     */
    public void selectFileWhenFinished(AbstractFile file) {
        this.fileToSelect = file;
    }
	
	
    /**
     * Starts file job in a separate thread.
     */
    public void start() {
        // Pause auto-refresh during file job if this job potentially modifies folders contents
        // and would potentially cause table to auto-refresh
        mainFrame.getFolderPanel1().getFileTable().setAutoRefreshActive(false);
        mainFrame.getFolderPanel2().getFileTable().setAutoRefreshActive(false);
		
        // Serves to differenciate between the 'stopped' and 'not started yet' states
        hasStarted = true;
        startDate = System.currentTimeMillis();
        jobThread = new Thread(this, getClass().getName());
        jobThread.start();
    }


    /**
     * Returns the timestamp in milliseconds when this job was started.
     */
    public long getStartDate() {
        return startDate;
    }


    /**
     * Returns the timestamp in milliseconds when this job was last paused.
     * If this job has not been paused yet, 0 is returned.
     */
    public long getPauseStartDate() {
        return pauseStartDate;
    }

    
    /**
     * Number of milliseconds during which this job has been paused (been waiting for some user response).
     * If this job has been paused multiple times, the total is returned.
     * If this job has not been paused yet, 0 is returned.
     */
    public long getPausedTime() {
        return pausedTime;
    }


    /**
     * Returns the number of milliseconds this job effectively spent processing files, exclusing any paused time.
     */
    public long getEffectiveJobTime() {
        // If job hasn't start yet, return 0
        if(startDate==0)
            return 0;
        
        return System.currentTimeMillis()-startDate-pausedTime;
    }

    
    /**
     * Stops this job.
     */	
    public void stop() {
        // Return if job has already been stopped
        if(jobThread==null)
            return;

        jobThread = null;
	
        // Notify that the job has been stopped
        jobStopped();
    }
	
	
    /**
     * Returns <code>true</code> if this file job has been interrupted.
     */
    public boolean isInterrupted() {
        return jobThread == null;
    }


    /**
     * Sets or unsets this job in paused mode.
     */
    public void setPaused(boolean paused) {
        // Lock the pause lock while updating paused status
        synchronized(pauseLock) {
            // Resume job if it was paused
            if(!paused && this.isPaused) {
                // Calculate pause time
                this.pausedTime += System.currentTimeMillis() - this.pauseStartDate;
                // Call the jobResumed method to notify of the new job's state
                jobResumed();

                // Wake up the job's thread that is potentially waiting for pause to be over 
                pauseLock.notify();
            }
            // Pause job if it not paused already
            else if(paused && !this.isPaused) {
                // Memorize pause time in order to calculate pause time when the job is resumed
                this.pauseStartDate = System.currentTimeMillis();
                // Call the jobPaused method to notify of the new job's state
                jobPaused();
            }

            this.isPaused = paused;
        }
    }


    /**
     * Returns true if this job is currently paused, waiting for user response.
     */
    public boolean isPaused() {
        return isPaused;        
    }


    /**
     * Changes current file. This method should be called by subclasses whenever the job
     * starts processing a new file other than a top-level file, i.e. one that was passed
     * as an argument to {@link #processFile(AbstractFile, Object) processFile()}.
     * ({#nextFile(AbstractFile) nextFile()} is automatically called for files in base folder).
     */
    protected void nextFile(AbstractFile file) {
        this.currentFile = file;

        // Lock the pause lock
        synchronized(pauseLock) {
            // Loop while job is paused, there shouldn't normally be more than one loop
            while(isPaused) {
                try {
                    // Wait for a call to notify()
                    pauseLock.wait();
                } catch(InterruptedException e) {
                    // No more problem, loop one more time
                }
            }
        }
//        if(this.currentFile!=null)
//            this.nbFilesProcessed++;
    }


//    protected void fileDiscovered(AbstractFile file) {
//        this.nbFilesDiscovered++;
//    }
//
//    protected void filesDiscovered(AbstractFile files[]) {
//        this.nbFilesDiscovered += files.length;
//    }
//
//    protected int getNbFilesDiscovered() {
//        return this.nbFilesDiscovered;
//    }
//
//    protected int getNbFilesProcessed() {
//        return this.nbFilesProcessed;
//    }
//

    /**
     * Returns some info about the file currently being processed, for example : "test.zip" (14KB)
     */
    protected String getCurrentFileInfo() {
        // Update current file information used by status string
        if(currentFile==null)
            return "";
        return "\""+currentFile.getName()+"\" ("+ SizeFormat.format(currentFile.getSize(), SizeFormat.DIGITS_MEDIUM| SizeFormat.UNIT_SHORT| SizeFormat.ROUND_TO_KB)+")";
    }
	
	
    /**
     * This method is called when this job starts, before the first call to {@link #processFile(AbstractFile,Object)} is made.
     * This method implementation does nothing but it can be overriden by subclasses to perform some first-time initializations.
     */
    protected void jobStarted() {
        if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("called");
    }
	

    /**
     * This method is called when this job has completed normal execution : all files have been processed without any interruption
     * (without any call to {@link #stop()}).
     *
     * <p>The call happens after the last call to {@link #processFile(AbstractFile,Object)} is made.
     * This method implementation does nothing but it can be overriden by subclasses to properly complete the job.</p>
	 
     * <p>Note that this method will NOT be called if a call to {@link #stop()} was made before all files were processed.</p>
     */
    protected void jobCompleted() {
        if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("called");
    }


    /**
     * This method is called when this job has been paused, either by the user, or by the job when asking for user input.
     * 
     * <p>This method implementation does nothing but it can be overriden by subclasses to do whatever is needed
     * when the job has been paused.
     */
    protected void jobPaused() {
        if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("called");
    }


    /**
     * This method is called when this job has been resumed after being paused.
     *
     * <p>This method implementation does nothing but it can be overriden by subclasses to do whatever is needed
     * when the job has returned from pause.
     */
    protected void jobResumed() {
        if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("called");
    }


    /**
     * This method is called when this job has been stopped. The call happens after all calls to {@link #processFile(AbstractFile,Object)} and
     * {@link #jobCompleted()}.
     * This method implementation does nothing but it can be overriden by subclasses to properly terminate the job.
     * This is where you want to close any opened connections.
     *
     * <p>Note that unlike {@link #jobCompleted()} this method is always called, whether the job has been completed (all
     * files were processed) or has been interrupted in the middle.</p>
     */
    protected void jobStopped() {
        if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("called");
    }
	
	
    /**
     * Actual job is performed in a separate thread.
     */
    public void run() {
        FileTable activeTable = mainFrame.getActiveTable();
        AbstractFile currentFile;

        // Notifies that this job starts
        jobStarted();

//this.nbFilesDiscovered += nbFiles;

        // Loop on all source files, checking that job has not been interrupted
        for(int i=0; i<nbFiles; i++) {
            currentFile = files.fileAt(i);
	
            // Change current file and advance file index
            currentFileIndex = i;
            nextFile(currentFile);
			
            // Process current file
            boolean success = processFile(currentFile, null);

            // Stop if job was interrupted by the user
            if(isInterrupted())
                break;

            // Unmark file in active table if 'auto unmark' is enabled
            // and file was processed successfully
            if(autoUnmark && success) {
                activeTable.setFileMarked(currentFile, false);
            }

            // If last file was reached without any user interruption, notify that
            // that the job has been completed (all files have been processed with or without errors).
            if(i==nbFiles-1) {
                jobCompleted();
            }
        }


        boolean jobInterrupted = isInterrupted();

        // If this job hasn't been stopped already, call stop()
        if(!jobInterrupted) {
            // Stop job
            stop();
        }
		
        // Dispose progress dialog (if any) 
        if(progressDialog!=null)
            progressDialog.dispose();

        // Refresh tables's current folders, based on the job's refresh policy.
        refreshTables();
		
        // Select file specified by selectFileWhenFinished (if any) only if job hasn't been interrupted
        // and file exists in the active table's folder
        if(fileToSelect!=null && !jobInterrupted && activeTable.getCurrentFolder().equals(fileToSelect.getParent()) && fileToSelect.exists())
            activeTable.selectFile(fileToSelect);
    }

	
    /**
     * Displays an error dialog with the specified title and message,
     * offers to skip the file, retry or cancel and waits for user choice.
     * The job is stopped if 'cancel' or 'close' was chosen, and the result 
     * is returned.
     */
    protected int showErrorDialog(String title, String message) {
        String actionTexts[] = new String[]{SKIP_TEXT, RETRY_TEXT, CANCEL_TEXT};
        int actionValues[] = new int[]{SKIP_ACTION, RETRY_ACTION, CANCEL_ACTION};
		
        return showErrorDialog(title, message, actionTexts, actionValues);
    }


	
    /**
     * Displays an error dialog with the specified title and message and returns the selection action's value.
     */
    protected int showErrorDialog(String title, String message, String actionTexts[], int actionValues[]) {
        QuestionDialog dialog;
		
        if(progressDialog==null)
            dialog = new QuestionDialog(mainFrame, 
                                        title,
                                        message,
                                        mainFrame,
                                        actionTexts,
                                        actionValues,
                                        0);
        else
            dialog = new QuestionDialog(progressDialog, 
                                        title,
                                        message,
                                        mainFrame,
                                        actionTexts,
                                        actionValues,
                                        0);

        // Cancel or close dialog stops this job
        int userChoice = waitForUserResponse(dialog);
        if(userChoice==-1 || userChoice==CANCEL_ACTION)
            stop();
		
        return userChoice;
    }
	
	
    /**
     * Waits for the user's answer to the given question dialog, putting this
     * job in pause mode while waiting for the user.
     */
    protected int waitForUserResponse(QuestionDialog dialog) {
        // Put this job in pause mode while waiting for user response
        setPaused(true);
        int retValue = dialog.getActionValue();
        // Back to work
        setPaused(false);
        return retValue;
    }
	
	
    /**
     * Creates and returns a dialog which notifies the user that a file already exists in the destination folder
     * under the same name and asks for what to do.
     */
    protected FileExistsDialog getFileExistsDialog(AbstractFile sourceFile, AbstractFile destFile, boolean multipleFilesMode) {
        if(progressDialog==null)
            return new FileExistsDialog(mainFrame, mainFrame, sourceFile, destFile, multipleFilesMode);
        else
            return new FileExistsDialog(progressDialog, mainFrame, sourceFile, destFile, multipleFilesMode);
    }


    /**
     * Check and if needed, refreshes both file tables's current folders, based on the job's refresh policy.
     */
    protected void refreshTables() {
        FileTable table1 = mainFrame.getFolderPanel1().getFileTable();
        FileTable table2 = mainFrame.getFolderPanel2().getFileTable();

        if(hasFolderChanged(table1.getCurrentFolder()))
            table1.getFolderPanel().tryRefreshCurrentFolder();

        if(hasFolderChanged(table2.getCurrentFolder()))
            table2.getFolderPanel().tryRefreshCurrentFolder();

        // Resume auto-refresh if auto-refresh has been paused
        table1.setAutoRefreshActive(true);
        table2.setAutoRefreshActive(true);
    }
	

    ////////////////////////////////////////////
    // Control methods used by ProgressDialog //
    ////////////////////////////////////////////
	
    /**
     * Returns <code>true</code> if the file job is finished.
     */
    public boolean hasFinished() {
        return hasStarted && jobThread == null;
    }
	
    /**
     * Returns the percentage of job completion, as a float comprised between 0 and 1.
     */
    public float getTotalPercentDone() {
        return getCurrentFileIndex()/(float)getNbFiles();
    }


    /**
     * Returns the index of the file currently being processed (has to be < {@link #getNbFiles()}).
     */
    public int getCurrentFileIndex() {
        return currentFileIndex==-1?0:currentFileIndex;
    }

    /**
     * Returns the number of file that this job contains.
     */
    public int getNbFiles() {
        return nbFiles;
    }

	
    //////////////////////
    // Abstract methods //
    //////////////////////

	
    /**
     * This method should return <code>true</code> if the given folder has or may have been modified. This method is
     * used to determine if current table folders should be refreshed after this job.
     */
    protected abstract boolean hasFolderChanged(AbstractFile folder);
	
	
    /**
     * Automatically called by {@link #run()} for each file that needs to be processed.
     *
     * @param file the file or folder to process
     * @param recurseParams array of parameters which can be used when calling this method recursively, contains <code>null</code> when called by {@link #run()}
     *
     * @return <code>true</code> if the operation was sucessful
     */
    protected abstract boolean processFile(AbstractFile file, Object recurseParams);

	
    /**
     * Returns a String describing the file what is currently being done.
     */
    public abstract String getStatusString();
	
}
