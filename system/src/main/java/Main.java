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

            mc.createNewAgent("Gateway","agents.Gateway",   null).start();
            mc.createNewAgent("ReverseProxy1","agents.ReverseProxy",   null).start();
            mc.createNewAgent("ReverseProxy2","agents.ReverseProxy",   null).start();


            mc.createNewAgent("Client1","agents.Client",   null).start();
            mc.createNewAgent("Client2","agents.Client",   null).start();
            mc.createNewAgent("Client3","agents.Client",   null).start();


        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
