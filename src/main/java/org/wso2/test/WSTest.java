package org.wso2.test;

import org.wso2.test.apache.client.HttpClient;
import org.wso2.test.rest.apis.AdminTest;
import org.wso2.test.rest.apis.DevPortalTest;
import org.wso2.test.rest.apis.PublisherTest;
import org.wso2.test.websocket.client.WebSocketChat;
import org.wso2.test.websocket.server.PythonServer;
import org.wso2.test.websocket.server.SSLWebsocketServer;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

public class WSTest {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";

    public static void main(String[] args){
        
        String path = args[0];
        Properties prop = new Properties();
        File initialFile = new File(path);
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(initialFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (inputStream != null) {
            try {
                prop.load(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("property file is not found");
        }
        String serverhome = prop.getProperty("serverhome");
        String storepassword = prop.getProperty("storepassword");
        String keypassword = prop.getProperty("keypassword");
        String websocketport = prop.getProperty("websocketport");
        String carbonport = prop.getProperty("carbonport");
        String passthruonport = prop.getProperty("passthruonport");
        String firstapipath = prop.getProperty("firstapipath");
        String secondapipath = prop.getProperty("secondapipath");
        String corshandlerpath = prop.getProperty("corshandlerpath");
        String scriptpath = prop.getProperty("scriptpath");


        /*String serverhome = "/Users/prabhash/Documents/support/wso2am-3.2.0";
        String storepassword = "wso2carbon";
        String keypassword = "wso2carbon";
        String websocketport = "8100";
        String carbonport = "9444";
        String passthruonport = "8244";
        String firstapipath = "/Users/prabhash/Documents/support/api1.json";
        String secondapipath = "/Users/prabhash/Documents/support/api2.json";
        String corshandlerpath = "/Users/prabhash/Documents/support/_cors_request_handler_.xml";
        String scriptpath = "/Users/prabhash/Documents/support/websocket.py";*/

        System.out.println(ANSI_YELLOW+"#### Starting websocket tests ######"+ANSI_RESET);
        HttpClient hc = new HttpClient(serverhome,storepassword);
        PublisherTest pt = new PublisherTest(hc,carbonport,passthruonport);
        AdminTest at = new AdminTest(hc,carbonport,passthruonport);
        DevPortalTest dt = new DevPortalTest(hc,carbonport,passthruonport);

        System.out.println(ANSI_YELLOW+"#### Creating websocket API ######"+ANSI_RESET);
        pt.createAPI(firstapipath);

        System.out.println(ANSI_YELLOW+"#### Publishing websocket API ######"+ANSI_RESET);
        pt.publishAPI(false,"TEST ");

        System.out.println(ANSI_YELLOW+"#### Creating Application via dev portal REST API ######"+ANSI_RESET);
        dt.addApplication();

        System.out.println(ANSI_YELLOW+"#### Subscribing API via dev portal REST API ######"+ANSI_RESET);
        dt.subscribeApi(pt.getApiid(),false);
        dt.geneateKeys();

        System.out.println(ANSI_YELLOW+"#### Starting to invoke WS API and check the Application level throttling engagement ######"+ANSI_RESET);
        System.out.println(ANSI_YELLOW+"#### TEST : Starting websocket server"+ANSI_RESET);

        PythonServer ssws = new PythonServer();
        ssws.run(scriptpath);
        System.out.println(ANSI_YELLOW+"#### Sleeping for 10 sec"+ANSI_RESET);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        WebSocketChat wsc = new WebSocketChat();
        wsc.run(true,serverhome,storepassword,keypassword,dt.getToken(),"wss://localhost:"+websocketport+"/test3/v1",new HashMap<String,String>(),21);

        while (true){
            if(wsc.getChatclient().isThrottledOut()){
                System.out.println(ANSI_GREEN+"##### TEST : Application level throttling worked. API throttled out after " +(wsc.getChatclient().getI()*2)+" websocket frames"+ANSI_RESET);
                break;
            }
            if(wsc.getClose()){
                System.out.println(ANSI_RED+"##### TEST failed: The Application level throttling did not work after "+(wsc.getChatclient().getI()*2)+" frames(should be throttled out after ~10 frames). Please check"+ANSI_RESET);
                try {
                    wsc.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }


        System.out.println(ANSI_YELLOW+"#### Adding throttle policy via admin REST API (5PerMin)######"+ANSI_RESET);
        at.addThrottlePolicy();

        System.out.println(ANSI_YELLOW+"#### Adding API level throttling ######"+ANSI_RESET);
        pt.addApiLevelThrottling();

        System.out.println(ANSI_YELLOW+"#### Subscribing API via dev portal REST API(default application) ######"+ANSI_RESET);
        dt.getDefaultAppId();
        dt.subscribeApi(pt.getApiid(),false);
        dt.getOauthKeys();

        System.out.println(ANSI_YELLOW+"#### Generate access token via dev portal REST API(default application) ######"+ANSI_RESET);
        dt.generateDefaultToken();

        System.out.println(ANSI_YELLOW+"#### Startig to invoke ws API and check the API level throttling engagement(5PerMin) ######"+ANSI_RESET);
        System.out.println(ANSI_YELLOW+"##### TEST : Starting websocket client and sending message"+ANSI_RESET);
        WebSocketChat wsc2 = new WebSocketChat();
        wsc2.run(true,serverhome,storepassword,keypassword,dt.getToken(),"wss://localhost:"+websocketport+"/test3/v1",new HashMap<String,String>(),21);

        while (true){
            if(wsc2.getChatclient().isThrottledOut()){
                System.out.println(ANSI_GREEN+"##### TEST : API level throttling worked. API throttled out after " +(wsc2.getChatclient().getI()*2)+" websocket frames"+ANSI_RESET);
                break;
            }
            if(wsc2.getClose()){
                System.out.println(ANSI_RED+"##### TEST failed: The API level throttling did not work after "+(wsc2.getChatclient().getI()*2)+" frames(should be throttled out after ~10 frames). Please check"+ANSI_RESET);
                try {
                    wsc2.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        System.out.println(ANSI_YELLOW+"#### Deleting API level throttling ######"+ANSI_RESET);
        pt.deleteApiLevelThrottling();

        System.out.println(ANSI_YELLOW+"#### Deleting Created API ######"+ANSI_RESET);
        dt.deleteSubscription(pt.getApiid());
        pt.deleteAPI();

        //ssws.stopChatServer();

        System.out.println(ANSI_YELLOW+"#### Sleeping for 10sec ######"+ANSI_RESET);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new WSTestException(e);
        }

        PythonServer ssws1 = new PythonServer();
        ssws1.run(scriptpath);

        System.out.println(ANSI_YELLOW+"#### Creating websocket API for CORS testing ######"+ANSI_RESET);
        pt.createAPI(secondapipath);

        System.out.println(ANSI_YELLOW+"#### Publishing websocket API for CORS testing ######"+ANSI_RESET);
        pt.publishAPI(false,"TEST");
        dt.getDefaultAppId();
        dt.subscribeApi(pt.getApiid(),true);
        dt.getOauthKeys();
        dt.generateDefaultToken();

        System.out.println(ANSI_YELLOW+"#### Sleeping for 5sec ######"+ANSI_RESET);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new WSTestException(e);
        }

        System.out.println(ANSI_RED+"#### Starting CORS testing ######"+ANSI_RESET);

        System.out.println(ANSI_YELLOW+"#### TEST scenario 01: When cors configuration in publisher UI is disabled without any modifications in the cors_request_handler sequence ######"+ANSI_RESET);
        System.out.println(ANSI_YELLOW+"#### TEST scenario 01: Sub scenario 01: Without origin in the request - The request should be accepted without any cors validation failure"+ANSI_RESET);


        WebSocketChat wsc3 = new WebSocketChat();
        String token = dt.getToken();
        Map<String, String> headermap = new HashMap<String, String>();
        //headermap.put("Origin","https://test.com");
        wsc3.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc3.getChatclient().isCorsfailed()){
                System.out.println(ANSI_RED+"##### TEST scenario 01: Sub scenario 01 failed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc3.getChatclient().getI() == 1){
                System.out.println(ANSI_GREEN  +"##### TEST scenario 01: Sub scenario 01 passed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc3.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        System.out.println(ANSI_YELLOW+"#### TEST scenario 01: Sub scenario 02: With any origin in the request - The request should be accepted without any cors validation failure"+ANSI_RESET);


        WebSocketChat wsc4 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        headermap.put("Origin","https://test.com");
        wsc4.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc4.getChatclient().isCorsfailed()){
                System.out.println(ANSI_RED+"##### TEST scenario 01: Sub scenario 02 failed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc4.getChatclient().getI() == 1){
                System.out.println(ANSI_GREEN  +"##### TEST scenario 01: Sub scenario 02 passed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc4.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        System.out.println(ANSI_YELLOW+"#### TEST scenario 02: When cors configuration in publisher UI is enabled but has no values(The tick is removed) without any modifications in the cors_request_handler sequence ######"+ANSI_RESET);
        System.out.println(ANSI_YELLOW+"#### TEST scenario 02: Sub scenario 01: Without origin in the request - The request should be accepted without any cors validation failure"+ANSI_RESET);
        String corschange = "{\n" +
                "        \"corsConfigurationEnabled\": true,\n" +
                "        \"accessControlAllowCredentials\": false,\n" +
                "        \"accessControlAllowOrigins\": [],\n" +
                "        \"accessControlAllowHeaders\": [\n" +
                "            \"authorization\",\n" +
                "            \"Access-Control-Allow-Origin\",\n" +
                "            \"Content-Type\",\n" +
                "            \"SOAPAction\",\n" +
                "            \"apikey\",\n" +
                "            \"testKey\"\n" +
                "        ],\n" +
                "        \"accessControlAllowMethods\": [\n" +
                "            \"GET\",\n" +
                "            \"PUT\",\n" +
                "            \"POST\",\n" +
                "            \"DELETE\",\n" +
                "            \"PATCH\",\n" +
                "            \"OPTIONS\"\n" +
                "        ]\n" +
                "    }";

        pt.addCorsChanges(corschange);
        WebSocketChat wsc5 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        //headermap.put("Origin","https://test.com");
        wsc5.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc5.getChatclient().isCorsfailed()){
                System.out.println(ANSI_RED+"##### TEST scenario 02: Sub scenario 01 failed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc5.getChatclient().getI() == 1){
                System.out.println(ANSI_GREEN  +"##### TEST scenario 02: Sub scenario 01 passed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc5.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        System.out.println(ANSI_YELLOW+"#### TEST scenario 02: Sub scenario 02: With any origin in the request - The request should be rejected with cors validation failure (403)"+ANSI_RESET);
        WebSocketChat wsc6 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        headermap.put("Origin","https://test.com");
        wsc6.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc6.getChatclient().isCorsfailed()){
                System.out.println(ANSI_GREEN+"##### TEST scenario 02: Sub scenario 02 passed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc6.getChatclient().getI() == 1){
                System.out.println(ANSI_RED  +"##### TEST scenario 02: Sub scenario 02 failed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc6.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        pt.deleteCorsChange();

        System.out.println(ANSI_YELLOW+"#### TEST scenario 03: When cors configuration in publisher UI is enabled but All Origins is ticked under \"Access Control Allow Origins\" and without any modifications in the cors_request_handler sequence ######"+ANSI_RESET);
        System.out.println(ANSI_YELLOW+"#### TEST scenario 03: Sub scenario 01: Without origin in the request - The request should be accepted without any cors validation failure"+ANSI_RESET);
        corschange = "{\n" +
                "        \"corsConfigurationEnabled\": true,\n" +
                "        \"accessControlAllowCredentials\": false,\n" +
                "        \"accessControlAllowOrigins\": [*],\n" +
                "        \"accessControlAllowHeaders\": [\n" +
                "            \"authorization\",\n" +
                "            \"Access-Control-Allow-Origin\",\n" +
                "            \"Content-Type\",\n" +
                "            \"SOAPAction\",\n" +
                "            \"apikey\",\n" +
                "            \"testKey\"\n" +
                "        ],\n" +
                "        \"accessControlAllowMethods\": [\n" +
                "            \"GET\",\n" +
                "            \"PUT\",\n" +
                "            \"POST\",\n" +
                "            \"DELETE\",\n" +
                "            \"PATCH\",\n" +
                "            \"OPTIONS\"\n" +
                "        ]\n" +
                "    }";

        pt.addCorsChanges(corschange);
        WebSocketChat wsc7 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        //headermap.put("Origin","https://test.com");
        wsc7.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc7.getChatclient().isCorsfailed()){
                System.out.println(ANSI_RED+"##### TEST scenario 03: Sub scenario 01 failed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc7.getChatclient().getI() == 1){
                System.out.println(ANSI_GREEN  +"##### TEST scenario 03: Sub scenario 01 passed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc7.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        System.out.println(ANSI_YELLOW+"#### TEST scenario 03: Sub scenario 02: With any origin in the request - The request should be accepted without any cors validation failure"+ANSI_RESET);
        WebSocketChat wsc8 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        headermap.put("Origin","https://test.com");
        wsc8.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc8.getChatclient().isCorsfailed()){
                System.out.println(ANSI_RED+"##### TEST scenario 03: Sub scenario 02 failed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc8.getChatclient().getI() == 1){
                System.out.println(ANSI_GREEN  +"##### TEST scenario 03: Sub scenario 02 passed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc8.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        pt.deleteCorsChange();

        System.out.println(ANSI_YELLOW+"#### TEST scenario 04: When cors configuration in publisher UI is enabled but, https://abc.com is configured as allow origin under Access Control Allow Origins " +
                "without any modifications in the cors_request_handler sequence ######"+ANSI_RESET);
        System.out.println(ANSI_YELLOW+"#### TEST scenario 04: Sub scenario 01: Without origin in the request - The request should be accepted without any cors validation failure"+ANSI_RESET);
        corschange = "{\n" +
                "        \"corsConfigurationEnabled\": true,\n" +
                "        \"accessControlAllowCredentials\": false,\n" +
                "        \"accessControlAllowOrigins\": [\n" +
                "            \"https://abc.com\"\n" +
                "        ],\n" +
                "        \"accessControlAllowHeaders\": [\n" +
                "            \"authorization\",\n" +
                "            \"Access-Control-Allow-Origin\",\n" +
                "            \"Content-Type\",\n" +
                "            \"SOAPAction\",\n" +
                "            \"apikey\",\n" +
                "            \"testKey\"\n" +
                "        ],\n" +
                "        \"accessControlAllowMethods\": [\n" +
                "            \"GET\",\n" +
                "            \"PUT\",\n" +
                "            \"POST\",\n" +
                "            \"DELETE\",\n" +
                "            \"PATCH\",\n" +
                "            \"OPTIONS\"\n" +
                "        ]\n" +
                "    }";

        pt.addCorsChanges(corschange);
        WebSocketChat wsc9 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        //headermap.put("Origin","https://test.com");
        wsc9.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc9.getChatclient().isCorsfailed()){
                System.out.println(ANSI_RED+"##### TEST scenario 04: Sub scenario 01 failed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc9.getChatclient().getI() == 1){
                System.out.println(ANSI_GREEN  +"##### TEST scenario 04: Sub scenario 01 passed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc9.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        System.out.println(ANSI_YELLOW+"#### TEST scenario 04: Sub scenario 02: With origin https://abc.com in the request - The request should be accepted without any cors validation failure"+ANSI_RESET);
        WebSocketChat wsc10 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        headermap.put("Origin","https://abc.com");
        wsc10.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc10.getChatclient().isCorsfailed()){
                System.out.println(ANSI_RED+"##### TEST scenario 04: Sub scenario 02 failed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc10.getChatclient().getI() == 1){
                System.out.println(ANSI_GREEN  +"##### TEST scenario 04: Sub scenario 02 passed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc10.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        System.out.println(ANSI_YELLOW+"#### TEST scenario 04: Sub scenario 03: With any origin except https://abc.com in the request - The request should be rejected with cors validation failure (403)"+ANSI_RESET);
        WebSocketChat wsc11 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        headermap.put("Origin","https://wso2.com");
        wsc11.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc11.getChatclient().isCorsfailed()){
                System.out.println(ANSI_GREEN+"##### TEST scenario 04: Sub scenario 03 passed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc11.getChatclient().getI() == 1){
                System.out.println(ANSI_RED  +"##### TEST scenario 04: Sub scenario 03 failed: CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc11.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        pt.deleteCorsChange();

        System.out.println(ANSI_YELLOW+"##### Going to update _cors_request_handler_ sequence######"+ANSI_RESET);
        pt.copyFile(corshandlerpath,new File(serverhome+"/repository/deployment/server/synapse-configs/default/sequences/_cors_request_handler_.xml"));


        System.out.println(ANSI_YELLOW+"#### TEST scenario 05: When cors configuration in publisher UI is disabled for the API ######"+ANSI_RESET);

        System.out.println(ANSI_YELLOW+"#### TEST scenario 05: Sub scenario 01: Without origin in the request - The request should be accepted without any cors validation failure"+ANSI_RESET);


        WebSocketChat wsc12 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        //headermap.put("Origin","https://test.com");
        wsc12.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc12.getChatclient().isCorsfailed()){
                System.out.println(ANSI_RED+"##### TEST scenario 05: Sub scenario 01 failed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc12.getChatclient().getI() == 1){
                System.out.println(ANSI_GREEN  +"##### TEST scenario 05: Sub scenario 01 passed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc12.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        System.out.println(ANSI_YELLOW+"#### TEST scenario 05: Sub scenario 02: With origin https://wso2.prod.london.com in the request - The request should be accepted without any cors validation failure"+ANSI_RESET);


        WebSocketChat wsc13 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        headermap.put("Origin","https://wso2.prod.london.com");
        wsc13.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc13.getChatclient().isCorsfailed()){
                System.out.println(ANSI_RED+"##### TEST scenario 05: Sub scenario 02 failed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc13.getChatclient().getI() == 1){
                System.out.println(ANSI_GREEN  +"##### TEST scenario 05: Sub scenario 02 passed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc13.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        System.out.println(ANSI_YELLOW+"#### TEST scenario 05: Sub scenario 03: With any origin in the request except https://wso2.prod.london.com - The request is rejected with cors validation failure (403)"+ANSI_RESET);

        WebSocketChat wsc14 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        headermap.put("Origin","https://test.com");
        wsc14.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc14.getChatclient().isCorsfailed()){
                System.out.println(ANSI_GREEN+"##### TEST scenario 05: Sub scenario 03 passed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc14.getChatclient().getI() == 1){
                System.out.println(ANSI_RED  +"##### TEST scenario 05: Sub scenario 03 failed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc14.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        corschange = "{\n" +
                "        \"corsConfigurationEnabled\": true,\n" +
                "        \"accessControlAllowCredentials\": false,\n" +
                "        \"accessControlAllowOrigins\": [],\n" +
                "        \"accessControlAllowHeaders\": [\n" +
                "            \"authorization\",\n" +
                "            \"Access-Control-Allow-Origin\",\n" +
                "            \"Content-Type\",\n" +
                "            \"SOAPAction\",\n" +
                "            \"apikey\",\n" +
                "            \"testKey\"\n" +
                "        ],\n" +
                "        \"accessControlAllowMethods\": [\n" +
                "            \"GET\",\n" +
                "            \"PUT\",\n" +
                "            \"POST\",\n" +
                "            \"DELETE\",\n" +
                "            \"PATCH\",\n" +
                "            \"OPTIONS\"\n" +
                "        ]\n" +
                "    }";

        pt.addCorsChanges(corschange);

        System.out.println(ANSI_YELLOW+"#### TEST scenario 06: When cors configuration in publisher UI is enabled but has no values(The tick is removed) ######"+ANSI_RESET);

        System.out.println(ANSI_YELLOW+"#### TEST scenario 06: Sub scenario 01: Without origin in the request - The request should be accepted without any cors validation failure"+ANSI_RESET);


        WebSocketChat wsc15 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        //headermap.put("Origin","https://test.com");
        wsc15.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc15.getChatclient().isCorsfailed()){
                System.out.println(ANSI_RED+"##### TEST scenario 06: Sub scenario 01 failed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc15.getChatclient().getI() == 1){
                System.out.println(ANSI_GREEN  +"##### TEST scenario 06: Sub scenario 01 passed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc15.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        System.out.println(ANSI_YELLOW+"#### TEST scenario 06: Sub scenario 02: With origin https://wso2.prod.london.com in the request - The request should be accepted without any cors validation failure"+ANSI_RESET);


        WebSocketChat wsc16 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        headermap.put("Origin","https://wso2.prod.london.com");
        wsc16.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc16.getChatclient().isCorsfailed()){
                System.out.println(ANSI_RED+"##### TEST scenario 06: Sub scenario 02 failed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc16.getChatclient().getI() == 1){
                System.out.println(ANSI_GREEN  +"##### TEST scenario 06: Sub scenario 02 passed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc16.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        System.out.println(ANSI_YELLOW+"#### TEST scenario 06: Sub scenario 03: With any origin in the request except https://wso2.prod.london.com - The request is rejected with cors validation failure (403)"+ANSI_RESET);

        WebSocketChat wsc17 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        headermap.put("Origin","https://test.com");
        wsc17.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc17.getChatclient().isCorsfailed()){
                System.out.println(ANSI_GREEN+"##### TEST scenario 06: Sub scenario 03 passed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc17.getChatclient().getI() == 1){
                System.out.println(ANSI_RED  +"##### TEST scenario 06: Sub scenario 03 failed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc17.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        pt.deleteCorsChange();

        corschange = "{\n" +
                "        \"corsConfigurationEnabled\": true,\n" +
                "        \"accessControlAllowCredentials\": false,\n" +
                "        \"accessControlAllowOrigins\": [*],\n" +
                "        \"accessControlAllowHeaders\": [\n" +
                "            \"authorization\",\n" +
                "            \"Access-Control-Allow-Origin\",\n" +
                "            \"Content-Type\",\n" +
                "            \"SOAPAction\",\n" +
                "            \"apikey\",\n" +
                "            \"testKey\"\n" +
                "        ],\n" +
                "        \"accessControlAllowMethods\": [\n" +
                "            \"GET\",\n" +
                "            \"PUT\",\n" +
                "            \"POST\",\n" +
                "            \"DELETE\",\n" +
                "            \"PATCH\",\n" +
                "            \"OPTIONS\"\n" +
                "        ]\n" +
                "    }";

        pt.addCorsChanges(corschange);

        System.out.println(ANSI_YELLOW+"#### TEST scenario 07: When cors configuration in publisher UI is enabled but  All Origins is ticked under \"Access Control Allow Origins\" ######"+ANSI_RESET);

        System.out.println(ANSI_YELLOW+"#### TEST scenario 07: Sub scenario 01: Without origin in the request - The request should be accepted without any cors validation failure"+ANSI_RESET);


        WebSocketChat wsc18 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        //headermap.put("Origin","https://test.com");
        wsc18.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc18.getChatclient().isCorsfailed()){
                System.out.println(ANSI_RED+"##### TEST scenario 07: Sub scenario 01 failed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc18.getChatclient().getI() == 1){
                System.out.println(ANSI_GREEN  +"##### TEST scenario 07: Sub scenario 01 passed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc18.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        System.out.println(ANSI_YELLOW+"#### TEST scenario 07: Sub scenario 02: With origin https://wso2.prod.london.com in the request - The request should be accepted without any cors validation failure"+ANSI_RESET);


        WebSocketChat wsc19 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        headermap.put("Origin","https://wso2.prod.london.com");
        wsc19.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc19.getChatclient().isCorsfailed()){
                System.out.println(ANSI_RED+"##### TEST scenario 07: Sub scenario 02 failed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc19.getChatclient().getI() == 1){
                System.out.println(ANSI_GREEN  +"##### TEST scenario 07: Sub scenario 02 passed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc19.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        System.out.println(ANSI_YELLOW+"#### TEST scenario 07: Sub scenario 03: With any origin in the request - The request should be accepted without any cors validation failure"+ANSI_RESET);

        WebSocketChat wsc20 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        headermap.put("Origin","https://test.com");
        wsc20.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc20.getChatclient().isCorsfailed()){
                System.out.println(ANSI_RED+"##### TEST scenario 07: Sub scenario 03 failed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc20.getChatclient().getI() == 1){
                System.out.println(ANSI_GREEN  +"##### TEST scenario 07: Sub scenario 03 passed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc20.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        pt.deleteCorsChange();

        corschange = "{\n" +
                "        \"corsConfigurationEnabled\": true,\n" +
                "        \"accessControlAllowCredentials\": false,\n" +
                "        \"accessControlAllowOrigins\": [\n" +
                "            \"https://abc.com\"\n" +
                "        ],\n" +
                "        \"accessControlAllowHeaders\": [\n" +
                "            \"authorization\",\n" +
                "            \"Access-Control-Allow-Origin\",\n" +
                "            \"Content-Type\",\n" +
                "            \"SOAPAction\",\n" +
                "            \"apikey\",\n" +
                "            \"testKey\"\n" +
                "        ],\n" +
                "        \"accessControlAllowMethods\": [\n" +
                "            \"GET\",\n" +
                "            \"PUT\",\n" +
                "            \"POST\",\n" +
                "            \"DELETE\",\n" +
                "            \"PATCH\",\n" +
                "            \"OPTIONS\"\n" +
                "        ]\n" +
                "    }";

        pt.addCorsChanges(corschange);

        System.out.println(ANSI_YELLOW+"#### TEST scenario 08: When cors configuration in publisher UI is enabled but  All Origins is not ticked under \"Access Control Allow Origins\" but, https://abc.com is configured as allow origin under Access Control Allow Origins ######"+ANSI_RESET);

        System.out.println(ANSI_YELLOW+"#### TEST scenario 08: Sub scenario 01: Without origin in the request - The request should be accepted without any cors validation failure"+ANSI_RESET);


        WebSocketChat wsc21 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        //headermap.put("Origin","https://test.com");
        wsc21.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc21.getChatclient().isCorsfailed()){
                System.out.println(ANSI_RED+"##### TEST scenario 08: Sub scenario 01 failed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc21.getChatclient().getI() == 1){
                System.out.println(ANSI_GREEN  +"##### TEST scenario 08: Sub scenario 01 passed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc21.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        System.out.println(ANSI_YELLOW+"#### TEST scenario 08: Sub scenario 02: With origin https://wso2.prod.london.com in the request - The request should be accepted without any cors validation failure"+ANSI_RESET);


        WebSocketChat wsc22 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        headermap.put("Origin","https://wso2.prod.london.com");
        wsc22.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc22.getChatclient().isCorsfailed()){
                System.out.println(ANSI_RED+"##### TEST scenario 08: Sub scenario 02 failed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc22.getChatclient().getI() == 1){
                System.out.println(ANSI_GREEN  +"##### TEST scenario 08: Sub scenario 02 passed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc22.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        System.out.println(ANSI_YELLOW+"#### TEST scenario 08: Sub scenario 03: With origin https://abc.com in the request - The request should be accepted without any cors validation failure"+ANSI_RESET);


        WebSocketChat wsc23 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        headermap.put("Origin","https://abc.com");
        wsc23.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc23.getChatclient().isCorsfailed()){
                System.out.println(ANSI_RED+"##### TEST scenario 08: Sub scenario 03 failed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc23.getChatclient().getI() == 1){
                System.out.println(ANSI_GREEN  +"##### TEST scenario 08: Sub scenario 03 passed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc23.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        System.out.println(ANSI_YELLOW+"#### TEST scenario 08: Sub scenario 04: With any origin except https://abc.com and https://wso2.prod.london.com  in the request - The request should be rejected with cors validation failure (403)"+ANSI_RESET);

        WebSocketChat wsc24 = new WebSocketChat();
        token = dt.getToken();
        headermap = new HashMap<String, String>();
        headermap.put("Origin","https://test.com");
        wsc24.run(true,serverhome,storepassword,keypassword,token,"wss://localhost:"+websocketport+"/test4/v1",headermap,2);
        while (true){
            if(wsc24.getChatclient().isCorsfailed()){
                System.out.println(ANSI_GREEN+"##### TEST scenario 08: Sub scenario 04 passed: CORS Failure with 403"+ANSI_RESET);
                break;
            }
            if(wsc24.getChatclient().getI() == 1){
                System.out.println(ANSI_RED  +"##### TEST scenario 08: Sub scenario 04 failed:  CORS failure did not occur" + ANSI_RESET);
                try {
                    wsc24.getChatclient().closeBlocking();
                } catch (InterruptedException e) {
                    throw new WSTestException(e);
                }
                break;
            }
        }

        pt.deleteCorsChange();


        System.out.println(ANSI_RED  +"######## All the test scenarios are completed, check the logs and verify ########" + ANSI_RESET);

    }
}
