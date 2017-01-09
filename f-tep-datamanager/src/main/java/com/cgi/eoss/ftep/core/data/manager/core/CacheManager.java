package com.cgi.eoss.ftep.core.data.manager.core;

import com.cgi.eoss.ftep.core.utils.FtepConstants;
import com.cgi.eoss.ftep.orchestrator.io.ZipHandler;
import com.google.common.collect.Iterables;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

// Singleton
// @SuppressWarnings("CallToPrintStackTrace")
@Slf4j
public class CacheManager {
    // Singleton instance
    private static final CacheManager cacheManagerInstance = new CacheManager();
    private static String downloadDir;

    // Providing the Singleton itself
    private CacheManager() {
        // LOG.debug("fn name: CacheManager();");
        // java.io.File newPathItem = new java.io.File(Variables.CACHE_PATH);
        // if (!newPathItem.exists()) {
        // newPathItem.mkdir();
        // }
        // populateItemsFromDir();
    }

    // To access the Singleton object
    public static CacheManager getInstance() {
        LOG.debug("fn name: CacheManager.getInstance(); params: -");
        return cacheManagerInstance;
    }

    // --------------------------------------------------------------------------

    // java.util.concurrent.LinkedBlockingQueue<E>
    // https://docs.oracle.com/javase/tutorial/collections/implementations/queue.html
    // interface:
    // https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/LinkedBlockingQueue.html
    // boolean contains(Object o)
    // E peek() //Retrieves, but does not remove, the head of this queue, or returns null if this
    // queue is empty.
    // E poll() //Retrieves and removes the head of this queue, or returns null if this queue is empty
    // E take() //Retrieves and removes the head of this queue, waiting if necessary until an element
    // becomes available.
    // void put(E e)
    // boolean remove(Object o)
    // int size()
    // Object[] toArray() //Returns an array containing all of the elements in this queue, in proper
    // sequence.
    // To store the recently downloaded URLs -- concurrent and efficient search / add / remove
    private static final BlockingQueue<String> list_recentDownloads = new LinkedBlockingQueue<>();

    // java.util.concurrent.ConcurrentHashMap<K,V>
    // https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentHashMap.html
    // interface:
    // void clear()
    // boolean containsKey(Object key)
    // boolean contains(Object value)
    // boolean containsValue(Object value)
    // V get(Object key) //Returns the value to which the specified key is mapped, or null if this map
    // contains no mapping for the key.
    // boolean isEmpty() //Returns true if this map contains no key-value mappings.
    // long mappingCount() //Returns the number of mappings.
    // int size()
    // V put(K key, V value) //Maps the specified key to the specified value in this table.
    // V putIfAbsent(K key, V value) //If the specified key is not already associated with a value,
    // associate it with the given value.
    // V remove(Object key) //Removes the key (and its corresponding value) from this map.
    // boolean replace(K key, V oldValue, V newValue) //Replaces the entry for a key only if currently
    // mapped to a given value.
    // Enumeration<V> elements() //Returns an enumeration of the values in this table.
    // Enumeration<K> keys() //Returns an enumeration of the keys in this table.
    // Collection<V> values() //Returns a Collection view of the values contained in this map.
    // To store important information for the recently downloaded items -- concurrent operations
    private static final Map<String, UrlData> list_recentDownloads_DATAMIRROR =
            new ConcurrentHashMap<>();

    // The stored attributes for the recently downloaded items
    private class UrlData {
        public static final int STATUS_NOTAVAILABLE = -1;
        public static final int STATUS_NONINITIALIZED = 0;
        public static final int STATUS_OK = 1;
        // Status -- NotAvail(-1) non-init(0) downloaded(1)
        public int status = STATUS_NONINITIALIZED;
        // This one will be symlinked
        public String mainFolderLocation = "";
    }

    // --------------------------------------------------------------------------

    public boolean checkIfUrlIsInRecentsList(String singleUrl) {
        // LOG.debug("fn name: CacheManager.checkIfUrlIsInRecentsList(); params: singleUrl '" +
        // singleUrl + "'");
        // Return true if in the list (at least tried)
        return list_recentDownloads.contains(singleUrl);
    }

    public boolean alreadyManagedToDownload(String oneUrlItem) {
        // LOG.debug("fn name: CacheManager.alreadyManagedToDownload(); params: oneUrlItem '" +
        // oneUrlItem + "'");
        // Return true if the status is OK (successfully downloaded)
        return (UrlData.STATUS_OK == list_recentDownloads_DATAMIRROR.get(oneUrlItem).status);
    }

