package agents;

import jade.core.Agent;

public class ManagerAgent extends Agent {
    private static final long serialVersionUID = 1L;

    @Override
    public void setup()
    {
        System.out.println("Node " + this.getLocalName() + " setup");
    }
}
