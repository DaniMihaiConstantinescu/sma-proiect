package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Date;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import utils.ServiceFinder;
import utils.ServiceType;
import utils.Utils;

import java.util.Arrays;

public class Gateway extends Agent {

    @Override
    protected void setup() {
        addBehaviour(new Utils.RegisterServiceBehaviour(this, ServiceType.GATEWAY, "gateway-service"));

        addBehaviour(new CyclicBehaviour(this) {
            private final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

            @Override
            public void action() {
                ACLMessage req = receive(mt);
                if (req != null) {
                    String resource = req.getContent();
                    System.out.printf("[%s] REQUEST pentru %s de la %s%n",
                            getLocalName(), resource, req.getSender().getLocalName());

                    addBehaviour(new ServiceFinder(
                            myAgent,
                            ServiceType.REVERSE_PROXY.toString(),
                            (DFAgentDescription[] proxies) -> {
                                sendCFPAndRespond(req.getSender(), resource, proxies);
                            }
                    ));
                } else {
                    block();
                }
            }
        });
    }

    private void sendCFPAndRespond(AID client, String resource, DFAgentDescription[] proxies) {
        addBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                try {
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    Arrays.stream(proxies).forEach(dfd -> cfp.addReceiver(dfd.getName()));
                    cfp.setContent(resource);
                    cfp.setReplyByDate(new Date(System.currentTimeMillis() + 5000));
                    send(cfp);

                    AID bestProxy = null;
                    int bestCap = -1;
                    int repliesCnt = 0;

                    while (repliesCnt < proxies.length) {
                        ACLMessage reply = blockingReceive(
                                MessageTemplate.or(
                                        MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                                        MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
                                ),
                                10000
                        );
                        if (reply != null && reply.getPerformative() == ACLMessage.PROPOSE) {
                            int cap = Integer.parseInt(reply.getContent());
                            System.out.printf("[%s] Proxy %s a propus cap=%d%n",
                                    getLocalName(), reply.getSender().getLocalName(), cap);
                            if (cap > bestCap) {
                                bestCap = cap;
                                bestProxy = reply.getSender();
                            }
                        }
                        repliesCnt++;
                    }

                    ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                    inform.addReceiver(client);
                    if (bestProxy != null) {
                        inform.setContent(bestProxy.getLocalName());
                        System.out.printf("[%s] Aleg %s cu cap=%d%n",
                                getLocalName(), bestProxy.getLocalName(), bestCap);
                    } else {
                        inform.setContent("no-proxy-available");
                    }
                    send(inform);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void takeDown() {
        Utils.deregisterService(this);
        System.out.println("[" + getLocalName() + "] Gateway oprit");
    }
}
