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
import models.SolutionModel;
import models.TaskModel;
import search.CentralizedSLS;

import java.io.File;
import java.util.*;

public class AuctionMain implements AuctionBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private List<TaskModel> taskModels;

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

    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
        this.taskModels = new ArrayList<>();

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
        double marginalCost = Measures.unitsToKM(distanceSum
                * vehicle.costPerKm());

        return Math.round(marginalCost);
    }

    @Override
    public void auctionResult(Task lastTask, int lastWinner, Long[] lastOffers) {
        // todo: do something based on lastOffers
        if (lastWinner == agent.id()) {
            this.taskModels.add(new TaskModel(lastTask, TaskTypeEnum.PICKUP));
            this.taskModels.add(new TaskModel(lastTask, TaskTypeEnum.DELIVERY));
        }
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long startTime = System.currentTimeMillis();

        // temp: to create initial solution
        Map<Vehicle, ArrayList<TaskModel>> vehicleTasksMap = new HashMap<>();
        vehicleTasksMap.put(vehicles.get(0), (ArrayList<TaskModel>) taskModels);
        SolutionModel initialSolution = new SolutionModel(vehicleTasksMap);
        //

        ArrayList<Vehicle> oneVehicle = new ArrayList<>();
        oneVehicle.add(vehicles.get(0));
        CentralizedSLS sls = new CentralizedSLS(oneVehicle, tasks,
                planTimeout - (System.currentTimeMillis() - startTime) - PLAN_CREATION_TIME,
                p, alpha, beta, initialSolution);

        sls.SLS();
        SolutionModel solution = sls.getBestSolution();

        List<Plan> plans = new ArrayList<>();
        double cost = 0;
        for (Vehicle vehicle : vehicles) {
            Topology.City currentCity = vehicle.getCurrentCity();
            List<TaskModel> TaskModels = solution.getVehicleTasksMap().get(vehicle);
            Plan plan = new Plan(currentCity);

            for (TaskModel task : TaskModels) {
                Topology.City nextCity;

                if (task.getType().equals(TaskTypeEnum.PICKUP)) {
                    nextCity = task.getTask().pickupCity;
                    List<Topology.City> intermediateCities = currentCity.pathTo(nextCity);
                    for (Topology.City city : intermediateCities) {
                        plan.appendMove(city);
                    }

                    plan.appendPickup(task.getTask());
                } else {
                    nextCity = task.getTask().deliveryCity;
                    List<Topology.City> intermediateCities = currentCity.pathTo(nextCity);
                    for (Topology.City city : intermediateCities) {
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

        return plans;
    }
}
