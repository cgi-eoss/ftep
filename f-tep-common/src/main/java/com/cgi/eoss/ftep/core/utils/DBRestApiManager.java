package com.cgi.eoss.ftep.core.utils;

import com.cgi.eoss.ftep.core.utils.beans.InsertResult;
import com.cgi.eoss.ftep.model.rest.ResourceJob;
import com.cgi.eoss.ftep.model.rest.ResourceLogin;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ResourceConverter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DBRestApiManager {

    private static final DBRestApiManager INSTANCE = new DBRestApiManager();

    public static DBRestApiManager getInstance() {
        return INSTANCE;
    }

    private SSLConnectionSocketFactory socketFactory;

    private String sessionId;

    private String sessionName;

    private String token;

    private CloseableHttpClient httpClient;

    private DBRestApiManager() {
        SSLContext sslcontext;
        try {
            sslcontext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    return true;
                }
            }).build();

            socketFactory = new SSLConnectionSocketFactory(sslcontext, new AllowAllHostnameVerifier());

            if (null != socketFactory) {
                httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
                authenticate();
            } else {
                LOG.error("Unable to create RestAPI Connector");
            }
        } catch (Exception e) {
            LOG.error("Cannot create HTTP Socket factory", e);
        }
    }

    private void authenticate() throws Exception {
        LOG.debug("API authentication starts");
        HttpPost httpPostRequest = new HttpPost(FtepConstants.DB_API_LOGIN_INT_ENDPOINT);
        httpPostRequest.setHeader("Content-type", FtepConstants.HTTP_JSON_CONTENT_TYPE);

        ResourceLogin resourceLogin = new ResourceLogin();
        resourceLogin.setUser(FtepConstants.DB_API_USER);
        resourceLogin.setPassword(FtepConstants.DB_API_PWD);
        ResourceConverter converter = new ResourceConverter(ResourceLogin.class);
        byte[] bytesToPost = converter.writeObject(resourceLogin);
        httpPostRequest.setEntity(new ByteArrayEntity(bytesToPost));

        LOG.debug("Submitting HTTP Post to endpoint: " + FtepConstants.DB_API_LOGIN_INT_ENDPOINT);
        HttpResponse httpResponse = httpClient.execute(httpPostRequest);
        LOG.debug("Executed HTTP Post request with Json :" + resourceLogin.toString());

        if (httpResponse.getStatusLine()
                .getStatusCode() > FtepConstants.HTTP_ERROR_RESPONSE_CODE_RANGE) {
            LOG.error("Failed to authenticate with REST API, HTTP error code : "
                    + httpResponse.getStatusLine().getStatusCode());
        }

        BufferedReader br =
                new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));

        String response = br.readLine();
        LOG.debug("HTTP Response for REST API authentication");
        LOG.debug("HTTP Response: " + response);
        JSONAPIDocument<ResourceLogin> document =
                converter.readDocument(response.getBytes(), ResourceLogin.class);

        ResourceLogin jsonData = document.get();
        sessionId = jsonData.getSessionId();
        sessionName = jsonData.getSessionName();
        token = jsonData.getToken();
        LOG.debug("Authentication successful, sessionId= " + sessionId + ", sessionName=" + sessionName
                + " ,token=" + token);

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
            String resourceRestEndpoint = document.getLinks().getSelf().getHref();
            String resourceId = document.get().getId();
            insertResult.setSuccess(true);
            insertResult.setResourceRestEndpoint(resourceRestEndpoint);
            insertResult.setResourceId(resourceId);

        } catch (IOException | IllegalAccessException e) {
            LOG.error("Exception while inserting job record via Database REST API: ", e);
            return insertResult;
        }
        return insertResult;
    }


    public Map<String, String> getCredentials(String httpEndpoint) {
        LOG.debug("Credentials lookup for endpoint:" + httpEndpoint);
        Map<String, String> credentialForDomain = new HashMap<>();
        HttpGet httpGetRequest = new HttpGet(httpEndpoint);
        String user = null;
        String password = null;
        String certificate = null;
        try {
            setHttpHeaders(httpGetRequest);
            HttpResponse httpResponse = httpClient.execute(httpGetRequest);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            BufferedReader br =
                    new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));
            String response = br.readLine();

            LOG.debug("HTTP response status code: " + statusCode);
            if (statusCode > FtepConstants.HTTP_ERROR_RESPONSE_CODE_RANGE) {
                LOG.error("Response Code from HTTP Get request:" + statusCode);
                LOG.error("HTTP Response: " + response);
                return credentialForDomain;
            }

            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement = jsonParser.parse(response);
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            jsonObject = jsonObject.getAsJsonObject("data");
            JsonArray jarray = jsonObject.getAsJsonArray("data");
            jsonObject = jarray.get(0).getAsJsonObject();
            String policy = jsonObject.get("policy").getAsString();
            String credentialsString = jsonObject.get("credentials_data").getAsString();
            JsonObject credentials = jsonParser.parse(credentialsString).getAsJsonObject();
            LOG.debug("Policy:" + policy);
            if (policy.equals("credentials")) {
                user = credentials.get(FtepConstants.DATASOURCE_CRED_USER).getAsString();
                password = credentials.get(FtepConstants.DATASOURCE_CRED_PASSWORD).getAsString();
            } else if (policy.equals("x509")) {
                certificate = credentials.get(FtepConstants.DATASOURCE_CRED_CERTIFICATE).getAsString();
            }
            LOG.debug("certificate:user:pass===" + certificate + "  " + user + "  " + password);
            credentialForDomain.put(FtepConstants.DATASOURCE_CRED_CERTIFICATE, certificate);
            credentialForDomain.put(FtepConstants.DATASOURCE_CRED_USER, user);
            credentialForDomain.put(FtepConstants.DATASOURCE_CRED_PASSWORD, password);

        } catch (IOException e) {
            LOG.error("", e);
        }

        return credentialForDomain;
    }

    public boolean updateOutputsInJobRecord(ResourceJob resourceJob, String httpEndpoint) {

        try {
            HttpPatch httpPatchRequest = getHttpPatchClient(httpEndpoint);

            ResourceConverter converter = new ResourceConverter(ResourceJob.class);
            byte[] bytesToPost = converter.writeObject(resourceJob);
            httpPatchRequest.setEntity(new ByteArrayEntity(bytesToPost));

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
        setHttpHeaders(httpPatchRequest);
        return httpPatchRequest;
    }

    private HttpPost getHttpPost(String httpEndpoint) {
        HttpPost httpPostRequest = new HttpPost(httpEndpoint);
        setHttpHeaders(httpPostRequest);
        return httpPostRequest;
    }

    private void setHttpHeaders(HttpRequestBase httpRequest) {
        LOG.debug("Setting HTTP Headers ");
        httpRequest.setHeader("X-CSRF-Token", token);
        httpRequest.setHeader("Cookie", sessionName + "=" + sessionId);
        httpRequest.setHeader("Content-type", FtepConstants.HTTP_JSON_CONTENT_TYPE);
    }

}
