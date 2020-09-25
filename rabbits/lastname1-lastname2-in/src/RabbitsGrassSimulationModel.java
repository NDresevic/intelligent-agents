import uchicago.src.sim.analysis.*;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author Nevena Dresevic & Dubravka Kutlesic
 */

public class RabbitsGrassSimulationModel extends SimModelImpl {

    public static void main(String[] args) {
        SimInit init = new SimInit();
        RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
        startTime = System.nanoTime();
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
    //private static final int BIRTH_THRESHOLD = 45;
    private static final int AGENT_MIN_ENERGY = 30;
    private static final int AGENT_MAX_ENERGY = 50;
    private static final int BABY_LIFE_SPAN = (AGENT_MAX_ENERGY + AGENT_MIN_ENERGY) / 2;
    private static final int BIRTH_FREQUENCY = 20;
    private static final float BIRTH_GIVING_LOSS = 0.3f;

    private int gridSize = GRID_SIZE;
    private int numInitRabbits = NUM_INIT_RABBITS;
    private int numInitGrass = NUM_INIT_GRASS;
    private int grassGrowthRate = GRASS_GROWTH_RATE;
    private int agentMinEnergy = AGENT_MIN_ENERGY;
    private int agentMaxEnergy = AGENT_MAX_ENERGY;
    private int birthThreshold = AGENT_MAX_ENERGY + 1;
    private int babyLifeSpan = BABY_LIFE_SPAN;
    private int birthFrequency = BIRTH_FREQUENCY;
    private float birthGivingLoss = BIRTH_GIVING_LOSS;

    //model
    private Schedule schedule;
    private List<RabbitsGrassSimulationAgent> agentList;
    private RabbitsGrassSimulationSpace grassSpace;
    private DisplaySurface displaySurf;

    //statistics
    private float averageLifeTime = 0;
    private float averageBornBabes = 0;
    private int deadAgents = 0;
    private static long startTime;

    private OpenSequenceGraph graph;
    private OpenHistogram agentEnergyDistribution;

    class GrassInSpace implements DataSource, Sequence {

        public Object execute() {
            return getSValue();
        }

        public double getSValue() {
            return grassSpace.getTotalGrassAmount();
        }
    }

    class RabbitsInSpace implements DataSource, Sequence {

        public Object execute() {
            return getSValue();
        }

        public double getSValue() {
            return grassSpace.getAgentsCount();
        }
    }

    class AgentEnergy implements BinDataSource {
        public double getBinValue(Object o) {
            RabbitsGrassSimulationAgent agent = (RabbitsGrassSimulationAgent) o;
            return agent.getEnergy();
        }
    }

    public void setup() {
        grassSpace = null;
        agentList = new CopyOnWriteArrayList<>();
        schedule = new Schedule(1);

        if (displaySurf != null) {
            displaySurf.dispose();
        }
        displaySurf = new DisplaySurface(this, "Rabbit Grass Model Window 1");
        registerDisplaySurface("Rabbit Grass Model Window 1", displaySurf);

        if (graph != null) {
            graph.dispose();
        }
        graph = new OpenSequenceGraph("Amount Of Grass and Rabbits In Space", this);
        graph.setYRange(0.0, 1000);
        registerMediaProducer("Plot", graph);

        if (agentEnergyDistribution != null) {
            agentEnergyDistribution.dispose();
        }
        agentEnergyDistribution = new OpenHistogram("Agent Energy", 50, 0);
//        agentEnergyDistribution.setYRange(0, 100);
    }

    public void begin() {
        checkParameters();

        buildModel();
        buildSchedule();
        buildDisplay();

        displaySurf.display();
        graph.display();
        agentEnergyDistribution.display();
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
                        agent.reproduce();
                    }
                }

