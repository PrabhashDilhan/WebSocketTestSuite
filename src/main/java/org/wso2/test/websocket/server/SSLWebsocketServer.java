package org.wso2.test.websocket.server;

import java.io.*;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.wso2.test.WSTestException;

public class SSLWebsocketServer extends Thread{

    ChatServer chatserver;

    public void run(String serverhome, String storepass, String keypass, int port){

        try {

            chatserver = new ChatServer(port);
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

            SSLContext sslContext = null;
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            chatserver.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext));

            chatserver.start();

        }catch (Exception e){
            throw new WSTestException(e);
        }

    }

    public void stopChatServer(){
        try {
            chatserver.stop();
        } catch (InterruptedException e) {
            throw new WSTestException(e);
        }
    }
}
