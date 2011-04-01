package hu.pagavcs.mug.findfile;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import com.mucommander.file.AbstractArchiveEntryFile;
import com.mucommander.file.AbstractFile;
import com.mucommander.file.ArchiveEntry;
import com.mucommander.file.UnsupportedFileOperationException;
import com.mucommander.ui.main.MainFrame;

public class FindManager {

	private static final FindManager            singleton = new FindManager();

	private HashMap<String, List<AbstractFile>> mapFind;
	private HashSet<String>                     setCancelSearch;

	private ScheduledExecutorService            oldSearchPurger;

	public FindManager() {
		mapFind = new HashMap<String, List<AbstractFile>>();
		setCancelSearch = new HashSet<String>();
		oldSearchPurger = Executors.newSingleThreadScheduledExecutor();
	}

	public static FindManager getInstance() {
		return singleton;
	}

	public List<AbstractFile> getResults(String id) {
		return mapFind.get(id);
	}

	public void removeResults(String id) {
		mapFind.remove(id);
	}

	public void createResults(final String id) {
		mapFind.put(id, new ArrayList<AbstractFile>());
		oldSearchPurger.schedule(new Runnable() {

			public void run() {
				removeResults(id);
			}
		}, 1, TimeUnit.HOURS);
	}

	public void addResult(String id, AbstractFile file) {
		mapFind.get(id).add(file);
	}

	public boolean isCancel(String findId) {
		return setCancelSearch.contains(findId);
	}

	public void removeCancel(String findId) {
		setCancelSearch.remove(findId);
	}

	public void cancelSearch(String findId) {
		setCancelSearch.add(findId);
	}

	public void startSearch(final MainFrame mainFrame, final String findId, final AbstractFile currentFolder, String searchFileName, String searchText,
	        String searchTextEncoding, boolean caseSensitive, boolean searchInArchive, List<String> lstInclude, List<String> lstExclude,
	        FindProcessCallback callback) {
		try {
			Charset searchEncoding = null;

			searchFileName = searchFileName.trim();
			searchTextEncoding = searchTextEncoding.trim();
			if (searchFileName.isEmpty()) {
				searchFileName = "*";
			}


			if (!searchText.isEmpty()) {

				if (!caseSensitive) {
					searchText = searchText.toUpperCase();
				}

				if (searchTextEncoding.isEmpty()) {
					searchTextEncoding = "ISO-8859-1";
				}
				searchEncoding = Charset.forName(searchTextEncoding);
			} else {
				searchText = null;
			}

			createResults(findId);

			Pattern searchFileNamePattern = getPatternFromSimplePattern(searchFileName);
			List<Pattern> lstIncludePattern = new ArrayList<Pattern>();
			for (String li : lstInclude) {
				lstIncludePattern.add(getPatternFromSimplePattern(li));
			}
			List<Pattern> lstExcludePattern = new ArrayList<Pattern>();
			for (String li : lstExclude) {
				lstExcludePattern.add(getPatternFromSimplePattern(li));
			}

			search(findId, currentFolder, searchFileNamePattern, searchText, searchEncoding, caseSensitive, searchInArchive, lstIncludePattern,
			        lstExcludePattern, callback);

			if (!isCancel(findId)) {
				SwingUtilities.invokeLater(new Runnable() {

					public void run() {
						mainFrame.getActivePanel().tryChangeCurrentFolder(new FindFileArchiveFile(findId, currentFolder));
					}
				});
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void search(String findId, AbstractFile file, Pattern searchFileNamePattern, String searchText, Charset searchEncoding, boolean caseSensitive,
	        boolean searchInArchive, List<Pattern> lstIncludePattern, List<Pattern> lstExcludePattern, FindProcessCallback callback) {
		if (isCancel(findId)) {
			return;
		}


		// prevent infinite cycle
		if (file.isSymlink()) {
			return;
		}

		if (file.isDirectory() || (searchInArchive && file.isBrowsable())) {
			callback.actualDir(file.getAbsolutePath());
			try {
				for (AbstractFile child : file.ls()) {
					search(findId, child, searchFileNamePattern, searchText, searchEncoding, caseSensitive, searchInArchive, lstIncludePattern,
					        lstExcludePattern, callback);
				}
			} catch (UnsupportedFileOperationException ex) {} catch (IOException ex) {}
		} else {
			callback.actualFile(file.getAbsolutePath());
			String absPath = file.getAbsolutePath();
			boolean matchInclude = false;
			if (lstIncludePattern.isEmpty()) {
				matchInclude = true;
			} else {
				for (Pattern li : lstIncludePattern) {
					if (li.matcher(absPath).matches()) {
						matchInclude = true;
						break;
					}
				}
			}
			if (matchInclude && !lstExcludePattern.isEmpty()) {
				for (Pattern li : lstExcludePattern) {
					if (li.matcher(absPath).matches()) {
						matchInclude = false;
						break;
					}
				}
			}

			if (matchInclude && searchFileNamePattern.matcher(file.getName()).matches()) {
				if (searchText == null) {
					FindManager.getInstance().addResult(findId, file);
				} else {
					try {
						searchText(findId, file, searchText, searchEncoding, caseSensitive);
					} catch (UnsupportedFileOperationException ex) {} catch (IOException ex) {}
				}
			}
		}
	}

	private void searchText(String findId, AbstractFile file, String searchText, Charset searchEncoding, boolean caseSensitive)
	        throws UnsupportedFileOperationException, IOException {

		BufferedReader in = new BufferedReader(new InputStreamReader(new BufferedInputStream(file.getInputStream(), 4 * 32768), searchEncoding), 4 * 32768);

		int checkCancelCounter = 100000;
		int j = 0;
		boolean found = false;

		if (isCancel(findId)) {
			return;
		}

		int data;
		while ((data = in.read()) != -1) {

			if (checkCancelCounter < 0) {
				if (isCancel(findId)) {
					return;
				} else {
					checkCancelCounter = 100000;
				}
			}
			checkCancelCounter--;

			char c = searchText.charAt(j);
			if (!caseSensitive) {
				data = Character.toUpperCase(data);
			}
			if (data == c) {
				if (j == 0) {
					in.mark(2 * 32768);
				}
				j++;
				if (j == searchText.length()) {
					found = true;
					break;
				}
			} else {
				if (j > 0) {
					in.reset();
					j = 0;
				}
			}
		}

		in.close();

		if (found) {
			FindManager.getInstance().addResult(findId, file);
		}

	}

	private static Pattern getPatternFromSimplePattern(String simplePattern) {
		StringBuilder sbPattern = new StringBuilder();
		StringBuilder sb = new StringBuilder();
		for (char c : simplePattern.toCharArray()) {
			if (c == '*') {
				if (sb.length() > 0) {
					sbPattern.append(Pattern.quote(sb.toString()));
				}
				sbPattern.append(".*");
				sb = new StringBuilder();
			} else {
				sb.append(c);
			}
		}
		if (sb.length() > 0) {
			sbPattern.append(Pattern.quote(sb.toString()));
		}

		return Pattern.compile(sbPattern.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	}

	public static AbstractFile getRealFile(AbstractFile file) {
		file = file.getAncestor();
		if (file instanceof AbstractArchiveEntryFile) {
			AbstractArchiveEntryFile a = ((AbstractArchiveEntryFile) file);
			ArchiveEntry entry = a.getEntry();
			if (entry instanceof RealFileProvider) {
				AbstractFile realFile = ((RealFileProvider) entry).getRealFile();
				return realFile;
			}
		}
		return file;
	}

}
