package ex2;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


public class Agent1 extends Agent {
    private static final long serialVersionUID = 1L;
    private List<String> agentsList = new ArrayList<>(List.of("Ana", "Maria", "Tudor", "Mihai", "George"));

    @Override
    public void setup()
    {
        addBehaviour(new ReciverBehaviour());

        Integer tickDuration = 5000;
        Object[] args = getArguments();
        if (args != null && args.length > 0)
            tickDuration = (Integer) args[0];

        Integer agentIndex = agentsList.indexOf( this.getLocalName() );
        agentsList.remove(agentIndex);

        addBehaviour(new MyTickBehaviour(this ,tickDuration, agentsList));
    }

    @Override
    protected void takeDown() {
        super.takeDown();
    }

    protected void sendMessage(Agent senderAgent ,String targetAgentName, String content) {
        ACLMessage messageSent = new ACLMessage();
        AID receiverAID = new AID(targetAgentName, AID.ISLOCALNAME);
        messageSent.addReceiver(receiverAID);
        messageSent.setContent(content);
        senderAgent.send(messageSent);
    }

    private class MyTickBehaviour extends TickerBehaviour {
        private List<String> agentList;

        public MyTickBehaviour(Agent a, long period, List<String> agents) {
            super(a, period);
            this.agentList = agents;
        }

        @Override
        protected void onTick() {
            int random = (int)(Math.random() * agentList.size() + 1);
            String reciver = agentList.get(random - 1);
            sendMessage(myAgent, reciver,"Cat este ora?");
        }
    }

    private class ReciverBehaviour extends CyclicBehaviour {
        @Override
        public void action() {

            ACLMessage message = myAgent.receive();
            if (message != null)
            {
                String content = message.getContent();

                switch (content) {
                    case "Cat este ora?": {
                        ACLMessage answer = new ACLMessage();
                        answer.addReceiver(message.getSender());
                        answer.setContent("Ora este: " + LocalTime.now());
                        answer.setConversationId("ID1");
                        myAgent.send(answer);
                        break;
                    }
                    case "Unde locuiesti?": {
                        ACLMessage answer = new ACLMessage();
                        answer.addReceiver(message.getSender());
                        answer.setContent("Locatia: "+ myAgent.getContainerController().getName());
                        answer.setConversationId("ID1");
                        myAgent.send(answer);
                        break;
                    }
                }

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
