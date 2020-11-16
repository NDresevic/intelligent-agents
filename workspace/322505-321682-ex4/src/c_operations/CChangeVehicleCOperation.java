package c_operations;

import c_enums.COperationTypeEnum;
import c_enums.CTaskTypeEnum;
import logist.simulation.Vehicle;
import logist.topology.Topology.City;
import c_models.CSolutionModel;
import c_models.CTaskModel;

import java.util.ArrayList;

public class CChangeVehicleCOperation extends COperation {

    private Vehicle v1;
    private Vehicle v2;
    private int i;
    private int j;

    public CChangeVehicleCOperation(CSolutionModel currentSolution, Vehicle v1, Vehicle v2, int i) {
        super(currentSolution, COperationTypeEnum.CHANGE_VEHICLE);
        this.v1 = v1;
        this.v2 = v2;
        int j = currentSolution.getTaskPairIndexMap().get(currentSolution.getVehicleTasksMap().get(v1).get(i));
        this.i = Math.min(i, j);
        this.j = Math.max(i, j);
    }

    @Override
    public CSolutionModel getNewSolution() {
        ArrayList<CTaskModel> tasksV1 = neighborSolution.getVehicleTasksMap().get(v1);
        ArrayList<CTaskModel> tasksV2 = neighborSolution.getVehicleTasksMap().get(v2);

        CTaskModel ti = tasksV1.get(i);
        CTaskModel tj = tasksV1.get(j);

        // return null if the weight of last task exceeds capacity of v2
        if (ti.getTask().weight > v2.capacity()) {
            return null;
        }

        double previousV1Cost = neighborSolution.getVehicleCostMap().get(v1);
        double previousV2Cost = neighborSolution.getVehicleCostMap().get(v2);

        // remove ti and tj from the first vehicle and update tasks and parameters
        City currentCity = v1.getCurrentCity();
        ArrayList<CTaskModel> newV1Tasks = new ArrayList<>();
        double v1Cost = 0d;
        for (CTaskModel task : tasksV1) {
            if (task.equals(ti) || task.equals(tj)) {
                continue;
            }

            newV1Tasks.add(task);
            neighborSolution.getTaskPairIndexMap().put(new CTaskModel(task.getTask(), task.getPairTaskType()),
                    newV1Tasks.indexOf(task));

            City nextCity = task.getType().equals(CTaskTypeEnum.PICKUP) ? task.getTask().pickupCity :
                    task.getTask().deliveryCity;
            v1Cost += currentCity.distanceTo(nextCity) * v1.costPerKm();
            currentCity = nextCity;
        }
        // update tasks and cost for first vehicle
        neighborSolution.getVehicleTasksMap().put(v1, newV1Tasks);
        neighborSolution.getVehicleCostMap().put(v1, v1Cost);

        // add ti and tj to the end of task list of second vehicle
        ArrayList<CTaskModel> newV2Tasks = new ArrayList<>(tasksV2);
        newV2Tasks.add(ti);
        newV2Tasks.add(tj);
        neighborSolution.getTaskPairIndexMap().put(ti, newV2Tasks.indexOf(tj));
        neighborSolution.getTaskPairIndexMap().put(tj, newV2Tasks.indexOf(ti));
        neighborSolution.getVehicleTasksMap().put(v2, newV2Tasks);

        // distance to pick up new task and deliver it
        double additionalDistance = ti.getTask().pickupCity.distanceTo(tj.getTask().deliveryCity);
        if (newV2Tasks.size() > 2) {
            additionalDistance += newV2Tasks.get(newV2Tasks.size() - 3).getTask().deliveryCity.
                    distanceTo(ti.getTask().pickupCity);
        } else {
            additionalDistance += v2.getCurrentCity().distanceTo(ti.getTask().pickupCity);
        }
        double v2Cost = previousV2Cost + additionalDistance * v2.costPerKm();
        neighborSolution.getVehicleCostMap().put(v2, v2Cost);

        // update total cost
        neighborSolution.setCost(neighborSolution.getCost() - previousV1Cost - previousV2Cost + v1Cost + v2Cost);

        return neighborSolution;
    }
}