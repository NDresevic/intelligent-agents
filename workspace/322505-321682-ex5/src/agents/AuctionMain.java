package agents;

import enums.TaskTypeEnum;
import logist.LogistSettings;
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
    // [id -> task], in this map only the tasks that agent has won are stored
    private Map<Integer, Task> wonTasksMap;
    // current solution for the agent
    private SolutionModel currentSolution;
    // the solution with added task that we bid for
    private SolutionModel nextBidSolution;
    private AgentsBidStrategy agentsBidStrategy;
    private TaskDistributionStrategy taskDistributionStrategy;
    // capacity of a vehicle with biggest capacity - used to check if it is possible to carry new task
    private int maxCapacity;
    // maximum marginal cost that agent has bid - used for calculating  bid for a task where others bid null
    private long maxMarginalCost;

    // parameters defined in config file /settings_auction.xml
    private long setupTimeout;
    private long planTimeout;
    private long bidTimeout;

    private static long SETUP_ESTIMATED_TIME = 50;
    // upper bound time needed to create plan from optimal solution
    private static long PLAN_CREATION_ESTIMATED_TIME = 500;
    // for 50 tasks and 5 vehicles we have max bid time of 3860
    // for 300 tasks 2 vehicles we have max bid time of 7374
    private static long BID_ESTIMATED_TIME = 5000;

    //approximation for others vehicles cost per km (approximate that all vehicles from other agents have average
    //      capacity of our agent vehicles)
    private double approximatedVehicleCost;

    // parameters defined in config file /agents.xml
    private double epsilon;
    //
    private double discount;
    // threshold for speculated probability
    private double probabilityThreshold;
    // discount for the bid if it is payable
    private double distributionDiscount;


    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        long startTime = System.currentTimeMillis();

        this.agent = agent;
        this.wonTasksMap = new HashMap<>();
        this.currentSolution = new SolutionModel(agent.vehicles());
        this.nextBidSolution = new SolutionModel(currentSolution);
        // approximate that all vehicles from other agents have average capacity of our agent vehicles
        this.approximatedVehicleCost = agent.vehicles().stream().map(Vehicle::costPerKm).reduce(0, Integer::sum)
                * 1.0 / agent.vehicles().size();
        this.maxCapacity = Integer.MIN_VALUE;
        this.maxMarginalCost = Long.MIN_VALUE;

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
        this.epsilon = agent.readProperty("epsilon", Double.class, 0.3);
        this.discount = agent.readProperty("discount", Double.class, 0.5);
        this.probabilityThreshold = agent.readProperty("probabilityThreshold", Double.class, 0.2);
        this.distributionDiscount = agent.readProperty("distributionDiscount", Double.class, 0.25);

        if (setupTimeout < System.currentTimeMillis() - startTime - SETUP_ESTIMATED_TIME) {
            System.err.println("Setup time is not long enough for the preprocessing. The planning is terminated.");
            System.exit(1);
        }

        this.agentsBidStrategy = new AgentsBidStrategy(epsilon, topology, approximatedVehicleCost, agent.id());
        this.taskDistributionStrategy = new TaskDistributionStrategy(distribution, approximatedVehicleCost,
                probabilityThreshold, distributionDiscount);
        for (Vehicle vehicle : agent.vehicles()) {
            this.maxCapacity = Math.max(vehicle.capacity(), maxCapacity);
        }
    }

    @Override
    public Long askPrice(Task task) {
        // return null if we don't have a vehicle big enough to carry the task
        if (task.weight > this.maxCapacity) {
            return null;
        }

        TaskModel pickupTask = new TaskModel(task, TaskTypeEnum.PICKUP);
        TaskModel deliveryTask = new TaskModel(task, TaskTypeEnum.DELIVERY);

        if (this.wonTasksMap.isEmpty()) {
            nextBidSolution = EstimateSolutionStrategy.addFirstTaskToSolution(new SolutionModel(currentSolution),
                    pickupTask, deliveryTask);
        }
        // if bid time is not long enough for our strategy agent bids the approximated maximal marginal cost
        else if (bidTimeout < BID_ESTIMATED_TIME) {
            nextBidSolution = EstimateSolutionStrategy.addTaskToEnd(new SolutionModel(currentSolution),
                    pickupTask, deliveryTask);

            long distance = (long) (agentsBidStrategy.getBiggestCityDistance() +
                    task.pickupCity.distanceTo(task.deliveryCity));
            long marginalCost = (long) (distance * discount * this.approximatedVehicleCost);
            this.maxMarginalCost = Math.max(marginalCost, maxMarginalCost);
            return marginalCost;
        }
        // create best next solution if agent receives the auction task
        else {
            nextBidSolution = EstimateSolutionStrategy.optimalSolutionWithTask(new SolutionModel(currentSolution),
                    pickupTask, deliveryTask);
        }


        long marginalCost = (long) (nextBidSolution.getCost() - currentSolution.getCost());
        this.maxMarginalCost = Math.max(marginalCost, maxMarginalCost);
        System.out.println("\nMarginal cost: " + marginalCost);

        Long myBid = agentsBidStrategy.calculateMyBid(task, marginalCost);
        myBid = taskDistributionStrategy.refineBid(task, marginalCost, myBid);

        if (myBid <= 1) {
            myBid = (long) (this.discount *
                    task.pickupCity.distanceTo(task.deliveryCity) * this.approximatedVehicleCost);
        }
        return myBid;
    }

    @Override
    public void auctionResult(Task lastTask, int lastWinner, Long[] lastOffers) {
        if (agentsBidStrategy.getAgentEstimatedCostsMap().isEmpty()) {
            agentsBidStrategy.initializeAgentCosts(lastOffers.length);
        }

        if (lastOffers[agent.id()] != null) {
            double diffFromOptimal = lastOffers[agent.id()] - lastOffers[lastWinner];
            System.out.println(String.format("My bid: %d | Difference from optimal bid: %f",
                    lastOffers[agent.id()], diffFromOptimal));
        }

        if (bidTimeout > BID_ESTIMATED_TIME) {
            agentsBidStrategy.updateTables(lastTask, lastWinner, lastOffers, maxMarginalCost);
        }

        // I won the auction, my solution is now next bid solution
        if (lastWinner == agent.id()) {
            taskDistributionStrategy.appendWonTask(lastTask);
            currentSolution = nextBidSolution;

            this.wonTasksMap.put(lastTask.id, lastTask);
        }
        nextBidSolution = new SolutionModel(currentSolution);
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long startTime = System.currentTimeMillis();

        System.out.println(wonTasksMap);
        System.out.println(tasks);

        System.out.println("Initial plan:");
        for (Map.Entry<Vehicle, ArrayList<TaskModel>> entry : currentSolution.getVehicleTasksMap().entrySet()) {
            Vehicle vehicle = entry.getKey();
            // change tasks because their rewards have changed after auction
            ArrayList<TaskModel> newTaskModels = new ArrayList<>();
            for (TaskModel taskModel : entry.getValue()) {
                newTaskModels.add(new TaskModel(this.wonTasksMap.get(taskModel.getTask().id), taskModel.getType()));
            }
            currentSolution.getVehicleTasksMap().put(vehicle, newTaskModels);

            System.out.print(String.format("Vehicle: %d | Number of tasks: %d | Cost: %.2f",
                    vehicle.id(), entry.getValue().size() / 2, currentSolution.getVehicleCostMap().get(vehicle)));
            System.out.println(" | Tasks: " + entry.getValue());
        }
        System.out.println();

        CentralizedSLS sls = new CentralizedSLS(vehicles,
                planTimeout - (System.currentTimeMillis() - startTime) - PLAN_CREATION_ESTIMATED_TIME,
                currentSolution);

        sls.SLS();
        SolutionModel solution = sls.getBestSolution();

        double reward = 0.0;
        for (Task task : tasks) {
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
