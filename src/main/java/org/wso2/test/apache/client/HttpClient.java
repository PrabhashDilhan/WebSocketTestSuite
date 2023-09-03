package org.wso2.test.apache.client;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.wso2.test.WSTestException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class HttpClient {
    private String maxTotal="100";

    private String defaultMaxPerRoute="100";

    private String connectionTimeout = "30000";

    private String connectionRequestTimeout = "30000";

    private String socketTimeout = "60000";

    private CloseableHttpClient httpClient;

    private String serverhome;

    private String keystorepassword;

    public HttpClient(String serverhome, String keystorepassword){
        this.serverhome=serverhome;
        this.keystorepassword=keystorepassword;
        PoolingHttpClientConnectionManager pool = null;
        try {
            pool = getPoolingHttpClientConnectionManager();

            pool.setMaxTotal(Integer.parseInt(maxTotal));
            pool.setDefaultMaxPerRoute(Integer.parseInt(defaultMaxPerRoute));

            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(Integer.parseInt(connectionTimeout))
                    .setConnectionRequestTimeout(Integer.parseInt(connectionRequestTimeout))
                    .setSocketTimeout(Integer.parseInt(socketTimeout)).build();

            HttpClientBuilder clientBuilder = HttpClients.custom().setConnectionManager(pool)
                    .setDefaultRequestConfig(config);

            // Create an HttpClient instance
            httpClient = clientBuilder.build();
        } catch (Exception e) {
            handleException("CustomHttpClient class mediator initialisation failed ",e);
        }
    }

    private SSLConnectionSocketFactory createSocketFactory() {

        String keyStorePath = serverhome+"/repository/resources/security/wso2carbon.jks";
        String keyStorePassword = keystorepassword;
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");

            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            sslContext.init(null,trustManagerFactory.getTrustManagers(),null);
            return new SSLConnectionSocketFactory(sslContext);
        } catch (KeyStoreException e) {
            handleException("Failed to read from Key Store", e);
        } catch (IOException e) {
            handleException("Key Store not found in " + keyStorePath, e);
        } catch (CertificateException e) {
            handleException("Failed to read Certificate", e);
        } catch (NoSuchAlgorithmException e) {
            handleException("Failed to load Key Store from " + keyStorePath, e);
        } catch (KeyManagementException e) {
            handleException("Failed to load key from" + keyStorePath, e);
        }

        return null;
    }

    public static void handleException(String msg, Throwable t){
        System.out.println(msg +": "+ t.toString());
        throw new WSTestException(msg, t);
    }

    private PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager(){

        PoolingHttpClientConnectionManager poolManager;
        SSLConnectionSocketFactory socketFactory = createSocketFactory();
        org.apache.http.config.Registry<ConnectionSocketFactory> socketFactoryRegistry =
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("https", socketFactory).build();
        poolManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        return poolManager;
    }

    public HttpResponse invoke(String method, String payload, String url, String bearerToken, String  basicToken){
        HttpPost httpPost;
        HttpGet httpGet;
        HttpPut httpPut;
        HttpDelete httpDelete;
        HttpResponse response;
        try {
            if (method.equals("POST")) {
                httpPost = new HttpPost(url);
                if(payload!=null){
                StringEntity stringEntity = new StringEntity(payload);
                httpPost.setEntity(stringEntity);
                httpPost.setHeader("Content-Type","application/json");
                }
                if(basicToken!=null){
                    httpPost.setHeader("Authorization", "Basic "+ basicToken);
                }else {
                    httpPost.setHeader("Authorization", "Bearer "+ bearerToken);
                }
                response = httpClient.execute(httpPost);

            } else if(method.equals("GET")) {
                httpGet = new HttpGet(url);
                if(basicToken!=null){
                    httpGet.setHeader("Authorization", "Basic "+ basicToken);
                }else {
                    httpGet.setHeader("Authorization", "Bearer "+ bearerToken);
                }
                response = httpClient.execute(httpGet);
            } else if (method.equals("DELETE")) {
                httpDelete = new HttpDelete(url);
                httpDelete.setHeader("Authorization", "Bearer "+ bearerToken);
                response = httpClient.execute(httpDelete);
            } else {
                httpPut = new HttpPut(url);
                httpPut.setHeader("Authorization", "Bearer "+ bearerToken);
                if(payload!=null){
                    StringEntity stringEntity = new StringEntity(payload);
                    httpPut.setEntity(stringEntity);
                    httpPut.setHeader("Content-Type","application/json");
                }
                response = httpClient.execute(httpPut);
            }
            return response;
        }catch (Exception e){
            handleException("sdfds",e);
        }
        return null;
    }

    public HttpResponse getToken(String url,  String consumerKey, String  consumerSecret){
        HttpPost httpPost;
        HttpResponse response;
        try{
            httpPost = new HttpPost(url);
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("grant_type", "client_credentials"));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            String basic = consumerKey+":"+consumerSecret;
            httpPost.setHeader("Authorization","Basic " + Base64.getEncoder().encodeToString(basic.getBytes(StandardCharsets.UTF_8)));
            response = httpClient.execute(httpPost);
            return response;
        }catch (Exception e){
            handleException("fsdfds",e);
        }
        return null;
    }
}
