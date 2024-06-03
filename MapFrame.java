import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class MapFrame extends JFrame {
    private final MapPanel mapPanel;
    private static boolean pleaseDraw = false;
    private static int agntSize = 16;
    private static int scale = WorldParams.mapScale;
    private static int cellWidth, cellHeight;
    public static ControlWindow controlWindow = new ControlWindow();

    public MapFrame() {
        mapPanel = new MapPanel(WorldParams.worldXsize, WorldParams.worldYsize);
        initializeFrame();
        controlWindow.setVisible(true);
    }

    private void initializeFrame() {
        setTitle("MAP");
        setLayout(new BorderLayout());
        add(mapPanel, BorderLayout.CENTER);
        pack(); // Resize frame to fit the map panel initially
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private class MapPanel extends JPanel {
        private final int rows;
        private final int cols;

        public MapPanel(int rows, int cols) {
            this.rows = rows;
            this.cols = cols;
            // Set initial size based on WorldParams
            setPreferredSize(new Dimension(cols * WorldParams.mapScale, rows * WorldParams.mapScale));
        }

        @Override
        protected void paintComponent(Graphics g) {
            setTitle(
                    "MAP| t:" + World2D.time +
                            "  | Agents:" + World2D.population +
                            "  | Energy available:" + World2D.worldEnergy * WorldParams.eGrass +
                            "  | Energy wait time:" +
                            (int) (WorldParams.worldXsize * WorldParams.worldYsize / (World2D.worldEnergy + 1)));
            super.paintComponent(g);
            if (!pleaseDraw) {
                return;
            }

            // Calculate cell sizes dynamically
            double panelWidth = getWidth();
            double panelHeight = getHeight();
            scale = (int) Math.min(panelWidth / cols, panelHeight / rows);
            if (scale < controlWindow.scale)
                scale = controlWindow.scale;
            cellWidth = scale;
            cellHeight = scale;

            if (controlWindow.agentSize > 0)
                agntSize = controlWindow.agentSize;

            synchronized (World2D.worldLock) {
                paintCells(g); // Stage 1: Draw cells
                paintAgents(g, new Random(WorldParams.version)); // Stage 2: Draw agents
            }
        }

        private void paintCells(Graphics g) {
            int cellActualSize = (int) Math.min(cellWidth, cellHeight);
            for (int col = 0; col < World2D.cWorld.length; col++) {
                for (int row = 0; row < World2D.cWorld[col].length; row++) {
                    int x = (int) (col * cellWidth);
                    int y = (int) (row * cellHeight);
                    g.setColor(new Color(20, 20, 20));
                    g.fillRect(x, y, cellActualSize, cellActualSize);
                    Color curColor = World2D.cWorld[col][row].hereIsGrass ? new Color(0, 40, 0) : new Color(0, 0, 0);
                    g.setColor(curColor);
                    g.fillRect(x + 1, y + 1, cellActualSize - 1, cellActualSize - 1);
                }
            }
        }

        private void paintAgents(Graphics g, Random place) {
            int cellActualSize = (int) Math.min(cellWidth, cellHeight);
            int offsetSize = cellActualSize - agntSize - 1;
            for (int col = 0; col < WorldParams.worldXsize; col++) {
                for (int row = 0; row < WorldParams.worldYsize; row++) {
                    int x = (int) (col * cellWidth);
                    int y = (int) (row * cellHeight);
                    for (int z = 0; z < World2D.cWorld[col][row].agents.size(); z++) {
                        int offsetX = place.nextInt(offsetSize);
                        int offsetY = place.nextInt(offsetSize);
                        Genome agent = (Genome) World2D.cWorld[col][row].agents.elementAt(z);
                        int agentX = x + offsetX;
                        int agentY = y + offsetY;
                        Color agentColor;
                        if (controlWindow.showKinCheckBox.isSelected()) {
                            agentColor = getColorForMarker(agent.marker);
                        } else {
                            agentColor = getColorForAction(agent.act);
                        }

                        if (agent.isSelected) {
                            // Draw bounding box
                            g.setColor(Color.WHITE);
                            g.drawRect(x + offsetX, y + offsetY, agntSize, agntSize);
                            //g.drawRect(agent. x + offsetX, y + offsetY, agntSize, agntSize); 
                        }

                        // Set semi-transparent color
                        Color semiTransparentColor = new Color(agentColor.getRed(),
                                agentColor.getGreen(), agentColor.getBlue(), 210); // 128 is half-transparent

                        g.setColor(semiTransparentColor);
                        g.fillOval(agentX, agentY, agntSize, agntSize);

                        String textLabel = "";
                        if (controlWindow.showIDCheckBox.isSelected()) {
                            textLabel += "ID:" + agent.id + " \r\n";
                        }
                        if (controlWindow.showEnergyCheckBox.isSelected()) {
                            textLabel += "E:" + agent.energy + " ";
                        }
                        if (controlWindow.showNeighbourCheckBox.isSelected()) {
                            textLabel += "N id:" + agent.neighbourID + " ";
                        }
                        if (controlWindow.showActCheckBox.isSelected()) {
                            textLabel += "act:" + agent.act + " ";
                        }
                        if (controlWindow.showAgeCheckBox.isSelected()) {
                            textLabel += "age:" + agent.age + " ";
                        }
                        if (!textLabel.isEmpty()) { // Only draw if there's something to display
                            g.setFont(new Font("Arial", 0, 10));
                            g.drawString(textLabel, x + offsetX + agntSize, y + offsetY);
                        }

                        // Draw direction indicator
                        g.setColor(Color.BLACK); // Black color for direction
                        int lineLength = agntSize / 2;
                        int lineX = agentX + agntSize / 2;
                        int lineY = agentY + agntSize / 2;

                        // Calculate the end point of the line based on direction
                        double angle = Math.toRadians(agent.dir * 90); // Assuming 0: up, 1: right, 2: down, 3: left
                        int endX = (int) (lineX + lineLength * Math.sin(angle));
                        int endY = (int) (lineY + lineLength * Math.cos(angle));

                        g.drawLine(lineX, lineY, endX, endY);
                    }
                }
            }
        }

        private Color getColorForAction(int action) {
            switch (action) {
                case 0:
                    return new Color(150, 150, 150); // rest
                case 1:
                    return new Color(255, 255, 0); // eat
                case 2:
                    return new Color(0, 80, 255); // moving
                case 3:
                case 4:
                    return new Color(80, 80, 190); // turn left/right
                case 5:
                    return new Color(255, 50, 255); // divide
                case 6:
                    return new Color(255, 0, 0); // fight
                case 7:
                    return new Color(0, 255, 225); // fight
                default:
                    return new Color(0, 0, 0);
            }
        }
    }

    private Color getColorForMarker(int[] marker) {
        Random random = new Random(0);
        int red = 0;
        int green = 0;
        int blue = 0;
        int rs = 100;

        for (int value : marker) {
            value = Math.abs(value) / WorldParams.maxMarkerValue;
            //float scaledValue = (value + WorldParams.maxMarkerValue) * 127 / WorldParams.maxMarkerValue;
            red += (int) random.nextInt(rs) * value;
            green += (int) random.nextInt(rs) * value;
            blue += (int) random.nextInt(rs) * value;
        }

        // Rescale values to cover full dynamic range

        int maxComponent = 1+Math.max(red, Math.max(green, blue));

        if (maxComponent > 0) { // Avoid division by zero
            float scaleFactor = 250f / maxComponent;
            red = (int) (red * scaleFactor);
            green = (int) (green * scaleFactor);
            blue = (int) (blue * scaleFactor);
        }

        return new Color(red, green, blue);
    }

    public static synchronized void setPleaseDraw(boolean draw) {
        pleaseDraw = draw;
    }

    public static synchronized boolean isPleaseDraw() {
        return pleaseDraw;
    }

    public void triggerRepaint() {
        if (SwingUtilities.isEventDispatchThread()) {
            repaint();
            pack();
        } else {
            SwingUtilities.invokeLater(this::repaint);
        }
    }
}
