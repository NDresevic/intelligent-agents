package strategy;

import logist.agent.Agent;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;

import java.util.HashSet;
import java.util.Set;

public class StrategyFuture {

    private TaskDistribution distribution;
    private Topology topology;
    private Agent agent;
    private Set<Task> wonTasks;

    public StrategyFuture(TaskDistribution distribution, Topology topology, Agent agent) {
        this.distribution = distribution;
        this.topology = topology;
        this.agent = agent;
        this.wonTasks = new HashSet<>();
    }

    public double speculateOnFuture(Task task) {
        double maxProbability = 0.0;
        for(Task wonTask : wonTasks){
            double probDeliveryPickup = distribution.probability(wonTask.deliveryCity, task.pickupCity);
            if (probDeliveryPickup > maxProbability)
                maxProbability = probDeliveryPickup;
            double probPickupDelivery = distribution.probability(task.deliveryCity, wonTask.pickupCity);
            if(probPickupDelivery > maxProbability)
                maxProbability = probPickupDelivery;
        }
        return maxProbability;
    }

    public void appendWonTask(Task task){
        wonTasks.add(task);
    }
}
