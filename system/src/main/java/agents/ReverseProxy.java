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

public class ReverseProxy extends Agent {
    private List<AID> loadBalancers = new ArrayList<>();

    @Override
    protected void setup() {
        addBehaviour(new Utils.RegisterServiceBehaviour(this, ServiceType.REVERSE_PROXY, "proxy-service"));
        System.out.printf("[%s] ReverseProxy pornit%n", getLocalName());

        // asculta pentru calcul capacitate
        addBehaviour(new CyclicBehaviour(this) {
            private final MessageTemplate capMt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            @Override
            public void action() {
                ACLMessage cfp = receive(capMt);
                if (cfp != null) {
                    addBehaviour(new CalculateCapacityBehaviour(cfp.getSender(), cfp.getContent()));
                } else {
                    block();
                }
            }
        });

        // ascult notificare selectie proxy
        addBehaviour(new CyclicBehaviour(this) {
            private final MessageTemplate selMt = MessageTemplate.MatchConversationId("proxy-selection");
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

    private class CalculateCapacityBehaviour extends OneShotBehaviour {
        private final AID gateway;
        private final String resource;

        public CalculateCapacityBehaviour(AID gateway, String resource) {
            super(ReverseProxy.this);
            this.gateway = gateway;
            this.resource = resource;
        }

        @Override
        public void action() {
            try {
                int maxCap = 0;
                if (!loadBalancers.isEmpty()) {
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    loadBalancers.forEach(lb -> cfp.addReceiver(lb));
                    cfp.setContent(resource);
                    cfp.setReplyByDate(new Date(System.currentTimeMillis() + 5000));
                    send(cfp);

                    int replies = 0;
                    while (replies < loadBalancers.size()) {
                        ACLMessage reply = blockingReceive(
                                MessageTemplate.MatchPerformative(ACLMessage.PROPOSE), 10000);
                        if (reply != null) {
                            int cap = Integer.parseInt(reply.getContent());
                            maxCap = Math.max(maxCap, cap);
                            replies++;
                        } else {
                            break;
                        }
                    }
                }
                ACLMessage propose = new ACLMessage(ACLMessage.PROPOSE);
                propose.addReceiver(gateway);
                propose.setContent(String.valueOf(maxCap));
                send(propose);
                System.out.printf("[%s] Replied capacity=%d to %s%n", getLocalName(), maxCap, gateway.getLocalName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
                addBehaviour(new EvaluateLBsBehaviour(resource));
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
                AgentController lb = getContainerController().createNewAgent(lbName, "agents.LoadBalancer", null);
                lb.start();
                loadBalancers.add(new AID(lbName, AID.ISLOCALNAME));
                System.out.printf("[%s] Created new LB %s%n", getLocalName(), lbName);
                // dupa 1s, trimite notificare pt lb ca a fost selectat + resursa
                addBehaviour(new WakerBehaviour(myAgent, 1000) {
                    @Override
                    protected void onWake() {
                        ACLMessage notify = new ACLMessage(ACLMessage.INFORM);
                        notify.addReceiver(new AID(lbName, AID.ISLOCALNAME));
                        notify.setConversationId("node-assignment");
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

    private class EvaluateLBsBehaviour extends OneShotBehaviour {
        private final String resource;

        public EvaluateLBsBehaviour(String resource) {
            super(ReverseProxy.this);
            this.resource = resource;
        }

        @Override
        public void action() {
            try {
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                loadBalancers.forEach(lb -> cfp.addReceiver(lb));
                cfp.setContent(resource);
                cfp.setReplyByDate(new Date(System.currentTimeMillis()+5000));
                send(cfp);

                List<ACLMessage> replies = new ArrayList<>();
                for (int i = 0; i < loadBalancers.size(); i++) {
                    ACLMessage reply = blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE), 10000);
                    if (reply != null) replies.add(reply);
                }
                int maxCap = replies.stream().mapToInt(m -> Integer.parseInt(m.getContent())).max().orElse(0);
                if (maxCap > 90) {
                    addBehaviour(new CreateLBBehaviour(resource));
                } else {
                    // alege LB cu max capacity
                    ACLMessage best = replies.stream()
                            .max((a,b) -> Integer.compare(Integer.parseInt(a.getContent()), Integer.parseInt(b.getContent())))
                            .orElse(null);
                    if (best != null) {

                        String chosen = best.getSender().getLocalName();
                        System.out.printf("[%s] Chosen LB %s with cap=%s%n", getLocalName(), chosen, best.getContent());

                        ACLMessage notify = new ACLMessage(ACLMessage.INFORM);
                        notify.addReceiver(new AID(chosen, AID.ISLOCALNAME));
                        notify.setConversationId("node-assignment");
                        notify.setContent(resource);
                        send(notify);
                        System.out.printf("[%s] Notified LB %s for resource %s%n", getLocalName(), chosen, resource);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