    public String getSymlinkForAlreadyExistingUrl(String oneUrlItem, String jobdir) {
        // LOG.debug("fn name: CacheManager.getSymlinkForAlreadyExistingUrl(); params:
        // oneUrlItem '" + oneUrlItem + "' jobdir '" + jobdir + "'");
        // Return or create and return symlink's path for the item, within the given jobdir
        // LOG.debug("** ** **
        // list_recentDownloads_DATAMIRROR.get(oneUrlItem).mainFolderLocation '" +
        // list_recentDownloads_DATAMIRROR.get(oneUrlItem).mainFolderLocation + "'!");
        return createSymlink(list_recentDownloads_DATAMIRROR.get(oneUrlItem).mainFolderLocation,
                jobdir);
    }

    public String createSymlink(String fileNameInCache, String targetJobdirForSymlinks) {
        // LOG.debug("fn name: CacheManager.createSymlink(); params: fileNameInCache '" +
        // fileNameInCache + "' targetJobdirForSymlinks '" + targetJobdirForSymlinks + "'");
        String targetFile = null;
        if (null != fileNameInCache) {
            // The jobdir is assigned automatically, before this is called but create if not exists
            File newPathItem = new File(targetJobdirForSymlinks);
            if (!newPathItem.exists()) {
                newPathItem.mkdirs();
            }
            // Attachable filename for every path
            String slashFileName = "/" + fileNameInCache;
            // The target for the symlink
            targetFile = targetJobdirForSymlinks + slashFileName;
            // The source for the symlink
            String mainFolderDownloadLocation = downloadDir + slashFileName;
            // Path item to be used as "target" in the link construction call
            Path linkTarget = new File(targetFile).toPath();
            // If the link is not there
            if (!Files.isSymbolicLink(linkTarget)) {
                // Path item to be used as "source" in the link construction call
                Path linkSource = new File(mainFolderDownloadLocation).toPath();
                try {
                    // Try and call the link construction method
                    Files.createSymbolicLink(linkTarget, linkSource);
                } catch (Exception e) {
                    LOG.error("Could not create symlink for '{}'!", mainFolderDownloadLocation, e);
                    // To show there was an error creating the link
                    targetFile = null;
                }
            }
        }
        // Return the symlink's path or null
        return targetFile;
    }

    public String unzipFile(String zipFile) {
        LOG.debug("Unzipping file '{}'", zipFile);
        String mainItemLocation = null;
        // Check if ZIP file only then unzip
        if (zipFile.toLowerCase().endsWith(".zip")) {
            Path zip = Paths.get(downloadDir, zipFile);
            Path targetDir = Paths.get(downloadDir, zipFile.replaceAll("\\.zip$", ""));

            try {
                Files.createDirectories(targetDir);
                ZipHandler.unzip(zip, targetDir);

                // If the zip contained a single directory, link to that, otherwise link to the created directory
                Set<Path> targetDirContents = Files.list(targetDir).collect(Collectors.toSet());
                if (targetDirContents.size() == 1 && Files.isDirectory(Iterables.getOnlyElement(targetDirContents))) {
                    mainItemLocation = Iterables.getOnlyElement(targetDirContents).toAbsolutePath().toString();
                } else {
                    mainItemLocation = targetDir.toAbsolutePath().toString();
                }

                // Always delete the ZIP
                Files.delete(zip);
            } catch (IOException e) {
                LOG.error("File I/O error! while performing file unzip for: {}", zipFile, e);
            }
        } else {
            mainItemLocation = (new File(downloadDir + zipFile)).getAbsolutePath();
            // LOG.debug("it is NOT a ZIP file '" + mainItemLocation + "'");
        }
        // If any
        if (null != mainItemLocation) {
            // Return the item's path relative to Variables.CACHE_PATH
            mainItemLocation = mainItemLocation.split(downloadDir)[1];
        }
        // The location of either the ZIP file's root item or the item itself (if not ZIP)
        return mainItemLocation;
    }

