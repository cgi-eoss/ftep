package com.cgi.eoss.ftep.core.data.manager.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.zoo.project.ZooConstants;

import com.cgi.eoss.ftep.core.utils.DBRestApiManager;
import com.cgi.eoss.ftep.core.utils.FtepConstants;

// Symlinks created by this module are regardless of elements removed

/*
 * == From the file URLs identify the files to download. Each file to be fetched has an unique URL.
 * Usually multiple data fetch requests come at the same time. Data fetch request may contain
 * request for same file to be downloaded. So maintain a cache on the file system and do not
 * download the same file again if it is already in the cache or currently is in the process of
 * downloading. == All the data are downloaded in the cache directory and only soft links are
 * created in the inputDirectory. One data fetch request should not block or make the other request
 * to wait until the first one completes. Think about creating the hash value for URL and checking
 * against the database which stores the hash-value and file availability status (like. downloaded,
 * downloading, not available). Make the cache as rolling cache, meaning oldest file should be
 * removed first. == Most likely the data to be downloaded is a zip file and you need to unpack it
 * when you place in the cache. The soft links should point to unpacked directory == LATER: postgres
 * db: store jobid, 2nd from last in the INPUTDIR store all the symlinks for the urls NOT THE KEYS
 * from the input hashmap --> - check: update progress or link it if started/done - try cache
 * management with values 3 and 5
 */

// Represents one session of downloading a bunch of items
public class DataManager {
  private static final Logger LOG = Logger.getLogger(DataManager.class);

  private boolean hasSkippedEntry = false;
  private boolean hasManagedEntry = false;
  // Domain is paired with access information for the current session
  private final Map<String, Map<String, String>> credentials = new HashMap<>();
  private Map<String, String> downloadConfigurationMap;


  private java.util.List<String> transformUrlsIntoSymlinks(
      java.util.List<String> listOfInputUrlsByRow, String jobdir) {
    LOG.debug("fn name: transformUrlsIntoSymlinks(); params: list len '"
        + listOfInputUrlsByRow.size() + "' jobdir '" + jobdir + "'");
    // Take one key's values from the initial HashMap which results in an ArrayList
    java.util.ArrayList<String> resultSymlinksByRow = new java.util.ArrayList();
    for (String oneUrlInRow : listOfInputUrlsByRow) {
      // Process each link one by one from the ArrayList
      String pathToSymlink = null;
      // Replace all dollar sign to %24 value with escaped % sign + Escape all apostrophe
      oneUrlInRow = oneUrlInRow.replaceAll("\\$", "\\%24").replaceAll("\\'", "\\%27");
      // Escape all parentheses
      oneUrlInRow = oneUrlInRow.replaceAll("\\(", "\\%28").replaceAll("\\)", "\\%29");
      //// oneUrlInRow = oneUrlInRow.replaceAll("\\-", "\\\\%2D");

      if (!CacheManager.getInstance(downloadConfigurationMap)
          .checkIfUrlIsInRecentsList(oneUrlInRow)) {
        // New content
        pathToSymlink = checkUrlAndGetCredentials(oneUrlInRow, jobdir);
      } else {
        // Already downloaded content
        if (CacheManager.getInstance(downloadConfigurationMap)
            .alreadyManagedToDownload(oneUrlInRow)) {
          // Successful previous trial -- regardless of already removed items!
          LOG.debug(
              "** ** ** getSymlinkForAlreadyExistingUrl for oneUrlInRow '" + oneUrlInRow + "'!");
          pathToSymlink = CacheManager.getInstance(downloadConfigurationMap)
              .getSymlinkForAlreadyExistingUrl(oneUrlInRow, jobdir);
          hasManagedEntry = true;
        } else {
          // Failed previous trial, probably broken link
          hasSkippedEntry = true;
        }
      }
      if (null != pathToSymlink) {
        // If managed to link a downloaded content put it into the result ArrayList
        resultSymlinksByRow.add(pathToSymlink);
      }
    }
    // Return one row (ArrayList) that will be a Value for a Key in the result HashMap
    return resultSymlinksByRow;
  }

