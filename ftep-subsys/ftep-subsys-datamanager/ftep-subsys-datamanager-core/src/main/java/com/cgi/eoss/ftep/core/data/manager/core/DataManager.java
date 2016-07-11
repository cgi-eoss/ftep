package com.cgi.eoss.ftep.core.data.manager.core;

// symlinks created by this module are regardless of elements removed

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DataManager {
    private java.util.concurrent.LinkedBlockingQueue<String> list_recentDownloads = new java.util.concurrent.LinkedBlockingQueue();
    private java.util.concurrent.ConcurrentHashMap<String, UrlData> list_recentDownloads_DATAMIRROR = new java.util.concurrent.ConcurrentHashMap();

    private boolean hasSkippedEntry = false;
    private boolean hasManagedEntry = false;

    private static java.util.HashMap actualJobsLists; //key:jobID, value: urllistmirror

    private static final String CACHE_PATH = "C:\\NinaFolder\\cache0";
    private final int disk_cache_current_number = 0;
    private final int disk_cache_number_limit = 1000;
    private final int disk_cache_removable_nr_of_items = 10;

    private boolean checkIfUrlIsInRecentsList(String singleUrl) {
        return list_recentDownloads.contains(singleUrl);
    }

    private boolean alreadyManagedToDownload(String oneUrlItem) {
        return (UrlData.STATUS_OK == list_recentDownloads_DATAMIRROR.get(oneUrlItem).status);
    }

    private String getSymlinkForAlreadyExistingUrl(String oneUrlItem, String jobdir) {
        return createSymlink(list_recentDownloads_DATAMIRROR.get(oneUrlItem).mainFolderLocation, jobdir);
    }

    private String createSymlink(String mainFolderDownloadLocation, String targetJobdir) {
        String symlinkPath = "";
        return symlinkPath;
    }

    private java.util.ArrayList<String> transformUrlsIntoSymlinks(java.util.ArrayList<String> jobList, String jobdir) {
        java.util.ArrayList<String> resultList = new java.util.ArrayList();
        boolean downloadState = false;
        for (String oneUrlItemIn: jobList) {
            if (!checkIfUrlIsInRecentsList(oneUrlItemIn)) {
                list_recentDownloads.add(oneUrlItemIn);
                UrlData oneData = new UrlData();
                list_recentDownloads_DATAMIRROR.put(oneUrlItemIn, oneData);
                updateCache(); //to make space if needed
                resultList.add(checkUrlsTypeAndCallTheProperDownloadFn(oneUrlItemIn, jobdir));
            } else {
                if (alreadyManagedToDownload(oneUrlItemIn)) {
                    resultList.add(getSymlinkForAlreadyExistingUrl(oneUrlItemIn, jobdir));
                    //this is regardless of already removed items!
                    hasManagedEntry = true;
                } else {
                    hasSkippedEntry = true;
                }
            }
        }
        if (downloadState) {
            //
        }
        return resultList;
    }

    private String checkUrlsTypeAndCallTheProperDownloadFn(String singleUrl, String jobdir) {
        //singleUrl is type1 then method1 else method2:
        if (singleUrl.contains("type1")) {
            return downloadAndAuthType1cert(jobdir);
        } else if (singleUrl.contains("type2")) {
            return downloadAndAuthType2userpass(jobdir);
        }
        return "";
    }

    private String downloadAndAuthType1cert(String jobdir) {
        return downloadAndUnpackZips("/tmp/certPath", "auth1user_if_any", "auth1pass_if_any", jobdir);
    }

    private String downloadAndAuthType2userpass(String jobdir) {
        //find user and pass in database
        return downloadAndUnpackZips("", "auth2user", "auth2pass", jobdir);
    }

    private String downloadAndUnpackZips(String certPath, String user, String pass, String jobdir) {
        String symlinkForUrl = "";
        //check in cache / recently downloaded's list
        //if new item:
            //start authenticated download
                //in the background! USING SECP SCRIPT!!!!
        if (false) { //on fail:
            hasSkippedEntry = true;
            //
        } else {
            hasManagedEntry = true;
            //unpack zips
            //get content's list
            String mainFolderDownloadLocation = "";
            symlinkForUrl = createSymlink(mainFolderDownloadLocation, jobdir);
        }
        return symlinkForUrl;
    }

    private boolean updateCache() {
        if (disk_cache_current_number > disk_cache_number_limit - disk_cache_removable_nr_of_items) {
            for (int t_idx = 0; t_idx < 10; t_idx++) {
                //clean: remove oldest
                String oldestUrl = list_recentDownloads.poll();
                if (null != oldestUrl) {
                    list_recentDownloads_DATAMIRROR.remove(oldestUrl);
                }
            }
        }
        return false;
    }

    private boolean removeFromDownloadingList(String urlToRemoveFromCurrentlyDownloadingList) {
        //remove url from downloading list and mirror:
        if (list_recentDownloads.remove(urlToRemoveFromCurrentlyDownloadingList)) {
            list_recentDownloads_DATAMIRROR.remove(urlToRemoveFromCurrentlyDownloadingList);
            updateCache();
            return true;
        }
        return false;
    }

    private class UrlData {
        //attribs needed for URL object:
            //String url ~KEY
            // URL details:
                //int unpackedsize
                //status: NotAvail(-1) non-init(0) downloading(1) done(2)
                //content: the symlink for the main folder
        public static final int STATUS_NOTAVAILABLE = -1;
        public static final int STATUS_NONINITIALIZED = 0;
        public static final int STATUS_OK = 1;
        public int unpackedSize = 0;
        public int status = STATUS_NONINITIALIZED;
        //this will be symlinked:
        public String mainFolderLocation = "";
    }

    //unzipFile: input zip file; output: zip file output folder
    //WORKS: unzipFile("/home/ivann/rakesh/vm.zip", "/home/ivann/rakesh/");
    public void unzipFile(String zipFile, String outputFolder) {
        byte[] buffer = new byte[1024];
        try {
            //create output directory is not exists
            File folder = new File(outputFolder);
            if (!folder.exists()) {
                folder.mkdir();
            }
            //get the zip file content
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
                //get the zipped file list entry
                ZipEntry ze = zis.getNextEntry();
                while (ze != null) {
                    String fileName = ze.getName();
                    //could be either dir or file!
                    File newPathItem = new File(outputFolder + File.separator + fileName);
                    if (ze.isDirectory()) {
                        newPathItem.mkdir();
                    } else {
                        try (FileOutputStream fos = new FileOutputStream(newPathItem)) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                    ze = zis.getNextEntry();
                }
                zis.closeEntry();
            }
        } catch(IOException e) {
            System.out.println("File I/O error!");
        }
    }

//--------------------------------------------------------------------------

    //                                dest dir        key + http(s)://urls.zip
    public DataManagerResult getData(String inputDir, java.util.HashMap<String, java.util.ArrayList<String>> inputListsWithJobID) {
        //to track the whole progress of one job having multiple IDs and Lists
        DataManagerResult dataManagerResult = new DataManagerResult();
        java.util.HashMap<String, java.util.ArrayList<String>> symlinksResultMap = new java.util.HashMap();
        for (String jobListRowID: inputListsWithJobID.keySet()) {
            java.util.ArrayList<String> listOfInputUrls = inputListsWithJobID.get(jobListRowID);
            System.out.println("Sub-List, job <row> ID: " + jobListRowID);
            symlinksResultMap.put(jobListRowID, transformUrlsIntoSymlinks(listOfInputUrls, inputDir));
        }
        dataManagerResult.setUpdatedInputItems(symlinksResultMap);
        return dataManagerResult;
    }
}
