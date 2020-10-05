package agents;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;

import java.util.Random;

public class GreedyAgent implements ReactiveBehavior {

    private int rewardThreshold;
    private int numActions;
    private Agent myAgent;

    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        // Reads the discount factor from the agents.xml file.
        // If the property is not present it defaults to 0.95

        this.rewardThreshold = agent.readProperty("reward-threshold", Integer.class,
                5000);
        this.numActions = 0;
        this.myAgent = agent;
    }

    @Override
    public Action act(Vehicle vehicle, Task availableTask) {
        Action action;

        if (availableTask != null && availableTask.reward >= rewardThreshold) {
            action = new Action.Pickup(availableTask);
        } else {
            Topology.City currentCity = vehicle.getCurrentCity();
            action = new Action.Move(currentCity.randomNeighbor(new Random()));
        }

        if (numActions >= 1) {
            System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
        }
        numActions++;

        return action;
    }
}
