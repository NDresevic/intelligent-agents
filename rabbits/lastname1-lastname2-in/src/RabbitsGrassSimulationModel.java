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
import java.util.ListIterator;
import java.util.concurrent.locks.ReentrantLock;
//TODO novi su na random mestu

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author
 */


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
		private static final int GRIDSIZE = 20;
		private static final int NUMINITRABBITS = 40;
		private static final int NUMINITGRASS = 10;
		public static final int GRASSGROWTHRATE = 1;
		public static final int BIRTHTRESHOLD = 1;
		private static final int AGENT_MIN_ENERGY = 30;
		private static final int AGENT_MAX_ENERGY = 50;
		private static final int BABY_LIFE_SPAN = (AGENT_MAX_ENERGY + AGENT_MIN_ENERGY) / 2;

		private int gridSize = GRIDSIZE;
		private int numInitRabbits = NUMINITRABBITS;
		private int numInitGrass = NUMINITGRASS;
		private int grassGrowthRate = GRASSGROWTHRATE;
		private int birthThreshold = BIRTHTRESHOLD;
		private int agentMinEnergy = AGENT_MIN_ENERGY;
		private int agentMaxEnergy = AGENT_MAX_ENERGY;
		private int babyLifeSpan = BABY_LIFE_SPAN;
	private ReentrantLock agentListLock = new ReentrantLock();

	private Schedule schedule;
	private ArrayList<RabbitsGrassSimulationAgent> agentList;
	private RabbitsGrassSimulationSpace grassSpace;
	private DisplaySurface displaySurf;

	public void setup() {
		grassSpace = null;
		agentList = new ArrayList();
		schedule = new Schedule(1);

		if (displaySurf != null){
			displaySurf.dispose();
		}
		displaySurf = null;
		displaySurf = new DisplaySurface(this, "Carry Drop Model Window 1");
		registerDisplaySurface("Rabbit Grass Model Window 1", displaySurf);
	}

	public void begin() {
		buildModel();
		buildSchedule();
		buildDisplay();
		displaySurf.display();
	}

	public void buildModel(){
		grassSpace = new RabbitsGrassSimulationSpace(gridSize, gridSize);
		grassSpace.growGrass(numInitGrass);

		for(int i = 0; i < numInitRabbits; i++){
			addNewAgent();
		}

		for (RabbitsGrassSimulationAgent agent : agentList) {
			agent.report();
		}
	}

	public void buildSchedule(){
		class SimulationStep extends BasicAction {
			public void execute() {
				SimUtilities.shuffle(agentList);
				for(RabbitsGrassSimulationAgent agent : agentList)
					agent.step();

				int deadAgents = reapDeadAgents();
				displaySurf.updateDisplay();
			}
		}
		schedule.scheduleActionBeginning(0, new SimulationStep());

		class CountLiving extends BasicAction {
			public void execute(){
				countLivingAgents();

				for(RabbitsGrassSimulationAgent agent : agentList)
					if(agent.getEnergy() > birthThreshold);
						//TODO add new agent to a specific location
						//addNewAgent();
			}
		}

		schedule.scheduleActionAtInterval(1, new CountLiving());

		class ChangeDirection extends BasicAction {
			public void execute(){
				for(RabbitsGrassSimulationAgent agent : agentList)
					agent.step();
			}
		}

		schedule.scheduleActionAtInterval(0.5, new ChangeDirection());

		class UpdateGrassInSpace extends BasicAction {
			public void execute(){
				grassSpace.growGrass(grassGrowthRate);
			}
		}

		schedule.scheduleActionAtInterval(1, new UpdateGrassInSpace());
	}

	public void buildDisplay(){
		ColorMap map = new ColorMap();

		for(int i = 1; i<16; i++){
			map.mapColor(i, new Color(i * 8 + 127, 0, 0));
		}
		map.mapColor(0, Color.white);
		Value2DDisplay displayGrass =
				new Value2DDisplay(grassSpace.getCurrentGrassSpace(), map);

		Object2DDisplay displayAgents = new Object2DDisplay(grassSpace.getCurrentAgentSpace());
		displayAgents.setObjectList(agentList);

		displaySurf.addDisplayableProbeable(displayGrass, "Grass");
		displaySurf.addDisplayableProbeable(displayAgents, "Agents");
	}

	private void addNewAgent(){
		RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(agentMinEnergy, agentMaxEnergy);
		agentList.add(a);
		grassSpace.addAgent(a);
	}

	private int reapDeadAgents() {
		int count = 0;
		ListIterator<RabbitsGrassSimulationAgent> iter = agentList.listIterator();
		while(iter.hasNext()){
			RabbitsGrassSimulationAgent agent = iter.next();
			if(agent.getEnergy() < 1){
				grassSpace.removeAgentAt(agent.getX(), agent.getY());
				iter.remove();
				count++;
			}
		}
		return count;
	}

	private int countLivingAgents(){
		int livingAgents = 0;
		for(RabbitsGrassSimulationAgent agent : agentList)
			if(agent.getEnergy() > 0)
				livingAgents++;
		System.out.println("Number of living agents is: " + livingAgents);
		return livingAgents;
	}

	public String[] getInitParam() {
			// Parameters to be set by users via the Repast UI slider bar
			// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want 
		return new String[]{ "GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold"
				, "AgentMinLifespan", "AgentMaxLifespan", "BabyLifeSpan"};
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

	public int getGrassGrowthRate(){
		return grassGrowthRate;
	}

	public void setGrassGrowthRate(int grassGrowthRate) {
		this.grassGrowthRate = grassGrowthRate;
	}

	public int getBirthThreshold(){
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
}
