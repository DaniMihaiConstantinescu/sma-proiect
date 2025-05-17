package ex1;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import java.time.LocalTime;


public class Agent1 extends Agent {
    private static final long serialVersionUID = 1L;

    @Override
    public void setup()
    {
        addBehaviour(new ReciverBehaviour());
    }

    @Override
    protected void takeDown() {
        super.takeDown();
    }

    private class ReciverBehaviour extends CyclicBehaviour {
        @Override
        public void action() {

            //receiving a message: retrieve a message from the queue
            ACLMessage message = myAgent.receive();
            if (message != null)
            {
                String content = message.getContent();
                String s = "Agent " + myAgent.getLocalName() + ": " + content + " de la " +
                        message.getSender().getLocalName();
                System.out.println(s);

                switch (content) {
                    case "Cat este ora?": {
                        ACLMessage answer = new ACLMessage();
                        answer.addReceiver(message.getSender());
                        answer.setContent("Data este: " + LocalTime.now());
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
