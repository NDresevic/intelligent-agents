package agents;

import enums.TaskTypeEnum;
import logist.LogistSettings;
import logist.Measures;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import models.SolutionModel;
import models.TaskModel;
import search.CentralizedSLS;
import strategy.EstimateSolutionStrategy;
import strategy.TaskDistributionStrategy;
import strategy.AgentsBidStrategy;

import java.io.File;
import java.util.*;

public class AuctionMain implements AuctionBehavior {

    private Agent agent;
    private List<TaskModel> taskModels;
    private SolutionModel solutionModel;
    private AgentsBidStrategy agentsBidStrategy;
    private TaskDistributionStrategy taskDistributionStrategy;
    private EstimateSolutionStrategy estimateSolutionStrategy;

    // parameters defined in config file /settings_auction.xml
    // todo: use set up time???
    private long setupTimeout;
    private long planTimeout;
    // todo: include bidTimeout
    private long bidTimeout;
    // upper bound time needed to create plan from optimal solution
    private static long PLAN_CREATION_TIME = 500;

    // parameters defined in config file /agents.xml
    private double p;
    private Double alpha;
    private Double beta;
    private Double epsilon;

    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        this.agent = agent;
        this.taskModels = new ArrayList<>();
        this.solutionModel = new SolutionModel(agent.vehicles());

