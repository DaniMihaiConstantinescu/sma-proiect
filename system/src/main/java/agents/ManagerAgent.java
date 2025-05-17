package agents;

import jade.core.Agent;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.WebSocket;

import java.net.InetSocketAddress;

public class ManagerAgent extends Agent {
    private static final long serialVersionUID = 1L;
    private transient MyWebSocketServer socketServer;

    @Override
    protected void setup() {
        System.out.printf("[%s] Manager pornit %n", getLocalName());

        socketServer = new MyWebSocketServer(new InetSocketAddress("localhost", 8887));
        socketServer.start();

        System.out.printf("[%s] WebSocket server started on ws://localhost:8887 %n", getLocalName());

    }

    @Override
    protected void takeDown() {
        try {
            if (socketServer != null) {
                socketServer.stop();
            }
            System.out.println("WebSocket server stopped.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class MyWebSocketServer extends WebSocketServer {
        public MyWebSocketServer(InetSocketAddress address) {
            super(address);
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            System.out.println("New connection from " + conn.getRemoteSocketAddress());
            conn.send("Hello from ManagerAgent WebSocket!");
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            System.out.println("Connection closed: " + reason);
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            System.out.println("[WebSockets] Received message: " + message);
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            ex.printStackTrace();
        }

        @Override
        public void onStart() {
            System.out.println("WebSocket Server started successfully.");
        }
    }
}
