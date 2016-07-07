package com.cgi.eoss.ftep.core.requesthandler.utils;

public final class FtepConstants {

  private FtepConstants() {}

  public static final String WPS_SIMULATE = "simulateWPS";

  public static final String JOB_DIR_PREFIX = "Job_";

  public static final String JOB_INPUT_DIR = "inDir";

  public static final String JOB_OUTPUT_DIR = "outDir";

  public static final String DOCKER_JOB_MOUNTPOINT = "/home/worker/workDir";

  public static final String DOCKER_CERT_PATH = "/home/ftep/.docker/";

  public static final String DOCKER_API_VERISON = "1.22";

  public static final String WPS_PROP_FILE = "FTEP-WPS-INPUT.properties";

  public static final int VNC_PORT = 5900;

  public static final int GUI_APP_MIN_PORT = 40000;

  public static final int GUI_APP_MAX_PORT = 41000;

  public static final int DOCKER_DAEMON_PORT = 2376;

  public static final String DB_API_LOGIN_INT_ENDPOINT = "https://192.168.3.83/api/v1.0/login";

  public static final String DB_API_JOBTABLE_INT_ENDPOINT = "https://192.168.3.83/api/v1.0/jobs";

  public static final String DB_API_USER = "wps";

  public static final String DB_API_PWD = "b8tB8%&3Hq";

  public static final String HTTP_JSON_CONTENT_TYPE = "application/json";
  
  public static final int HTTP_ERROR_RESPONSE_CODE_RANGE = 399;

  public static final int GUI_APPL_TIMEOUT_MINUTES = 120;

}