    public boolean addToRecentlyDownloadedList(String oneUrlInRow, boolean statusOk,
                                               String itemLocation) {
        // LOG.debug("fn name: CacheManager.addToRecentlyDownloadedList(); params: oneUrlInRow
        // '" + oneUrlInRow + "' itemLocation '" + itemLocation + "' bool_statusOk '" + statusOk + "'");
        // Never null: DataManager.transformUrlsIntoSymlinks() --> for (String oneUrlInRow:
        // listOfInputUrlsByRow)
        if (null != oneUrlInRow) {
            // Add to the recents' list
            list_recentDownloads.add(oneUrlInRow);
            // Create the URL data based on the given parameters
            UrlData oneData = new UrlData();
            if (statusOk) {
                // For the successfully downloaded item set the location as well
                oneData.status = UrlData.STATUS_OK;
                oneData.mainFolderLocation = itemLocation;
            } else {
                // Item failed
                oneData.status = UrlData.STATUS_NOTAVAILABLE;
            }
            // Add the data into the details' list
            list_recentDownloads_DATAMIRROR.put(oneUrlInRow, oneData);
            saveItemsInDir(oneUrlInRow, itemLocation);
            // Check the cache -- make space if needed
            updateCache();
            // Mark as "succeed" -- return value not used (yet)
            return true;
        }
        // Mark as "failed" -- return value not used (yet)
        return false;
    }

    private boolean removeFromDownloadedList(String urlToRemoveFromDownloadedList) {
        // Function not used (yet)
        // LOG.debug("fn name: private CacheManager.removeFromDownloadedList(); params:
        // urlToRemoveFromDownloadedList '" + urlToRemoveFromDownloadedList + "'");
        // Remove the URL from the recently downloaded items' list and details-mirror
        if (list_recentDownloads.remove(urlToRemoveFromDownloadedList)) {
            // If managed to remove the entry delete the file/folder as well
            new File(
                    list_recentDownloads_DATAMIRROR.get(urlToRemoveFromDownloadedList).mainFolderLocation)
                    .delete();
            // Remove deleted entry's details from the mirror
            list_recentDownloads_DATAMIRROR.remove(urlToRemoveFromDownloadedList);
            // Check cache -- if more items needs to be removed
            updateCache();
            // Mark as "succeed"
            return true;
        }
        // Mark as "failed"
        return false;
    }

    // --------------------------------------------------------------------------

    private boolean updateCache() {
        // LOG.debug("fn name: private CacheManager.updateCache(); params: -");
        ////////////////////////////////////////////////////////////////////////////////
        // LOG.debug("nr of items: '" + new java.io.File(Variables.CACHE_PATH).list().length +
        // "'");
        // LOG.debug("nr of items in ArrayList: '" + list_recentDownloads.size() + "'");
        // LOG.debug("limit: '" + Variables.DISK_CACHE_NUMBER_LIMIT + "'");
        // LOG.debug("2b removed: '" + Variables.DISK_CACHE_REMOVABLE_COUNT + "'");
        // LOG.debug("****recents: '");
        // for (String s : list_recentDownloads) {
        // LOG.debug(s);
        // }
        // LOG.debug("****datakeyset: '");
        // for (String s : list_recentDownloads_DATAMIRROR.keySet()) {
        // LOG.debug(s);
        // }
        ////////////////////////////////////////////////////////////////////////////////
        // Check the storage constraints
        if (new File(downloadDir).list().length > FtepConstants.DISK_CACHE_NUMBER_LIMIT
                - FtepConstants.DISK_CACHE_REMOVABLE_COUNT) {
            // If the given limit is near remove some items -- FIFO
            for (int t_idx = 0; t_idx < FtepConstants.DISK_CACHE_REMOVABLE_COUNT; t_idx++) {
                // Get the oldest item's name -- remove the entry as well
                String oldestUrl = list_recentDownloads.poll();
                // Delete the oldest item and remove the entry
                if (null != oldestUrl) {
                    new File(list_recentDownloads_DATAMIRROR.get(oldestUrl).mainFolderLocation).delete();
                    list_recentDownloads_DATAMIRROR.remove(oldestUrl);
                }
            }
            // Change happened -- return value not used (yet)
            return true;
        }
        // No change -- return value not used (yet)
        return false;
    }

    // --------------------------------------------------------------------------

    // Currently the actually downloaded items are stored in memory
    // On system-crash or reboot either the whole Cache should be emptied or reload the items

    private void populateItemsFromDir() {
        // Read the Cache folder contents -- from a file or something! ~ links and paths mappings
        // Update folder contents based on this previously saved list -- remove non-listed items
        // Status for these items will be "UrlData.STATUS_OK"
    }

    private void saveItemsInDir(String urlToSave, String itemLocationToSave) {
        // Create/extend links and paths mappings for restoration purpose
    }

    public void setDownloadDir(String downloadDir) {
        CacheManager.downloadDir = downloadDir;
    }
}
