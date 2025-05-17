package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.WebSocket;
import utils.Utils;
import utils.enums.ConversationId;
import utils.enums.ServiceType;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ManagerAgent extends Agent {
    private static final long serialVersionUID = 1L;
    private transient MyWebSocketServer socketServer;
    private final List<String> infrastructureUpdates = Collections.synchronizedList(new ArrayList<>());

    @Override
    protected void setup() {
        System.out.printf("[%s] Manager pornit %n", getLocalName());

        socketServer = new MyWebSocketServer(new InetSocketAddress("localhost", 8887));
        socketServer.start();
        addBehaviour(new Utils.RegisterServiceBehaviour(this, ServiceType.WEBSOCKET_SERVER, "websocket-server-service"));

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg != null) {
                    if (ConversationId.INFRASTRUCTURE_UPDATE.getClassName().equals(msg.getConversationId())) {
                        System.out.printf("[%s] Received update message: %s%n", getLocalName(), msg.getContent());

                        // Broadcast to all WebSocket clients
                        if (socketServer != null) {
                            socketServer.broadcast(msg.getContent());
                        }

                        // save for when a new user is connecting
                        infrastructureUpdates.add(msg.getContent());

                    }
                } else {
                    block();
                }
            }
        });

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

    private class MyWebSocketServer extends WebSocketServer {
        public MyWebSocketServer(InetSocketAddress address) {
            super(address);
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            System.out.println("New connection from " + conn.getRemoteSocketAddress());

            synchronized (infrastructureUpdates) {
                for (String update : infrastructureUpdates) {
                    conn.send(update);
                }
            }
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
