package strategy;

import logist.task.Task;
import logist.task.TaskDistribution;

import java.util.HashSet;
import java.util.Set;

public class TaskDistributionStrategy {

    private TaskDistribution distribution;
    private Set<Task> wonTasks;

    public TaskDistributionStrategy(TaskDistribution distribution) {
        this.distribution = distribution;
        this.wonTasks = new HashSet<>();
    }

    // todo (Duka): prokomentarisi ovo
    public double speculateOnTaskDistribution(Task task) {
        double maxProbability = 0.0;
        for (Task wonTask : wonTasks) {
            double probDeliveryPickup = distribution.probability(wonTask.deliveryCity, task.pickupCity);
            double probPickupDelivery = distribution.probability(task.deliveryCity, wonTask.pickupCity);
            maxProbability = Math.max(Math.max(probDeliveryPickup, probPickupDelivery), maxProbability);
        }

        return maxProbability;
    }

    public void appendWonTask(Task task) {
        wonTasks.add(task);
    }
}
