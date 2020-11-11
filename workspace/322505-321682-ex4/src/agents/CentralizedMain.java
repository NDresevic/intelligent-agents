package agents;

import c_enums.CTaskTypeEnum;
import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import c_models.CSolutionModel;
import c_models.CTaskModel;
import c_search.CStochasticLocalSearch;

import java.io.File;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;

public class CentralizedMain implements CentralizedBehavior {

    private Topology topology;
    private Agent agent;

    // parameters defined in config file /settings_default.xml
    private long setupTimeout;
    private long planTimeout;
    // upper bound time needed to create plan from optimal solution
    private static long PLAN_CREATION_TIME = 500;

    // parameters defined in config file /agents.xml
    private double p;
    private Double alpha;
    private Double beta;
    private String initialSolutionName;

    private Map<City, Vehicle> closestBigVehicle;
    private List<Vehicle> biggestVehicles;

    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        this.topology = topology;
        this.agent = agent;
        this.closestBigVehicle = new HashMap<>();
        this.biggestVehicles = new ArrayList<>();

        // this code is used to get the timeouts
        try {
            LogistSettings ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");

            // the setup method cannot last more than timeout_setup milliseconds
            this.setupTimeout = ls.get(LogistSettings.TimeoutKey.SETUP);
            // the plan method cannot execute more than timeout_plan milliseconds
            this.planTimeout = ls.get(LogistSettings.TimeoutKey.PLAN);
        } catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }

        // loading model parameters
        this.p = agent.readProperty("p", Double.class, 0.4);
        this.alpha = agent.readProperty("alpha", Double.class, 4.0);
        this.beta = agent.readProperty("beta", Double.class, 0.4);
        this.initialSolutionName = agent.readProperty("initialSolution", String.class, "FairBasedOnHomeCity");


        // 50 was approximated (for 250 agents and 1m tasks preprocessing lasts for approx 20ms)
        if (setupTimeout > 50) {
            topologyPreprocessing();
        } else {
            System.err.println("Setup time is not long enough for the preprocessing. The planning is terminated.");
            System.exit(1);
        }
    }

    private void topologyPreprocessing() {
        // grouping vehicles by home towns
        Map<City, List<Vehicle>> homeTowns = agent.vehicles().stream().collect(groupingBy(Vehicle::homeCity));

        // choosing which vehicle is the best for a given home town
        // if two have the same home town, a vehicle with bigger capacity has an advantage
        Map<City, Vehicle> bestVehicleForHomeTown = new HashMap<>();
        for (Map.Entry<City, List<Vehicle>> entry : homeTowns.entrySet()) {
            List<Vehicle> vehicles = entry.getValue();
            vehicles.sort(Comparator.comparingInt(Vehicle::capacity));
            bestVehicleForHomeTown.put(entry.getKey(), vehicles.get(0));
        }

        // choosing which vehicle is the best option for a given city
        for (City city : topology.cities()) {
            closestBigVehicle.putIfAbsent(city, agent.vehicles().get(0));
            for (Map.Entry<City, Vehicle> entry : bestVehicleForHomeTown.entrySet()) {
                if (city.distanceTo(entry.getKey()) < city.distanceTo(closestBigVehicle.get(city).homeCity()))
                    closestBigVehicle.put(city, entry.getValue());
            }
        }

        // sort vehicle by capacity (if equal, give priority to the vehicle with smaller cost)
        biggestVehicles = new ArrayList<>(agent.vehicles());
        biggestVehicles.sort((v1, v2) -> v1.capacity() == v2.capacity() ?
                v2.costPerKm() - v1.costPerKm() : v1.capacity() - v2.capacity());
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long startTime = System.currentTimeMillis();

        CStochasticLocalSearch sls = new CStochasticLocalSearch(vehicles, tasks,
                planTimeout - (System.currentTimeMillis() - startTime) - PLAN_CREATION_TIME,
                p, alpha, beta, initialSolutionName, closestBigVehicle, biggestVehicles);

        sls.SLS();
        CSolutionModel solution = sls.getBestSolution();

        List<Plan> plans = new ArrayList<>();
        double cost = 0;
        for (Vehicle vehicle : vehicles) {
            City currentCity = vehicle.getCurrentCity();
            List<CTaskModel> CTaskModels = solution.getVehicleTasksMap().get(vehicle);
            Plan plan = new Plan(currentCity);

            for (CTaskModel task : CTaskModels) {
                City nextCity;

                if (task.getType().equals(CTaskTypeEnum.PICKUP)) {
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
                    vehicle.id(), CTaskModels.size() / 2, vehicleCost));
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
