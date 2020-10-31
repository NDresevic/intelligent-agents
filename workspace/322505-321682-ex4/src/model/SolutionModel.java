package model;

import enums.TaskTypeEnum;
import logist.simulation.Vehicle;
import logist.topology.Topology.City;

import java.util.*;

public class SolutionModel {

    // [vehicle -> list of TaskModel that vehicle is carrying in order]
    private Map<Vehicle, ArrayList<TaskModel>> vehicleTasksMap;
    // [TaskModel -> index of it's pair]
    // TaskModel-s ti and tj are pairs iff ti = TaskModel(t, PICKUP) and tj = TaskModel(t, DELIVERY)
    private Map<TaskModel, Integer> taskPairIndexMap;
    private double cost;

    public SolutionModel(Map<Vehicle, ArrayList<TaskModel>> vehicleTasksMap) {
        this.vehicleTasksMap = vehicleTasksMap;
        this.taskPairIndexMap = new HashMap<>();
        this.cost = 0.0;
        this.createInitialSolutionParameters();
    }

    public SolutionModel(SolutionModel solution) {
        this.vehicleTasksMap = new HashMap<>(solution.vehicleTasksMap);
        this.taskPairIndexMap = new HashMap<>(solution.taskPairIndexMap);
        this.cost = solution.cost;
    }

    /**
     * Method that initializes for first solution cost and creates a mapping for each task index of it's corresponding
     * pair.
     */
    private void createInitialSolutionParameters() {
        for (Map.Entry<Vehicle, ArrayList<TaskModel>> entry : vehicleTasksMap.entrySet()) {
            Vehicle vehicle = entry.getKey();
            City currentCity = vehicle.getCurrentCity();
            List<TaskModel> tasks = entry.getValue();

            for (TaskModel task : tasks) {
                // update cost
                City nextCity = task.getType().equals(TaskTypeEnum.PICKUP) ?
                        task.getTask().pickupCity : task.getTask().deliveryCity;
                cost += currentCity.distanceTo(nextCity) * vehicle.costPerKm();

                // update task index map
                taskPairIndexMap.put(new TaskModel(task.getTask(), task.getPairTaskType()), tasks.indexOf(task));

                currentCity = nextCity;
            }
            System.out.println("pocetni cost: " + cost);
        }
    }

    public Map<Vehicle, ArrayList<TaskModel>> getVehicleTasksMap() {
        return vehicleTasksMap;
    }

    public Map<TaskModel, Integer> getTaskPairIndexMap() {
        return taskPairIndexMap;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }
}
