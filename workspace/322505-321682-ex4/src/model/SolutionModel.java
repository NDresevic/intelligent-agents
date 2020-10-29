package model;

import enums.TaskTypeEnum;
import logist.simulation.Vehicle;
import logist.topology.Topology.City;

import java.util.List;
import java.util.Map;

public class SolutionModel {

    private Map<Vehicle, List<TaskModel>> vehicleTasksMap;
    private double cost;

    public SolutionModel(Map<Vehicle, List<TaskModel>> vehicleTasksMap, double cost) {
        this.vehicleTasksMap = vehicleTasksMap;
        this.cost = cost;
    }

    public SolutionModel(Map<Vehicle, List<TaskModel>> vehicleTasksMap) {
        this.vehicleTasksMap = vehicleTasksMap;
        this.cost = calculateRootCost();
    }

    private double calculateRootCost() {
        double cost = 0.0;

        for (Map.Entry<Vehicle, List<TaskModel>> entry: vehicleTasksMap.entrySet()) {
            Vehicle vehicle = entry.getKey();
            City currentCity = vehicle.getCurrentCity();
            List<TaskModel> tasks = entry.getValue();

            for (TaskModel task: tasks) {
                City nextCity = task.getType().equals(TaskTypeEnum.PICKUP) ?
                        task.getTask().pickupCity : task.getTask().deliveryCity;
                cost += currentCity.distanceTo(nextCity) * vehicle.costPerKm();
            }
        }

        return cost;
    }

    public Map<Vehicle, List<TaskModel>> getVehicleTasksMap() {
        return vehicleTasksMap;
    }

    public double getCost() {
        return cost;
    }
}
