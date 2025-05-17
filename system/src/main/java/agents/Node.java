package agents;

import jade.core.Agent;

public class Node extends Agent {
    private static final long serialVersionUID = 1L;
    private Integer maxCapacity;
    private final Integer currentCapacity = 0;

    @Override
    public void setup()
    {
        System.out.println("Node " + this.getLocalName() + " setup");
    }
}



