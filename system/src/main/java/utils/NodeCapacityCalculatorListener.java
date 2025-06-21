package utils;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public class NodeCapacityCalculatorListener extends CyclicBehaviour {
    private final MessageTemplate cfpMt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
    private final Map<String, List<AID>> childrenMap;

    public NodeCapacityCalculatorListener(Agent a, Map<String, List<AID>> childrenMap) {
        super(a);
        this.childrenMap = childrenMap;
    }

    @Override
    public void action() {
        ACLMessage cfp = myAgent.receive(cfpMt);
        if (cfp != null) {
            String resource = cfp.getContent();
            List<AID> children = childrenMap.getOrDefault(resource, Collections.emptyList());
            myAgent.addBehaviour(new CalculateCapacity(myAgent, children, cfp.getSender(), resource));
        } else {
            block();
        }
    }

    private static class CalculateCapacity extends OneShotBehaviour {
        private final List<AID> children;
        private final AID sender;
        private final String resource;

        public CalculateCapacity(Agent a, List<AID> children, AID sender, String resource) {
            super(a);
            this.children = children;
            this.sender = sender;
            this.resource = resource;
        }

        @Override
        public void action() {
            try {
                int maxCap = 0;
                if (children.isEmpty()) {
                    maxCap = 101;
                } else {
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    children.forEach(cfp::addReceiver);
                    cfp.setContent(resource);
                    long timeout = 10000;
                    cfp.setReplyByDate(new Date(System.currentTimeMillis() + timeout));
                    myAgent.send(cfp);

                    int replies = 0;
                    while (replies < children.size()) {
                        ACLMessage reply = myAgent.blockingReceive(
                                MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                                timeout
                        );
                        if (reply != null) {
                            int cap = Integer.parseInt(reply.getContent());
                            if (cap > maxCap) {
                                maxCap = cap;
                            }
                            replies++;
                        } else {
                            break;
                        }
                    }
                }
                // Send result back
                ACLMessage propose = new ACLMessage(ACLMessage.PROPOSE);
                propose.addReceiver(sender);
                propose.setContent(String.valueOf(maxCap));
                myAgent.send(propose);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
