package com.cgi.eoss.ftep.core.data.manager.core;

// symlinks created by this module are regardless of elements removed

// idea: class that handles 'tasks' for downloading and another class having
// multiple instances which are representing the download-task-in-progress

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
 * from the input hashmap --> maintain current ant recent downloads list (*) get each urls into FIFO
 * queue, if inside, skip, else download also have a list for the actual job for the symlinking of
 * the files check url and decide on type --> different auth is required from database update
 * progress or link it if started/done
 */
public class DataManager {
  private boolean hasSkippedEntry = false;
  private boolean hasManagedEntry = false;

  private java.util.ArrayList<String> transformUrlsIntoSymlinks(
      java.util.ArrayList<String> listOfInputUrlsByRow, String jobdir) {
    System.out.println("fn name: transformUrlsIntoSymlinks(); params: list len '"
        + listOfInputUrlsByRow.size() + "' jobdir '" + jobdir + "'");
    java.util.ArrayList<String> resultSymlinksByRow = new java.util.ArrayList();
    for (String oneUrlInRow : listOfInputUrlsByRow) {
      String pathToSymlink = null;
      if (!CacheManager.getInstance().checkIfUrlIsInRecentsList(oneUrlInRow)) {
        pathToSymlink = checkUrlAndGetCredentials(oneUrlInRow, jobdir);
      } else {
        if (CacheManager.getInstance().alreadyManagedToDownload(oneUrlInRow)) {
          // this is regardless of already removed items!
          pathToSymlink =
              CacheManager.getInstance().getSymlinkForAlreadyExistingUrl(oneUrlInRow, jobdir);
          hasManagedEntry = true;
        } else {
          hasSkippedEntry = true;
        }
      }
      if (null != pathToSymlink) {
        resultSymlinksByRow.add(pathToSymlink);
      }
    }
    return resultSymlinksByRow;
  }

  private String checkUrlAndGetCredentials(String oneUrlInRow, String jobdir) {
    System.out.println("fn name: checkUrlAndGetCredentials(); params: oneUrlInRow '" + oneUrlInRow
        + "' jobdir '" + jobdir + "'");
    String restUrl = "https://192.171.139.83/datasources?searchAuthentication="
        + oneUrlInRow.split("://")[1].split("/")[0];
    java.util.HashMap<String, String> credentials = getCredentialsRestCall(restUrl);
    return downloadAndUnpackZips(oneUrlInRow, credentials.get("certpath"), credentials.get("user"),
        credentials.get("pass"), jobdir);
  }

  private java.util.HashMap getCredentialsRestCall(String restUrl) {
    System.out.println("fn name: getCredentialsRestCall(); params: restUrl '" + restUrl + "'");
    java.util.HashMap<String, String> credentials = new java.util.HashMap<>();
    // restUrl; DO SOMETHING
    credentials.put("certpath", null);
    credentials.put("user", "");
    credentials.put("pass", "");
    return credentials;
  }

