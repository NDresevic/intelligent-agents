package model;

import logist.simulation.Vehicle;

import java.util.List;
import java.util.Map;

public class SolutionModel {

    private Map<Vehicle, List<TaskModel>> vehicleTasksMap;
    private double cost;

    public SolutionModel(Map<Vehicle, List<TaskModel>> vehicleTasksMap, double cost) {
        this.vehicleTasksMap = vehicleTasksMap;
        this.cost = cost;
    }

    public Map<Vehicle, List<TaskModel>> getVehicleTasksMap() {
        return vehicleTasksMap;
    }

    public double getCost() {
        return cost;
    }
}
