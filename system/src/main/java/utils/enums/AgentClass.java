package utils.enums;

public enum AgentClass {
    CLIENT("agents.Client"),
    MANAGER("agents.ManagerAgent"),
    GATEWAY("agents.Gateway"),
    REVERSE_PROXY("agents.ReverseProxy"),
    LOAD_BALANCER("agents.LoadBalancer"),
    NODE("agents.Node");


    private final String className;

    AgentClass(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}

