package models;

import enums.TaskTypeEnum;
import logist.simulation.Vehicle;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolutionModel {

    // [vehicle -> list of TaskModel that vehicle is carrying in order]
    private Map<Vehicle, ArrayList<TaskModel>> vehicleTasksMap;
    // [TaskModel -> index of it's pair]
    // TaskModel-s ti and tj are pairs iff ti = TaskModel(t, PICKUP) and tj = TaskModel(t, DELIVERY)
    private Map<TaskModel, Integer> taskPairIndexMap;
    // [vehicle -> cost of that vehicle for it's current plan]
    private Map<Vehicle, Double> vehicleCostMap;
    // cost of the whole solution
    private double cost;

    public SolutionModel(List<Vehicle> vehicles) {
        this.vehicleTasksMap = new HashMap<>();
        for (Vehicle vehicle : vehicles) {
            vehicleTasksMap.put(vehicle, new ArrayList<>());
        }
        this.taskPairIndexMap = new HashMap<>();
        this.vehicleCostMap = new HashMap<>();
        this.cost = 0.0;
    }

    public SolutionModel(Map<Vehicle, ArrayList<TaskModel>> vehicleTasksMap) {
        this.vehicleTasksMap = vehicleTasksMap;
        this.taskPairIndexMap = new HashMap<>();
        this.vehicleCostMap = new HashMap<>();
        this.cost = 0.0;
        this.createInitialSolutionParameters();
    }

    public SolutionModel(SolutionModel solution) {
        this.vehicleTasksMap = new HashMap<>(solution.vehicleTasksMap);
        this.taskPairIndexMap = new HashMap<>(solution.taskPairIndexMap);
        this.vehicleCostMap = new HashMap<>(solution.vehicleCostMap);
        this.cost = solution.cost;
    }

    /**
     * Method that initializes for first solution cost for each vehicle, total cost and creates a mapping for each task
     * index of it's corresponding pair.
     */
    private void createInitialSolutionParameters() {
        for (Map.Entry<Vehicle, ArrayList<TaskModel>> entry : vehicleTasksMap.entrySet()) {
            Vehicle vehicle = entry.getKey();
            City currentCity = vehicle.getCurrentCity();
            List<TaskModel> tasks = entry.getValue();

            double vehicleCost = 0;
            for (TaskModel task : tasks) {
                // update task index map
                taskPairIndexMap.put(new TaskModel(task.getTask(), task.getPairTaskType()), tasks.indexOf(task));

                // update cost
                City nextCity = task.getType().equals(TaskTypeEnum.PICKUP) ?
                        task.getTask().pickupCity : task.getTask().deliveryCity;
                vehicleCost += currentCity.distanceTo(nextCity) * vehicle.costPerKm();
                currentCity = nextCity;
            }
            vehicleCostMap.put(vehicle, vehicleCost);
            cost += vehicleCost;

            System.out.println(String.format("Vehicle: %d | Starting cost: %.2f", vehicle.id(),
                    vehicleCostMap.get(vehicle)));
        }
        System.out.println();
    }

    public Map<Vehicle, ArrayList<TaskModel>> getVehicleTasksMap() {
        return vehicleTasksMap;
    }

    public Map<TaskModel, Integer> getTaskPairIndexMap() {
        return taskPairIndexMap;
    }

    public Map<Vehicle, Double> getVehicleCostMap() {
        return vehicleCostMap;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    @Override
    public String toString() {
        return "SolutionModel{" +
                "vehicleTasksMap=" + vehicleTasksMap +
                ", taskPairIndexMap=" + taskPairIndexMap +
                ", vehicleCostMap=" + vehicleCostMap +
                ", cost=" + cost +
                '}';
    }
}