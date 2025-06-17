package utils;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.enums.ConversationId;
import utils.enums.InformType;
import utils.enums.ServiceType;

import java.util.*;

public class NodeCleanupBehaviour extends TickerBehaviour {
    private static final long CHECK_INTERVAL = 10_000;
    private final Map<String, List<AID>> nodesMap;
    private final AID webSocket;

    public NodeCleanupBehaviour(Agent a, Map<String, List<AID>> nodesMap, AID webSocket) {
        super(a, CHECK_INTERVAL);
        this.nodesMap = nodesMap;
        this.webSocket = webSocket;
    }

    @Override
    protected void onTick() {
        nodesMap.forEach((resource, nodes) -> {
            if (nodes.size() < 2) return;

            Map<AID,Integer> capMap = new HashMap<>();
            nodes.forEach(node -> {
                try {
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    cfp.addReceiver(node);
                    cfp.setContent(resource);
                    cfp.setReplyByDate(new Date(System.currentTimeMillis() + 5_000));
                    myAgent.send(cfp);

                    ACLMessage reply = myAgent.blockingReceive(
                            MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                            5_000
                    );
                    if (reply != null) {
                        capMap.put(node, Integer.parseInt(reply.getContent()));
                    }
                } catch (Exception ignored) {}
            });

            List<AID> toRemove = new ArrayList<>();
            capMap.forEach((node, cap) -> {
                if (cap > 80) toRemove.add(node);
            });

            for (AID candidate : toRemove) {
                int candLoadPercent = 100 - capMap.get(candidate);
                int freeCapacityPercent = capMap.entrySet().stream()
                        .filter(e -> !e.getKey().equals(candidate))
                        .mapToInt(Map.Entry::getValue)
                        .sum();

                if (freeCapacityPercent >= candLoadPercent) {
                    ACLMessage kill = new ACLMessage(ACLMessage.INFORM);
                    kill.addReceiver(candidate);
                    kill.setConversationId(ConversationId.NODE_SHUTDOWN.getClassName());
                    kill.setContent("shutdown");
                    myAgent.send(kill);

                    String description = String.format("Load balancer deleting node %s for resource %s", candidate.getLocalName(), resource);
                    myAgent.addBehaviour(new InformWebSocketServer(
                            myAgent,
                            description,
                            InformType.LOG,
                            ServiceType.LOAD_BALANCER,
                            webSocket
                    ));

                    nodes.remove(candidate);
                    System.out.printf("[%s] Removed node %s for resource %s%n",
                            myAgent.getLocalName(), candidate.getLocalName(), resource);
                }
            }
        });
    }
}