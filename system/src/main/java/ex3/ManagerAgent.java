package ex3;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ManagerAgent extends Agent {
    private static final long serialVersionUID = 1L;
    private List<String> agentsList = new ArrayList<>(List.of("Ana", "Maria", "Tudor", "Mihai", "George"));
    private List<AgentMessage> messageQueue = new ArrayList<>();

    @Override
    public void setup()
    {
        addBehaviour(new ReciverBehaviour());
        addBehaviour(new MyTickBehavior(this, 5000));
    }

    @Override
    protected void takeDown() {
        super.takeDown();
    }

    private class MyTickBehavior extends TickerBehaviour {

        public MyTickBehavior(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            if (!agentsList.isEmpty()) {
                for (String agent : agentsList) {
                    addBehaviour(new SendMessageBehaviour(agent, "PING"));
                }
                addBehaviour(new VerifyStatusBehaviour(myAgent, 2000));
            }else {
                myAgent.doDelete();
            }
        }
    }

    private class VerifyStatusBehaviour extends WakerBehaviour {
        public VerifyStatusBehaviour(Agent a, long timeout) {
            super(a, timeout);
        }

        protected void onWake() {
            List<String> activeAgents = new ArrayList<>();
            System.out.println(messageQueue);
            for (String agent : agentsList) {
                if (messageQueue.stream().anyMatch(m -> Objects.equals(m.sender, agent)))
                    activeAgents.add(agent);
            }
            messageQueue.clear();
            agentsList = activeAgents;

            if (activeAgents.isEmpty()) {
                System.out.println("No active agents");
                myAgent.doDelete();
            } else {
                System.out.println("activeAgents: " + activeAgents);
            }
        }
    }

    private class SendMessageBehaviour extends OneShotBehaviour {
        String content;
        String agentName;

        public SendMessageBehaviour(String agentName, String content) {
            this.agentName = agentName;
            this.content = content;
        }

        @Override
        public void action() {
            ACLMessage messageSent = new ACLMessage();
            AID receiverAID = new AID(agentName, AID.ISLOCALNAME);
            messageSent.addReceiver(receiverAID);
            messageSent.setContent(content);
            myAgent.send(messageSent);
        }
    }

    private class ReciverBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage message = myAgent.receive();
            if (message != null)
            {
                messageQueue.add(new AgentMessage(
                        message.getSender().getLocalName(),
                        myAgent.getName(), message.getContent())
                );
            } //end if
            else
            {
                /* foarte important: fara acest else comportamentul se executa la infinit, nepermitand executarea altor comportamente
                 */
                block();
            }
        }
    }


}
