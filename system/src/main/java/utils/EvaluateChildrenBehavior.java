package utils;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;

import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import utils.enums.AgentClass;
import utils.enums.ConversationId;
import utils.enums.InformType;
import utils.enums.ServiceType;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;


public class EvaluateChildrenBehavior extends OneShotBehaviour {
    List<AID> children;
    String resource;
    int threshold;
    long delayMs;
    AgentClass agentClass;
    ConversationId convId;
    private AID webSocket;


    public EvaluateChildrenBehavior(Agent a, List<AID> children, String resource, int threshold, long delayMs, AgentClass agentClass, ConversationId convId, AID webSocket) {
        super(a);
        this.children = children;
        this.resource = resource;
        this.threshold = threshold;
        this.delayMs = delayMs;
        this.agentClass = agentClass;
        this.convId = convId;
        this.webSocket = webSocket;
    }

    @Override
    public void action() {
        try {
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            children.forEach(cfp::addReceiver);
            cfp.setContent(resource);
            cfp.setReplyByDate(new Date(System.currentTimeMillis() + delayMs));
            myAgent.send(cfp);

            List<ACLMessage> replies =
                    children.stream()
                            .map(c -> myAgent.blockingReceive(
                                    MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                                    delayMs))
                            .filter(m -> m != null)
                            .toList();

            int maxCap = replies.stream()
                    .mapToInt(m -> {
                        try { return Integer.parseInt(m.getContent()); }
                        catch (Exception e){ return 0; }
                    })
                    .max().orElse(0);

            if (children.isEmpty() || 100 - maxCap > threshold) {

                String newName = "";
                if (agentClass.getClassName().substring(agentClass.getClassName().lastIndexOf('.') + 1) == "Node"){
                    newName += resource + "-";
                }

                newName +=  agentClass.getClassName().substring(agentClass.getClassName().lastIndexOf('.') + 1)
                        + "-" + UUID.randomUUID();
                AgentController ac = myAgent.getContainerController().createNewAgent(
                    newName,
                    agentClass.getClassName(),
                     new Object[]{myAgent.getLocalName(), resource}
                );
                ac.start();
                AID newAID = new AID(newName, AID.ISLOCALNAME);
                children.add(newAID);
                System.out.printf("[%s] Created new %s %s%n",
                        myAgent.getLocalName(),
                        agentClass, newName);

                final String finalName = newName;
                myAgent.addBehaviour(new WakerBehaviour(myAgent, 1000) {
                    @Override
                    protected void onWake() {
                        ACLMessage notify = new ACLMessage(ACLMessage.INFORM);
                        notify.addReceiver(newAID);
                        notify.setConversationId(convId.getClassName());
                        notify.setContent(resource);
                        myAgent.send(notify);
                        System.out.printf("[%s] Notified new %s %s for resource %s%n",
                                myAgent.getLocalName(),
                                agentClass, finalName, resource);
                    }
                });

            } else {
                // alege agentul cu maxCap
                ACLMessage best = replies.stream()
                        .max(Comparator.comparingInt(a -> Integer.parseInt(a.getContent())))
                        .orElse(null);

                if (best != null) {
                    AID chosen = best.getSender();
                    System.out.printf("[%s] Chosen %s %s with cap=%s%n",
                            myAgent.getLocalName(),
                            agentClass, chosen.getLocalName(),
                            best.getContent());

                    String description = String.format("Chosen %s %s with cap=%s",
                            agentClass, chosen.getLocalName(),
                            best.getContent());

                    myAgent.addBehaviour(new InformWebSocketServer(
                            myAgent,
                            description,
                            InformType.LOG,
                            ServiceType.GATEWAY,
                            webSocket
                    ));

                    ACLMessage notify = new ACLMessage(ACLMessage.INFORM);
                    notify.addReceiver(chosen);
                    notify.setConversationId(convId.getClassName());
                    notify.setContent(resource);
                    myAgent.send(notify);
                    System.out.printf("[%s] Notified %s %s for resource %s%n",
                            myAgent.getLocalName(),
                            agentClass, chosen.getLocalName(),
                            resource);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


