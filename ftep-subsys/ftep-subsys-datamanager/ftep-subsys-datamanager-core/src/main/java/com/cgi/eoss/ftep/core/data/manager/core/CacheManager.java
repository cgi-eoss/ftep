package com.cgi.eoss.ftep.core.data.manager.core;

//Singleton
public class CacheManager {
    private static final CacheManager cacheManagerInstance = new CacheManager();

    protected CacheManager() {}

    public static CacheManager getInstance() {
        System.out.println("fn name: CacheManager.getInstance(); params: -");
        return cacheManagerInstance;
    }

    //--------------------------------------------------------------------------

    // java.util.concurrent.LinkedBlockingQueue<E> https://docs.oracle.com/javase/tutorial/collections/implementations/queue.html
        // interface: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/LinkedBlockingQueue.html
        //boolean contains(Object o)
        //E peek() //Retrieves, but does not remove, the head of this queue, or returns null if this queue is empty.
        //E poll() //Retrieves and removes the head of this queue, or returns null if this queue is empty
        //E take() //Retrieves and removes the head of this queue, waiting if necessary until an element becomes available.
        //void put(E e)
        //boolean remove(Object o)
        //int size()
        //Object[] toArray() //Returns an array containing all of the elements in this queue, in proper sequence.
    private static final java.util.concurrent.LinkedBlockingQueue<String> list_recentDownloads = new java.util.concurrent.LinkedBlockingQueue();
    // java.util.concurrent.ConcurrentHashMap<K,V> https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentHashMap.html
        //interface:
        //void clear()
        //boolean containsKey(Object key)
        //boolean contains(Object value)
        //boolean containsValue(Object value)
        //V get(Object key) //Returns the value to which the specified key is mapped, or null if this map contains no mapping for the key.
        //boolean isEmpty() //Returns true if this map contains no key-value mappings.
        //long mappingCount() //Returns the number of mappings.
        //int size()
        //V put(K key, V value) //Maps the specified key to the specified value in this table.
        //V putIfAbsent(K key, V value) //If the specified key is not already associated with a value, associate it with the given value.
        //V remove(Object key) //Removes the key (and its corresponding value) from this map.
        //boolean replace(K key, V oldValue, V newValue) //Replaces the entry for a key only if currently mapped to a given value.
        //Enumeration<V> elements() //Returns an enumeration of the values in this table.
        //Enumeration<K> keys() //Returns an enumeration of the keys in this table.
        //Collection<V>	values() //Returns a Collection view of the values contained in this map.
    private static final java.util.concurrent.ConcurrentHashMap<String, UrlData> list_recentDownloads_DATAMIRROR = new java.util.concurrent.ConcurrentHashMap();

    private static final int disk_cache_number_limit = 1000; //number limit 1000 folders
    private static final int disk_cache_removable_nr_of_items = 10;

    private static final String TARGET_ROOT_PATH = "/tmp/targetlinks/";
    private static final String CACHE_PATH = "/tmp/cache0/";

    private class UrlData {
        //attribs needed for URL object:
            //String url ~KEY
            // URL details:
                //status: NotAvail(-1) non-init(0) downloaded(1)
                //content: the symlink for the main folder
        public static final int STATUS_NOTAVAILABLE = -1;
        public static final int STATUS_NONINITIALIZED = 0;
        public static final int STATUS_OK = 1;
        public int status = STATUS_NONINITIALIZED;
        //this will be symlinked:
        public String mainFolderLocation = "";
    }

    //--------------------------------------------------------------------------

    public boolean checkIfUrlIsInRecentsList(String singleUrl) {
        System.out.println("fn name: CacheManager.checkIfUrlIsInRecentsList(); params: singleUrl '" + singleUrl + "'");
        return list_recentDownloads.contains(singleUrl);
    }

    public boolean alreadyManagedToDownload(String oneUrlItem) {
        System.out.println("fn name: CacheManager.alreadyManagedToDownload(); params: oneUrlItem '" + oneUrlItem + "'");
        return (UrlData.STATUS_OK == list_recentDownloads_DATAMIRROR.get(oneUrlItem).status);
    }

    public String getSymlinkForAlreadyExistingUrl(String oneUrlItem, String jobdir) {
        System.out.println("fn name: CacheManager.getSymlinkForAlreadyExistingUrl(); params: oneUrlItem '" + oneUrlItem + "' jobdir '" + jobdir + "'");
        return createSymlink(list_recentDownloads_DATAMIRROR.get(oneUrlItem).mainFolderLocation, jobdir);
    }

