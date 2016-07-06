package com.cgi.eoss.ftep.core.requesthandler.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import com.cgi.eoss.ftep.core.requesthandler.beans.TableFtepJob;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public enum DBRestApiManager {

  DB_API_CONNECTOR_INSTANCE;

  private SSLConnectionSocketFactory socketFactory;

  private String sessId, sessionName, token;

  private CloseableHttpClient httpClient;
  private static final Logger LOG = Logger.getLogger(DBRestApiManager.class);

  DBRestApiManager() {
    init();
  }

  private void init() {
    SSLContext sslcontext;
    try {
      sslcontext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
        public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
          return true;
        }
      }).build();

      socketFactory = new SSLConnectionSocketFactory(sslcontext, new AllowAllHostnameVerifier());

    } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
      LOG.error("Cannot create HTTP Socket factory", e);
    }
  }

  private boolean setHttpClient(HttpHost proxy) {
    if (null != socketFactory) {
      if (null != proxy) {
        httpClient =
            HttpClients.custom().setSSLSocketFactory(socketFactory).setProxy(proxy).build();
      } else {
        httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
      }
    } else {
      LOG.error("Unable to create Drupal RestAPI Connector");
      return false;
    }
    return authenticate();
  }

  private boolean authenticate() {
    try {
      HttpPost httpPostRequest = new HttpPost(FtepConstants.DB_API_LOGIN_INT_ENDPOINT);
      String postBodyStr = "{\"user\":\"" + FtepConstants.DB_API_USER + "\",\"password\":\""
          + FtepConstants.DB_API_PWD + "\"}";
      StringEntity postBody = new StringEntity(postBodyStr);
      postBody.setContentType(FtepConstants.HTTP_POST_JSON_CONTENT_TYPE);
      httpPostRequest.setEntity(postBody);
      LOG.debug("Submitting HTTP Post to endpoint: " + FtepConstants.DB_API_LOGIN_INT_ENDPOINT);
      HttpResponse httpResponse = httpClient.execute(httpPostRequest);
      LOG.debug("Executed HTTP Post request with Json :" + postBodyStr);

      if (httpResponse.getStatusLine().getStatusCode() != 200) {
        LOG.error("Failed to authenticate with REST API, HTTP error code : "
            + httpResponse.getStatusLine().getStatusCode());
        return false;
      }

      BufferedReader br =
          new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));

      LOG.debug("HTTP Response for REST API authentication");
      JsonObject jsonObject = new JsonParser().parse(br.readLine()).getAsJsonObject();
      JsonObject result = jsonObject.get("result").getAsJsonObject();
      sessId = result.get("sessid").getAsString();
      sessionName = result.get("session_name").getAsString();
      token = result.get("token").getAsString();
      LOG.debug("REST API Session ID: " + sessId);
      LOG.debug("REST API Session Name: " + sessionName);
      LOG.debug("REST API Session token: " + token);

    } catch (IOException e) {
      LOG.error("Exception in performing REST API authentication: ", e);
      return false;
    }
    return true;
  }


  public boolean setHttpClient() {
    return setHttpClient(null);
  }

  public boolean setHttpClientWithProxy(String URL, int port, String protocol) {
    HttpHost proxy = new HttpHost(URL, port, protocol);
    return setHttpClient(proxy);
  }

  public boolean insertJobRecord(TableFtepJob jobRecord) {

    // if (authenticate()) {
    try {
      HttpPost httpPostRequest = new HttpPost(FtepConstants.DB_API_JOBTABLE_INT_ENDPOINT);
      httpPostRequest.setHeader("Referer", "https://192.168.3.83/ftep_testui_jobs");
      httpPostRequest.setHeader("X-CSRF-Token", token);
      httpPostRequest.setHeader("Cookie", sessionName + "=" + sessId);
      httpPostRequest.setHeader("X-CSRF-Token", token);

      String postBodyStr = "{\"jid\":\"" + jobRecord.getJobID() + "\",\"inputs\":"
          + jobRecord.getInputs() + ",\"outputs\":" + jobRecord.getOutputs() + ",\"guiendpoint\":\""
          + jobRecord.getGuiEndpoint() + "\",\"uid\":\"" + jobRecord.getUserID() + "\"}";
      StringEntity postBody = new StringEntity(postBodyStr);
      postBody.setContentType(FtepConstants.HTTP_POST_JSON_CONTENT_TYPE);
      httpPostRequest.setEntity(postBody);
      LOG.debug("HTTP Post request with Json :" + postBodyStr);
      LOG.debug("Submitting HTTP Post to endpoint: " + FtepConstants.DB_API_JOBTABLE_INT_ENDPOINT);

      HttpResponse httpResponse = httpClient.execute(httpPostRequest);
      int statusCode = httpResponse.getStatusLine().getStatusCode();
      LOG.debug("HTTP response status code: " + statusCode);

      BufferedReader br =
          new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));
      String response = br.readLine();
      if (statusCode > 399) {
        LOG.error("Response Code from HTTP Post request:" + statusCode);
        return false;
      }
      LOG.debug("HTTP Response: " + response);

    } catch (IOException e) {
      LOG.error("Exception while inserting job record via Database REST API: ", e);
      return false;
    }
    return true;
  }

}
