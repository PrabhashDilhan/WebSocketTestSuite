package org.wso2.test.websocket.client;

import org.wso2.test.WSTestException;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Map;

public class WebSocketChat extends Thread{

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";

    WebSocketChatClient chatclient;

    boolean chatclose = false;
    public boolean getClose(){
        return chatclose;
    }

    public WebSocketChatClient getChatclient(){
        return chatclient;
    }

    public void run(boolean issecured, String serverhome, String storepass, String keypass, String token, String url, Map headermap, int iterations){

        try {
            Map httpHeaders = headermap;

            httpHeaders.put("Authorization", "Bearer " + token);
            chatclient = new WebSocketChatClient(new URI(url), httpHeaders);

            if (issecured) {

                String STORETYPE = "JKS";
                String KEYSTORE = serverhome +"/repository/resources/security/wso2carbon.jks";
                String STOREPASSWORD = storepass;
                String KEYPASSWORD = keypass;

                KeyStore ks = KeyStore.getInstance(STORETYPE);
                File kf = new File(KEYSTORE);
                ks.load(new FileInputStream(kf), STOREPASSWORD.toCharArray());

                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(ks, KEYPASSWORD.toCharArray());
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(ks);
                TrustManager[] trustAllCerts = new TrustManager[]{new X509ExtendedTrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {

                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {

                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {

                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {

                    }

                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }

                }};

                SSLContext sslContext = null;
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(kmf.getKeyManagers(), trustAllCerts, null);
                // sslContext.init( null, null, null ); // will use java's default key and trust store which is sufficient unless you deal with self-signed certificates

                SSLSocketFactory factory = sslContext
                        .getSocketFactory();// (SSLSocketFactory) SSLSocketFactory.getDefault();

                chatclient.setSocketFactory(factory);

                chatclient.connectBlocking();
            } else {

                chatclient.connectBlocking();
            }

            for(int i=1; i<iterations ;i++) {
                String line = "Hi :" + i;
                if (chatclient.close) {
                    System.out.println(ANSI_YELLOW+"###### Shutting Down the chat client"+ANSI_RESET);
                    chatclient.closeBlocking();
                    chatclose = true;
                    break;
                } else if (line.equals("open")) {
                    chatclient.reconnect();
                } else {
                    System.out.println(ANSI_YELLOW+"sending :" + i + " Hi  message"+ANSI_RESET);
                    chatclient.send(line);
                }
                System.out.println(ANSI_YELLOW+"####  Web socket client sleeping for 2 sec"+ANSI_RESET);
                Thread.sleep(2000);
            }
            chatclose = true;
        }catch (Exception e){
            if(chatclient.corsfailed){
                chatclient.close();
            }else {
                throw new WSTestException(e);
            }
        }
    }
}