  private String downloadAndUnpackZips(String oneUrlInRow, String certPath, String user,
      String pass, String jobdir) {
    System.out.println(
        "fn name: downloadAndUnpackZips(); params: oneUrlInRow '" + oneUrlInRow + "' jobdir '"
            + jobdir + "' certPath '" + certPath + "' user '" + user + "' pass '" + pass + "'");
    String symlinkForUrl = null;
    // start authenticated download USING SECP SCRIPT
    String shellScript = Variables.DOWNLOAD_SCRIPT_PATH;
    String params1 = " -f -o ";
    String paramsOutDir = jobdir;
    String params2 = " -c -U -r 1 -rt 5 -s ";
    String paramsCredUser = user;
    String paramsCredPass = pass;
    String paramsCredentials = "-C " + paramsCredUser + ":" + paramsCredPass;
    if (null != certPath) {
      // get and use cert!
      paramsCredentials = "-C ?:?";
    }
    // String paramsLinks = "
    // http://localhost:8082/thredds/fileServer/ziptest/S2A_OPER_PRD_MSIL1C_PDMC_20160624T053223_R103_V20160624T020653_20160624T020653.SAFE.zip
    // http://localhost:8082/thredds/fileServer/ziptest/S2A_OPER_PRD_MSIL1C_PDMC_20160624T053019_R103_V20160624T020653_20160624T020653.SAFE.zip";
    String paramsLinks = " " + oneUrlInRow;
    String command =
        shellScript + params1 + paramsOutDir + params2 + paramsCredentials + paramsLinks;
    String zipFile = null;
    int exitCode = -1;
    try {
      // Downloading:
      Process scriptProcessCall = Runtime.getRuntime().exec(command);
      // ProcessBuilder scriptProcessCallBuilder = new ProcessBuilder(command);
      // scriptProcessCallBuilder.redirectErrorStream(true);
      // Process scriptProcessCall = scriptProcessCallBuilder.start();
      scriptProcessCall.waitFor();
      exitCode = scriptProcessCall.exitValue();
      if (0 != exitCode) { // on fail:
        // System.out.println("** ** dwnld fail");
        hasSkippedEntry = true;
        CacheManager.getInstance().addToRecentlyDownloadedList(oneUrlInRow, false, "");
      } else { // on success:
        // System.out.println("** ** dwnld succ");
        //// String scriptOutput = "";
        try (java.io.BufferedReader outputReader = new java.io.BufferedReader(
            new java.io.InputStreamReader(scriptProcessCall.getInputStream()))) {
          String outputLine;
          while ((outputLine = outputReader.readLine()) != null) {
            System.out.println("** ** line read: '" + outputLine + "'");
            if (outputLine.contains("downloaded item: '")) {
              zipFile = outputLine.split("downloaded item: '")[1].split("'")[0];
              //// System.out.println("** ** miao '" + zipFile + "'");
            }
            //// scriptOutput += outputLine;
          }
        } catch (final Exception e) {
          //
        }
        // unpack zips, get content's list
        String mainFolderDownloadLocation = CacheManager.getInstance().unzipFile(zipFile);
        symlinkForUrl =
            CacheManager.getInstance().createSymlink(mainFolderDownloadLocation, jobdir);
        // if managed to download, add to the list!
        if (null != symlinkForUrl) {
          hasManagedEntry = true;
          CacheManager.getInstance().addToRecentlyDownloadedList(oneUrlInRow, true, symlinkForUrl);
        }
        System.out.println("** ** managed to dwnld: " + zipFile);
      }
    } catch (java.io.IOException | InterruptedException e) {
      System.out.println("Could not execute command '" + command + "'");
    }
    /*
     * ~~> parse the output for the filename!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! then unzip it ////secp
     * needs a fix: "curl --remote-name -O -J" at 3 places!!!! //+lines from 402 to 420 #Try to
     * download the file target_file=`echo $2 | rev | cut -f1 -d/ | rev` message=
     * "`curl --remote-name -O -J $curlopt -o "$2" "$1" 2>&1`" res=$? if [[ "$res" -ne "0" ]]; then
     * if [[ "${message/The requested URL returned error: 403//}" != "$message" ]]; then echo
     * "[ERROR  ][secp] Forbidden. Please check your proxy certificate or your username/password."
     * 1>&2 res=2 elif [[ "${message/The requested URL returned error: 404//}" != "$message" ]];
     * then res=1 else echo "[ERROR  ][secp][failed] url '$URI' - $message" 1>&2 fi rm -f "$2"
     * return $res fi mv $target_file $2 //!! ./secp -f -o /home/ivann/rakesh/dwnl/odir -c -U -r 1
     * -rt 5 -s -C tomcat:tomcat http://localhost:8082/thredds/fileServer/ziptest/
     * S2A_OPER_PRD_MSIL1C_PDMC_20160624T053223_R103_V20160624T020653_20160624T020653.SAFE.zip
     * http://localhost:8082/thredds/fileServer/ziptest/
     * S2A_OPER_PRD_MSIL1C_PDMC_20160624T053019_R103_V20160624T020653_20160624T020653.SAFE.zip
     * ////exit code: 0 all URLs were successfully downloaded secp _CREATE_OUTPUT_DIR true false
     * _OUTPUT_DIR "/path/somedir/" _INPUT_URI "somelink?" _UNCOMPRESS false or it will uncompress
     * .gz files! _OVERWRITE false true probably better not to overwrite _USER_USERNAME
     * _USER_PASSWORD -q quiet mode, local filenames are not echoed to stdout after transfer -f
     * force transfer of a physical copy of the file in case of nfs URLs -F <url-list> get URLs from
     * the <url-list> file -o|O <out-dir> defines the output directory for transfers (default is
     * $PWD) -o outdir with -O the sink files or directories possibly existing in the output -O
     * ovrwrt directory will be overwritten. -c creates the output directory if it does not exist -p
     * <prefix> prepend the given prefix to all output names -U|--no-uzip disable file automatic
     * decompression of .gz, .tgz and .zip files. -r <num-retries> defines the maximum number of
     * retries (default is 5) -R do not retry transfer after timeout -s skip download if sink path
     * already exists -K private mode, disable storing of authentication session in ~/.secp_sess
     * file and passwords in the ~/.secp_cred file. -C <user>:<pass> force <user> and <pass>
     * authentication (NOTE: these passwords will be stored in clear text in the ~/.secp_cred file.
     * NOTE: If username/passowrd is specified it has precedence over the certificate/proxy
     * authentication)
     */
    return symlinkForUrl;
  }

  private DataManagerResult.DataDownloadStatus calculateDownloadStatus() {
    System.out.println("fn name: calculateDownloadStatus(); params: -");
    // only hasSkippedEntry NONE,
    DataManagerResult.DataDownloadStatus calculatedStatus =
        DataManagerResult.DataDownloadStatus.NONE;
    if (hasManagedEntry) {
      // having both skipped and managed then PARTIAL;
      if (hasSkippedEntry) {
        calculatedStatus = DataManagerResult.DataDownloadStatus.PARTIAL;
      } else {
        // only hasManagedEntry COMPLETE!
        calculatedStatus = DataManagerResult.DataDownloadStatus.COMPLETE;
      }
    }
    return calculatedStatus;
  }

  // --------------------------------------------------------------------------

  // key + http(s)://urls.zip
  public DataManagerResult getData(String destDir,
      java.util.HashMap<String, java.util.ArrayList<String>> inputUrlListsWithJobID) {
    System.out.println("fn name: getData(); params: destDir '" + destDir + "' map size '"
        + inputUrlListsWithJobID.size() + "'");
    // to track the whole progress of one job having multiple IDs and Lists
    DataManagerResult dataManagerResult = new DataManagerResult();
    java.util.HashMap<String, java.util.ArrayList<String>> symlinksResultMap =
        new java.util.HashMap();
    for (String jobListRowID : inputUrlListsWithJobID.keySet()) {
      java.util.ArrayList<String> listOfInputUrlsByRow = inputUrlListsWithJobID.get(jobListRowID);
      System.out.println("Sub-List, job <row> ID: " + jobListRowID);
      symlinksResultMap.put(jobListRowID, transformUrlsIntoSymlinks(listOfInputUrlsByRow, destDir));
    }
    dataManagerResult.setUpdatedInputItems(symlinksResultMap);
    dataManagerResult.setDownloadStatus(calculateDownloadStatus());
    return dataManagerResult;
  }
}
