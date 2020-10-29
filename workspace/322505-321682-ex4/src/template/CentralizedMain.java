package template;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import model.SolutionModel;
import model.TaskModel;
import enums.TaskTypeEnum;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CentralizedMain implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long setupTimeout;
    private long planTimeout;

    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;

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
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long startTime = System.currentTimeMillis();

        List<TaskModel> taskModelList = new ArrayList<>();
        for (Task task : tasks) {
            taskModelList.add(new TaskModel(task, TaskTypeEnum.PICKUP));
            taskModelList.add(new TaskModel(task, TaskTypeEnum.DELIVERY));
        }
        StochasticLocalSearch sls = new StochasticLocalSearch(vehicles, taskModelList,
                // todo: set time which includes later plan computation
                System.currentTimeMillis() - startTime);
        SolutionModel bestSolution = sls.getBestSolution();

        List<Plan> plans = new ArrayList<>();
        double cost = 0;
        for (Map.Entry<Vehicle, List<TaskModel>> entry : bestSolution.getVehicleTasksMap().entrySet()) {
            Vehicle currentVehicle = entry.getKey();
            City currentCity = currentVehicle.getCurrentCity();
            List<TaskModel> taskModels = entry.getValue();
            Plan plan = new Plan(currentCity);

            for (TaskModel task : taskModels) {
                if (task.getType().equals(TaskTypeEnum.PICKUP)) {
                    plan.appendMove(task.getTask().pickupCity);
                    plan.appendPickup(task.getTask());
                } else {
                    plan.appendMove(task.getTask().deliveryCity);
                    plan.appendDelivery(task.getTask());
                }
            }

            double vehicleCost = plan.totalDistance() * currentVehicle.costPerKm();
            System.out.println("Cost for vehicle " + currentVehicle.id() + ": " + vehicleCost);
            cost += vehicleCost;
            plans.add(plan);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Plan generation execution: " + duration + " ms.");
        System.out.println("Total cost of plans: " + cost);

        return plans;
    }
}