package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.InformWebSocketServer;
import utils.ServiceFinder;
import utils.enums.ConversationId;
import utils.enums.InformType;
import utils.enums.ServiceType;

public class Node extends Agent {
    private static final long serialVersionUID = 1L;
    private AID webSocket;
    private int currentLoad = 0;
    private int maxLoad = 5;
    private String parentId;
    private String resourceType;
    private volatile boolean shuttingDown = false;

    @Override
    protected void setup() {
        System.out.printf("[%s] Node pornit%n", getLocalName());

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            parentId = (String) args[0];
            resourceType = (String) args[1];
        }

        addBehaviour(new ServiceFinder(
                this,
                ServiceType.WEBSOCKET_SERVER.toString(),
                (DFAgentDescription[] results) -> {
                    webSocket = results[0].getName();

                    addBehaviour(new InformWebSocketServer(
                            this,
                            InformType.CREATE,
                            ServiceType.NODE,
                            webSocket,
                            parentId,
                            resourceType
                    ));

                    String description = String.format("Node created by %s", parentId);
                    addBehaviour(new InformWebSocketServer(
                            this,
                            description,
                            InformType.LOG,
                            ServiceType.NODE,
                            webSocket
                    ));
                }
        ));

        addBehaviour(new CyclicBehaviour(this) {
            private final MessageTemplate cfpMt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            private final MessageTemplate selMt = MessageTemplate.MatchConversationId(
                    ConversationId.NODE_SHUTDOWN.getClassName());
            private final MessageTemplate mt = MessageTemplate.or(cfpMt, selMt);

            @Override
            public void action() {
                ACLMessage msg = receive(mt);
                if (msg == null) {
                    block();
                    return;
                }

                if (msg.getPerformative() == ACLMessage.CFP && !shuttingDown) {
                    int capacity = (int)(((double)(maxLoad - currentLoad) / maxLoad) * 100);
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(capacity));
                    send(reply);
                    System.out.printf("[%s] Am PROPUS capacitate =%d către %s%n",
                            getLocalName(), capacity, msg.getSender().getLocalName());

                    String description = String.format(
                            "Am PROPUS capacitate =%d către %s",
                            capacity, msg.getSender().getLocalName());
                    addBehaviour(new InformWebSocketServer(
                            myAgent, description,
                            InformType.LOG, ServiceType.NODE, webSocket));

                } else if (msg.getConversationId().equals(ConversationId.NODE_SHUTDOWN.getClassName())) {
                    String content = msg.getContent();
                    if ("shutdown".equals(content)) {
                        shuttingDown = true;
                        System.out.printf("[%s] Received shutdown, will terminate after pending tasks%n",
                                getLocalName());
                        addBehaviour(new TickerBehaviour(myAgent, 1000) {
                            @Override
                            protected void onTick() {
                                if (currentLoad == 0) {
                                    System.out.printf("[%s] No pending tasks, shutting down%n",
                                            getLocalName());
                                    doDelete();
                                    stop();
                                }
                            }
                        });
                    }
                } else if (!shuttingDown) {
                    String content = msg.getContent();
                    System.out.printf("[%s] Am primit request pentru %s%n",
                            getLocalName(), content);
                    String description = String.format("Am primit request pentru %s", content);
                    addBehaviour(new InformWebSocketServer(
                            myAgent, description,
                            InformType.LOG, ServiceType.NODE, webSocket));
                    addBehaviour(new ComputeRequest(myAgent, 3000));
                }
            }
        });
    }

    private class ComputeRequest extends WakerBehaviour {
        public ComputeRequest(Agent a, long timeout) {
            super(a, timeout);
            currentLoad++;
        }

        @Override
        protected void onWake() {
            super.onWake();
            currentLoad--;
        }
    }

    @Override
    protected void takeDown() {
        InformWebSocketServer deleteNotifier = new InformWebSocketServer(
                this,
                InformType.DELETE,
                ServiceType.NODE,
                webSocket,
                parentId,
                resourceType
        );
        deleteNotifier.action();

        System.out.printf("[%s] Node oprit%n", getLocalName());

        super.takeDown();
    }
}
