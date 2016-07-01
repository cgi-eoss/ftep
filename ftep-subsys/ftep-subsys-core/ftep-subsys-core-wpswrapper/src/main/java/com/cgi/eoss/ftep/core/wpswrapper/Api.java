package com.cgi.eoss.ftep.core.wpswrapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Api {

  public static void main(String[] args) {
    Api api = new Api();
    api.start();
  }

  private void start() {

    try {

      SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
        public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
          return true;
        }
      }).build();
      SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslcontext,
          SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
      HttpHost proxy = new HttpHost("proxy.logica.com", 80, "http");
      CloseableHttpClient httpclient =
          HttpClients.custom().setSSLSocketFactory(socketFactory).setProxy(proxy).build();

      HttpPost request = new HttpPost("https://192.171.139.83/api/v1.0/login");
      String message = "{\"user\":\"wps\",\"password\":\"b8tB8%&3Hq\"}";
      StringEntity input = new StringEntity(message);
      input.setContentType("application/json");
      request.setEntity(input);
      HttpResponse response = httpclient.execute(request);

      if (response.getStatusLine().getStatusCode() != 200) {
        throw new RuntimeException(
            "Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
      }

      BufferedReader br =
          new BufferedReader(new InputStreamReader((response.getEntity().getContent())));

      String output;
      // StringBuffer totalOutput = new StringBuffer();
      System.out.println("Output from Server .... \n");
      // while ((output = br.readLine()) != null) {
      JsonObject jsonObject = new JsonParser().parse(br.readLine()).getAsJsonObject();
      JsonObject result = jsonObject.get("result").getAsJsonObject();
      String sessid = result.get("sessid").getAsString();
      String session_name = result.get("session_name").getAsString();
      String token = result.get("token").getAsString();
      System.out.println(sessid);
      System.out.println(session_name);
      System.out.println(token);


      HttpPost request2 = new HttpPost("https://192.171.139.83/api/v1.0/jobs");
      request2.setHeader("Referer", "https://192.171.139.83/ftep_testui_jobs");
      request2.setHeader("X-CSRF-Token", token);
      request2.setHeader("Cookie", session_name + "=" + sessid);
      request2.setHeader("X-CSRF-Token", token);

      String message2 = "{\"inputdb\":\"RP\",\"outputdb\":\"AG\",\"id\":\"WPS-Job-test\"}";
      StringEntity input2 = new StringEntity(message2);
      input2.setContentType("application/json");
      request2.setEntity(input2);
      HttpResponse response2 = httpclient.execute(request2);


      BufferedReader br2 =
          new BufferedReader(new InputStreamReader((response2.getEntity().getContent())));

      System.out.println(br2.readLine());


    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }



}
