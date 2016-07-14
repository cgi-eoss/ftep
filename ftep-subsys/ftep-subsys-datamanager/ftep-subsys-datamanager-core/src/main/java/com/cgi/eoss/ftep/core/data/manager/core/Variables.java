package com.cgi.eoss.ftep.core.data.manager.core;

public class Variables {
  private static final Variables variables = new Variables();

  private Variables() {
    System.out.println("fn name: Variables()");
    init();
  }

  // --------------------------------------------------------------------------

  public static String DOWNLOAD_SCRIPT_PATH = "/home/ivann/rakesh/secp";
  public static String TARGET_ROOT_PATH = "/tmp/targetlinks/";
  public static String CACHE_PATH = "/tmp/cache0/";
  public static int DISK_CACHE_NUMBER_LIMIT = 1000;
  public static int DISK_CACHE_REMOVABLE_COUNT = 10;

  private static void init() {
    System.out.println("fn name: Variables.init()");
    DOWNLOAD_SCRIPT_PATH = "/home/ivann/rakesh/secp";
    DISK_CACHE_NUMBER_LIMIT = 1000;
    DISK_CACHE_REMOVABLE_COUNT = 10;
    TARGET_ROOT_PATH = "/tmp/targetlinks/";
    CACHE_PATH = "/tmp/cache0/";
  }
}
