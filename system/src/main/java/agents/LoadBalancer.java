package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.*;
import utils.enums.AgentClass;
import utils.enums.ConversationId;
import utils.enums.InformType;
import utils.enums.ServiceType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jade.wrapper.AgentController;

public class LoadBalancer extends Agent {
    private Map<String, List<AID>> nodesMap = new HashMap<>();
    private AID webSocket;
    private String parentId;

    @Override
    protected void setup() {
        addBehaviour(new Utils.RegisterServiceBehaviour(this, ServiceType.LOAD_BALANCER, "load-balancer-service"));
        System.out.printf("[%s] LoadBalancer pornit%n", getLocalName());

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            parentId = (String) args[0];
        }

        // Connect to manager and send creation update
        addBehaviour(new ServiceFinder(
                this,
                ServiceType.WEBSOCKET_SERVER.toString(),
                (DFAgentDescription[] results) -> {
                    webSocket = results[0].getName();

                    addBehaviour(new InformWebSocketServer(
                            this,
                            InformType.CREATE,
                            ServiceType.LOAD_BALANCER,
                            webSocket,
                            parentId
                    ));

                    String description = String.format("Load balancer created by %s", parentId);
                    addBehaviour(new InformWebSocketServer(
                            this,
                            description,
                            InformType.LOG,
                            ServiceType.LOAD_BALANCER,
                            webSocket
                    ));
                }
        ));

        // Listen for capacity requests
        addBehaviour(new NodeCapacityCalculatorListener(this, nodesMap));

        // Listen for node assignment notifications
        addBehaviour(new CyclicBehaviour(this) {
            private final MessageTemplate selMt = MessageTemplate.MatchConversationId(
                    ConversationId.NODE_ASSIGNMENT.getClassName());

            @Override
            public void action() {
                ACLMessage msg = receive(selMt);
                if (msg != null) {
                    String resource = msg.getContent();
                    addBehaviour(new HandleSelectionBehaviour(resource));
                } else {
                    block();
                }
            }
        });

        // Behavior to remove unused nodes every 10s
        addBehaviour(new NodeCleanupBehaviour(this, nodesMap, webSocket));
    }

    @Override
    protected void takeDown() {
        Utils.deregisterService(this);
        System.out.printf("[%s] LoadBalancer stopped%n", getLocalName());
    }

    private class HandleSelectionBehaviour extends OneShotBehaviour {
        private final String resource;

        public HandleSelectionBehaviour(String resource) {
            super(LoadBalancer.this);
            this.resource = resource;
        }

        @Override
        public void action() {
            List<AID> nodes = nodesMap.computeIfAbsent(resource, k -> new ArrayList<>());
            if (nodes.isEmpty()) {
                addBehaviour(new CreateNodeBehaviour(resource));
            } else {
                addBehaviour(new EvaluateChildrenBehavior(
                        myAgent,
                        nodes,
                        resource,
                        90,
                        5000,
                        AgentClass.NODE,
                        ConversationId.NODE_ASSIGNMENT,
                        webSocket
                ));
            }
        }
    }

    private class CreateNodeBehaviour extends OneShotBehaviour {
        private final String resource;

        public CreateNodeBehaviour(String resource) {
            super(LoadBalancer.this);
            this.resource = resource;
        }

        @Override
        public void action() {
            try {
                String nodeName = resource + "-Node-" + java.util.UUID.randomUUID();
                AgentController nc = getContainerController().createNewAgent(
                        nodeName,
                        AgentClass.NODE.getClassName(),
                        new Object[]{getLocalName(), resource}
                );
                nc.start();
                AID newNodeAID = new AID(nodeName, AID.ISLOCALNAME);

                // Add to map
                nodesMap.computeIfAbsent(resource, k -> new ArrayList<>()).add(newNodeAID);
                System.out.printf("[%s] Created new Node %s for resource %s%n", getLocalName(), nodeName, resource);

                // Notify after 1s
                addBehaviour(new WakerBehaviour(myAgent, 1000) {
                    @Override
                    protected void onWake() {
                        ACLMessage notify = new ACLMessage(ACLMessage.INFORM);
                        notify.addReceiver(newNodeAID);
                        notify.setConversationId(ConversationId.NODE_ASSIGNMENT.getClassName());
                        notify.setContent(resource);
                        send(notify);
                        System.out.printf("[%s] Notified new Node %s for resource %s%n", getLocalName(), nodeName, resource);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