  private String checkUrlAndGetCredentials(String oneUrlInRow, String jobdir) {
    LOG.debug("fn name: checkUrlAndGetCredentials(); params: oneUrlInRow '" + oneUrlInRow
        + "' jobdir '" + jobdir + "'");
    // Create REST call
    String domain = oneUrlInRow.split("://")[1].split("/")[0];
    Map<String, String> credentialForDomain = getCredentialsRestCall(domain);
    // Return the path to the symlink or null
    return downloadAndUnpackZips(oneUrlInRow,
        credentialForDomain.get(FtepConstants.DATASOURCE_CRED_CERTIFICATE),
        credentialForDomain.get(FtepConstants.DATASOURCE_CRED_USER),
        credentialForDomain.get(FtepConstants.DATASOURCE_CRED_PASSWORD), jobdir);
  }

  private Map<String, String> getCredentialsRestCall(String domain) {
    LOG.debug("fn name: getCredentialsRestCall(); params: restUrl '" + domain + "'");
    // The credentials should have user/password and/or certificate content
    Map<String, String> credentialForDomain = new HashMap<>();
    if (credentials.keySet().contains(domain)) {
      credentialForDomain.put(FtepConstants.DATASOURCE_CRED_CERTIFICATE,
          credentials.get(domain).get(FtepConstants.DATASOURCE_CRED_CERTIFICATE));
      credentialForDomain.put(FtepConstants.DATASOURCE_CRED_USER,
          credentials.get(domain).get(FtepConstants.DATASOURCE_CRED_USER));
      credentialForDomain.put(FtepConstants.DATASOURCE_CRED_PASSWORD,
          credentials.get(domain).get(FtepConstants.DATASOURCE_CRED_PASSWORD));
    } else {
      String httpEndpoint = FtepConstants.DATASOURCE_QUERY_ENDPOINT + domain;

      LOG.debug("Trying to get credentials from endpoint " + httpEndpoint);
      DBRestApiManager dataBaseMgr = DBRestApiManager.DB_API_CONNECTOR_INSTANCE;
      credentialForDomain = dataBaseMgr.getCredentials(httpEndpoint);
      credentials.put(domain, credentialForDomain);
    }
    // Return the acquired credential-details as key-value pairs
    return credentialForDomain;
  }

