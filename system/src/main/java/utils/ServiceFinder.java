package utils;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.function.Consumer;

public class ServiceFinder extends OneShotBehaviour {

    private static final long serialVersionUID = 1L;

    private final String serviceType;
    private final Consumer<DFAgentDescription[]> onFound;

    public ServiceFinder(Agent a, String serviceType, Consumer<DFAgentDescription[]> onFound) {
        super(a);
        this.serviceType = serviceType;
        this.onFound = onFound;
    }

    @Override
    public void action() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(myAgent, template);
            if (result.length > 0) {
                onFound.accept(result);
            } else {
                myAgent.addBehaviour(new NotificationBehaviour(template));
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private class NotificationBehaviour extends OneShotBehaviour {
        private final DFAgentDescription template;

        public NotificationBehaviour(DFAgentDescription template) {
            this.template = template;
        }

        @Override
        public void action() {
            SearchConstraints sc = new SearchConstraints();
            sc.setMaxResults(1L);

            ACLMessage subscribe = DFService.createSubscriptionMessage(
                    myAgent,
                    myAgent.getDefaultDF(),
                    template,
                    sc
            );
            myAgent.send(subscribe);

            myAgent.addBehaviour(new WaitForServiceNotification());
        }
    }

    private class WaitForServiceNotification extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(MessageTemplate.MatchSender(myAgent.getDefaultDF()));
            if (msg != null) {
                try {
                    DFAgentDescription[] dfds = DFService.decodeNotification(msg.getContent());
                    if (dfds.length > 0) {
                        System.out.println(myAgent.getLocalName() + ": Serviciu aparut prin notificare.");
                        onFound.accept(dfds);
                        myAgent.removeBehaviour(this);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }
    }
}

