package com.cgi.eoss.ftep.core.data.manager.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class TestDataManager {
    private final int size_of_somethreadpool = Runtime.getRuntime().availableProcessors();
    private final java.util.concurrent.Executor somethreadpool = java.util.concurrent.Executors.newFixedThreadPool(size_of_somethreadpool);
    private final java.util.concurrent.ExecutorService executorService =
        new java.util.concurrent.ThreadPoolExecutor(
            size_of_somethreadpool, // core thread pool size
            size_of_somethreadpool, // maximum thread pool size
            0L, // time to wait before resizing pool
            java.util.concurrent.TimeUnit.MILLISECONDS, 
            new java.util.concurrent.LinkedBlockingQueue<>(size_of_somethreadpool), //<Runnable>
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
    );

    private static final String esaArr1[] = {"https://scihub.copernicus.eu/dhus/odata/v1/Products('f7f70dd2-5469-49ce-877f-e288f935a558')/$value",
                                            "https://scihub.copernicus.eu/dhus/odata/v1/Products('faafd4a0-b54b-464c-953c-1e9fc2cd6fb3')/$value"};
    private static final String pepsArr1[] = {"http://peps.mapshup.com/resto/collections/S2/948b0873-9b0e-5b0d-98d5-80ea7e9f81f8/download"};
    private static final java.util.concurrent.Callable<DataManagerResult> runnable1 = new java.util.concurrent.Callable<DataManagerResult>() {
        @Override
        public DataManagerResult call() {
            System.out.println("Thread 1 Started");
            DataManager dataManager = new DataManager();
            HashMap<String, ArrayList<String>> inputItemMap = createSampleInputs(esaArr1, pepsArr1);
            DataManagerResult result = dataManager.getData(testDir1, inputItemMap);
            System.out.println("Thread 1 Finished...");
            return result;
        }
    };
    private static final String esaArr2[] = {"https://scihub.copernicus.eu/dhus/odata/v1/Products('f7f70dd2-5469-49ce-877f-e288f935a558')/$value"};
    private static final String pepsArr2[] = {"http://peps.mapshup.com/resto/collections/S2/948b0873-9b0e-5b0d-98d5-80ea7e9f81f8/download",
                                            "http://peps.mapshup.com/resto/collections/S2/a347ef81-7d19-592d-acb4-eaad6840dc3d/download"};
    private static final java.util.concurrent.Callable<DataManagerResult> runnable2 = new java.util.concurrent.Callable<DataManagerResult>() {
        @Override
        public DataManagerResult call() {
            System.out.println("Thread 2 Started");
            DataManager dataManager = new DataManager();
            HashMap<String, ArrayList<String>> inputItemMap = createSampleInputs(esaArr2, pepsArr2);
            DataManagerResult result = dataManager.getData(testDir2, inputItemMap);
            System.out.println("Thread 2 Finished...");
            return result;
        }
    };

    private static String testDir1 = "E:\\temp1\\inDir";
    private static String testDir2 = "E:\\temp2\\inDir";
    private static final String testLink1 = "http://localhost:8082/thredds/fileServer/ziptest/S2A_OPER_PRD_MSIL1C_PDMC_20160624T053223_R103_V20160624T020653_20160624T020653.SAFE.zip";
    private static final String testLink2 = "http://localhost:8082/thredds/fileServer/ziptest/S2A_OPER_PRD_MSIL1C_PDMC_20160624T053019_R103_V20160624T020653_20160624T020653.SAFE.zip";

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
        System.out.println("We have a threadpool of size " + size_of_somethreadpool + ".");
        java.util.concurrent.Future<?> runnableFuture1 = executorService.submit(runnable1);
        java.util.concurrent.Future<?> runnableFuture2 = executorService.submit(runnable2);
    }

    private static HashMap<String, ArrayList<String>> createSampleInputs(String[] esaArray, String[] pepsArray) {
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
                //the pool didn't terminate after the second try
            }
        } catch (InterruptedException ex) {
            System.out.println("Interrupting threadpool!");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
            System.out.println("Done!");
        }
    }
}
