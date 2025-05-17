package ex3;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

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

            ACLMessage message = myAgent.receive();
            if (message != null)
            {
                String content = message.getContent();

                switch (content) {
                    case "PING": {
                        ACLMessage answer = new ACLMessage();
                        answer.addReceiver(message.getSender());
                        answer.setContent("PONG");
                        answer.setConversationId("ID1");
                        myAgent.send(answer);
                        break;
                    }
                    case "STOP": {
                        myAgent.doDelete();
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
