package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.ServiceType;
import utils.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import jade.wrapper.AgentController;

public class LoadBalancer extends Agent {
    private List<AID> nodes = new ArrayList<>();

    @Override
    protected void setup() {
        addBehaviour(new Utils.RegisterServiceBehaviour(this, ServiceType.LOAD_BALANCER, "load-balancer-service"));
        System.out.printf("[%s] LoadBalancer pornit%n", getLocalName());

        // asculta pentru calcul capacitate
        addBehaviour(new CyclicBehaviour(this) {
            private final MessageTemplate cfpMt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            @Override
            public void action() {
                ACLMessage cfp = receive(cfpMt);
                if (cfp != null) {
                    addBehaviour(new CalculateCapacityBehaviour(cfp.getSender(), cfp.getContent()));
                } else {
                    block();
                }
            }
        });

        // asculta notificare de selectie nod
        addBehaviour(new CyclicBehaviour(this) {
            private final MessageTemplate selMt = MessageTemplate.MatchConversationId("node-assignment");
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

    private class CalculateCapacityBehaviour extends OneShotBehaviour {
        private final AID proxy;
        private final String resource;

        public CalculateCapacityBehaviour(AID proxy, String resource) {
            super(LoadBalancer.this);
            this.proxy = proxy;
            this.resource = resource;
        }

        @Override
        public void action() {
            try {
                int maxCap = 0;
                if (!nodes.isEmpty()) {
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    nodes.forEach(n -> cfp.addReceiver(n));
                    cfp.setContent(resource);
                    cfp.setReplyByDate(new Date(System.currentTimeMillis() + 5000));
                    send(cfp);

                    int replies = 0;
                    while (replies < nodes.size()) {
                        ACLMessage reply = blockingReceive(
                                MessageTemplate.MatchPerformative(ACLMessage.PROPOSE), 10000);
                        if (reply != null) {
                            int cap = Integer.parseInt(reply.getContent());
                            maxCap = Math.max(maxCap, cap);
                            replies++;
                        } else break;
                    }
                }
                ACLMessage propose = new ACLMessage(ACLMessage.PROPOSE);
                propose.addReceiver(proxy);
                propose.setContent(String.valueOf(maxCap));
                send(propose);
                System.out.printf("[%s] Am propus capacitate=%d către %s%n", getLocalName(), maxCap, proxy.getLocalName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
                addBehaviour(new EvaluateNodesBehaviour(resource));
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
                AgentController nc = getContainerController().createNewAgent(nodeName, "agents.Node", null);
                nc.start();
                nodes.add(new AID(nodeName, AID.ISLOCALNAME));
                System.out.printf("[%s] Created new Node %s%n", getLocalName(), nodeName);

                // Notificare după 1s către nodul creat
                addBehaviour(new WakerBehaviour(myAgent, 1000) {
                    @Override
                    protected void onWake() {
                        ACLMessage notify = new ACLMessage(ACLMessage.INFORM);
                        notify.addReceiver(new AID(nodeName, AID.ISLOCALNAME));
                        notify.setConversationId("node-assignment");
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

    private class EvaluateNodesBehaviour extends OneShotBehaviour {
        private final String resource;

        public EvaluateNodesBehaviour(String resource) {
            super(LoadBalancer.this);
            this.resource = resource;
        }

        @Override
        public void action() {
            try {
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                nodes.forEach(n -> cfp.addReceiver(n));
                cfp.setContent(resource);
                cfp.setReplyByDate(new Date(System.currentTimeMillis() + 5000));
                send(cfp);

                List<ACLMessage> replies = new ArrayList<>();
                for (int i = 0; i < nodes.size(); i++) {
                    ACLMessage reply = blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE), 10000);
                    if (reply != null) replies.add(reply);
                }
                int maxCap = replies.stream()
                        .mapToInt(m -> Integer.parseInt(m.getContent()))
                        .max().orElse(0);

                if (maxCap > 90) {
                    addBehaviour(new CreateNodeBehaviour(resource));
                } else {
                    ACLMessage best = replies.stream()
                            .max((a, b) -> Integer.compare(Integer.parseInt(a.getContent()), Integer.parseInt(b.getContent())))
                            .orElse(null);
                    if (best != null) {
                        String chosen = best.getSender().getLocalName();
                        System.out.printf("[%s] Chosen Node %s with cap=%s%n", getLocalName(), chosen, best.getContent());

                        ACLMessage notify = new ACLMessage(ACLMessage.INFORM);
                        notify.addReceiver(new AID(chosen, AID.ISLOCALNAME));
                        notify.setConversationId("node-assignment");
                        notify.setContent(resource);
                        send(notify);
                        System.out.printf("[%s] Notified Node %s for resource %s%n", getLocalName(), chosen, resource);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
