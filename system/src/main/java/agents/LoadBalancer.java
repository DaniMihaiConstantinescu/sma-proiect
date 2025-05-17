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
import java.util.Arrays;
import java.util.List;
import jade.wrapper.AgentController;

public class LoadBalancer extends Agent {
    private List<AID> nodes = new ArrayList<>();
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
                            ServiceType.LOAD_BALANCER,
                            webSocketAID,
                            parentId
                            ));
                }
        ));

        // asculta pentru calcul capacitate
        addBehaviour(new CapacityCalculatorListener(this,nodes));

        // asculta notificare de selectie nod
        addBehaviour(new CyclicBehaviour(this) {
            private final MessageTemplate selMt = MessageTemplate.MatchConversationId(ConversationId.NODE_ASSIGNMENT.getClassName());
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
    }

    @Override
    protected void takeDown() {
        Utils.deregisterService(this);
        System.out.printf("[%s] LoadBalancer oprit%n", getLocalName());
    }

    private class HandleSelectionBehaviour extends OneShotBehaviour {
        private final String resource;

        public HandleSelectionBehaviour(String resource) {
            super(LoadBalancer.this);
            this.resource = resource;
        }

        @Override
        public void action() {
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
                        ConversationId.NODE_ASSIGNMENT
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
                String nodeName = "node-" + java.util.UUID.randomUUID();
                AgentController nc = getContainerController().createNewAgent(
                        nodeName,
                        AgentClass.NODE.getClassName(),
                        new Object[]{getLocalName()}
                );
                nc.start();
                nodes.add(new AID(nodeName, AID.ISLOCALNAME));
                System.out.printf("[%s] Created new Node %s%n", getLocalName(), nodeName);

                // Notificare după 1s catre nodul creat
                addBehaviour(new WakerBehaviour(myAgent, 1000) {
                    @Override
                    protected void onWake() {
                        ACLMessage notify = new ACLMessage(ACLMessage.INFORM);
                        notify.addReceiver(new AID(nodeName, AID.ISLOCALNAME));
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
