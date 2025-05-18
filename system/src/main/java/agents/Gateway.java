package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Date;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import utils.InformWebSocketServer;
import utils.ServiceFinder;
import utils.enums.ConversationId;
import utils.enums.InformType;
import utils.enums.ServiceType;
import utils.Utils;
import java.util.Arrays;

public class Gateway extends Agent {
    private AID webSocket;

    @Override
    protected void setup() {
        addBehaviour(new Utils.RegisterServiceBehaviour(this, ServiceType.GATEWAY, "gateway-service"));
        System.out.printf("[%s] Gateway pornit %n", getLocalName());

        // conectare catre manager + update de creare
        addBehaviour(new ServiceFinder(
                this,
                ServiceType.WEBSOCKET_SERVER.toString(),
                (DFAgentDescription[] results) -> {
                    webSocket = results[0].getName();

                    addBehaviour(new InformWebSocketServer(
                            this,
                            "API Gateway created",
                            InformType.LOG,
                            ServiceType.GATEWAY,
                            webSocket
                    ));
                }
        ));

        // asculta dupa request-uri de la client
        addBehaviour(new CyclicBehaviour(this) {
            private final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

            @Override
            public void action() {
                ACLMessage req = receive(mt);
                if (req != null) {
                    String resource = req.getContent();
                    AID client = req.getSender();
                    System.out.printf("[%s] REQUEST for %s from %s%n", getLocalName(), resource, client.getLocalName());

                    String description = String.format("REQUEST for resource %s from %s", resource, client.getLocalName());
                    addBehaviour(new InformWebSocketServer(
                            myAgent,
                            description,
                            InformType.LOG,
                            ServiceType.GATEWAY,
                            webSocket
                    ));

                    addBehaviour(new ServiceFinder(
                            myAgent,
                            ServiceType.REVERSE_PROXY.toString(),
                            (DFAgentDescription[] proxies) -> sendCFPAndRespond(client, resource, proxies)
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
                    // Send CFP to proxies
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    Arrays.stream(proxies).forEach(dfd -> cfp.addReceiver(dfd.getName()));
                    cfp.setContent(resource);
                    cfp.setReplyByDate(new Date(System.currentTimeMillis()+5000));
                    send(cfp);

                    AID bestProxy = null;
                    int bestCap = 101;
                    int replies = 0;

                    while (replies < proxies.length) {
                        ACLMessage reply = blockingReceive(
                                MessageTemplate.or(
                                        MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                                        MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
                                ),
                                10000
                        );
                        if (reply != null && reply.getPerformative()==ACLMessage.PROPOSE) {
                            int cap = Integer.parseInt(reply.getContent());
                            System.out.printf("[%s] Proxy %s proposed cap=%d%n", getLocalName(), reply.getSender().getLocalName(), cap);
                            if (cap < bestCap) {
                                bestCap = cap;
                                bestProxy = reply.getSender();
                            }
                        }
                        replies++;
                    }

                    // Inform client
                    ACLMessage informClient = new ACLMessage(ACLMessage.INFORM);
                    informClient.addReceiver(client);
                    informClient.setContent(bestProxy!=null?bestProxy.getLocalName():"no-proxy-available");
                    send(informClient);

                    // Notify proxy of selection
                    if (bestProxy != null) {
                        ACLMessage notify = new ACLMessage(ACLMessage.INFORM);
                        notify.addReceiver(bestProxy);
                        notify.setConversationId(ConversationId.PROXY_SELECTION.getClassName());
                        notify.setContent(resource);
                        send(notify);
                        System.out.printf("[%s] Notified %s of selection%n", getLocalName(), bestProxy.getLocalName());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void takeDown() {
        Utils.deregisterService(this);
        System.out.printf("[%s] Gateway oprit%n", getLocalName());
    }
}