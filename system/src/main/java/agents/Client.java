package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.Resource;
import utils.ServiceFinder;
import utils.enums.ServiceType;
import utils.Utils;

public class Client extends Agent {
    @Override
    protected void setup() {
        addBehaviour(new ServiceFinder(
                this,
                ServiceType.GATEWAY.toString(),
                (DFAgentDescription[] results) -> {
                    AID gatewayAID = results[0].getName();
                    System.out.printf("[%s] Trimit REQUEST pentru resursa 'user' catre %s%n",
                           this.getLocalName(), gatewayAID.getLocalName());

                    addBehaviour(new SendRequestBehaviour(this, gatewayAID, Resource.USER));
                }
        ));
    }

    @Override
    protected void takeDown() {
        Utils.deregisterService(this);
        System.out.println("[" + getLocalName() + "] Client oprit");
    }

    private class SendRequestBehaviour extends OneShotBehaviour {
        AID gatewayAID;
        Resource resource;

        public SendRequestBehaviour(Agent a, AID gatewayAID, Resource resource) {
            super(a);
            this.gatewayAID = gatewayAID;
            this.resource = resource;
        }

        @Override
        public void action() {
            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
            req.addReceiver(gatewayAID);
            req.setContent(resource.toString());
            send(req);

            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage resp = blockingReceive(mt);
            if (resp != null) {
                System.out.printf("[Client] Proxy ales: %s%n", resp.getContent());
            }
        }
    }
}