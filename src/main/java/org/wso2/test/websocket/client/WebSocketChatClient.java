package org.wso2.test.websocket.client;

import java.net.URI;
import java.util.Map;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class WebSocketChatClient extends WebSocketClient{

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";

    boolean close = false;

    boolean corsfailed = false;

    int iteration  =  0;

    public WebSocketChatClient(URI serverUri, Map<String,String> httpHeaders) {
        super(serverUri,httpHeaders);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected");

    }

    @Override
    public void onMessage(String message) {
        if(message.equals("Websocket frame throttled out")){
            System.out.println(ANSI_YELLOW+"Received from the server: " + message + ANSI_RESET);
            System.out.println(ANSI_GREEN+"API throttled out after " +(iteration*2)+" websocket frames"+ ANSI_RESET);
            close = true;
        }else {
            System.out.println(ANSI_YELLOW+"Received from the server: " + message + ANSI_RESET);
            iteration++;
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

        if(reason.contains("403 Status line: HTTP/1.1 403 Forbidden")){
            corsfailed = true;
        }
        System.out.println("Disconnected");

    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();

    }

    public int getI(){
        return iteration;
    }

    public boolean isThrottledOut(){
        return close;
    }

    public boolean isCorsfailed(){
        return corsfailed;
    }

}