                displaySurf.updateDisplay();
                grassSpace.growGrass(grassGrowthRate);
            }
        }
        schedule.scheduleActionBeginning(100, new SimulationStep());

        class CountLiving extends BasicAction {
            public void execute() {
                int liveAgents = countLivingAgents();
                if (liveAgents == 0) {
                    RabbitsGrassSimulationModel.this.stop();
                }
            }
        }

        schedule.scheduleActionAtInterval(10, new CountLiving());

        class CalculateStatistics extends BasicAction {
            public void execute() {
                System.out.println(
                        "\n\nAverage lifetime: " + averageLifeTime
                                + "\nAverage babes born per rabbit: " + averageBornBabes
                                + "\nPopulation size: " + RabbitsGrassSimulationAgent.getAgentID()
                                + "\nSimulation duration: " + (System.nanoTime() - startTime) / 1_000_000_000.0 + " seconds"
                );

            }
        }
        schedule.scheduleActionAtEnd(new CalculateStatistics());

        class UpdateGrassAndRabbitsInSpace extends BasicAction {
            public void execute() {
                graph.step();
            }
        }
        schedule.scheduleActionAtInterval(10, new UpdateGrassAndRabbitsInSpace());

        class UpdateAgentEnergy extends BasicAction {
            public void execute() {
                if (agentList.size() > 0) {
                    agentEnergyDistribution.step();
                }
            }
        }
        schedule.scheduleActionAtInterval(10, new UpdateAgentEnergy());
    }

    private void buildDisplay() {
        ColorMap mapGrass = new ColorMap();
        for (int i = 1; i < RabbitsGrassSimulationSpace.getGrassOnCellBoundary(); i++) {
            mapGrass.mapColor(i, new Color(0, 127 + 127 / i, 0));
        }
        mapGrass.mapColor(0, Color.white);

        Value2DDisplay displayGrass = new Value2DDisplay(grassSpace.getCurrentGrassSpace(), mapGrass);
        Object2DDisplay displayAgents = new Object2DDisplay(grassSpace.getCurrentAgentSpace());
        displayAgents.setObjectList(agentList);

        displaySurf.addDisplayableProbeable(displayGrass, "Grass");
        displaySurf.addDisplayableProbeable(displayAgents, "Agents");

        graph.addSequence("Grass In Space", new GrassInSpace());
        graph.addSequence("Rabbits In Space", new RabbitsInSpace());

        agentEnergyDistribution.createHistogramItem("Agent Energy", agentList, new AgentEnergy());
    }

    private boolean didAddNewAgentToList() {
        RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(agentMinEnergy, agentMaxEnergy, birthGivingLoss);
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
                averageLifeTime += (agent.getLifeTime() - averageLifeTime) / deadAgents;
                averageBornBabes += (agent.getBornBabies() - averageBornBabes) / deadAgents;
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
        return new String[]{"AgentMinEnergy", "AgentMaxEnergy", "GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold",
                "BabyLifeSpan", "BirthFrequency", "BirthGivingLoss"};
    }

    public String getName() {
        return "RabbitGrass";
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public int getGridSize() {
        return gridSize;
    }

    public void setGridSize(int gridSize) {
        this.gridSize = gridSize;
    }

    public int getNumInitRabbits() {
        return numInitRabbits;
    }

    public void setNumInitRabbits(int numInitRabbits) {
        this.numInitRabbits = numInitRabbits;
    }

    public int getNumInitGrass() {
        return numInitGrass;
    }

    public void setNumInitGrass(int numInitGrass) {
        this.numInitGrass = numInitGrass;
    }

    public int getGrassGrowthRate() {
        return grassGrowthRate;
    }

    public void setGrassGrowthRate(int grassGrowthRate) {
        this.grassGrowthRate = grassGrowthRate;
    }

    public int getBirthThreshold() {
        return birthThreshold;
    }

    public void setBirthThreshold(int birthThreshold) {
        this.birthThreshold = birthThreshold;
    }

    public int getBabyLifeSpan() {
        return babyLifeSpan;
    }

    public void setBabyLifeSpan(int babyLifeSpan) {
        this.babyLifeSpan = babyLifeSpan;
    }

    public int getAgentMinEnergy() {
        return agentMinEnergy;
    }

    public void setAgentMinEnergy(int agentMinEnergy) {
        this.agentMinEnergy = agentMinEnergy;
    }

    public int getAgentMaxEnergy() {
        return agentMaxEnergy;
    }

    public void setAgentMaxEnergy(int agentMaxEnergy) {
        this.agentMaxEnergy = agentMaxEnergy;
    }

    public int getBirthFrequency() {
        return birthFrequency;
    }

    public void setBirthFrequency(int birthFrequency) {
        this.birthFrequency = birthFrequency;
    }

    public float getBirthGivingLoss() {
        return birthGivingLoss;
    }

    public void setBirthGivingLoss(float birthGivingLoss) {
        this.birthGivingLoss = birthGivingLoss;
    }

    private void checkParameters() {
        if (numInitRabbits < 0) {
            numInitRabbits = 0;
            System.err.println("Number of initial rabbits must be non-negative number. Parameter NumInitRabbits set to 0");
        }
        if (numInitGrass < 0) {
            numInitGrass = 0;
            System.err.println("Number of initial grass must be non-negative number. Parameter NumInitRabbits set to 0");
        }
        if (agentMinEnergy < 0) {
            agentMinEnergy = 1;
            System.err.println("Agent min energy must be positive number. Parameter AgentMinEnergy set to 1");
        }
        if (agentMaxEnergy < agentMinEnergy) {
            agentMaxEnergy = agentMinEnergy;
            System.err.println("Agent max energy can not be lower than min agent energy. Parameter AgentMaxEnergy set to "
                    + agentMinEnergy + " (min agent energy)");
        }
        if (birthGivingLoss > 1) {
            birthGivingLoss = 1;
            System.err.println("Birthgiving loss must be in [0,1]. Parameter BirthgivingLoss set to 1");
        }
        if (babyLifeSpan < 1) {
            babyLifeSpan = 1;
            System.err.println("Baby life span must be positive number. Parameter BabyLifeSpan set to 1");
        }
        if (birthFrequency < 1 || birthFrequency < 0) {
            birthFrequency = 1;
            System.err.println("Birth Frequency span must be positive number. Parameter BirthFrequency set to 1");
        }
        if (grassGrowthRate < 0) {
            grassGrowthRate = 0;
            System.err.println("Grass Growth Rate must be non-negative number. Parameter GrassGrowthRate set to 0");
        }
    }
}
