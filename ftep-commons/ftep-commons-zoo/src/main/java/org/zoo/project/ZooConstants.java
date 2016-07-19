package org.zoo.project;

public final class ZooConstants {

  private ZooConstants() {}


  // ZOO WPS server constants
  public static final Integer WPS_SERVICE_SUCCEEDED = 3;

  public static final Integer WPS_SERVICE_FAILED = 4;

  // ZOO configuration maps
  public static final String ZOO_MAIN_CFG_MAP = "main";
  
  public static final String ZOO_ENV_CFG_MAP = "env";

  public static final String ZOO_LENV_CFG_MAP = "lenv";

  public static final String ZOO_SENV_CFG_MAP = "senv";

  public static final String ZOO_RENV_CFG_MAP = "renv";

  public static final String ZOO_FTEP_CFG_MAP = "ftep";

  // ZOO configuration parameters
  public static final String ZOO_MAIN_CACHE_DIR_PARAM = "cacheDir";
  
  public static final String ZOO_LENV_USID_PARAM = "usid";

  public static final String ZOO_LENV_UUSID_PARAM = "uusid";

  public static final String ZOO_LENV_IDENTIFIER_PARAM = "Identifier";

  public static final String ZOO_LENV_MESSAGE_PARAM = "message";

  public static final String ZOO_FTEP_DATA_DOWNLOAD_DIR_PARAM = "dataDownloadDir";

  public static final String ZOO_FTEP_LOG4J_FILENAME_PARAM = "log4jPropertyFile";

  public static final String ZOO_FTEP_WORKER_VM_IP_ADDR_PARAM = "workerVmIpAddr";
  
  public static final String ZOO_FTEP_DOWNLOAD_TOOL_PATH_PARAM = "dataDownloadTool";

  public static final String ZOO_RENV_SSO_USERID_PARAM = "HTTP_EOSSO_PERSON_COMMONNAME";

}
