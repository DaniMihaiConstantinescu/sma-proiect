package utils.enums;

public enum ConversationId {
    PROXY_SELECTION("proxy-selection"),
    NODE_ASSIGNMENT("node-assignment"),
    LB_ASSIGNMENT("lb-assignment"),
    INFRASTRUCTURE_UPDATE("infrastructure-update");

    private final String className;

    ConversationId(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}

