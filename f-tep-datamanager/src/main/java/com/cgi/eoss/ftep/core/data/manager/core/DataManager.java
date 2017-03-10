package com.cgi.eoss.ftep.core.data.manager.core;

import com.cgi.eoss.ftep.core.utils.DBRestApiManager;
import com.cgi.eoss.ftep.core.utils.FtepConstants;
import lombok.extern.log4j.Log4j2;
import org.zoo.project.ZooConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
@Log4j2
public class DataManager {

    private boolean hasSkippedEntry = false;
    private boolean hasManagedEntry = false;
    // Domain is paired with access information for the current session
    private final Map<String, Map<String, String>> credentials = new HashMap<>();

    private String downloadDir;

    private CacheManager cacheManagerIns;

    private String scriptPath;

    private List<String> transformUrlsIntoSymlinks(List<String> listOfInputUrlsByRow, String jobdir) {
        // Take one key's values from the initial HashMap which results in an ArrayList
        List<String> resultSymlinksByRow = new ArrayList<>();
        for (String oneUrlInRow : listOfInputUrlsByRow) {
            // Process each link one by one from the ArrayList
            String pathToSymlink = null;
            // Replace all dollar sign to %24 value with escaped % sign + Escape all apostrophe
            oneUrlInRow = oneUrlInRow.replaceAll("\\$", "\\%24").replaceAll("\\'", "\\%27");
            // Escape all parentheses
            oneUrlInRow = oneUrlInRow.replaceAll("\\(", "\\%28").replaceAll("\\)", "\\%29");
            //// oneUrlInRow = oneUrlInRow.replaceAll("\\-", "\\\\%2D");
            if (!cacheManagerIns.checkIfUrlIsInRecentsList(oneUrlInRow)) {
                // New content
                pathToSymlink = checkUrlAndGetCredentials(oneUrlInRow, jobdir);
            } else {
                // Already downloaded content
                if (cacheManagerIns.alreadyManagedToDownload(oneUrlInRow)) {
                    // Successful previous trial -- regardless of already removed items!
                    pathToSymlink = cacheManagerIns.getSymlinkForAlreadyExistingUrl(oneUrlInRow, jobdir);
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
        if (null == oneUrlInRow || oneUrlInRow.length() < 1) {
            return null;
        }
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
        LOG.debug("fn name: getCredentialsRestCall(); params: restUrl '{}'", domain);
        // The credentials should have user/password and/or certificate content
        if (!credentials.containsKey(domain)) {
            String httpEndpoint = FtepConstants.DATASOURCE_QUERY_ENDPOINT + domain;

            LOG.debug("Getting credentials from endpoint {}", httpEndpoint);
            DBRestApiManager dataBaseMgr = DBRestApiManager.getInstance();
            credentials.put(domain, dataBaseMgr.getCredentials(httpEndpoint));
        }
        // Return the acquired credential-details as key-value pairs
        return credentials.get(domain);
    }

    private String downloadAndUnpackZips(String oneUrlInRow, String certPath, String user,
                                         String pass, String jobdir) {
        // If the symlink will be returned as null it means the URL was broken
        String symlinkForUrl = null;
        // Start authenticated download using a modified third-party script

        String params1 = " -f -o ";
        String paramsOutDir = downloadDir;
        String params2 = " -c -U -r 1 -rt 5 -s ";
        String paramsCredUser = user != null ? user : "?";
        String paramsCredPass = pass != null ? pass : "?";
        String paramsCredentials = "-C " + paramsCredUser + ":" + paramsCredPass;
        // TODO -- the parameters should be customized for certificate-based downloads!
        if (null != certPath) {
            paramsCredentials = "-C ?:?";
        }
        String paramsLinks = " " + oneUrlInRow;
        // Put the Strings together to form the command -- bash -x needed to get the filename
        String command = "bash -x " + scriptPath + params1 + paramsOutDir + params2 + paramsCredentials
                + paramsLinks;
        LOG.debug("Command: '{}'", command);
        String zipFile = "";
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
            LOG.debug("Starting SECP script execution at: {}", LocalDateTime.now());
            Process scriptProcessCall = Runtime.getRuntime().exec(command, null, new File(downloadDir));

            List<String> errorLogs;
            List<String> outputLogs;
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(scriptProcessCall.getErrorStream()));
                 BufferedReader outputReader = new BufferedReader(new InputStreamReader(scriptProcessCall.getInputStream()))
            ) {
                // Wait for until the script terminates
                scriptProcessCall.waitFor();
                LOG.debug("Completed SECP script execution at: {}", LocalDateTime.now());

                errorLogs = errorReader.lines().collect(Collectors.toList());
                outputLogs = outputReader.lines().collect(Collectors.toList());
            }

            LOG.debug("+++++++++++++++--------- SECP script stderr logs ----------++++++++++++++++++");
            errorLogs.forEach(LOG::debug);
            LOG.debug("+++++++++++++++--------- SECP script stdout logs ----------++++++++++++++++++");
            outputLogs.forEach(LOG::debug);
            LOG.debug("+++++++++++++++----------------------------------++++++++++++++++++");

            // When ready check the exit code
            int exitVal = scriptProcessCall.exitValue();

            if (0 != exitVal) {
                // On fail
                LOG.error("Data download failed with exit value: {} for {}", exitVal, oneUrlInRow);
                hasSkippedEntry = true;
                cacheManagerIns.addToRecentlyDownloadedList(oneUrlInRow, false, "");
            } else {
                // On success
                LOG.debug("Data successfully downloaded for {}", oneUrlInRow);
                // while ((outputLine = outputReader.readLine()) != null) {
                for (String outputLine : outputLogs) {
                    //// Saved to filename
                    //// '\''S2A_OPER_PRD_MSIL1C_PDMC_20160624T033158_R102_V20160624T004855_20160624T004855.zip'\'''
                    // Content-Disposition: inline;filename=".."
                    if (outputLine.contains("Saved to filename")) {
                        LOG.debug(outputLine);
                        // if (outputLine.contains("Content-Disposition: inline;filename")) {
                        zipFile = outputLine.split("Saved to filename '")[1].split("'")[0];
                        // zipFile = outputLine.split("Content-Disposition:
                        // inline;filename=\"\"")[1].split("\"")[0];
                        LOG.debug("Downloaded file's name: '{}'", zipFile);
                        Process mvProcessCall = Runtime.getRuntime().exec("mv " + zipFile + " " + downloadDir,
                                null, new File(downloadDir));
                        mvProcessCall.waitFor();
                        break;
                    }
                }

                LOG.debug("Managed to download '{}'", zipFile);
                // Unpack if ZIP, get main item's location
                String mainFolderDownloadLocation = cacheManagerIns.unzipFile(zipFile);
                if (null == mainFolderDownloadLocation) {
                    hasSkippedEntry = true;
                    // When unpack failed in case of ZIP file
                    cacheManagerIns.addToRecentlyDownloadedList(oneUrlInRow, false, "");
                } else {
                    hasManagedEntry = true;
                    // If managed to download (and unpack) the item add to the download-list
                    cacheManagerIns.addToRecentlyDownloadedList(oneUrlInRow, true,
                            mainFolderDownloadLocation);
                }
                // Create the symlink for the main item in the jobdir and return the symlink's path
                LOG.debug("Creating Symlink for '{}'!", mainFolderDownloadLocation);
                symlinkForUrl = cacheManagerIns.createSymlink(mainFolderDownloadLocation, jobdir);
            }
        } catch (Exception e) {
            LOG.error("Could not execute command '{}'", command, e);
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

    public DataManagerResult getData(Map<String, String> downloadConfMap, String destDir,
                                     Map<String, List<String>> inputUrlListsWithJobID) {
        cacheManagerIns = CacheManager.getInstance();
        downloadDir = downloadConfMap.get(ZooConstants.ZOO_FTEP_DATA_DOWNLOAD_DIR_PARAM);
        downloadDir = downloadDir + "/";
        cacheManagerIns.setDownloadDir(downloadDir);
        scriptPath = downloadConfMap.get(ZooConstants.ZOO_FTEP_DOWNLOAD_TOOL_PATH_PARAM);

        // The inputUrlListsWithJobID has more ID+list pairs look like "key" + "http(s)://urls.zip"
        LOG.debug("fn name: getData(); params: destDir '{}' map size '{}'", destDir, inputUrlListsWithJobID.size());
        // Aggregated result to track the whole progress of one job ("inputUrlListsWithJobID")
        DataManagerResult dataManagerResult = new DataManagerResult();
        // UpdatedInputItems for the DataManagerResult object
        Map<String, List<String>> symlinksResultMap = new HashMap<>();
        // For each key
        for (String jobListRowID : inputUrlListsWithJobID.keySet()) {
            // Get one "row" (key) at a time and process its URLs in the ArrayList (value)
            List<String> listOfInputUrlsByRow = inputUrlListsWithJobID.get(jobListRowID);
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
            LOG.debug("Key<in> is '{}'", inputKey);
            for (String inputRow : inputUrlListsWithJobID.get(inputKey)) {
                LOG.debug("Value<in> is '{}'", inputRow);
            }
        }
        for (String outputKey : dataManagerResult.getUpdatedInputItems().keySet()) {
            LOG.debug("Key<out> is '{}'", outputKey);
            for (String outputRow : dataManagerResult.getUpdatedInputItems().get(outputKey)) {
                LOG.debug("Value<out> is '{}'", outputRow);
            }
        }
        LOG.debug("----------------------------------");
        ////////////////////////////////////////////////////////////////////////////////
        return dataManagerResult;
    }

}
