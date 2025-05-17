package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.ServiceType;
import utils.Utils;

public class ReverseProxy extends Agent {
    private int capacity;

    @Override
    protected void setup() {
        addBehaviour(new Utils.RegisterServiceBehaviour(this, ServiceType.REVERSE_PROXY, "proxy-service"));

        // Random capacity between 50 and 150
        this.capacity = 50 + (int) (Math.random() * 100);
        System.out.printf("[%s] Capacity = %d%n", getLocalName(), capacity);

        addBehaviour(new CyclicBehaviour(this) {
            private final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);

            @Override
            public void action() {
                ACLMessage cfp = receive(mt);
                if (cfp != null) {
                    // Răspund cu capacity
                    ACLMessage reply = cfp.createReply();
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(capacity));
                    send(reply);
                    System.out.printf("[%s] Am PROPUS capacity=%d către %s%n",
                            getLocalName(), capacity, cfp.getSender().getLocalName());
                } else {
                    block();
                }
            }
        });
    }

    @Override
    protected void takeDown() {
        Utils.deregisterService(this);
        System.out.println("[" + getLocalName() + "] Reverse proxy oprit");
    }
}