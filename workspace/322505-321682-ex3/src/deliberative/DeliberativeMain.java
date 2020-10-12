package deliberative;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

public class DeliberativeMain implements DeliberativeBehavior {

    private TaskSet carriedTasks;
    private Topology topology;
    private int capacity;
    private String algorithmName;
    private String heuristicName;

    @Override
    public void setup(Topology topology, TaskDistribution taskDistribution, Agent agent) {
        carriedTasks = null;
        this.topology = topology;
        this.capacity = agent.vehicles().get(0).capacity();

        this.algorithmName = agent.readProperty("algorithm", String.class, "BFS");
        if (!algorithmName.equalsIgnoreCase("BFS") && !algorithmName.equalsIgnoreCase("ASTAR"))
            throw new AssertionError("Unsupported algorithm.");
        if (this.algorithmName.equalsIgnoreCase("ASTAR"))
            this.heuristicName = agent.readProperty("heuristic", String.class, "");
        else
            this.heuristicName = null;
    }

    @Override
    public Plan plan(Vehicle vehicle, TaskSet taskSet) {
        if (algorithmName.equalsIgnoreCase("BFS"))
            return new BFS(taskSet, carriedTasks, topology, vehicle).getPlan();
        else
            return new AStar(taskSet, carriedTasks, topology, vehicle, heuristicName).getPlan();
    }

    @Override
    public void planCancelled(TaskSet carriedTasks) {
        this.carriedTasks = carriedTasks;
    }
}
