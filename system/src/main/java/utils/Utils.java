package utils;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.enums.ServiceType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Utils {
    public static class RegisterServiceBehaviour extends OneShotBehaviour {
        private final ServiceType type;
        private final String name;

        public RegisterServiceBehaviour(Agent a, ServiceType type, String name) {
            super(a);
            this.type = type;
            this.name = name;
        }

        @Override
        public void action() {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(myAgent.getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType(type.toString());
            sd.setName(name);
            dfd.addServices(sd);

            try {
                DFService.register(myAgent, dfd);
                System.out.printf("[%s] Registered service: type=%s, name=%s%n",
                        myAgent.getLocalName(), type, name);
            } catch (FIPAException fe) {
                System.err.printf("[%s] DFService.register failed: %s%n",
                        myAgent.getLocalName(), fe.getMessage());
                fe.printStackTrace();
            }
        }
    }

    public static void deregisterService(Agent agent) {
        try {
            DFService.deregister(agent);
            System.out.printf("[%s] Deregistered all services%n",
                    agent.getLocalName());
        } catch (FIPAException fe) {
            System.err.printf("[%s] DFService.deregister failed: %s%n",
                    agent.getLocalName(), fe.getMessage());
            fe.printStackTrace();
        }
    }

    public static int calcutateMaxCapacity(Agent agent ,List<AID> children, String resource) {
        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        children.forEach(lb -> cfp.addReceiver(lb));
        cfp.setContent(resource);
        cfp.setReplyByDate(new Date(System.currentTimeMillis()+5000));
        agent.send(cfp);

        List<ACLMessage> replies = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            ACLMessage reply = agent.blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE), 10000);
            if (reply != null) replies.add(reply);
        }

        return replies.stream().mapToInt(m -> Integer.parseInt(m.getContent())).max().orElse(0);
    }
}
