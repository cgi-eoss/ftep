package com.cgi.eoss.ftep.core.data.manager.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

@SuppressWarnings("Convert2Lambda")
public class TestDataManager {
  // Limit for the threadpool -- telling how many parallel processes can be executed at once
  // TODO -- can be less than the available processor core count
  private final int size_of_somethreadpool = Runtime.getRuntime().availableProcessors();

  // Time to eait before resizing pool -- not used (yet)
  private static final long NO_WAIT_TIMER = 0L;

  // Threadpool -- providing parallel access
  private final java.util.concurrent.ExecutorService executorService =
      // The core and maximum threadpool size set equally
      new java.util.concurrent.ThreadPoolExecutor(size_of_somethreadpool, size_of_somethreadpool,
          NO_WAIT_TIMER, java.util.concurrent.TimeUnit.MILLISECONDS,
          new java.util.concurrent.LinkedBlockingQueue<>(size_of_somethreadpool),
          new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

  ////////////////////////////////////////////////////////////////////////////////
  // Example calls as follows
  private static final String esaArr1[] = {
      "https://scihub.copernicus.eu/dhus/odata/v1/Products('f7f70dd2-5469-49ce-877f-e288f935a558')/$value",
      "https://scihub.copernicus.eu/dhus/odata/v1/Products('faafd4a0-b54b-464c-953c-1e9fc2cd6fb3')/$value"};
  private static final String pepsArr1[] = {
      "http://peps.mapshup.com/resto/collections/S2/948b0873-9b0e-5b0d-98d5-80ea7e9f81f8/download"};
  private static final java.util.concurrent.Callable<DataManagerResult> runnable1 =
      new java.util.concurrent.Callable<DataManagerResult>() {
        @Override
        public DataManagerResult call() {
          // System.out.println("Thread 1 Started");
          DataManager dataManager = new DataManager();
          HashMap<String, ArrayList<String>> inputItemMap = createSampleInputs(esaArr1, pepsArr1);
          DataManagerResult result = dataManager.getData(testDir1, inputItemMap);
          // System.out.println("Thread 1 Finished...");
          return result;
        }
      };
  ////////////////////////////////////////////////////////////////////////////////
  private static final String esaArr2[] = {
      "https://scihub.copernicus.eu/dhus/odata/v1/Products('f7f70dd2-5469-49ce-877f-e288f935a558')/$value"};
  private static final String pepsArr2[] = {
      "http://peps.mapshup.com/resto/collections/S2/948b0873-9b0e-5b0d-98d5-80ea7e9f81f8/download",
      "http://peps.mapshup.com/resto/collections/S2/a347ef81-7d19-592d-acb4-eaad6840dc3d/download"};
  private static final java.util.concurrent.Callable<DataManagerResult> runnable2 =
      new java.util.concurrent.Callable<DataManagerResult>() {
        @Override
        public DataManagerResult call() {
          // System.out.println("Thread 2 Started");
          DataManager dataManager = new DataManager();
          HashMap<String, ArrayList<String>> inputItemMap = createSampleInputs(esaArr2, pepsArr2);
          DataManagerResult result = dataManager.getData(testDir2, inputItemMap);
          // System.out.println("Thread 2 Finished...");
          return result;
        }
      };
  ////////////////////////////////////////////////////////////////////////////////
  private static final String testDir1 = "/home/ftep/01-Data/cems/jobIn";
  private static final String testDir2 = "/tmp/inDir2";
  private static final String testLink1 =
      "http://localhost:8082/thredds/fileServer/ziptest/S2A_OPER_PRD_MSIL1C_PDMC_20160624T053223_R103_V20160624T020653_20160624T020653.SAFE.zip";
  private static final String testLink2 =
      "http://localhost:8082/thredds/fileServer/ziptest/S2A_OPER_PRD_MSIL1C_PDMC_20160624T053019_R103_V20160624T020653_20160624T020653.SAFE.zip";
  private static final String esaArrSd[] = {"ftp://ftp.ceda.ac.uk/neodc/sentinel1a/data/EW/L1_GRD/m/IPF_v2/2016/01/04/S1A_EW_GRDM_1SDH_20160104T183014_20160104T183110_009348_00D849_944A.zip"};
  private static final String pepsArrSd[] = {};
//      {"http://localhost:8082/thredds/fileServer/testEnhanced/2004050312_eta_211.nc", testLink1};
  private static final java.util.concurrent.Callable<DataManagerResult> smalldemo =
      new java.util.concurrent.Callable<DataManagerResult>() {
        @Override
        public DataManagerResult call() {
           System.out.println("Thread smalldemo Started");
          DataManager dataManager = new DataManager();
          HashMap<String, ArrayList<String>> inputItemMap = createSampleInputs(esaArrSd, pepsArrSd);
          DataManagerResult result = dataManager.getData(testDir1, inputItemMap);
           System.out.println("Thread smalldemo Finished...");
          return result;
        }
      };
  ////////////////////////////////////////////////////////////////////////////////

  public static void main(String[] args) {
    // It worth changing the working directory so the in-progress downloads will happen here
    // System.out.println("fn name: main();");
    // Create a working directory wherein the download-in-progress operation could be done
    java.io.File newPathItem = new java.io.File(Variables.WORKINGDIR_PATH);
    if (!newPathItem.exists()) {
      newPathItem.mkdir();
    }
    // Set the JVM's working directory
    System.setProperty("user.dir", Variables.WORKINGDIR_PATH);
     System.out.println(System.getProperty("user.dir") + " current wd path");
    // Instantiate the current class
    TestDataManager testDataManager = new TestDataManager();
    // Start the test
    testDataManager.startTest();
    // Shut the program down when the tests are finished
    testDataManager.stopTest();
  }

  private static HashMap<String, ArrayList<String>> createSampleInputs(String[] esaArray,
      String[] pepsArray) {
    HashMap<String, ArrayList<String>> inputItemMap = new HashMap<>();
    ArrayList<String> sentinelDataEsa = new ArrayList<>();
    sentinelDataEsa.addAll(Arrays.asList(esaArray));
    ArrayList<String> sentinelDataPeps = new ArrayList<>();
    sentinelDataPeps.addAll(Arrays.asList(pepsArray));
    inputItemMap.put("inputCollection1", sentinelDataEsa);
    inputItemMap.put("inputCollection2", sentinelDataPeps);
    return inputItemMap;
  }

  private void startTest() {
    // System.out.println("We have a threadpool of size " + size_of_somethreadpool + ".");
    // java.util.concurrent.Future<?> runnableFuture1 = executorService.submit(runnable1);
    // java.util.concurrent.Future<?> runnableFuture2 = executorService.submit(runnable2);
    java.util.concurrent.Future<?> smalldemoFuture = executorService.submit(smalldemo);
  }

  private void stopTest() {
    // Initiate shutdown -- the pool will not accept new requests
    executorService.shutdown();
    try {
      // System.out.println("Stopping test...");
      // Can be waiting some time for the termination
      if (!executorService.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
        // System.out.println("60 sec passed, calling shutdownNow...");
        // Immediate shutdown request set
        executorService.shutdownNow();
      }
      // Second try if necessary
      if (!executorService.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
        // If the pool didn't terminate after the second try
        // TODO -- The catch branch probably will not be executed, handle error if needed
        // System.out.println("Another 60 sec passed... What happens now?");
        // Try and interrupt the thread
        Thread.currentThread().interrupt();
      }
    } catch (InterruptedException ex) {
      // System.out.println("Interrupting threadpool!");
      // Immediate shutdown request
      executorService.shutdownNow();
      // Interruption to the thread
      Thread.currentThread().interrupt();
      // System.out.println("Done!");
    }
  }
}
