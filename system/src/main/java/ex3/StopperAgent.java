package ex3;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.List;

public class StopperAgent extends Agent {
    private static final long serialVersionUID = 1L;
    private List<String> agentsList = new ArrayList<>(List.of("Ana", "Maria", "Tudor", "Mihai", "George"));

    @Override
    public void setup()
    {
        addBehaviour(new MyTickBehavior(this, 3000, agentsList));
    }

    @Override
    protected void takeDown() {
        super.takeDown();
    }

    private class MyTickBehavior extends TickerBehaviour {
        private List<String> agents;

        public MyTickBehavior(Agent a, long period, List<String> agents) {
            super(a, period);
            this.agents = agents;
        }

        @Override
        protected void onTick() {
            if (agents.size() > 0) {
                int random = (int)(Math.random() * agents.size());
                addBehaviour(new SendMessageBehaviour(agents.get(random), "STOP"));
                agents.remove(random);
            }else {
                myAgent.doDelete();
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

}
