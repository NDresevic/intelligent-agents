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
import strategy.StrategyFuture;
import strategy.StrategyPast;

import java.io.File;
import java.util.*;

public class AuctionMain implements AuctionBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private List<TaskModel> taskModels;
    private SolutionModel solutionModel;
    private StrategyPast strategyPast;
    private StrategyFuture strategyFuture;

    // parameters defined in config file /settings_auction.xml
    private long setupTimeout;
    private long planTimeout;
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
        this.topology = topology;
        this.distribution = distribution;
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

        this.strategyPast = new StrategyPast(epsilon, topology, agent);
        this.strategyFuture = new StrategyFuture(distribution, topology, agent);
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

        strategyPast.extractBidPriceForOthers(task);
        Long bidOfOtherAgents = strategyPast.getExtractedBid();
        double beliefForExtractedBid = strategyPast.getBeliefForExtractedBid();
        System.out.println("Extracted bid: " + bidOfOtherAgents + " | Belief: " + beliefForExtractedBid);
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



        double speculatedProbability = strategyFuture.speculateOnFuture(task);

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

        if(strategyPast.getAgentsCosts().isEmpty()){
            strategyPast.initializeAgentCosts(lastOffers.length);
        }

        double diffFromOptimal = lastOffers[agent.id()] - lastOffers[lastWinner];
        System.out.println("My bid: " + lastOffers[agent.id()] + "| Difference from optimal bid: " + diffFromOptimal);

        strategyPast.updateTables(lastTask, lastWinner, lastOffers);

        // todo: do something based on lastOffers
        // I won the auction, add it to optimal position in my current tasks
        if (lastWinner == agent.id()) {
            strategyFuture.appendWonTask(lastTask);
            TaskModel pickupTask = new TaskModel(lastTask, TaskTypeEnum.PICKUP);
            TaskModel deliveryTask = new TaskModel(lastTask, TaskTypeEnum.DELIVERY);

            if (this.taskModels.isEmpty()) {
                this.addFirstTaskToSolution(solutionModel, pickupTask, deliveryTask);
                System.out.println(solutionModel);
            } else {
                // find optimal place
                SolutionModel newSolution = this.optimalSolutionWithTask(solutionModel, pickupTask, deliveryTask);
                double marginalCost = newSolution.getCost() - solutionModel.getCost();
                solutionModel = newSolution;
            }

            this.taskModels.add(new TaskModel(lastTask, TaskTypeEnum.PICKUP));
            this.taskModels.add(new TaskModel(lastTask, TaskTypeEnum.DELIVERY));
        }
    }

    /**
     * Adds the first task to the biggest vehicle and properly updates the solution.
     * @param solution - initial solution (empty)
     * @param pickupTask - first task model for pick up
     * @param deliveryTask - first task model for delivery
     */
    private void addFirstTaskToSolution(SolutionModel solution,  TaskModel pickupTask, TaskModel deliveryTask) {
        // find vehicle with biggest capacity
        int biggestCapacity = 0;
        Vehicle biggestVehicle = null;
        for (Map.Entry<Vehicle, ArrayList<TaskModel>> entry : solution.getVehicleTasksMap().entrySet()) {
            if (entry.getKey().capacity() > biggestCapacity) {
                biggestVehicle = entry.getKey();
                biggestCapacity = biggestVehicle.capacity();
            }
            solution.getVehicleCostMap().put(entry.getKey(), 0.0);
        }

        // add tasks and update task pair map
        ArrayList<TaskModel> taskModels = new ArrayList<>();
        taskModels.add(pickupTask);
        taskModels.add(deliveryTask);
        solution.getVehicleTasksMap().put(biggestVehicle, taskModels);
        solution.getTaskPairIndexMap().put(pickupTask, 1);
        solution.getTaskPairIndexMap().put(deliveryTask, 0);

        // calculate and update costs
        double distance = biggestVehicle.getCurrentCity().distanceTo(pickupTask.getTask().pickupCity) +
                pickupTask.getTask().pickupCity.distanceTo(deliveryTask.getTask().deliveryCity);
        double cost = distance * biggestVehicle.costPerKm();
        solution.getVehicleCostMap().put(biggestVehicle, cost);
        solution.setCost(cost);
    }

    /**
     * Method that tries all possible combinations of adding new task in the current solution and returns the optimal
     * one based on overall cost.
     * @param currentSolution - current optimal solution
     * @param pickupTask - new task model for pick up
     * @param deliveryTask - new task model for delivery
     * @return - best solution when inserting new task or null if no such solution is valid
     */
    private SolutionModel optimalSolutionWithTask(SolutionModel currentSolution, TaskModel pickupTask,
                                                  TaskModel deliveryTask) {
        double bestCost = Double.MAX_VALUE;
        SolutionModel bestSolution = null;

        for (Map.Entry<Vehicle, ArrayList<TaskModel>> entry : currentSolution.getVehicleTasksMap().entrySet()) {
            Vehicle vehicle = entry.getKey();
            List<TaskModel> tasks = entry.getValue();

            for (int i = 0; i < tasks.size() + 1; i++) {   // i - position of a pick up task
                for (int j = i + 1; j < tasks.size() + 2; j++) {   // j - position of a delivery task
                    ArrayList<TaskModel> newTaskModels = new ArrayList<>(tasks);
                    newTaskModels.add(i, pickupTask);
                    newTaskModels.add(j, deliveryTask);

                    SolutionModel newSolution = new SolutionModel(currentSolution);
                    newSolution.getVehicleTasksMap().put(vehicle, newTaskModels);
                    double cost = updateSolutionAndGetCost(newSolution, vehicle);

                    // update best solution if the solution is valid and new best
                    if (Double.compare(cost, - 1) != 0 && cost < bestCost) {
                        bestCost = cost;
                        bestSolution = newSolution;
                    }
                }
            }
        }

        return bestSolution;
    }

    /**
     * Checks if the new solution with a new task added to a vehicle is possible. If so, the costs and task pair
     * map are updated.
     * @param solutionModel - solution with added new task
     * @param vehicle - vehicle in which new task is added
     * @return - solution cost or -1 if the plan is not valid
     */
    private double updateSolutionAndGetCost(SolutionModel solutionModel, Vehicle vehicle) {
        Map<Vehicle, ArrayList<TaskModel>> vehicleTasksMap = solutionModel.getVehicleTasksMap();

        double cost = 0;
        for (Map.Entry<Vehicle, ArrayList<TaskModel>> entry : vehicleTasksMap.entrySet()) {
            if (vehicle.id() != entry.getKey().id()) {  // vehicle which tasks are unchanged
                cost += solutionModel.getVehicleCostMap().get(entry.getKey());
                continue;
            }
            City currentCity = vehicle.getCurrentCity();
            ArrayList<TaskModel> taskModels = entry.getValue();

            double vehicleCost = 0;
            double vehicleLoad = 0;
            for (int i = 0; i < taskModels.size(); i++) {
                TaskModel task = taskModels.get(i);
                City nextCity = task.getType().equals(TaskTypeEnum.PICKUP) ?
                        task.getTask().pickupCity : task.getTask().deliveryCity;
                vehicleCost += currentCity.distanceTo(nextCity) * vehicle.costPerKm();

                vehicleLoad += task.getUpdatedLoad();
                // return -1 if the plan is not valid because load is bigger than capacity
                if (vehicleLoad > vehicle.capacity()) {
                    return -1;
                }

                solutionModel.getTaskPairIndexMap().put(new TaskModel(task.getTask(), task.getPairTaskType()), i);
                currentCity = nextCity;
            }
            // update vehicle cost
            solutionModel.getVehicleCostMap().put(vehicle, vehicleCost);
            cost += vehicleCost;
        }

        solutionModel.setCost(cost);
        return cost;
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
