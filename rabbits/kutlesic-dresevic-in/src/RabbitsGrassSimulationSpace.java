import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 */
public class RabbitsGrassSimulationSpace {

    private static final int GRASS_ON_CELL_BOUNDARY = 16;

    private Object2DGrid grassSpace;
    private Object2DGrid agentSpace;
    private int gridSize;
    private int totalGrassAmount;

    public RabbitsGrassSimulationSpace(int xSize, int ySize) {
        grassSpace = new Object2DGrid(xSize, ySize);
        agentSpace = new Object2DGrid(xSize, ySize);
        for (int i = 0; i < xSize; i++) {
            for (int j = 0; j < ySize; j++) {
                grassSpace.putObjectAt(i, j, 0);
            }
        }
        gridSize = xSize;
        totalGrassAmount = 0;
    }

    public void growGrass(int grass) {
        // Randomly place grass in grassSpace
        for (int i = 0; i < grass; i++) {
            // Choose coordinates
            int x = (int) (Math.random() * grassSpace.getSizeX());
            int y = (int) (Math.random() * grassSpace.getSizeY());

            // Get the value of the object at those coordinates
            int currentValue = getGrassAt(x, y);

            // Replace the Integer object with another one with the new value
            int newGrassAmount = Math.min(currentValue + 1, GRASS_ON_CELL_BOUNDARY);
            grassSpace.putObjectAt(x, y, newGrassAmount);
            totalGrassAmount += newGrassAmount - currentValue;
        }
    }

    private int getGrassAt(int x, int y) {
        return grassSpace.getObjectAt(x, y) == null ? 0 : (Integer) grassSpace.getObjectAt(x, y);
    }

    public Object2DGrid getCurrentGrassSpace() {
        return grassSpace;
    }

    private boolean isCellOccupied(int x, int y) {
        return agentSpace.getObjectAt(x, y) != null;
    }

    public boolean didAddAgentToSpace(RabbitsGrassSimulationAgent agent) {
        boolean retVal = false;
        int count = 0;
        int countLimit = (int) (agentSpace.getSizeX() * agentSpace.getSizeY() * 0.7);

        while (!retVal && (count < countLimit)) {
            int x = (int) (Math.random() * agentSpace.getSizeX());
            int y = (int) (Math.random() * agentSpace.getSizeY());
            synchronized (this) {
                if (!isCellOccupied(x, y)) {
                    agentSpace.putObjectAt(x, y, agent);
                    agent.setXY(x, y);
                    agent.setGrassSpace(this);
                    retVal = true;
                }
            }
            count++;
        }

        return retVal;
    }

    public Object2DGrid getCurrentAgentSpace() {
        return agentSpace;
    }

    public void removeAgentAt(int x, int y) {
        agentSpace.putObjectAt(x, y, null);
    }

    public int removeGrassAt(int x, int y) {
        int grassAmount = getGrassAt(x, y);
        grassSpace.putObjectAt(x, y, 0);
        totalGrassAmount -= grassAmount;
        return grassAmount;
    }

    public synchronized boolean didMoveAgentAt(int x, int y, int newX, int newY) {
        if (isCellOccupied(newX, newY)) {
            return false;
        }
        RabbitsGrassSimulationAgent agent = (RabbitsGrassSimulationAgent) agentSpace.getObjectAt(x, y);
        removeAgentAt(x, y);
        agent.setXY(newX, newY);
        agentSpace.putObjectAt(newX, newY, agent);
        return true;
    }

    public int getGridSize() {
        return gridSize;
    }

    public int getTotalGrassAmount() {
        return totalGrassAmount;
    }

    public static int getGrassOnCellBoundary() {
        return GRASS_ON_CELL_BOUNDARY;
    }
}
