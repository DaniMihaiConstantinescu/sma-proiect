package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.ServiceType;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ReverseProxy extends Agent {
    private List<AID> loadBalancers = new ArrayList<>();

    @Override
    protected void setup() {
        addBehaviour(new Utils.RegisterServiceBehaviour(this, ServiceType.REVERSE_PROXY, "proxy-service"));

        System.out.printf("[%s] ReverseProxy pornit cu %d LB(s)%n", getLocalName(), loadBalancers.size());

        addBehaviour(new CyclicBehaviour(this) {
            private final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);

            @Override
            public void action() {
                ACLMessage cfp = receive(mt);
                if (cfp != null) {
                    String resource = cfp.getContent();
                    AID requester = cfp.getSender();
                    addBehaviour(new CalculateCapacityBehaviour(requester, resource));
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
                                MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                                10000
                        );
                        if (reply != null) {
                            int cap = Integer.parseInt(reply.getContent());
                            maxCap = Math.max(maxCap, cap);
                            replies++;
                        } else {
                            break;
                        }
                    }
                }

                // Trimitem capacitatea catre gateway (0 daca nu exista LB)
                ACLMessage inform = new ACLMessage(ACLMessage.PROPOSE);
                inform.addReceiver(gateway);
                inform.setContent(String.valueOf(maxCap));
                send(inform);
                System.out.printf("[%s] Am PROPUS maxCap=%d către %s%n", getLocalName(), maxCap, gateway.getLocalName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
