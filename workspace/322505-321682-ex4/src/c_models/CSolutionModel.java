package c_models;

import c_enums.CTaskTypeEnum;
import logist.simulation.Vehicle;
import logist.topology.Topology.City;

import java.util.*;

public class CSolutionModel {

    // [vehicle -> list of TaskModel that vehicle is carrying in order]
    private Map<Vehicle, ArrayList<CTaskModel>> vehicleTasksMap;
    // [TaskModel -> index of it's pair]
    // TaskModel-s ti and tj are pairs iff ti = TaskModel(t, PICKUP) and tj = TaskModel(t, DELIVERY)
    private Map<CTaskModel, Integer> taskPairIndexMap;
    // [vehicle -> cost of that vehicle for it's current plan]
    private Map<Vehicle, Double> vehicleCostMap;
    // cost of the whole solution
    private double cost;

    public CSolutionModel(Map<Vehicle, ArrayList<CTaskModel>> vehicleTasksMap) {
        this.vehicleTasksMap = vehicleTasksMap;
        this.taskPairIndexMap = new HashMap<>();
        this.vehicleCostMap = new HashMap<>();
        this.cost = 0.0;
        this.createInitialSolutionParameters();
    }

    public CSolutionModel(CSolutionModel solution) {
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
        for (Map.Entry<Vehicle, ArrayList<CTaskModel>> entry : vehicleTasksMap.entrySet()) {
            Vehicle vehicle = entry.getKey();
            City currentCity = vehicle.getCurrentCity();
            List<CTaskModel> tasks = entry.getValue();

            double vehicleCost = 0;
            for (CTaskModel task : tasks) {
                // update task index map
                taskPairIndexMap.put(new CTaskModel(task.getTask(), task.getPairTaskType()), tasks.indexOf(task));

                // update cost
                City nextCity = task.getType().equals(CTaskTypeEnum.PICKUP) ?
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

    public Map<Vehicle, ArrayList<CTaskModel>> getVehicleTasksMap() {
        return vehicleTasksMap;
    }

    public Map<CTaskModel, Integer> getTaskPairIndexMap() {
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
}
