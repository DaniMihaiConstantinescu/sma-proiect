package agents;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class LoadBalancer extends Agent {
    private Integer maxCapacity;
    private final Integer currentCapacity = 0;

    @Override
    public void setup()
    {
        System.out.println("Load balancer setup");
        addBehaviour(new WakerBehaviour(this, 3000) {
            @Override
            protected void onWake() {
                super.onWake();
                addBehaviour(new CreateAgentBehaviour("Agent 1"));

            }
        });
    }

    private class CreateAgentBehaviour extends OneShotBehaviour {
        String agentName;
        public CreateAgentBehaviour(String agentName) {
            this.agentName = agentName;
        }

        @Override
        public void action() {
            try {
                ContainerController container = getContainerController();

                AgentController newAgent = container.createNewAgent(
                    agentName,
                    "agents.Node",
                    null
            );

                newAgent.start();
            } catch (StaleProxyException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
