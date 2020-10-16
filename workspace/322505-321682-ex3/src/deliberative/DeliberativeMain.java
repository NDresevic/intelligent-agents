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

    private Agent agent;
    private Set<Task> carriedTasks;
    private String algorithmName;
    private String heuristicName;

    @Override
    public void setup(Topology topology, TaskDistribution taskDistribution, Agent agent) {
        this.carriedTasks = new HashSet<>();
        this.agent = agent;

        this.algorithmName = agent.readProperty("algorithm", String.class, "BFS");
        if (!algorithmName.equalsIgnoreCase("BFS") && !algorithmName.equalsIgnoreCase("ASTAR"))
            throw new AssertionError("Unsupported algorithm.");

        if (this.algorithmName.equalsIgnoreCase("ASTAR")) {
            this.heuristicName = agent.readProperty("heuristic", String.class, "");
        } else {
            this.heuristicName = null;
        }
    }

    @Override
    public Plan plan(Vehicle vehicle, TaskSet taskSet) {

        System.out.println("Starting city:" + vehicle.getCurrentCity());
        System.out.println("Tasks:");
        for (Task task : taskSet) {
            System.out.println(task.id + ": from " + task.pickupCity + " to " + task.deliveryCity);
        }

        SearchAlgorithm algorithm;
        if (algorithmName.equalsIgnoreCase("BFS")) {
            algorithm = new BFS(taskSet, carriedTasks, vehicle);
        } else {
            algorithm = new AStar(taskSet, carriedTasks, vehicle, heuristicName);
        }

        Plan plan = algorithm.getPlan();

        System.out.println("Search algorithm: " + algorithmName);
        System.out.println("Total distance: " + plan.totalDistance());
        System.out.println("Visited states in graph: " + algorithm.visitedStates);
        return plan;
    }

    @Override
    public void planCancelled(TaskSet carriedTasks) {
        this.carriedTasks = carriedTasks;
    }
}
