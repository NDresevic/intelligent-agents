package operations;

import enums.OperationTypeEnum;
import enums.TaskTypeEnum;
import logist.simulation.Vehicle;
import logist.topology.Topology.City;
import models.SolutionModel;
import models.TaskModel;

import java.util.ArrayList;

public class SwapTasksOperation extends Operation {

    private Vehicle vehicle;
    private int i; // first task index in vehicle's list to be swapped
    private int j; // second task index in vehicle's list to be swapped

    public SwapTasksOperation(SolutionModel currentSolution, int i, int j, Vehicle vehicle) {
        super(currentSolution, OperationTypeEnum.CHANGE_TASK_ORDER);
        this.vehicle = vehicle;
        this.i = Math.min(i, j);
        this.j = Math.max(i, j);
    }

    @Override
    public SolutionModel getNewSolution() {
        ArrayList<TaskModel> tasks = neighborSolution.getVehicleTasksMap().get(vehicle);

        TaskModel ti = tasks.get(i);
        TaskModel tj = tasks.get(j);
        // return null if it is not possible to change order of the tasks (for example: ti delivery is before ti pickup)
        if ((ti.getType() == TaskTypeEnum.PICKUP && neighborSolution.getTaskPairIndexMap().get(ti) <= j)
                || (tj.getType() == TaskTypeEnum.DELIVERY && neighborSolution.getTaskPairIndexMap().get(tj) >= i)) {
            return null;
        }

        City currentCity = vehicle.getCurrentCity();
        ArrayList<TaskModel> neighborTasks = new ArrayList<>();
        int load = 0;
        double vehicleCost = 0d;

        for (int k = 0; k < tasks.size(); k++) {
            TaskModel newTask = tasks.get(k);

            if (k == i) {
                newTask = tasks.get(j);
                neighborSolution.getTaskPairIndexMap().put(new TaskModel(newTask.getTask(), newTask.getPairTaskType()), i);
            } else if (k == j){
                newTask = tasks.get(i);
                neighborSolution.getTaskPairIndexMap().put(new TaskModel(newTask.getTask(), newTask.getPairTaskType()), j);
            }
            load += newTask.getUpdatedLoad();
            neighborTasks.add(newTask);

            // return null if the plan is not valid because load is bigger than capacity
            if (load > vehicle.capacity()) {
                return null;
            }

            City nextCity = newTask.getType().equals(TaskTypeEnum.PICKUP) ?
                    newTask.getTask().pickupCity : newTask.getTask().deliveryCity;
            vehicleCost += currentCity.distanceTo(nextCity) * vehicle.costPerKm();
            currentCity = nextCity;
        }
        neighborSolution.getVehicleTasksMap().put(vehicle, neighborTasks);

        double previousVehicleCost = neighborSolution.getVehicleCostMap().get(vehicle);
        neighborSolution.getVehicleCostMap().put(vehicle, vehicleCost);
        neighborSolution.setCost(neighborSolution.getCost() - previousVehicleCost + vehicleCost);

        return neighborSolution;
    }
}
