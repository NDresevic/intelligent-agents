package model;

import enums.OperationTypeEnum;
import enums.TaskTypeEnum;
import logist.simulation.Vehicle;
import logist.topology.Topology;

import java.util.ArrayList;

public class SwapTasksOperation extends Operation {

    private int i; //first task index in vehicle's list to be swapped
    private int j; //second task index in vehicle's list to be swapped
    private Vehicle vehicle;

    public SwapTasksOperation(SolutionModel currentSolution, int i, int j, Vehicle vehicle) {
        super(currentSolution, OperationTypeEnum.CHANGE_TASK_ORDER);
        this.i = Math.min(i, j);
        this.j = Math.max(i, j);
        this.vehicle = vehicle;
    }

    @Override
    public SolutionModel getNewSolution() {
        SolutionModel neighborSolution = new SolutionModel(currentSolution);
        ArrayList<TaskModel> tasks = neighborSolution.getVehicleTasksMap().get(vehicle);

        TaskModel ti = tasks.get(i);
        TaskModel tj = tasks.get(j);
        if (ti.getType() == TaskTypeEnum.PICKUP && neighborSolution.getTaskPairIndexMap().get(ti) <= j
                ||
                tj.getType() == TaskTypeEnum.DELIVERY && neighborSolution.getTaskPairIndexMap().get(tj) >= i)
            return null;

        ArrayList<TaskModel> neighborTasks = new ArrayList<>();

        int load = 0;
        double vehicleCost = 0d;

        Topology.City currentCity = vehicle.getCurrentCity();
        for (int k = 0; k < tasks.size(); k++) {
            TaskModel task = tasks.get(k);
            TaskModel newTask;
            if (k != i && k != j) {
                newTask = task;
            } else if (k == i) {
                newTask = tasks.get(j);
                neighborSolution.getTaskPairIndexMap().put(new TaskModel(newTask.getTask(), newTask.getPairTaskType()), i);
            } else { //k==j
                newTask = tasks.get(i);
                neighborSolution.getTaskPairIndexMap().put(new TaskModel(newTask.getTask(), newTask.getPairTaskType()), j);
            }
            load = load + newTask.getUpdatedLoad();
            neighborTasks.add(newTask);
            if (load > vehicle.capacity())
                return null;

            Topology.City nextCity = newTask.getType().equals(TaskTypeEnum.PICKUP) ?
                    newTask.getTask().pickupCity : newTask.getTask().deliveryCity;
            vehicleCost += currentCity.distanceTo(nextCity) * vehicle.costPerKm();
            currentCity = nextCity;
        }

        double previousVehicleCost = neighborSolution.getVehicleCostMap().get(vehicle);
        neighborSolution.getVehicleTasksMap().put(vehicle, neighborTasks);
        neighborSolution.getVehicleCostMap().put(vehicle, vehicleCost);
        neighborSolution.setCost(neighborSolution.getCost() - previousVehicleCost + vehicleCost);

        return neighborSolution;
    }
}
