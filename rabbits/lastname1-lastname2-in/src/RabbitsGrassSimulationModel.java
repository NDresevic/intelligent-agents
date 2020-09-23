import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.util.SimUtilities;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 */

//FIXME
//    1. I don't see paramters min and max life span in parameter list

public class RabbitsGrassSimulationModel extends SimModelImpl {

    public static void main(String[] args) {
        System.out.println("Rabbit skeleton");

        SimInit init = new SimInit();
        RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
        // Do "not" modify the following lines of parsing arguments
        if (args.length == 0) // by default, you don't use parameter file nor batch mode
            init.loadModel(model, "", false);
        else
            init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));
    }

    // Default Values
    private static final int GRID_SIZE = 20;
    private static final int NUM_INIT_RABBITS = 40;
    private static final int NUM_INIT_GRASS = 10;
    private static final int GRASS_GROWTH_RATE = 10;
    private static final int BIRTH_THRESHOLD = 45;
    private static final int AGENT_MIN_ENERGY = 30;
    private static final int AGENT_MAX_ENERGY = 50;
    private static final int BABY_LIFE_SPAN = (AGENT_MAX_ENERGY + AGENT_MIN_ENERGY) / 2;
    private static final int BIRTH_FREQUENCY = 20;
    private  static final float BIRTHGIVING_LOSS = 0.5f;

    private int gridSize = GRID_SIZE;
    private int numInitRabbits = NUM_INIT_RABBITS;
    private int numInitGrass = NUM_INIT_GRASS;
    private int grassGrowthRate = GRASS_GROWTH_RATE;
    private int birthThreshold = BIRTH_THRESHOLD;
    private int agentMinEnergy = AGENT_MIN_ENERGY;
    private int agentMaxEnergy = AGENT_MAX_ENERGY;
    private int babyLifeSpan = BABY_LIFE_SPAN;
    private int birthFrequency = BIRTH_FREQUENCY;
    private float birthgivingLoss = BIRTHGIVING_LOSS;

    //model
    private Schedule schedule;
    private List<RabbitsGrassSimulationAgent> agentList;
    private RabbitsGrassSimulationSpace grassSpace;
    private DisplaySurface displaySurf;

    //statistics
    private float averageLifeTime = 0;
    private float averageBornBabes = 0;
    private int deadAgents = 0;

    public void setup() {
        grassSpace = null;
        agentList = new CopyOnWriteArrayList<>();
        schedule = new Schedule(1);

        if (displaySurf != null) {
            displaySurf.dispose();
        }
        displaySurf = new DisplaySurface(this, "Rabbit Grass Model Window 1");
        registerDisplaySurface("Rabbit Grass Model Window 1", displaySurf);
    }

    public void begin() {
        buildModel();
        buildSchedule();
        buildDisplay();
        displaySurf.display();
    }

    private void buildModel() {
        grassSpace = new RabbitsGrassSimulationSpace(gridSize, gridSize);
        grassSpace.growGrass(numInitGrass);

        for (int i = 0; i < numInitRabbits; i++) {
            didAddNewAgentToList();
        }

        for (RabbitsGrassSimulationAgent agent : agentList) {
            agent.report();
        }
    }

    private void buildSchedule() {
        class SimulationStep extends BasicAction {
            public void execute() {
                SimUtilities.shuffle(agentList);
                for (RabbitsGrassSimulationAgent agent : agentList) {
                    agent.step();
                }

                int toBeDeadAgents = reapDeadAgents();

                for (RabbitsGrassSimulationAgent agent : agentList) {
                    if (agent.getEnergy() > birthThreshold && agent.getBirthFrequency() > birthFrequency
                            //adding baby as an agent
                            && didAddNewAgentToList()) {
                        // todo: specify the position of new born agent if we want
                        agent.reproduce();
                    }
                }

                displaySurf.updateDisplay();
                grassSpace.growGrass(grassGrowthRate);
            }
        }
        schedule.scheduleActionBeginning(100, new SimulationStep());

        class CountLiving extends BasicAction {
            public void execute(){
                countLivingAgents();
            }
        }

        schedule.scheduleActionAtInterval(10, new CountLiving());

        class CalculateStatistics extends BasicAction {
            public void execute() {
                System.out.println("\n\nAverage lifetime: " + averageLifeTime);
                System.out.println("Average babes born per rabbit: " + averageBornBabes);
            }
        }

        schedule.scheduleActionAtEnd(new CalculateStatistics());
    }

    private void buildDisplay() {
        ColorMap map = new ColorMap();
        for (int i = 1; i < 16; i++) {
            map.mapColor(i, new Color(i * 8 + 127, 0, 0));
        }
        map.mapColor(0, Color.white);

        Value2DDisplay displayGrass = new Value2DDisplay(grassSpace.getCurrentGrassSpace(), map);
        Object2DDisplay displayAgents = new Object2DDisplay(grassSpace.getCurrentAgentSpace());
        displayAgents.setObjectList(agentList);

        displaySurf.addDisplayableProbeable(displayGrass, "Grass");
        displaySurf.addDisplayableProbeable(displayAgents, "Agents");
    }

    private boolean didAddNewAgentToList() {
        RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(agentMinEnergy, agentMaxEnergy, birthgivingLoss);
		if (!grassSpace.didAddAgentToSpace(a)) {
		    System.err.println("Can't add new agent: " + a);
		    return false;
		}
        agentList.add(a);
		return true;
    }

    private int reapDeadAgents() {
        int count = 0;
        for (int i = 0; i < agentList.size(); i++) {
            RabbitsGrassSimulationAgent agent = agentList.get(i);
            if (agent.getEnergy() < 1) {
                grassSpace.removeAgentAt(agent.getX(), agent.getY());
                agentList.remove(agent);
                System.out.println("Agent " + agent.getId() + " lived for " + agent.getLifeTime() + " steps and gave birth to " + agent.getBornBabies() + " babies.");
                deadAgents++;
                averageLifeTime += (agent.getLifeTime() - averageLifeTime)/deadAgents;
                averageBornBabes += (agent.getBornBabies() - averageBornBabes)/deadAgents;
                count++;
            }
        }
        return count;
    }

    private int countLivingAgents() {
        int livingAgents = 0;
        // todo: thread safe? -> Duda: I think yes, cuz each Action is different thread
        for (RabbitsGrassSimulationAgent agent : agentList) {
            if (agent.getEnergy() > 0) {
                livingAgents++;
            }
        }
        System.out.println("Number of living agents is: " + livingAgents);
        return livingAgents;
    }

    public String[] getInitParam() {
        // Parameters to be set by users via the Repast UI slider bar
        // Do "not" modify the parameters names provided in the skeleton code, you can add more if you want
        return new String[]{"GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold",
                "AgentMinLifespan", "AgentMaxLifespan", "BabyLifeSpan", "BirthFrequency", "BirthgivingLoss"};
        //FIXME why I don't see min and max life span in setup?
    }

    public String getName() { return "RabbitGrass"; }

    public Schedule getSchedule() { return schedule; }

    public int getGridSize() { return gridSize; }

    public void setGridSize(int gridSize) { this.gridSize = gridSize; }

    public int getNumInitRabbits() { return numInitRabbits; }

    public void setNumInitRabbits(int numInitRabbits) { this.numInitRabbits = numInitRabbits; }

    public int getNumInitGrass() { return numInitGrass; }

    public void setNumInitGrass(int numInitGrass) { this.numInitGrass = numInitGrass; }

    public int getGrassGrowthRate() { return grassGrowthRate; }

    public void setGrassGrowthRate(int grassGrowthRate) { this.grassGrowthRate = grassGrowthRate; }

    public int getBirthThreshold() { return birthThreshold; }

    public void setBirthThreshold(int birthThreshold) { this.birthThreshold = birthThreshold; }

    public int getBabyLifeSpan() { return babyLifeSpan; }

    public void setBabyLifeSpan(int babyLifeSpan) { this.babyLifeSpan = babyLifeSpan; }

    public int getAgentMinEnergy() { return agentMinEnergy; }

    public void setAgentMinEnergy(int agentMinEnergy) { this.agentMinEnergy = agentMinEnergy; }

    public int getAgentMaxEnergy() { return agentMaxEnergy; }

    public void setAgentMaxEnergy(int agentMaxEnergy) { this.agentMaxEnergy = agentMaxEnergy; }

    public int getBirthFrequency() { return birthFrequency; }

    public void setBirthFrequency(int birthFrequency) { this.birthFrequency = birthFrequency; }

    public float getBirthgivingLoss() { return birthgivingLoss; }

    public void setBirthgivingLoss(float birthgivingLoss) { this.birthgivingLoss = birthgivingLoss; }
}
