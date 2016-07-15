package com.cgi.eoss.ftep.core.data.manager.core;

// Singleton
public class CacheManager {
  // Singleton instance
  private static final CacheManager cacheManagerInstance = new CacheManager();

  // Providing the Singleton itself
  private CacheManager() {
    // System.out.println("fn name: CacheManager();");
    java.io.File newPathItem = new java.io.File(Variables.CACHE_PATH);
    if (!newPathItem.exists()) {
      newPathItem.mkdir();
    }
    populateItemsFromDir();
  }

  // To access the Singleton object
  public static CacheManager getInstance() {
    // System.out.println("fn name: CacheManager.getInstance(); params: -");
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
  private static final java.util.concurrent.LinkedBlockingQueue<String> list_recentDownloads =
      new java.util.concurrent.LinkedBlockingQueue();

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
  private static final java.util.concurrent.ConcurrentHashMap<String, UrlData> list_recentDownloads_DATAMIRROR =
      new java.util.concurrent.ConcurrentHashMap();

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
    // System.out.println("fn name: CacheManager.checkIfUrlIsInRecentsList(); params: singleUrl '" +
    // singleUrl + "'");
    // Return true if in the list (at least tried)
    return list_recentDownloads.contains(singleUrl);
  }

  public boolean alreadyManagedToDownload(String oneUrlItem) {
    // System.out.println("fn name: CacheManager.alreadyManagedToDownload(); params: oneUrlItem '" +
    // oneUrlItem + "'");
    // Return true if the status is OK (successfully downloaded)
    return (UrlData.STATUS_OK == list_recentDownloads_DATAMIRROR.get(oneUrlItem).status);
  }

  public String getSymlinkForAlreadyExistingUrl(String oneUrlItem, String jobdir) {
    // System.out.println("fn name: CacheManager.getSymlinkForAlreadyExistingUrl(); params:
    // oneUrlItem '" + oneUrlItem + "' jobdir '" + jobdir + "'");
    // Return or create and return symlink's path for the item, within the given jobdir
    // System.out.println("** ** **
    // list_recentDownloads_DATAMIRROR.get(oneUrlItem).mainFolderLocation '" +
    // list_recentDownloads_DATAMIRROR.get(oneUrlItem).mainFolderLocation + "'!");
    return createSymlink(list_recentDownloads_DATAMIRROR.get(oneUrlItem).mainFolderLocation,
        jobdir);
  }

  public String createSymlink(String fileNameInCache, String targetJobdirForSymlinks) {
    System.out.println("fn name: CacheManager.createSymlink(); params: fileNameInCache '"
        + fileNameInCache + "' targetJobdirForSymlinks '" + targetJobdirForSymlinks + "'");
    String targetFile = null;
    if (null != fileNameInCache) {
      String mainFolderDownloadLocation = null;
      // The jobdir is assigned automatically, before this is called but create if not exists
      // System.out.println("TG root '" + Variables.TARGET_ROOT_PATH + "'!");
      System.out.println("TG '" + targetJobdirForSymlinks + "'!");
      System.out.println("TG full path '" + targetJobdirForSymlinks + "'!");
      java.io.File newPathItem = new java.io.File(targetJobdirForSymlinks);
      if (!newPathItem.exists()) {
        newPathItem.mkdirs();
      }
      // Attachable filename for every path
      String slashFileName = "/" + fileNameInCache;
      // The target for the symlink
      targetFile =  targetJobdirForSymlinks + slashFileName;
      // The source for the symlink
      System.out.println("**** fileincache '" + fileNameInCache + "'!");
      System.out.println("**** slashfileincache '" + slashFileName + "'!");
      mainFolderDownloadLocation = Variables.CACHE_PATH + slashFileName;
      System.out.println("**** cacheslashfileincache '" + mainFolderDownloadLocation + "'!");

      // Path item to be used as "target" in the link construction call
      java.nio.file.Path linkTarget = new java.io.File(targetFile).toPath();
      // If the link is not there
      if (!java.nio.file.Files.isSymbolicLink(linkTarget)) {
        // Path item to be used as "source" in the link construction call
        java.nio.file.Path linkSource = new java.io.File(mainFolderDownloadLocation).toPath();
        try {
          // Try and call the link construction method
          java.nio.file.Files.createSymbolicLink(linkTarget, linkSource);
        } catch (Exception e) {
          System.out.println("Could not create symlink for '" + mainFolderDownloadLocation + "'!");
          // To show there was an error creating the link
          targetFile = null;
          e.printStackTrace();
        }
      }
    }
    // Return the symlink's path or null
    return targetFile;
  }

