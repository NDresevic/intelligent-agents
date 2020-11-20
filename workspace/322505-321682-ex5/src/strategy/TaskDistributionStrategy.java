package strategy;

import logist.task.Task;
import logist.task.TaskDistribution;

import java.util.HashSet;
import java.util.Set;

public class TaskDistributionStrategy {

    //TODO
    private static double PROBABILITY_THRESHOLD = 0.2;
    //TODO
    private static double DISCOUNT = 0.25;

    private TaskDistribution distribution;
    // the tasks that the agent already won in the auction
    private Set<Task> wonTasks;
    // approximation for cost per kilometer for other agents
    private double approximatedVehicleCost;

    public TaskDistributionStrategy(TaskDistribution distribution, double approximatedVehicleCost) {
        this.distribution = distribution;
        this.approximatedVehicleCost = approximatedVehicleCost;
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

    public Long refineBid(Task task, Long marginalCost, Long myBid) {
        double speculatedProbability = speculateOnTaskDistribution(task);
        System.out.println("Speculated probability: " + speculatedProbability);
        if (speculatedProbability > PROBABILITY_THRESHOLD && myBid == marginalCost) {
            System.out.println("Decided to bid lower!");
            //myBid = (long) (0.95 * myBid);
            myBid -= (long) (DISCOUNT * task.pickupCity.distanceTo(task.deliveryCity) * approximatedVehicleCost);
        }
        return myBid;
    }
}
