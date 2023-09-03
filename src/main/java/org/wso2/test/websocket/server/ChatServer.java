package org.wso2.test.websocket.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 * A simple WebSocketServer implementation. Keeps track of a "chatroom".
 */
public class ChatServer extends WebSocketServer {

    public ChatServer(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));
    }

    public ChatServer(InetSocketAddress address) {
        super(address);
    }

    public ChatServer(int port, Draft_6455 draft) {
        super(new InetSocketAddress(port), Collections.<Draft>singletonList(draft));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        //conn.send("Welcome to the server!"); //This method sends a message to the new client
        //broadcast("new connection: " + handshake.getResourceDescriptor()); //This method sends a message to all clients connected
        System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " client connected to the server!");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        broadcast(conn + " Close message received, stopping the server!");
        if (conn != null) {
            // some errors like port binding failed may not be assignable to a specific websocket
            conn.close();
        }
        //System.out.println(conn + " has left the room!");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        broadcast(message);
        //System.out.println(conn + ": " + message);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        broadcast(message.array());
        //System.out.println(conn + ": " + message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        if (conn != null) {
            // some errors like port binding failed may not be assignable to a specific websocket
            conn.close();
        }
    }

    @Override
    public void onStart() {
        System.out.println("##### Server started!");
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }

}
