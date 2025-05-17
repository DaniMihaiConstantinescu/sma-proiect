package ex1;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import java.time.LocalTime;


public class Agent2 extends Agent {
    private static final long serialVersionUID = 1L;

    @Override
    public void setup()
    {
        addBehaviour(new ReciverBehaviour());

        String content = "";
        Object[] args = getArguments();
        if (args != null && args.length > 0)
            content = (String) args[0];
        if (content != null && !content.isEmpty())
            addBehaviour(new SendMessageBehaviour("Ion", content));
    }

    @Override
    protected void takeDown() {
        super.takeDown();
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
            //trimiterea unui mesaj: creare obiect de tipul ACLMessage //si setarea destinatarului si a continutului mesajului
            ACLMessage messageSent = new ACLMessage();
            AID receiverAID = new AID(agentName, AID.ISLOCALNAME); // in acelasi container
            messageSent.addReceiver(receiverAID);
            messageSent.setContent(content);
            myAgent.send(messageSent);
        }
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
