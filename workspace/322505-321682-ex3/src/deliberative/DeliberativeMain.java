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
    private SearchAlgorithm algorithm;

    @Override
    public void setup(Topology topology, TaskDistribution taskDistribution, Agent agent) {
        this.carriedTasks = new HashSet<>();
        this.agent = agent;

        this.algorithmName = agent.readProperty("algorithm", String.class, "BFS");
        if (!algorithmName.equalsIgnoreCase("BFS") && !algorithmName.equalsIgnoreCase("A-star"))
            throw new AssertionError("Unsupported algorithm.");
    }

    @Override
    public Plan plan(Vehicle vehicle, TaskSet taskSet) {
        long startTime = System.currentTimeMillis();
        if (algorithmName.equalsIgnoreCase("BFS")) {
            algorithm = new BFS(taskSet, carriedTasks, vehicle);
        } else {
            algorithm = new AStar(taskSet, carriedTasks, vehicle);
        }

        Plan plan = algorithm.getPlan();
        System.out.println("\nAgent " + agent.id() + ":");
        System.out.println("Search algorithm: " + algorithmName);
        System.out.println("Total cost (until now + planned): " + (vehicle.getDistance()* vehicle.costPerKm()  + algorithm.getPlanCost()));
        System.out.println("New plan total distance: " + plan.totalDistance());
        System.out.println("Visited states during the last planning: " + algorithm.visitedStates);
        long endTime = System.currentTimeMillis();
        System.out.println("Total execution time: " + (endTime - startTime) + "ms");

        return plan;
    }

    @Override
    public void planCancelled(TaskSet carriedTasks) {
        this.carriedTasks = carriedTasks;
    }
}