    public String createSymlink(String fileNameInCache, String targetJobdirForSymlinks) {
        System.out.println("fn name: CacheManager.createSymlink(); params: fileNameInCache '" + fileNameInCache + "' targetJobdirForSymlinks '" + targetJobdirForSymlinks + "'");
        String mainFolderDownloadLocation = null;
        ////jobdir is assigned automatically, before this is called
        //check dest dir: jobdir
        //create symlinks in jobdir
        try {
            String slashFileName = "/" + fileNameInCache;
            String targetFile = TARGET_ROOT_PATH + targetJobdirForSymlinks + slashFileName;
            mainFolderDownloadLocation = CACHE_PATH + slashFileName;
            java.nio.file.Path linkSource = new java.io.File(mainFolderDownloadLocation).toPath();
            java.nio.file.Path linkTarget = new java.io.File(targetFile).toPath();
            java.nio.file.Files.createSymbolicLink(linkTarget, linkSource);
            //DETERMINE SOURCE OF SYMLINK -- aka 'check if worked'...
            //System.out.println("Target of link " + linkTarget + " is " + java.nio.file.Files.readSymbolicLink(linkTarget));
        } catch (Exception e) {
            //Some file systems do not support symbolic links.
            System.out.println("Could not create symlink for '" + mainFolderDownloadLocation + "'!");
        }
        return mainFolderDownloadLocation;
    }

    //unzipFile: input zip file; output: zip file output folder
    public String unzipFile(String zipFile) {
        System.out.println("fn name: CacheManager.unzipFile(); params: zipFile '" + zipFile + "'");
        String mainFolder = null;
        try {
            boolean topLevelDirFound = false;
            byte[] buffer = new byte[1024];
            //get the zip file content
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile))) {
                //get the zipped file list entry
                java.util.zip.ZipEntry ze = zis.getNextEntry();
                while (ze != null) {
                    String fileName = ze.getName();
                    //could be either dir or file!
                    java.io.File newPathItem = new java.io.File(CACHE_PATH + fileName);
                    if (ze.isDirectory()) {
                        //create output directory is not exists
                        newPathItem.mkdir();
                        //System.out.println("dir created: " + newPathItem.getAbsolutePath());
                        if (!topLevelDirFound) {
                            topLevelDirFound = true;
                            mainFolder = newPathItem.getAbsolutePath();
                        }
                    } else {
                        //////create all non exists folders else you will hit FileNotFoundException for compressed folder
                        ////new File(newPathItem.getParent()).mkdirs();
                        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(newPathItem)) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                            //System.out.println("file unzip: " + newPathItem.getAbsoluteFile());
                        }
                    }
                    ze = zis.getNextEntry();
                }
                zis.closeEntry();
                //System.out.println("Done");
            }
        } catch(java.io.IOException e) {
            System.out.println("File I/O error!");
        }
        return mainFolder;
    }

    public boolean addToRecentlyDownloadedList(String oneUrlInRow, boolean statusOk, String itemLocation) {
        System.out.println("fn name: CacheManager.addToRecentlyDownloadedList(); params: oneUrlInRow '" + oneUrlInRow + "' itemLocation '" + itemLocation + "' bool_statusOk '" + statusOk + "'");
        if (null != oneUrlInRow) {
            list_recentDownloads.add(oneUrlInRow);
                UrlData oneData = new UrlData();
                    if (statusOk) {
                        oneData.status = UrlData.STATUS_OK;
                        oneData.mainFolderLocation = itemLocation;
                    } else {
                        oneData.status = UrlData.STATUS_NOTAVAILABLE;
                    }
                list_recentDownloads_DATAMIRROR.put(oneUrlInRow, oneData);
                updateCache(); //to make space if needed
            return true;
        }
        return false;
    }

    private boolean removeFromDownloadedList(String urlToRemoveFromDownloadedList) {
        System.out.println("fn name: private CacheManager.removeFromDownloadedList(); params: urlToRemoveFromDownloadedList '" + urlToRemoveFromDownloadedList + "'");
        //remove url from downloading list and mirror:
        if (list_recentDownloads.remove(urlToRemoveFromDownloadedList)) {
            new java.io.File(list_recentDownloads_DATAMIRROR.get(urlToRemoveFromDownloadedList).mainFolderLocation).delete();
            list_recentDownloads_DATAMIRROR.remove(urlToRemoveFromDownloadedList);
            updateCache();
            return true;
        }
        return false;
    }

    //--------------------------------------------------------------------------

    private boolean updateCache() {
        System.out.println("fn name: private CacheManager.updateCache(); params: -");
        if (list_recentDownloads.size() > disk_cache_number_limit - disk_cache_removable_nr_of_items) {
            for (int t_idx = 0; t_idx < disk_cache_removable_nr_of_items; t_idx++) {
                //clean: delete and remove oldest
                String oldestUrl = list_recentDownloads.poll();
                if (null != oldestUrl) {
                    new java.io.File(list_recentDownloads_DATAMIRROR.get(oldestUrl).mainFolderLocation).delete();
                    list_recentDownloads_DATAMIRROR.remove(oldestUrl);
                }
            }
        }
        return false;
    }
}
