package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.Utils;

public class LoadBalancer extends Agent {
    @Override
    protected void setup() {
        System.out.printf("[%s] LoadBalancer pornit%n", getLocalName());

        // raspunde cu capacitate random intre 1% si 100%
        addBehaviour(new CyclicBehaviour(this) {
            private final MessageTemplate cfpMt = MessageTemplate.MatchPerformative(ACLMessage.CFP);

            @Override
            public void action() {
                ACLMessage cfp = receive(cfpMt);
                if (cfp != null) {
                    int randomCap = 1 + (int) (Math.random() * 100);
                    ACLMessage reply = cfp.createReply();
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(randomCap));
                    send(reply);
                    System.out.printf("[%s] Am PROPUS capacitate random=%d către %s%n", getLocalName(), randomCap, cfp.getSender().getLocalName());
                } else {
                    block();
                }
            }
        });

        // asculta notificare de selectie
        addBehaviour(new CyclicBehaviour(this) {
            private final MessageTemplate selMt = MessageTemplate.MatchConversationId("lb-assignment");

            @Override
            public void action() {
                ACLMessage msg = receive(selMt);
                if (msg != null) {
                    String resource = msg.getContent();
                    System.out.printf("[%s] Am fost selectat pentru resource '%s'!%n", getLocalName(), resource);
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
}
