package com.cgi.eoss.ftep.core.data.manager.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

@SuppressWarnings("Convert2Lambda")
public class TestDataManager {
  private final int size_of_somethreadpool = Runtime.getRuntime().availableProcessors();
  // executor.execute(new some_Runnable());
  // https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Executor.html
  // if need to terminate tasks: "Usage Examples" here:
  // https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html
  // tutorial for the latter: http://tutorials.jenkov.com/java-util-concurrent/executorservice.html
  // timeout example:
  // http://winterbe.com/posts/2015/04/07/java8-concurrency-tutorial-thread-executor-examples/
  private final java.util.concurrent.Executor somethreadpool =
      java.util.concurrent.Executors.newFixedThreadPool(size_of_somethreadpool);
  // idea: class TaskType --> two types, one for download and one for queue up taskpacks AKA reqs
  // the pool should handle both types
  // --> new class for the pooling?
  /*
   * public static ExecutorService newFixedThreadPool(int nThreads) { return new
   * ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new
   * LinkedBlockingQueue<Runnable>()); } ArrayBlockingQueue is backed by an array that size will
   * never change after creation. Setting the capacity to Integer.MAX_VALUE would create a big array
   * with high costs in space. ArrayBlockingQueue is always bounded. LinkedBlockingQueue creates
   * nodes dynamically until the capacity is reached. This is by default Integer.MAX_VALUE. Using
   * such a big capacity has no extra costs in space. LinkedBlockingQueue is optionally bounded.
   */
  private final java.util.concurrent.ExecutorService executorService =
      new java.util.concurrent.ThreadPoolExecutor(size_of_somethreadpool, // core thread pool size
          size_of_somethreadpool, // maximum thread pool size
          0L, // time to wait before resizing pool
          java.util.concurrent.TimeUnit.MILLISECONDS,
          // new java.util.concurrent.ArrayBlockingQueue<>(size_of_somethreadpool, true),
          // //<Runnable>
          new java.util.concurrent.LinkedBlockingQueue<>(size_of_somethreadpool), // <Runnable>
          new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

  private static final String esaArr1[] = {
      "https://scihub.copernicus.eu/dhus/odata/v1/Products('f7f70dd2-5469-49ce-877f-e288f935a558')/$value",
      "https://scihub.copernicus.eu/dhus/odata/v1/Products('faafd4a0-b54b-464c-953c-1e9fc2cd6fb3')/$value"};
  private static final String pepsArr1[] = {
      "http://peps.mapshup.com/resto/collections/S2/948b0873-9b0e-5b0d-98d5-80ea7e9f81f8/download"};
  // private static final Runnable runnable1 = new Runnable() {
  private static final java.util.concurrent.Callable<DataManagerResult> runnable1 =
      new java.util.concurrent.Callable<DataManagerResult>() {
        @Override
        // public void run() {
        public DataManagerResult call() {
          System.out.println("Thread 1 Started");
          DataManager dataManager = new DataManager();
          HashMap<String, ArrayList<String>> inputItemMap = createSampleInputs(esaArr1, pepsArr1);
          // dataManager.getData(new File(testDir1), inputItemMap);
          //// providing a List of Lists:
          //// DataManagerResult result = dataManager.getData(testDir1, new
          // java.util.ArrayList(inputItemMap.values()));
          DataManagerResult result = dataManager.getData(testDir1, inputItemMap);
          System.out.println("Thread 1 Finished...");
          return result;
        }
      };
  private static final String esaArr2[] = {
      "https://scihub.copernicus.eu/dhus/odata/v1/Products('f7f70dd2-5469-49ce-877f-e288f935a558')/$value"};
  private static final String pepsArr2[] = {
      "http://peps.mapshup.com/resto/collections/S2/948b0873-9b0e-5b0d-98d5-80ea7e9f81f8/download",
      "http://peps.mapshup.com/resto/collections/S2/a347ef81-7d19-592d-acb4-eaad6840dc3d/download"};
  // private static final Runnable runnable2 = new Runnable() {
  private static final java.util.concurrent.Callable<DataManagerResult> runnable2 =
      new java.util.concurrent.Callable<DataManagerResult>() {
        @Override
        // public void run() {
        public DataManagerResult call() {
          System.out.println("Thread 2 Started");
          DataManager dataManager = new DataManager();
          HashMap<String, ArrayList<String>> inputItemMap = createSampleInputs(esaArr2, pepsArr2);
          // dataManager.getData(new File(testDir2), inputItemMap);
          //// providing a List of Lists:
          //// DataManagerResult result = dataManager.getData(testDir2, new
          // java.util.ArrayList(inputItemMap.values()));
          DataManagerResult result = dataManager.getData(testDir2, inputItemMap);
          System.out.println("Thread 2 Finished...");
          return result;
        }
      };

  private static String testDir1 = "E:\\temp1\\inDir";
  private static String testDir2 = "E:\\temp2\\inDir";
  private static final String testLink1 =
      "http://localhost:8082/thredds/fileServer/ziptest/S2A_OPER_PRD_MSIL1C_PDMC_20160624T053223_R103_V20160624T020653_20160624T020653.SAFE.zip";
  private static final String testLink2 =
      "http://localhost:8082/thredds/fileServer/ziptest/S2A_OPER_PRD_MSIL1C_PDMC_20160624T053019_R103_V20160624T020653_20160624T020653.SAFE.zip";
  // private static final String testUserPass = "tomcat";

  private static final String esaArrSd[] = {testLink2};
  private static final String pepsArrSd[] = {};
  // private static final Runnable runnable2 = new Runnable() {
  private static final java.util.concurrent.Callable<DataManagerResult> smalldemo =
      new java.util.concurrent.Callable<DataManagerResult>() {
        @Override
        // public void run() {
        public DataManagerResult call() {
          System.out.println("Thread smalldemo Started");
          DataManager dataManager = new DataManager();
          HashMap<String, ArrayList<String>> inputItemMap = createSampleInputs(esaArrSd, pepsArrSd);
          // dataManager.getData(new File(testDir2), inputItemMap);
          //// providing a List of Lists:
          //// DataManagerResult result = dataManager.getData(testDir2, new
          // java.util.ArrayList(inputItemMap.values()));
          DataManagerResult result = dataManager.getData(testDir1, inputItemMap);
          System.out.println("Thread smalldemo Finished...");
          return result;
        }
      };

  public static void main(String[] args) {
    testDir1 = "C:\\NinaFolder\\inDir1";
    testDir2 = "C:\\NinaFolder\\inDir2";
    testDir1 = "/tmp/inDir1";
    testDir2 = "/tmp/inDir2";
    TestDataManager testDataManager = new TestDataManager();
    testDataManager.startTest();
    testDataManager.stopTest();
  }

  private void startTest() {
    // runnable1.run();
    // runnable2.run();
    System.out.println("We have a threadpool of size " + size_of_somethreadpool + ".");
    // java.util.concurrent.Future<?> runnableFuture1 = executorService.submit(runnable1);
    // java.util.concurrent.Future<?> runnableFuture2 = executorService.submit(runnable2);
    java.util.concurrent.Future<?> smalldemoFuture2 = executorService.submit(smalldemo);
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

  private void stopTest() {
    executorService.shutdown();
    try {
      System.out.println("Stopping test...");
      if (!executorService.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
        System.out.println("60 sec passed, calling shutdownNow...");
        executorService.shutdownNow();
      }
      if (!executorService.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
        // the pool didn't terminate after the second try
        // this probably does not cause it to execute the catch branch
        System.out.println("Another 60 sec passed... What happens now?");
      }
    } catch (InterruptedException ex) {
      System.out.println("Interrupting threadpool!");
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
      System.out.println("Done!");
    }
  }
}
