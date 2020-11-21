package strategy;

import logist.task.Task;
import logist.task.TaskDistribution;

import java.util.HashSet;
import java.util.Set;

public class TaskDistributionStrategy {

    //the threshold level for probability speculation
    // if the speculated probability is above this probability,
    // the refining of the bid is done
    // the refining means that the agent accepts to bid a bit lower than it intended
    // since the the probability to fullfill
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

    public void appendWonTask(Task task) {
        wonTasks.add(task);
    }

    //TODO explain
    public Long refineBid(Task task, Long marginalCost, Long myBid) {
        //speculates on the future tasks (the tasks that will show up for auction in the future)
        //calculate how likely is to have task [task.delivery, wontask.pickup] or [wontask.delivery, task.pickup]
        //in the future where won task is the task that the agent already won on the auction
        // it returns the biggest over all probabilities
        double speculatedProbability = 0.0;
        for (Task wonTask : wonTasks) {
            double probDeliveryPickup = distribution.probability(wonTask.deliveryCity, task.pickupCity);
            double probPickupDelivery = distribution.probability(task.deliveryCity, wonTask.pickupCity);
            speculatedProbability = Math.max(Math.max(probDeliveryPickup, probPickupDelivery), speculatedProbability);
        }
        System.out.println("Speculated probability: " + speculatedProbability);

        //refining bid if the speculated probability is greater than the threshold
        //the discount .. TODO TODO TODO explain
        if (speculatedProbability > PROBABILITY_THRESHOLD && myBid == marginalCost) {
            System.out.println("Decided to bid lower!");
            myBid = (long) (0.75 * myBid);
//            myBid -= (long) (DISCOUNT * task.pickupCity.distanceTo(task.deliveryCity) * approximatedVehicleCost);
//            if (myBid < 0)
//                myBid = 0L;
        }
        return myBid;
    }
}

