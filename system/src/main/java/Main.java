import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import utils.enums.AgentClass;

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

        try {
            AgentController rma = mc.createNewAgent("rma", "jade.tools.rma.rma", null);
            rma.start();
            AgentController snif = mc.createNewAgent("snif", "jade.tools.sniffer.Sniffer", null);
            snif.start();

            mc.createNewAgent("Gateway", AgentClass.GATEWAY.getClassName(), null).start();
            mc.createNewAgent("ReverseProxy1", AgentClass.REVERSE_PROXY.getClassName(), null).start();
            mc.createNewAgent("ReverseProxy2", AgentClass.REVERSE_PROXY.getClassName(), null).start();


            mc.createNewAgent("Client1", AgentClass.CLIENT.getClassName(), null).start();
            mc.createNewAgent("Client2", AgentClass.CLIENT.getClassName(), null).start();
            mc.createNewAgent("Client3", AgentClass.CLIENT.getClassName(), null).start();


        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
