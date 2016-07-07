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
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import com.cgi.eoss.ftep.core.requesthandler.beans.InsertResult;
import com.cgi.eoss.ftep.core.requesthandler.rest.resources.ResourceJob;
import com.cgi.eoss.ftep.core.requesthandler.rest.resources.ResourceLogin;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ResourceConverter;


public enum DBRestApiManager {

  DB_API_CONNECTOR_INSTANCE;

  private SSLConnectionSocketFactory socketFactory;

  private String sessionId, sessionName, token;

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
      httpPostRequest.setHeader("Content-type", FtepConstants.HTTP_JSON_CONTENT_TYPE);

      ResourceLogin resourceLogin = new ResourceLogin();
      resourceLogin.setUser(FtepConstants.DB_API_USER);
      resourceLogin.setPassword(FtepConstants.DB_API_PWD);
      ResourceConverter converter = new ResourceConverter(ResourceLogin.class);
      byte[] bytesToPost = converter.writeObject(resourceLogin);
      httpPostRequest.setEntity(new ByteArrayEntity(bytesToPost));


      // String postBodyStr = "{\"user\":\"" + FtepConstants.DB_API_USER + "\",\"password\":\""
      // + FtepConstants.DB_API_PWD + "\"}";
      // StringEntity postBody = new StringEntity(postBodyStr);
      // postBody.setContentType(FtepConstants.HTTP_JSON_CONTENT_TYPE);
      // httpPostRequest.setEntity(postBody);
      LOG.debug("Submitting HTTP Post to endpoint: " + FtepConstants.DB_API_LOGIN_INT_ENDPOINT);
      HttpResponse httpResponse = httpClient.execute(httpPostRequest);
      LOG.debug("Executed HTTP Post request with Json :" + resourceLogin);

      if (httpResponse.getStatusLine().getStatusCode() > FtepConstants.HTTP_ERROR_RESPONSE_CODE_RANGE) {
        LOG.error("Failed to authenticate with REST API, HTTP error code : "
            + httpResponse.getStatusLine().getStatusCode());
        return false;
      }

      BufferedReader br =
          new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));

      String response = br.readLine();
      LOG.debug("HTTP Response: " + response);
      JSONAPIDocument<ResourceLogin> document =
          converter.readDocument(response.getBytes(), ResourceLogin.class);


      LOG.debug("HTTP Response for REST API authentication");
      // JsonObject jsonObject = new JsonParser().parse(br.readLine()).getAsJsonObject();
      // JsonObject result = jsonObject.get("result").getAsJsonObject();
      // sessId = result.get("sessid").getAsString();
      // sessionName = result.get("session_name").getAsString();
      // token = result.get("token").getAsString();

      ResourceLogin jsonData = document.get();
      sessionId = jsonData.getSessionId();
      sessionName = jsonData.getSessionName();
      token = jsonData.getToken();

      LOG.debug("REST API Session ID: " + sessionId);
      LOG.debug("REST API Session Name: " + sessionName);
      LOG.debug("REST API Session token: " + token);

    } catch (IOException | IllegalAccessException e) {
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

  public InsertResult insertJobRecord(ResourceJob resourceJob) {
    InsertResult insertResult = new InsertResult();

    try {
      HttpPost httpPostRequest = getHttpPost(FtepConstants.DB_API_JOBTABLE_INT_ENDPOINT);

      ResourceConverter converter = new ResourceConverter(ResourceJob.class);
      byte[] bytesToPost = converter.writeObject(resourceJob);
      httpPostRequest.setEntity(new ByteArrayEntity(bytesToPost));

      LOG.debug("HTTP Post request with Json :" + resourceJob.toString());
      LOG.debug("Submitting HTTP Post to endpoint: " + FtepConstants.DB_API_JOBTABLE_INT_ENDPOINT);

      HttpResponse httpResponse = httpClient.execute(httpPostRequest);
      int statusCode = httpResponse.getStatusLine().getStatusCode();
      LOG.debug("HTTP response status code: " + statusCode);

      BufferedReader br =
          new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));
      String response = br.readLine();

      if (statusCode > FtepConstants.HTTP_ERROR_RESPONSE_CODE_RANGE) {
        LOG.error("Response Code from HTTP Post request:" + statusCode);
        LOG.debug("HTTP Response: " + response);
        return insertResult;
      }
      LOG.debug("HTTP Response: " + response);
      JSONAPIDocument<ResourceJob> document =
          converter.readDocument(response.getBytes(), ResourceJob.class);
      String restResourceId = document.getLinks().getSelf().getHref();
      insertResult.setStatus(true);
      insertResult.setRestResourceId(restResourceId);

    } catch (IOException | IllegalAccessException e) {
      LOG.error("Exception while inserting job record via Database REST API: ", e);
      return insertResult;
    }

    return insertResult;
  }



  public boolean updateOutputsInJobRecord(ResourceJob resourceJob, String httpEndpoint) {

    try {
      HttpPatch httpPatchRequest = getHttpPatchClient(httpEndpoint);

      ResourceConverter converter = new ResourceConverter(ResourceJob.class);
      byte[] bytesToPost = converter.writeObject(resourceJob);
      httpPatchRequest.setEntity(new ByteArrayEntity(bytesToPost));


      // String patchBodyStr = "{\"jid\":\"" + jobRecord.getJobID() + "\",\"inputs\":"
      // + jobRecord.getInputs() + ",\"outputs\":" + jobRecord.getOutputs() + ",\"guiendpoint\":\""
      // + jobRecord.getGuiEndpoint() + "\",\"uid\":\"" + jobRecord.getUserID() + "\"}";
      // StringEntity patchBody = new StringEntity(patchBodyStr);
      // patchBody.setContentType(FtepConstants.HTTP_JSON_CONTENT_TYPE);
      // httpPatchRequest.setEntity(patchBody);
      LOG.debug("HTTP Patch request with Json :" + resourceJob);
      LOG.debug("Submitting HTTP Patch to endpoint: " + httpEndpoint);

      HttpResponse httpResponse = httpClient.execute(httpPatchRequest);
      int statusCode = httpResponse.getStatusLine().getStatusCode();
      LOG.debug("HTTP response status code: " + statusCode);

      BufferedReader br =
          new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));
      String response = br.readLine();
      if (statusCode > FtepConstants.HTTP_ERROR_RESPONSE_CODE_RANGE) {
        LOG.error("Response Code from HTTP Patch request:" + statusCode);
        LOG.debug("HTTP Response: " + response);
        return false;
      }
      LOG.debug("HTTP Response: " + response);

    } catch (IOException | IllegalAccessException e) {
      LOG.error("Exception while updaing outputs in job record via Database REST API: ", e);
      return false;
    }
    return true;
  }

  private HttpPatch getHttpPatchClient(String httpEndpoint) {
    HttpPatch httpPatchRequest = new HttpPatch(httpEndpoint);
    setHeaders(httpPatchRequest);
    return httpPatchRequest;
  }

  private HttpPost getHttpPost(String httpEndpoint) {
    HttpPost httpPostRequest = new HttpPost(httpEndpoint);
    setHeaders(httpPostRequest);
    return httpPostRequest;
  }

  private void setHeaders(HttpEntityEnclosingRequestBase httpRequest) {
    httpRequest.setHeader("X-CSRF-Token", token);
    httpRequest.setHeader("Cookie", sessionName + "=" + sessionId);
    httpRequest.setHeader("Content-type", FtepConstants.HTTP_JSON_CONTENT_TYPE);
    // httpRequest.setHeader("Referer", "https://192.168.3.83/ftep_testui_jobs");
  }

}
