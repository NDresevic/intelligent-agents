package deliberative;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.HashSet;
import java.util.Set;

public class DeliberativeMain implements DeliberativeBehavior {

    private Set<Task> carriedTasks;
    private Topology topology;
    private int capacity;
    private String algorithmName;
    private String heuristicName;

    private SearchAlgorithm searchAlgorithm;

    @Override
    public void setup(Topology topology, TaskDistribution taskDistribution, Agent agent) {
        this.carriedTasks = new HashSet<>();
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
        Set<Task> availableTasks = new HashSet<>();
        for (Task task: taskSet) {
            availableTasks.add(task);
        }
        if (algorithmName.equalsIgnoreCase("BFS"))
            return new BFS(availableTasks, carriedTasks, topology, vehicle).getPlan();
        else
            return new AStar(availableTasks, carriedTasks, topology, vehicle, heuristicName).getPlan();
    }

    @Override
    public void planCancelled(TaskSet carriedTasks) {
        this.carriedTasks = carriedTasks;
    }
}
