package model;

import enums.TaskTypeEnum;
import logist.simulation.Vehicle;
import logist.topology.Topology.City;

import java.util.*;

public class SolutionModel {

    private Map<Vehicle, TaskModel[]> vehicleTasksMap;
    private Map<Vehicle, double[]> vehicleLoad;
    private Map<TaskModel, Integer> taskPairIndex;
    private double cost;

    public SolutionModel(Map<Vehicle, TaskModel[]> vehicleTasksMap) {
        this.vehicleTasksMap = vehicleTasksMap;
        this.calculateInitialSolutionCost();
        this.createVehicleLoad();
        this.createTaskPairIndex();
    }

    public SolutionModel(SolutionModel solution) {
        this.vehicleTasksMap = new HashMap<>(solution.vehicleTasksMap);
        this.vehicleLoad = new HashMap<>(solution.vehicleLoad);
        this.taskPairIndex = new HashMap<>(solution.taskPairIndex);
        this.cost = solution.cost;
    }

    private void calculateInitialSolutionCost() {
        cost = 0.0;

        for (Map.Entry<Vehicle, TaskModel[]> entry : vehicleTasksMap.entrySet()) {
            Vehicle vehicle = entry.getKey();
            City currentCity = vehicle.getCurrentCity();
            TaskModel[] tasks = entry.getValue();

            System.out.println(tasks.length);
            for (int i = 0; i < tasks.length; i++) {
                TaskModel task = tasks[i];
                City nextCity = task.getType().equals(TaskTypeEnum.PICKUP) ?
                        task.getTask().pickupCity : task.getTask().deliveryCity;
                cost += currentCity.distanceTo(nextCity) * vehicle.costPerKm();
            }
        }
    }

    private void createVehicleLoad() {
        vehicleLoad = new HashMap<>();
        for (Map.Entry<Vehicle, TaskModel[]> entry : vehicleTasksMap.entrySet()) {
            TaskModel[] tasks = entry.getValue();
            double[] loads = new double[tasks.length];

            double currentLoad = 0;
            for (int i = 0; i < tasks.length; i++) {
                TaskModel task = tasks[i];
                currentLoad += task.updateLoad();
                loads[i] = currentLoad;
            }
            vehicleLoad.put(entry.getKey(), loads);
        }
    }

    private void createTaskPairIndex() {
        taskPairIndex = new HashMap<>();
        for (Map.Entry<Vehicle, TaskModel[]> entry : vehicleTasksMap.entrySet()) {
            TaskModel[] tasks = entry.getValue();
            for (int i = 0; i < tasks.length; i++) {
                taskPairIndex.put(new TaskModel(tasks[i].getTask(), tasks[i].getPairOperation()), i);
            }
        }
    }

    public Map<Vehicle, TaskModel[]> getVehicleTasksMap() {
        return vehicleTasksMap;
    }

    public Map<Vehicle, double[]> getVehicleLoad() {
        return vehicleLoad;
    }

    public double getCost() { return cost; }

    public void setCost(double cost) { this.cost = cost; }

    public Map<TaskModel, Integer> getTaskPairIndex() {
        return taskPairIndex;
    }
}
