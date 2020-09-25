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
    private static int UNABLE_MOVES_BOUNDARY = 10;

    //class fields
    private final int id;
    private int x;
    private int y;
    private int energy;
    private int birthFrequency;
    private final float birthGivingLoss;
    private int lifeTime;
    private int bornBabies;
    private int unableMoves;

    private RabbitsGrassSimulationSpace grassSpace;

    public RabbitsGrassSimulationAgent(int minEnergy, int maxEnergy, float birthGivingLoss) {
        x = -1;
        y = -1;
        energy = (int) ((Math.random() * (maxEnergy - minEnergy)) + maxEnergy);
        birthFrequency = 0;
        lifeTime = 0;
        bornBabies = 0;
        unableMoves = 0;
        this.birthGivingLoss = birthGivingLoss;

        id = ++agentID;
    }

    public void draw(SimGraphics arg0) {
        arg0.drawFastRoundRect(Color.black);
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

    public int getEnergy() {
        return energy;
    }

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
        lifeTime++;

        if (tryMove(newX, newY)) {
            energy += grassSpace.removeGrassAt(x, y);
            birthFrequency++;
            unableMoves = 0;
        } else { // collision -> try again
            //FIXME stack can blow up and it did when I set e.g. frequency to 1
            // so Duda changed it to bounded depth, but try to find out an elegant way
            if (unableMoves < UNABLE_MOVES_BOUNDARY) {
                unableMoves++;
                this.step();
            } else {
                energy--;
            }
        }
    }

    public void reproduce() {
        energy = (int) (1 - birthGivingLoss) * energy;
        birthFrequency = 0;
        bornBabies++;
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
                "energy: " + energy);
    }

    public int getBirthFrequency() {
        return birthFrequency;
    }

    public int getLifeTime() {
        return lifeTime;
    }

    public int getBornBabies() {
        return bornBabies;
    }

    public static int getAgentID() {
        return agentID;
    }

    public int getId() {
        return id;
    }
}
