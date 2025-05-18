package utils;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import utils.enums.ConversationId;
import utils.enums.InformType;
import utils.enums.ServiceType;

public class InformWebSocketServer extends OneShotBehaviour {
    private final InformType informType;
    private final ServiceType instanceType;
    private final AID serverAID;
    private String parentId = "";
    private String description = "";

    public InformWebSocketServer(Agent a, InformType informType, ServiceType instanceType, AID serverAID) {
        super(a);
        this.informType = informType;
        this.instanceType = instanceType;
        this.serverAID = serverAID;
    }

    public InformWebSocketServer(Agent agent, InformType informType, ServiceType instanceType, AID serverAID, String parentId) {
        super(agent);
        this.informType = informType;
        this.instanceType = instanceType;
        this.serverAID = serverAID;
        this.parentId = parentId;
    }

    public InformWebSocketServer(Agent a, String description, InformType informType, ServiceType instanceType, AID serverAID) {
        super(a);
        this.description = description;
        this.serverAID = serverAID;
        this.instanceType = instanceType;
        this.informType = informType;
    }

    @Override
    public void action() {

        String content;
        if (informType == InformType.LOG) {

            String timestamp = java.time.Instant.now().toString();
            String instance = myAgent.getLocalName();

            content = String.format(
                    "{\"type\": \"log\", \"data\": { \"timestamp\": \"%s\", \"instance\": \"%s\", \"description\": \"%s\", \"instanceType\": \"%s\" }}",
                    timestamp,
                    instance,
                    description,
                    instanceType
            );

        } else {
            String type = mapToPayloadType(informType, instanceType);
            if (type == null) return;

            content = String.format(
                    "{\"type\": \"%s\", \"data\": { \"id\": \"%s\", \"childrenIds\": [], \"capacity\": 0, \"parentId\": \"%s\" }}",
                    type,
                    myAgent.getLocalName(),
                    parentId
            );
        }

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(serverAID);
        msg.setConversationId(ConversationId.INFRASTRUCTURE_UPDATE.getClassName());
        msg.setContent(content);
        myAgent.send(msg);
    }

    private String mapToPayloadType(InformType informType, ServiceType serviceType) {
        if (informType == InformType.CREATE) {
            return switch (serviceType) {
                case NODE -> "newNode";
                case LOAD_BALANCER -> "newLoadBalancer";
                case REVERSE_PROXY -> "newReverseProxy";
                default -> null;
            };
        }
        return null;
    }
}
