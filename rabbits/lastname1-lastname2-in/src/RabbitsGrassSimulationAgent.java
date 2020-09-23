import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;

import java.util.Random;

import java.awt.Color;

/**
 * Class that implements the simulation agent for the rabbits grass simulation.
 */
public class RabbitsGrassSimulationAgent implements Drawable {

    private static int agentID = 0;

    //class fields
	private final int id;
	private int x;
    private int y;
    private int energy;
    private int birthFrequency;
    private final int birthLoss;

    private RabbitsGrassSimulationSpace grassSpace;

    public RabbitsGrassSimulationAgent(int minEnergy, int maxEnergy, int birthLoss) {
        x = -1;
        y = -1;
        energy = (int) ((Math.random() * (maxEnergy - minEnergy)) + maxEnergy);
        id = agentID++;
		birthFrequency = 0;
        this.birthLoss = birthLoss;
    }

    public void draw(SimGraphics arg0) {
        arg0.drawFastRoundRect(Color.green);
        //arg0.drawImage();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setXY(int newX, int newY) {
        x = newX;
        y = newY;
    }

    public String getID() { return "" + id; }

    public int getEnergy() { return energy; }

    public void step() {
        int dX, dY;
        //mapping -1 -> (-1,0), 0 -> (0,-1), 0 -> (0,1), 1 -> (1,0)
        int[] generatingSeq = {-1, 0, 0, 1};
        int randomNumber = new Random().nextInt(generatingSeq.length);
        dX = generatingSeq[randomNumber];
        if (dX == 0) {
            dY = randomNumber == 1 ? -1 : 1;
        } else {
            dY = 0;
        }

        int newX = (x + dX) % grassSpace.getGridSize();
        int newY = (y + dY) % grassSpace.getGridSize();

        Object2DGrid grid = grassSpace.getCurrentAgentSpace();
        newX = (newX + grid.getSizeX()) % grid.getSizeX();
        newY = (newY + grid.getSizeY()) % grid.getSizeY();

        energy--;
        if (tryMove(newX, newY)) {
            energy += grassSpace.removeGrassAt(x, y);
            birthFrequency++;
        } else { // collision -> try again
            this.step();
        }
    }

    public void reproduce() {
    	energy -= birthLoss;
    	birthFrequency = 0;
	}

    private boolean tryMove(int newX, int newY) {
        return grassSpace.didMoveAgentAt(x, y, newX, newY);
    }

    public void setGrassSpace(RabbitsGrassSimulationSpace grassSpace) {
        this.grassSpace = grassSpace;
    }

    public void report() {
        System.out.println("Agent " + id +
                " is at (" +
                x + ", " + y +
                ") and has " +
                " energy: " + energy);
    }

	public int getBirthFrequency() {
		return birthFrequency;
	}
}
