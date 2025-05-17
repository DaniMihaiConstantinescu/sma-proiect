package ex3;

public class AgentMessage {
    public String sender;
    public String receiver;
    public String content;

    public AgentMessage(String sender, String receiver, String content) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }
}
