package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.enums.ConversationId;

public class Node extends Agent {
    private static final long serialVersionUID = 1L;

    @Override
    protected void setup() {
        System.out.printf("[%s] Node pornit%n", getLocalName());

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

        addBehaviour(new CyclicBehaviour(this) {
            private final MessageTemplate selMt = MessageTemplate.MatchConversationId(ConversationId.NODE_ASSIGNMENT.getClassName());
            @Override
            public void action() {
                ACLMessage msg = receive(selMt);
                if (msg != null) {
                    String resource = msg.getContent();
                    System.out.printf("[%s] Am primit request pentru %s%n", getLocalName(), resource);
                } else {
                    block();
                }
            }
        });
    }

    @Override
    protected void takeDown() {
        System.out.printf("[%s] Node oprit%n", getLocalName());
    }
}
