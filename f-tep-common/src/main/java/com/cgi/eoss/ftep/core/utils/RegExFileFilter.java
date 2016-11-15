package com.cgi.eoss.ftep.core.utils;

import java.io.File;
import java.io.FilenameFilter;

public class RegExFileFilter implements FilenameFilter {

  // default match all files
  private String regularExpression = "*";

  public RegExFileFilter() {}

  public RegExFileFilter(final String regEx) {
    regularExpression = regEx;
  }

  @Override
  public boolean accept(File dir, String name) {
    return name.matches(regularExpression.replace(".", "\\.").replace("*", ".*"));
  }

}
