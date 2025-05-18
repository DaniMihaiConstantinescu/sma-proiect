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
import java.util.List;
import jade.wrapper.AgentController;

public class ReverseProxy extends Agent {
    private List<AID> loadBalancers = new ArrayList<>();
    private AID webSocket;

    @Override
    protected void setup() {
        addBehaviour(new Utils.RegisterServiceBehaviour(this, ServiceType.REVERSE_PROXY, "proxy-service"));
        System.out.printf("[%s] ReverseProxy pornit%n", getLocalName());

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
                            ServiceType.REVERSE_PROXY,
                            webSocketAID));

                    addBehaviour(new InformWebSocketServer(
                            this,
                            "Reverse proxy created",
                            InformType.LOG,
                            ServiceType.LOAD_BALANCER,
                            webSocket
                    ));
                }
        ));

        // asculta pentru calcul capacitate
        addBehaviour(new CapacityCalculatorListener(this,loadBalancers));

        // ascult notificare selectie proxy
        addBehaviour(new CyclicBehaviour(this) {
            private final MessageTemplate selMt = MessageTemplate.MatchConversationId(ConversationId.PROXY_SELECTION.getClassName());
            @Override
            public void action() {
                ACLMessage msg = receive(selMt);
                if (msg != null) {
                    addBehaviour(new HandleSelectionBehaviour(msg.getContent()));
                } else {
                    block();
                }
            }
        });
    }

    @Override
    protected void takeDown() {
        Utils.deregisterService(this);
        System.out.printf("[%s] ReverseProxy oprit%n", getLocalName());
    }

    private class HandleSelectionBehaviour extends OneShotBehaviour {
        private final String resource;

        public HandleSelectionBehaviour(String resource) {
            super(ReverseProxy.this);
            this.resource = resource;
        }

        @Override
        public void action() {
            if (loadBalancers.isEmpty()) {
                addBehaviour(new CreateLBBehaviour(resource));
            } else {
                addBehaviour(new EvaluateChildrenBehavior(
                        myAgent,
                        loadBalancers,
                        resource,
                        90,
                        5000,
                        AgentClass.LOAD_BALANCER,
                        ConversationId.NODE_ASSIGNMENT,
                        webSocket
                ));
            }
        }
    }

    private class CreateLBBehaviour extends OneShotBehaviour {
        private final String resource;
        public CreateLBBehaviour(String resource) {
            super(ReverseProxy.this);
            this.resource = resource;
        }

        @Override
        public void action() {
            try {
                String lbName = "lb-" + java.util.UUID.randomUUID();
                AgentController lb = getContainerController().createNewAgent(
                        lbName,
                        AgentClass.LOAD_BALANCER.getClassName(),
                        new Object[]{getLocalName()}
                );
                lb.start();
                loadBalancers.add(new AID(lbName, AID.ISLOCALNAME));
                System.out.printf("[%s] Created new LB %s%n", getLocalName(), lbName);

                // dupa 1s, trimite notificare pt lb ca a fost selectat + resursa
                addBehaviour(new WakerBehaviour(myAgent, 1000) {
                    @Override
                    protected void onWake() {
                        ACLMessage notify = new ACLMessage(ACLMessage.INFORM);
                        notify.addReceiver(new AID(lbName, AID.ISLOCALNAME));
                        notify.setConversationId(ConversationId.NODE_ASSIGNMENT.getClassName());
                        notify.setContent(resource);
                        send(notify);
                        System.out.printf("[%s] Notified new LB %s for resource %s%n", getLocalName(), lbName, resource);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
