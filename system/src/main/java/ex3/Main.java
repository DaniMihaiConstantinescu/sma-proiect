package ex3;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class Main {
    public static void main(String[] args) {
        jade.core.Runtime rt = jade.core.Runtime.instance();
        Profile pMain = new ProfileImpl();
        AgentContainer mc = rt.createMainContainer(pMain);
        Profile pC1 = new ProfileImpl();
        pC1.setParameter(Profile.CONTAINER_NAME,"Container-1");
        pC1.setParameter(Profile.MAIN, "false");
        pC1.setParameter(Profile.MAIN_HOST, "localhost");
        pC1.setParameter(Profile.MAIN_PORT, "1099");
        pC1.setParameter(Profile.LOCAL_HOST, "localhost");
        pC1.setParameter(Profile.LOCAL_PORT, "1099");
        AgentContainer c1 = rt.createAgentContainer(pC1);

        try {
            AgentController rma = mc.createNewAgent("rma", "jade.tools.rma.rma", null);
            rma.start();
            AgentController snif = mc.createNewAgent("snif", "jade.tools.sniffer.Sniffer", null);
            snif.start();

            // normal agents
            mc.createNewAgent("Ana","ex3.Agent1", null).start();
            c1.createNewAgent("Maria","ex3.Agent1", null).start();
            c1.createNewAgent("Tudor","ex3.Agent1", null).start();
            c1.createNewAgent("Mihai","ex3.Agent1", null).start();
            c1.createNewAgent("George","ex3.Agent1", null).start();

            // manager agent
            c1.createNewAgent("Manager","ex3.ManagerAgent", null).start();

            // stopper agents
            c1.createNewAgent("Stopper","ex3.StopperAgent", null).start();

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
