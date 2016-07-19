package com.cgi.eoss.ftep.core.data.manager.core;

// Singleton
public class Variables {
  // Singleton instance -- without access method (since not needed)
  private static final Variables variables = new Variables();

  // Providing the Singleton itself
  private Variables() {
    // System.out.println("fn name: Variables()");
    // Initialize the variables
    // TODO -- preferably based on a configuration item (eg. file or REST call)
    init();
    // System.out.println(System.getProperty("user.dir") + " current wd path");
  }

  // --------------------------------------------------------------------------

  // Keys for the credential storage
  public static final String KEY_CRED_CERTPATH = "certpath";
  public static final String KEY_CRED_USER = "user";
  public static final String KEY_CRED_PASS = "pass";
  // Prefix for the REST call getting the access credentials
  public static String REST_URL = "https://192.171.139.83/datasources?searchAuthentication=";
  // Third-party script
//  public static String DOWNLOAD_SCRIPT_PATH = "/home/ivann/rakesh/secp";
  // Directories
//  public static String WORKINGDIR_PATH = "/tmp/datamanager_wd/";
//  public static String TARGET_ROOT_PATH = "/tmp/targetlinks/";
//  public static String CACHE_PATH = "/tmp/cache0/";
  // Cache limits
  public static int DISK_CACHE_NUMBER_LIMIT = 1000;
  public static int DISK_CACHE_REMOVABLE_COUNT = 10;

  private static void init() {
    // System.out.println("fn name: Variables.init()");
    REST_URL = "https://192.171.139.83/datasources?searchAuthentication=";
//    DOWNLOAD_SCRIPT_PATH = "/home/ivann/rakesh/secp";
//    WORKINGDIR_PATH = "/tmp/datamanager_wd/";
//    TARGET_ROOT_PATH = "/tmp/targetlinks/";
//    CACHE_PATH = "/tmp/cache0/";
    DISK_CACHE_NUMBER_LIMIT = 1000;
    DISK_CACHE_REMOVABLE_COUNT = 10;
  }
}
