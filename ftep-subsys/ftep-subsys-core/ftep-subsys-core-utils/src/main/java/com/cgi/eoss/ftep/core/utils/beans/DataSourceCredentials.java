package com.cgi.eoss.ftep.core.utils.beans;

public class DataSourceCredentials {

  private String sourceName;
  private String authenticationPolicy;
  private String certificatePath;
  private String userName;
  private String password;


  public String getSourceName() {
    return sourceName;
  }

  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  public String getAuthenticationPolicy() {
    return authenticationPolicy;
  }

  public void setAuthenticationPolicy(String authenticationPolicy) {
    this.authenticationPolicy = authenticationPolicy;
  }

  public String getCertificatePath() {
    return certificatePath;
  }

  public void setCertificatePath(String certificatePath) {
    this.certificatePath = certificatePath;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }



}
