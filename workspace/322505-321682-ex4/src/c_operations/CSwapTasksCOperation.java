package c_operations;

import c_enums.COperationTypeEnum;
import c_enums.CTaskTypeEnum;
import logist.simulation.Vehicle;
import logist.topology.Topology.City;
import c_models.CSolutionModel;
import c_models.CTaskModel;

import java.util.ArrayList;

public class CSwapTasksCOperation extends COperation {

    private Vehicle vehicle;
    private int i; // first task index in vehicle's list to be swapped
    private int j; // second task index in vehicle's list to be swapped

    public CSwapTasksCOperation(CSolutionModel currentSolution, int i, int j, Vehicle vehicle) {
        super(currentSolution, COperationTypeEnum.CHANGE_TASK_ORDER);
        this.vehicle = vehicle;
        this.i = Math.min(i, j);
        this.j = Math.max(i, j);
    }

    @Override
    public CSolutionModel getNewSolution() {
        ArrayList<CTaskModel> tasks = neighborSolution.getVehicleTasksMap().get(vehicle);

        CTaskModel ti = tasks.get(i);
        CTaskModel tj = tasks.get(j);
        // return null if it is not possible to change order of the tasks (for example: ti delivery is before ti pickup)
        if ((ti.getType() == CTaskTypeEnum.PICKUP && neighborSolution.getTaskPairIndexMap().get(ti) <= j)
                || (tj.getType() == CTaskTypeEnum.DELIVERY && neighborSolution.getTaskPairIndexMap().get(tj) >= i)) {
            return null;
        }

        City currentCity = vehicle.getCurrentCity();
        ArrayList<CTaskModel> neighborTasks = new ArrayList<>();
        int load = 0;
        double vehicleCost = 0d;

        for (int k = 0; k < tasks.size(); k++) {
            CTaskModel newTask = tasks.get(k);

            if (k == i) {
                newTask = tasks.get(j);
                neighborSolution.getTaskPairIndexMap().put(new CTaskModel(newTask.getTask(), newTask.getPairTaskType()), i);
            } else if (k == j){
                newTask = tasks.get(i);
                neighborSolution.getTaskPairIndexMap().put(new CTaskModel(newTask.getTask(), newTask.getPairTaskType()), j);
            }
            load += newTask.getUpdatedLoad();
            neighborTasks.add(newTask);

            // return null if the plan is not valid because load is bigger than capacity
            if (load > vehicle.capacity()) {
                return null;
            }

            City nextCity = newTask.getType().equals(CTaskTypeEnum.PICKUP) ?
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