  private String downloadAndUnpackZips(String oneUrlInRow, String certPath, String user,
      String pass, String jobdir) {
    LOG.debug("fn name: downloadAndUnpackZips(); params: oneUrlInRow '" + oneUrlInRow + "' jobdir '"
        + jobdir + "' certPath '" + certPath + "' user '" + user + "' pass '" + pass + "'");
    // If the symlink will be returned as null it means the URL was broken
    String symlinkForUrl = null;
    // Start authenticated download using a modified third-party script
    String shellScript =
        downloadConfigurationMap.get(ZooConstants.ZOO_FTEP_DOWNLOAD_TOOL_PATH_PARAM);
    String params1 = " -f -o ";
    String paramsOutDir =
        downloadConfigurationMap.get(ZooConstants.ZOO_FTEP_DATA_DOWNLOAD_DIR_PARAM);
    String params2 = " -c -U -r 1 -rt 5 -s ";
    String paramsCredUser = user;
    String paramsCredPass = pass;
    String paramsCredentials = "-C " + paramsCredUser + ":" + paramsCredPass;
    // TODO -- the parameters should be customized for certificate-based downloads!
    if (null != certPath) {
      paramsCredentials = "-C ?:?";
    }
    // String paramsLinks_2 = "
    // http://localhost:8082/thredds/fileServer/ziptest/S2A_OPER_PRD_MSIL1C_PDMC_20160624T053223_R103_V20160624T020653_20160624T020653.SAFE.zip
    // http://localhost:8082/thredds/fileServer/ziptest/S2A_OPER_PRD_MSIL1C_PDMC_20160624T053019_R103_V20160624T020653_20160624T020653.SAFE.zip";
    // String paramsLinks_1 = "
    // http://localhost:8082/thredds/fileServer/testEnhanced/2004050312_eta_211.nc";
    String paramsLinks = " " + oneUrlInRow;
    // Put the Strings together to form the command
    String command =
        shellScript + params1 + paramsOutDir + params2 + paramsCredentials + paramsLinks;
    LOG.debug("Command: '" + command + "'");
    String zipFile = null;
    try {
      // Downloading -- calling the third-party script
      /*
       * secp needs some fixes: //"curl --remote-name -J" at 3 lines: 265 405 554 and removed -o
       * "$2" //+lines from 402 to 422 target_file + USER-PASS !! #Try to download the file
       * target_file=`echo $2 | rev | cut -f1 -d/ | rev` if [[ -n "$_USER_USERNAME" && -n
       * "$_USER_PASSWORD" ]]; then userpass="-u $_USER_USERNAME:$_USER_PASSWORD" fi message=
       * "`curl $curlopt $userpass --remote-name -J "$1" 2>&1`" res=$? if [[ "$res" -ne "0" ]]; then
       * if [[ "${message/The requested URL returned error: 403//}" != "$message" ]]; then echo
       * "[ERROR  ][secp] Forbidden. Please check your proxy certificate or your username/password."
       * 1>&2 res=2 elif [[ "${message/The requested URL returned error: 404//}" != "$message" ]];
       * then res=1 else echo "[ERROR  ][secp][failed] url '$URI' - $message" 1>&2 fi rm -f "$2"
       * return $res fi mv $target_file $2 return 0 //+453 _PRINT_FILENAME #Search if local file
       * already exists (if so, overwrite or exit) [..] _LOCAL_FILE="${_LOCAL_FILE%\?*}";
       * _PRINT_FILENAME="${_LOCAL_FILE}"; _LOCAL_FILE="$_OUTPUT_DIR$_OUTPUT_PREFIX$_LOCAL_FILE" if
       * [[ -e "$_LOCAL_FILE" ]]; then //+463 _PRINT_FILENAME #in case of already existing zip
       * files: echo "[INFO   ][file $filecounter] downloaded item: '$_PRINT_FILENAME'" continue
       * else //+665 _PRINT_FILENAME #in case of new zip files: echo
       * "[INFO   ][file $filecounter] downloaded item: '$_PRINT_FILENAME'" done
       */
      // Execute the command
      Process scriptProcessCall = Runtime.getRuntime().exec(command);
      // Wait for until terminates
      scriptProcessCall.waitFor();

      // InputStream errStream = scriptProcessCall.getErrorStream();
      // LOG.debug("errorStream ------->>> ");
      // LOG.debug(getStringFromInputStream(errStream));

      // InputStream outStream = scriptProcessCall.getInputStream();
      // LOG.debug("outStream ------->>> ");
      // LOG.debug(getStringFromInputStream(outStream));

      // When ready check the exit code
      if (0 != scriptProcessCall.exitValue()) {
        // On fail
        LOG.debug("** ** download failed");
        hasSkippedEntry = true;
        CacheManager.getInstance(downloadConfigurationMap).addToRecentlyDownloadedList(oneUrlInRow,
            false, "");
        try (java.io.BufferedReader outputReader = new java.io.BufferedReader(
            new java.io.InputStreamReader(scriptProcessCall.getInputStream()))) {
          String outputLine;
          while ((outputLine = outputReader.readLine()) != null) {
            LOG.debug("** ** line read: '" + outputLine + "'");
            if (outputLine.contains("downloaded item: '")) {
              LOG.debug("** ** downloaded file's name: '" + zipFile + "'");
            }
          }
        } catch (final Exception e) {
          LOG.error(e);
        }
      } else {
        // On success
        LOG.debug("** ** download succeed");
        try (java.io.BufferedReader outputReader = new java.io.BufferedReader(
            new java.io.InputStreamReader(scriptProcessCall.getInputStream()))) {
          String outputLine;
          while ((outputLine = outputReader.readLine()) != null) {
            LOG.debug("** ** line read: '" + outputLine + "'");
            if (outputLine.contains("downloaded item: '")) {
              zipFile = outputLine.split("downloaded item: '")[1].split("'")[0];
              LOG.debug("** ** downloaded file's name: '" + zipFile + "'");
            }
          }
        } catch (final Exception e) {
          LOG.error(e);
        }
        LOG.debug("** ** managed to dwnld? '" + zipFile + "'");
        // Unpack if ZIP, get main item's location
        String mainFolderDownloadLocation =
            CacheManager.getInstance(downloadConfigurationMap).unzipFile(zipFile);
        if (null == mainFolderDownloadLocation) {
          hasSkippedEntry = true;
          // When unpack failed in case of ZIP file
          CacheManager.getInstance(downloadConfigurationMap)
              .addToRecentlyDownloadedList(oneUrlInRow, false, "");
        } else {
          hasManagedEntry = true;
          // If managed to download (and unpack) the item add to the download-list
          CacheManager.getInstance(downloadConfigurationMap)
              .addToRecentlyDownloadedList(oneUrlInRow, true, mainFolderDownloadLocation);
        }
        // Create the symlink for the main item in the jobdir and return the symlink's path
        LOG.debug("** ** ** createSymlink for mainFolderDownloadLocation '"
            + mainFolderDownloadLocation + "'!");
        symlinkForUrl = CacheManager.getInstance(downloadConfigurationMap)
            .createSymlink(mainFolderDownloadLocation, jobdir);
      }
    } catch (java.io.IOException | InterruptedException e) {
      LOG.debug("Could not execute command '" + command + "'");
      // TODO -- implement some error handling if needed
    }
    // Return the symlink or null
    return symlinkForUrl;
  }

