package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.Resource;
import utils.ServiceFinder;
import utils.enums.ServiceType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Client extends Agent {
    private Resource selectedResource;
    private int requestCount;
    private AID gatewayAID;
    private JFrame frame;

    @Override
    protected void setup() {
        SwingUtilities.invokeLater(this::createAndShowGUI);
        addBehaviour(new ServiceFinder(
                this,
                ServiceType.GATEWAY.toString(),
                (DFAgentDescription[] results) -> gatewayAID = results[0].getName()
        ));
    }

    private void createAndShowGUI() {
        frame = new JFrame("Client Dashboard");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setSize(400, 200);
        frame.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel resourceLabel = new JLabel("Select Resource:");
        gbc.gridx = 0; gbc.gridy = 0;
        frame.add(resourceLabel, gbc);

        JComboBox<Resource> resourceCombo = new JComboBox<>(Resource.values());
        gbc.gridx = 1; gbc.gridy = 0;
        frame.add(resourceCombo, gbc);

        JLabel countLabel = new JLabel("Number of Requests:");
        gbc.gridx = 0; gbc.gridy = 1;
        frame.add(countLabel, gbc);

        Integer[] counts = {1, 3, 5, 10, 15, 30};
        JComboBox<Integer> countCombo = new JComboBox<>(counts);
        gbc.gridx = 1; gbc.gridy = 1;
        frame.add(countCombo, gbc);

        JButton goButton = new JButton("Send Requests");
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        frame.add(goButton, gbc);

        goButton.addActionListener((ActionEvent e) -> {
            selectedResource = (Resource) resourceCombo.getSelectedItem();
            requestCount = (Integer) countCombo.getSelectedItem();
            if (gatewayAID == null) {
                JOptionPane.showMessageDialog(frame, "Gateway agent not found yet. Please wait.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            sendRequests();
        });

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int choice = JOptionPane.showConfirmDialog(frame,
                        "Are you sure you want to exit?",
                        "Confirm Exit", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    doDelete();
                    frame.dispose();
                }
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void sendRequests() {
        System.out.printf("[%s] Sending %d REQUEST(s) for resource '%s' to %s%n",
                getLocalName(), requestCount, selectedResource, gatewayAID.getLocalName());

        for (int i = 1; i <= requestCount; i++) {
            addBehaviour(new SendRequestBehaviour(this, gatewayAID, selectedResource, i));
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("[" + getLocalName() + "] Client terminated");
        if (frame != null) frame.dispose();
    }

    private class SendRequestBehaviour extends OneShotBehaviour {
        private final AID gatewayAID;
        private final Resource resource;
        private final int requestNumber;

        public SendRequestBehaviour(Agent a, AID gatewayAID, Resource resource, int requestNumber) {
            super(a);
            this.gatewayAID = gatewayAID;
            this.resource = resource;
            this.requestNumber = requestNumber;
        }

        @Override
        public void action() {
            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
            req.addReceiver(gatewayAID);
            req.setContent(resource.toString());
            send(req);
            System.out.printf("[Client] Request #%d sent for '%s'%n", requestNumber, resource);

            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage resp = blockingReceive(mt);
            if (resp != null) {
                System.out.printf("[Client] Response to request #%d: Proxy chosen: %s%n",
                        requestNumber, resp.getContent());
            }
        }
    }
}
