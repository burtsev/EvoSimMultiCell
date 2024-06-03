import org.knowm.xchart.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Map;

public class AgentDiagram extends JFrame {
    private Genome agent;

    public AgentDiagram(Genome agent) {
        this.agent = agent;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("ID: " + agent.id
                + " t: " + World2D.time
                + " act: " + agent.act
                + " nID: " + agent.neighbourID
                + " E: " + agent.energy
                + " age: " + agent.age
                + " kinship: " + agent.cellNeighbourKinship);
        setLayout(new BorderLayout()); // Use BorderLayout

        JPanel chartsPanel = new JPanel(new GridLayout(1, 2)); // Panel for charts

        // Create and add rotated input chart panel
        CategoryChart inputChart = createBarChart("in", agent.inputValue, agent.inputNames);
        chartsPanel.add(new RotatedPanel(getChartImage(inputChart)));

        // Create and add rotated output chart panel
        CategoryChart outputChart = createBarChart("out", agent.outputValue, agent.actionNames);
        chartsPanel.add(new RotatedPanel(getChartImage(outputChart)));

        add(chartsPanel, BorderLayout.CENTER); // Add charts to the frame

        pack();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private CategoryChart createBarChart(String title, int[] values, Map<Integer, String> names) {
        // Create Chart
        CategoryChart chart = new CategoryChartBuilder().width(300).height(300)
                .title(title)
                .build();

        // Customize Chart
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setPlotGridVerticalLinesVisible(false);
        chart.getStyler().setPlotGridHorizontalLinesVisible(true);
        chart.getStyler().setXAxisLabelRotation(90);

        // Series
        String[] categories = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            categories[i] = names.getOrDefault(i, "Unknown");
        }
        chart.addSeries(title,
                Arrays.asList(categories),
                Arrays.asList(Arrays.stream(values).boxed().toArray(Integer[]::new)));

        return chart;
    }

    public void refreshDisplay() {
        // Store current location
        Point currentLocation = getLocation();

        // Clear existing content and reinitialize UI
        getContentPane().removeAll();
        initializeUI();

        // Refresh and restore location
        revalidate();
        repaint();
        setLocation(currentLocation);
    }

    class RotatedPanel extends JPanel {
        private BufferedImage chartImage;

        public RotatedPanel(BufferedImage chartImage) {
            this.chartImage = chartImage;
            setPreferredSize(new Dimension(chartImage.getHeight(), chartImage.getWidth()));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();

            // Calculate the rotation point
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            // Translate and rotate the graphics context
            g2d.translate(cx, cy);
            g2d.rotate(Math.toRadians(90));
            g2d.translate(-cy, -cx);

            // Draw the rotated image
            g2d.drawImage(chartImage, 0, 0, this);
            g2d.dispose();
        }
    }

    private BufferedImage getChartImage(CategoryChart chart) {
        // Directly return the BufferedImage from the chart
        return BitmapEncoder.getBufferedImage(chart);
    }
}