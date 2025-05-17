package utils;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

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
}
