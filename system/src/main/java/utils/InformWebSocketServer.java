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
    private String resourceType = null;

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

    public InformWebSocketServer(Agent agent, InformType informType, ServiceType instanceType, AID serverAID, String parentId, String resourceType) {
        super(agent);
        this.informType = informType;
        this.instanceType = instanceType;
        this.serverAID = serverAID;
        this.parentId = parentId;
        this.resourceType = resourceType;
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

        } else if (informType == InformType.DELETE && instanceType == ServiceType.NODE) {


            content = String.format(
                    "{\"type\": \"deleteNode\", \"data\": { \"id\": \"%s\", \"parentId\": \"%s\", \"resourceType\": \"%s\" }}",
                    myAgent.getLocalName(),
                    parentId,
                    resourceType != null ? resourceType : ""
            );

        } else {
            String type = mapToPayloadType(informType, instanceType);
            if (type == null) return;

            StringBuilder sb = new StringBuilder();
            sb.append(String.format(
                    "{\"type\": \"%s\", \"data\": { \"id\": \"%s\", \"childrenIds\": [], \"capacity\": 0, \"parentId\": \"%s\"",
                    type,
                    myAgent.getLocalName(),
                    parentId
            ));

            if (instanceType == ServiceType.NODE && resourceType != null) {
                sb.append(String.format(", \"resourceType\": \"%s\"", resourceType));
            }
            sb.append(" }}");
            content = sb.toString();
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
