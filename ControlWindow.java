import javax.swing.*;

//import javafx.stage.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class ControlWindow extends JFrame {
    public int scale;
    public int agentSize = 16;
    private JButton startMapButton;
    private JButton stopMapButton;
    private JButton stepButton;
    private JPanel checkboxPanel = new JPanel();
    private JPanel buttonsPanel = new JPanel();
    private JPanel scalePanel = new JPanel();
    private JPanel wrapPanel = new JPanel();
    public JCheckBox showIDCheckBox = new JCheckBox("ID", false);
    public JCheckBox showEnergyCheckBox = new JCheckBox("Energy", false);
    public JCheckBox showNeighbourCheckBox = new JCheckBox("nID", false);
    public JCheckBox showKinCheckBox = new JCheckBox("Kin clusters", false);
    public JCheckBox showActCheckBox = new JCheckBox("Act", false);
    public JCheckBox showAgeCheckBox = new JCheckBox("Age", false);
    private JTextField scaleField = new JTextField(5); // Text field for scale input
    private JLabel scaleLabel = new JLabel("Map scale: ");
    private JTextField agentSizeField = new JTextField(5);
    private JLabel agentSizeLabel = new JLabel("Agent size: ");
    private JTextField agentIdField = new JTextField(5); // Text field for agent ID
    private JButton showDiagramButton = new JButton("Show Diagram");
    // List to keep track of open AgentDiagrams
    private List<AgentDiagram> openDiagrams = new ArrayList<>();

    public ControlWindow() {
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.X_AXIS));
        checkboxPanel.add(showIDCheckBox);
        checkboxPanel.add(showEnergyCheckBox);
        checkboxPanel.add(showNeighbourCheckBox);
        checkboxPanel.add(showKinCheckBox);
        checkboxPanel.add(showActCheckBox);
        checkboxPanel.add(showAgeCheckBox);

        // Setup scale field
        scaleField.setText(String.valueOf(WorldParams.mapScale));
        scaleField.addActionListener(e -> updateScale());
        scalePanel.add(scaleLabel);
        scalePanel.add(scaleField);
        agentSizeField.setText(String.valueOf(agentSize));
        agentSizeField.addActionListener(e -> updateScale());
        scalePanel.add(agentSizeLabel);
        scalePanel.add(agentSizeField);

        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
        startMapButton = new JButton("Run Map");
        stopMapButton = new JButton("Stop Map");
        stepButton = new JButton("Step");
        buttonsPanel.add(startMapButton);
        buttonsPanel.add(stepButton);
        buttonsPanel.add(stopMapButton);

        wrapPanel.setLayout(new BoxLayout(wrapPanel, BoxLayout.Y_AXIS));
        wrapPanel.add(buttonsPanel);
        wrapPanel.add(checkboxPanel);
        wrapPanel.add(scalePanel);

        JPanel agentIdPanel = new JPanel();
        agentIdPanel.add(new JLabel("Agent ID: "));
        agentIdPanel.add(agentIdField);
        agentIdPanel.add(showDiagramButton);

        // Adding the panel to the window
        wrapPanel.add(agentIdPanel);
        add(wrapPanel);

        // Action listener for the Show Diagram button
        showDiagramButton.addActionListener(e -> {
            try {
                int agentId = Integer.parseInt(agentIdField.getText());
                openAgentDiagram(agentId);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid Agent ID", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Layout
        setLayout(new FlowLayout());
        add(wrapPanel);

        // Event Listeners
        startMapButton.addActionListener(startMapListener());
        stopMapButton.addActionListener(stopMapListener());
        stepButton.addActionListener(stepListener()); // Add listener for step button

        // Frame Settings
        setTitle("Control Panel");
        // setSize(300, 80); // Adjust the size to accommodate the new button
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private ActionListener startMapListener() {
        return e -> {
            synchronized (World2D.worldLock) {
                World2D.running = true;
                World2D.worldLock.notifyAll(); // Resume continuous execution
            }
            MapFrame.setPleaseDraw(true); // Start map updating
        };
    }

    private void updateScale() {
        try {
            int newScale = Integer.parseInt(scaleField.getText());
            scale = newScale > 0 ? newScale : scale;
            agentSize = Integer.parseInt(agentSizeField.getText());
        } catch (NumberFormatException e) {
            // Handle invalid input, e.g., reset to the default scale or display an error
            // message
        }
    }

    private ActionListener stopMapListener() {
        return e -> {
            MapFrame.setPleaseDraw(false); // Stop map updating
        };
    }

    private ActionListener stepListener() {
        return e -> {
            synchronized (World2D.worldLock) {
                World2D.step = true;
                World2D.running = false;
                World2D.worldLock.notifyAll(); // Proceed one step
                // Update all open AgentDiagrams
                for (AgentDiagram diagram : openDiagrams) {
                    diagram.refreshDisplay();
                }
            }
        };
    }

    private void openAgentDiagram(int agentId) {
        try {
            // int agentId = Integer.parseInt(agentIdField.getText());
            Genome agent = findAgentById(agentId);

            if (agent == null) {
                JOptionPane.showMessageDialog(this, "Agent ID not found", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                agent.isSelected = true;
                AgentDiagram agentDiagram = new AgentDiagram(agent);
                agentDiagram.setVisible(true);
                openDiagrams.add(agentDiagram); // Add to list of open diagrams

                // Ensure diagrams are removed from the list when closed
                agentDiagram.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        agent.isSelected = false;
                        openDiagrams.remove(agentDiagram);
                    }
                });
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid Agent ID: Not a Number", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Genome findAgentById(int id) {
        for (Genome genome : World2D.v) {
            if (genome.id == id) {
                return genome;
            }
        }
        return null; // Return null if no matching agent is found
    }
}
