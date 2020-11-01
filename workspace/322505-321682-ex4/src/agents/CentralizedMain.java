package agents;

import enums.TaskTypeEnum;
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
import models.SolutionModel;
import models.TaskModel;
import search.StochasticLocalSearch;

import java.io.File;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;

public class CentralizedMain implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private double p;
    private long setupTimeout;
    private long planTimeout;
    private Map<City, Vehicle> closestBigVehicle;

    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
        this.p = p;
        this.closestBigVehicle = new HashMap<>();

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

        this.p = agent.readProperty("p", Double.class, 0.4);

        Map<City, List<Vehicle>> homeTowns = agent.vehicles().stream()
                .collect(groupingBy(Vehicle::homeCity));
        Map<City, Vehicle> bestVehicleForHomeTown = new HashMap<>();
        for (Map.Entry<City, List<Vehicle>> entry : homeTowns.entrySet()) {
            List<Vehicle> vehicles = entry.getValue();
            vehicles.sort(Comparator.comparingInt(Vehicle::capacity));
            bestVehicleForHomeTown.put(entry.getKey(), vehicles.get(0));
        }

        for (City city : topology.cities()) {
            closestBigVehicle.putIfAbsent(city, agent.vehicles().get(0));
            for (Map.Entry<City, Vehicle> entry : bestVehicleForHomeTown.entrySet()) {
                if (city.distanceTo(entry.getKey()) < city.distanceTo(closestBigVehicle.get(city).homeCity()))
                    closestBigVehicle.put(city, entry.getValue());
            }
        }
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        System.out.println(vehicles);
        long startTime = System.currentTimeMillis();

        StochasticLocalSearch sls = new StochasticLocalSearch(vehicles, tasks,
                // todo: set time which includes later plan computation
                planTimeout - (System.currentTimeMillis() - startTime) - 500, p, closestBigVehicle);

        sls.SLS();
        SolutionModel solution = sls.getBestSolution();

        List<Plan> plans = new ArrayList<>();
        double cost = 0;
        for (Vehicle vehicle : vehicles) {
            City currentCity = vehicle.getCurrentCity();
            List<TaskModel> taskModels = solution.getVehicleTasksMap().get(vehicle);
            Plan plan = new Plan(currentCity);

            for (TaskModel task : taskModels) {
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
                    vehicle.id(), taskModels.size() / 2, vehicleCost));
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
