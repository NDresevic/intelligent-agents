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
import model.SolutionModel;
import model.TaskModel;
import enums.TaskTypeEnum;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long startTime = System.currentTimeMillis();

        List<TaskModel> taskModelList = new ArrayList<>();
        for (Task task: tasks) {
            taskModelList.add(new TaskModel(task, TaskTypeEnum.PICKUP));
            taskModelList.add(new TaskModel(task, TaskTypeEnum.DELIVERY));
        }
        StochasticLocalSearch sls = new StochasticLocalSearch(vehicles, taskModelList);
        SolutionModel bestSolution = sls.getBestSolution();

        List<Plan> plans = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {
            // add each plan
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Plan generation execution: " + duration + " ms.");

        return plans;
    }
}