  public String unzipFile(String zipFile) {
    // System.out.println("fn name: CacheManager.unzipFile(); params: zipFile '" + zipFile + "'");
    String mainItemLocation = null;
    // Check if ZIP file only then unzip
    if (zipFile.toLowerCase().endsWith(".zip")) {
      // System.out.println("it is ZIP file " + zipFile + "'");
      try {
        boolean topLevelDirFound = false;
        byte[] buffer = new byte[1024];
        // Get the ZIP file's content
        try (java.util.zip.ZipInputStream zis =
            new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile))) {
          // For each file and folder
          java.util.zip.ZipEntry ze = zis.getNextEntry();
          while (ze != null) {
            // Get the name of the item
            String fileName = ze.getName();
            java.io.File newPathItem = new java.io.File(Variables.CACHE_PATH + fileName);
            // The root item is needed, that will be linked
            if (!topLevelDirFound) {
              topLevelDirFound = true;
              // The full path of the root item (preferably folder, rarely file)
              mainItemLocation = newPathItem.getAbsolutePath();
            }
            // Can be either directory or file, each comes once
            if (ze.isDirectory()) {
              // Create each directory, no need to check for existence
              newPathItem.mkdir();
              // System.out.println("dir created: " + newPathItem.getAbsolutePath());
            } else {
              // If it is a file create it
              try (java.io.FileOutputStream fos = new java.io.FileOutputStream(newPathItem)) {
                int len;
                while ((len = zis.read(buffer)) > 0) {
                  fos.write(buffer, 0, len);
                }
                // System.out.println("file unzip: " + newPathItem.getAbsoluteFile());
              }
            }
            // Take the next entry from the ZIP
            ze = zis.getNextEntry();
          }
          // Close the stream
          zis.closeEntry();
          // System.out.println("Done");
        }
      } catch (java.io.IOException e) {
        System.out.println("File I/O error!");
        // TODO -- add error handling if needed
      }
      // Always delete the ZIP
      new java.io.File(Variables.CACHE_PATH + zipFile).delete();
    } else {
      mainItemLocation = (new java.io.File(Variables.CACHE_PATH + zipFile)).getAbsolutePath();
      // System.out.println("it is NOT a ZIP file '" + mainItemLocation + "'");
    }
    // If any
    if (null != mainItemLocation) {
      // Return the item's path relative to Variables.CACHE_PATH
      mainItemLocation = mainItemLocation.split(Variables.CACHE_PATH)[1];
    }
    // The location of either the ZIP file's root item or the item itself (if not ZIP)
    return mainItemLocation;
  }

  public boolean addToRecentlyDownloadedList(String oneUrlInRow, boolean statusOk,
      String itemLocation) {
    // System.out.println("fn name: CacheManager.addToRecentlyDownloadedList(); params: oneUrlInRow
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
    // System.out.println("fn name: private CacheManager.removeFromDownloadedList(); params:
    // urlToRemoveFromDownloadedList '" + urlToRemoveFromDownloadedList + "'");
    // Remove the URL from the recently downloaded items' list and details-mirror
    if (list_recentDownloads.remove(urlToRemoveFromDownloadedList)) {
      // If managed to remove the entry delete the file/folder as well
      new java.io.File(
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
    // System.out.println("fn name: private CacheManager.updateCache(); params: -");
    ////////////////////////////////////////////////////////////////////////////////
    // System.out.println("nr of items: '" + new java.io.File(Variables.CACHE_PATH).list().length +
    // "'");
    // System.out.println("nr of items in ArrayList: '" + list_recentDownloads.size() + "'");
    // System.out.println("limit: '" + Variables.DISK_CACHE_NUMBER_LIMIT + "'");
    // System.out.println("2b removed: '" + Variables.DISK_CACHE_REMOVABLE_COUNT + "'");
    // System.out.println("****recents: '");
    // for (String s : list_recentDownloads) {
    // System.out.println(s);
    // }
    // System.out.println("****datakeyset: '");
    // for (String s : list_recentDownloads_DATAMIRROR.keySet()) {
    // System.out.println(s);
    // }
    ////////////////////////////////////////////////////////////////////////////////
    // Check the storage constraints
    if (new java.io.File(Variables.CACHE_PATH).list().length > Variables.DISK_CACHE_NUMBER_LIMIT
        - Variables.DISK_CACHE_REMOVABLE_COUNT) {
      // If the given limit is near remove some items -- FIFO
      for (int t_idx = 0; t_idx < Variables.DISK_CACHE_REMOVABLE_COUNT; t_idx++) {
        // Get the oldest item's name -- remove the entry as well
        String oldestUrl = list_recentDownloads.poll();
        // Delete the oldest item and remove the entry
        if (null != oldestUrl) {
          new java.io.File(list_recentDownloads_DATAMIRROR.get(oldestUrl).mainFolderLocation)
              .delete();
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
}
