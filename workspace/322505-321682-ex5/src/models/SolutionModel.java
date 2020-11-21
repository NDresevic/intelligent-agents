package models;

import logist.simulation.Vehicle;

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

    public SolutionModel(SolutionModel solution) {
        this.vehicleTasksMap = new HashMap<>(solution.vehicleTasksMap);
        this.taskPairIndexMap = new HashMap<>(solution.taskPairIndexMap);
        this.vehicleCostMap = new HashMap<>(solution.vehicleCostMap);
        this.cost = solution.cost;
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