        // this code is used to get the timeouts
        try {
            LogistSettings ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");

            // the setup method cannot last more than timeout_setup milliseconds
            this.setupTimeout = ls.get(LogistSettings.TimeoutKey.SETUP);
            // the plan method cannot execute more than timeout_plan milliseconds
            this.planTimeout = ls.get(LogistSettings.TimeoutKey.PLAN);
            // the bidding cannot execute more than timeout_bid milliseconds
            this.bidTimeout = ls.get(LogistSettings.TimeoutKey.BID);
        } catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }

        // loading model parameters
        this.p = agent.readProperty("p", Double.class, 0.4);
        this.alpha = agent.readProperty("alpha", Double.class, 4.0);
        this.beta = agent.readProperty("beta", Double.class, 0.4);
        this.epsilon = agent.readProperty("epsilon", Double.class, 0.1);

        this.agentsBidStrategy = new AgentsBidStrategy(epsilon, topology, agent);
        this.taskDistributionStrategy = new TaskDistributionStrategy(distribution);
        this.estimateSolutionStrategy = new EstimateSolutionStrategy(solutionModel);
    }

    @Override
    public Long askPrice(Task task) {
        Vehicle vehicle = agent.vehicles().get(0);
        if (vehicle.capacity() < task.weight) {
            return null;
        }

        long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
        long distanceSum = distanceTask
                + vehicle.getCurrentCity().distanceUnitsTo(task.pickupCity);
        Long marginalCost = (long) Math.ceil(Measures.unitsToKM(distanceSum
                * vehicle.costPerKm()));
        System.out.println("\nMarginal cost : " + marginalCost);

        double speculatedProbability = taskDistributionStrategy.speculateOnTaskDistribution(task);
        agentsBidStrategy.extractBidPriceForOthers(task);

        Long bidOfOtherAgents = agentsBidStrategy.getExtractedBid();
        double beliefForExtractedBid = agentsBidStrategy.getBeliefForExtractedBid();
        System.out.println(String.format("\nExtracted bid: %d | Speculated probability: %f",
                bidOfOtherAgents, speculatedProbability));
        System.out.println("Belief: " + beliefForExtractedBid);
        Long approximateBidOfOthers;

        if(bidOfOtherAgents != null)
            approximateBidOfOthers = (long) Math.ceil(beliefForExtractedBid * bidOfOtherAgents + (1 - beliefForExtractedBid) * marginalCost) - 1;
        else
            approximateBidOfOthers = (long) Math.ceil(marginalCost) - 1;
        System.out.println("Approximated bid: " + approximateBidOfOthers);

        //Long myBid = Math.max(approximateBidOfOthers - 1, Math.ceil(marginalCost));
        Long myBid;
        if (approximateBidOfOthers > marginalCost)
            myBid = approximateBidOfOthers;
        else
            myBid = marginalCost;


        if (speculatedProbability > 0.2 && myBid == marginalCost) {
            System.out.println("Decided to bid lower!");
            //myBid = (long) (0.95 * myBid);
            myBid -= (long) (0.25 * task.pickupCity.distanceTo(task.deliveryCity) * agent.vehicles().get(0).costPerKm());
        }
        System.out.println("My bid: " + myBid);

        System.out.println("Speculated probability: " + speculatedProbability);

        return myBid;
    }

    @Override
    public void auctionResult(Task lastTask, int lastWinner, Long[] lastOffers) {
        //TODO: handle null values in lastOffers (that means that the agent did not participate in the auction)
        //TODO: can we do better than assuming that all all vehicles have the same cost

        if (agentsBidStrategy.getAgentEstimatedCostsMap().isEmpty()) {
            agentsBidStrategy.initializeAgentCosts(lastOffers.length);
        }

        double diffFromOptimal = lastOffers[agent.id()] - lastOffers[lastWinner];
        System.out.println(String.format("My bid: %d | Difference from optimal bid: %f",
                lastOffers[agent.id()], diffFromOptimal));

        agentsBidStrategy.updateTables(lastTask, lastWinner, lastOffers);

        // todo: calculate the bid based on agent bids and task distribution
        // I won the auction, add it to optimal position in my current tasks
        if (lastWinner == agent.id()) {
            taskDistributionStrategy.appendWonTask(lastTask);
            TaskModel pickupTask = new TaskModel(lastTask, TaskTypeEnum.PICKUP);
            TaskModel deliveryTask = new TaskModel(lastTask, TaskTypeEnum.DELIVERY);

            if (this.taskModels.isEmpty()) {
                estimateSolutionStrategy.addFirstTaskToSolution(pickupTask, deliveryTask);
                solutionModel = estimateSolutionStrategy.getSolution();
            } else {
                // find optimal place
                SolutionModel newSolution = estimateSolutionStrategy.optimalSolutionWithTask(pickupTask, deliveryTask);
                // todo: use marginal cost
                double marginalCost = newSolution.getCost() - solutionModel.getCost();
                estimateSolutionStrategy.setSolution(newSolution);
                solutionModel = estimateSolutionStrategy.getSolution();
            }

            this.taskModels.add(new TaskModel(lastTask, TaskTypeEnum.PICKUP));
            this.taskModels.add(new TaskModel(lastTask, TaskTypeEnum.DELIVERY));
        }
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        System.out.println("Initial plan:");
        for (Map.Entry<Vehicle, ArrayList<TaskModel>> entry : solutionModel.getVehicleTasksMap().entrySet()) {
            Vehicle vehicle = entry.getKey();
            System.out.print(String.format("Vehicle: %d | Number of tasks: %d | Cost: %.2f",
                    vehicle.id(), entry.getValue().size() / 2, solutionModel.getVehicleCostMap().get(vehicle)));
            System.out.println(" | Tasks: " + entry.getValue());
        }
        System.out.println();

        long startTime = System.currentTimeMillis();

        // todo: proveriti da li imamo bug onog lika sa moodle - ne koristiti task set nego svoje taskove?
        CentralizedSLS sls = new CentralizedSLS(vehicles, tasks,
                planTimeout - (System.currentTimeMillis() - startTime) - PLAN_CREATION_TIME,
                p, alpha, beta, solutionModel);

        sls.SLS();
        SolutionModel solution = sls.getBestSolution();

        Double reward = 0.0;
        for(Task task : tasks){
            reward += task.reward;
        }
        reward -= solution.getCost();


        List<Plan> plans = new ArrayList<>();
        double cost = 0;
        for (Vehicle vehicle : vehicles) {
            City currentCity = vehicle.getCurrentCity();
            List<TaskModel> TaskModels = solution.getVehicleTasksMap().get(vehicle);
            Plan plan = new Plan(currentCity);

            for (TaskModel task : TaskModels) {
                City nextCity;

                if (task.getType().equals(TaskTypeEnum.PICKUP)) {
                    nextCity = task.getTask().pickupCity;
                    List<City> intermediateCities = currentCity.pathTo(nextCity);
                    for (City city : intermediateCities) {
                        plan.appendMove(city);
                    }

                    plan.appendPickup(task.getTask());
                } else {
                    nextCity = task.getTask().deliveryCity;
                    List<City> intermediateCities = currentCity.pathTo(nextCity);
                    for (City city : intermediateCities) {
                        plan.appendMove(city);
                    }

                    plan.appendDelivery(task.getTask());
                }

                currentCity = nextCity;
            }

            double vehicleCost = plan.totalDistance() * vehicle.costPerKm();
            System.out.println(String.format("Vehicle: %d | Number of tasks: %d | Cost: %.2f",
                    vehicle.id(), TaskModels.size() / 2, vehicleCost));
            cost += vehicleCost;
            plans.add(plan);
            System.out.println(plan);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Plan generation execution: " + duration + " ms.");
        System.out.println("Total cost of plans: " + cost);
        System.out.println("Total reward: " + reward);

        return plans;
    }
}
