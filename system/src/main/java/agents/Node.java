package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
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

    @Override
    protected void setup() {
        System.out.printf("[%s] Node pornit%n", getLocalName());

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            parentId = (String) args[0];
        }

        // conectare catre manager + update de creare
        addBehaviour(new ServiceFinder(
                this,
                ServiceType.WEBSOCKET_SERVER.toString(),
                (DFAgentDescription[] results) -> {
                    AID webSocketAID = results[0].getName();
                    webSocket = webSocketAID;

                    addBehaviour(new InformWebSocketServer(
                            this,
                            InformType.CREATE,
                            ServiceType.NODE,
                            webSocketAID,
                            parentId
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

            @Override
            public void action() {
                ACLMessage cfp = receive(cfpMt);
                if (cfp != null) {
                    int capacity = (int) ( ((double)(maxLoad - currentLoad) / maxLoad) * 100 );

                    ACLMessage reply = cfp.createReply();
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(capacity));
                    send(reply);
                    System.out.printf("[%s] Am PROPUS capacitate =%d catre %s%n", getLocalName(), capacity, cfp.getSender().getLocalName());

                    String description = String.format("Am PROPUS capacitate =%d catre %s", capacity, cfp.getSender().getLocalName());
                    addBehaviour(new InformWebSocketServer(
                            myAgent,
                            description,
                            InformType.LOG,
                            ServiceType.NODE,
                            webSocket
                    ));

                } else {
                    block();
                }
            }
        });

        addBehaviour(new CyclicBehaviour(this) {
            private final MessageTemplate selMt = MessageTemplate.MatchConversationId(ConversationId.NODE_ASSIGNMENT.getClassName());
            @Override
            public void action() {
                ACLMessage msg = receive(selMt);
                if (msg != null) {
                    String resource = msg.getContent();
                    System.out.printf("[%s] Am primit request pentru %s%n", getLocalName(), resource);

                    String description = String.format("Am primit request pentru %s", resource);
                    addBehaviour(new InformWebSocketServer(
                            myAgent,
                            description,
                            InformType.LOG,
                            ServiceType.NODE,
                            webSocket
                    ));
                    addBehaviour(new ComputeRequest(myAgent, 1000));
                } else {
                    block();
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
        System.out.printf("[%s] Node oprit%n", getLocalName());
    }
}