  private DataManagerResult.DataDownloadStatus calculateDownloadStatus() {
    // LOG.debug("fn name: calculateDownloadStatus(); params: -");
    // Only hasSkippedEntry == true --> NONE;
    DataManagerResult.DataDownloadStatus calculatedStatus =
        DataManagerResult.DataDownloadStatus.NONE;
    if (hasManagedEntry) {
      // Having both hasSkippedEntry == true and hasManagedEntry == true --> PARTIAL;
      if (hasSkippedEntry) {
        calculatedStatus = DataManagerResult.DataDownloadStatus.PARTIAL;
      } else {
        // Only hasManagedEntry == true --> COMPLETE;
        calculatedStatus = DataManagerResult.DataDownloadStatus.COMPLETE;
      }
    }
    // Return appropriate status information
    return calculatedStatus;
  }

  // --------------------------------------------------------------------------

  public DataManagerResult getData(HashMap<String, String> downloadConfMap, String destDir,
      java.util.HashMap<String, List<String>> inputUrlListsWithJobID) {
    downloadConfigurationMap = downloadConfMap;
    // The inputUrlListsWithJobID has more ID+list pairs look like "key" + "http(s)://urls.zip"
    LOG.debug("fn name: getData(); params: destDir '" + destDir + "' map size '"
        + inputUrlListsWithJobID.size() + "'");
    // Aggregated result to track the whole progress of one job ("inputUrlListsWithJobID")
    DataManagerResult dataManagerResult = new DataManagerResult();
    // UpdatedInputItems for the DataManagerResult object
    java.util.HashMap<String, java.util.List<String>> symlinksResultMap = new java.util.HashMap();
    // For each key
    for (String jobListRowID : inputUrlListsWithJobID.keySet()) {
      // Get one "row" (key) at a time and process its URLs in the ArrayList (value)
      java.util.List<String> listOfInputUrlsByRow = inputUrlListsWithJobID.get(jobListRowID);
      // LOG.debug("Sub-List, job <row> ID: " + jobListRowID);
      // For the very same ID (key) put the result ArrayList or symlink locations into the result
      symlinksResultMap.put(jobListRowID, transformUrlsIntoSymlinks(listOfInputUrlsByRow, destDir));
    }
    // When all keys processed assign the result to the object returned
    dataManagerResult.setUpdatedInputItems(symlinksResultMap);
    // DownloadStatus for the DataManagerResult object
    dataManagerResult.setDownloadStatus(calculateDownloadStatus());
    ////////////////////////////////////////////////////////////////////////////////
    // double-check for the input-output items!
    LOG.debug("++++++++++++++++++++++++++++++++++");
    for (String inputKey : inputUrlListsWithJobID.keySet()) {
      LOG.debug("Key<in> is '" + inputKey + "'");
      for (String inputRow : inputUrlListsWithJobID.get(inputKey)) {
        LOG.debug("Value<in> is '" + inputRow + "'");
      }
    }
    for (String outputKey : dataManagerResult.getUpdatedInputItems().keySet()) {
      LOG.debug("Key<out> is '" + outputKey + "'");
      for (String outputRow : dataManagerResult.getUpdatedInputItems().get(outputKey)) {
        LOG.debug("Value<out> is '" + outputRow + "'");
      }
    }
    LOG.debug("----------------------------------");
    ////////////////////////////////////////////////////////////////////////////////
    return dataManagerResult;
  }

  private static String getStringFromInputStream(InputStream is) {

    BufferedReader br = null;
    StringBuilder sb = new StringBuilder();

    String line;
    try {

      br = new BufferedReader(new InputStreamReader(is));
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }

    } catch (IOException e) {
      LOG.error(e);
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          LOG.error(e);
        }
      }
    }

    return sb.toString();

  }
}
