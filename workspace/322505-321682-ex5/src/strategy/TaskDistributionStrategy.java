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

    /**
     * speculates on the future tasks (the tasks that will show up for auction in the future)
     * calculate how likely is to have task [task.delivery, wontask.pickup] or [wontask.delivery, task.pickup]
     * in the future where won task is the task that the agent already won on the auctions
     * it returns the biggest over all probabilities
     *
     * @param task
     * @return
     */
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
