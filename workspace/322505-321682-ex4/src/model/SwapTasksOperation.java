package model;

import enums.OperationTypeEnum;
import enums.TaskTypeEnum;
import logist.simulation.Vehicle;

import java.util.List;
import java.util.Map;

public class SwapTasksOperation extends Operation {

    private int i; //first task index in vehicle's list to be swapped
    private int j; //second task index in vehicle's list to be swapped
    private Vehicle vehicle;

    public SwapTasksOperation(SolutionModel currentSolution, OperationTypeEnum operationType,
                              int i, int j, Vehicle vehicle) {
        super(currentSolution, operationType);
        this.i = i;
        this.j = j;
        this.vehicle = vehicle;
    }

    @Override
    public SolutionModel getNewSolution() {
        SolutionModel neighborSolution = new SolutionModel(currentSolution);
        TaskModel[] tasks = neighborSolution.getVehicleTasksMap().get(vehicle);
        List<Double> loads = neighborSolution.getVehicleLoad().get(vehicle);

        TaskModel ti = tasks[i];
        TaskModel tj = tasks[j];

        if (ti.getType() == TaskTypeEnum.PICKUP && neighborSolution.getTaskPairIndex().get(ti) <= j
                ||
                tj.getType() == TaskTypeEnum.DELIVERY && neighborSolution.getTaskPairIndex().get(tj) >= i)
            return null;

        loads.add(j, loads.get(j) + tasks[i].updateLoad());
        if (loads.get(j) > vehicle.capacity())
            return null;
        for (int k = i + 1; k < j; k++) {
            loads.add(k, loads.get(k) + tasks[k].updateLoad());
            if (loads.get(k) > vehicle.capacity())
                return null;
        }
        loads.add(j, loads.get(j) + tasks[i].updateLoad());
        if (loads.get(j) > vehicle.capacity())
            return null;
        for (int k = j + 1; k < tasks.length; k++) {
            loads.add(k, loads.get(k) + tasks[k].updateLoad());
            if (loads.get(k) > vehicle.capacity())
                return null;
        }
        neighborSolution.getVehicleLoad().put(vehicle, loads);

        tasks[i] = tj;
        tasks[j] = ti;
        neighborSolution.getVehicleTasksMap().put(vehicle, tasks);
        neighborSolution.getTaskPairIndex().put(ti, j);
        neighborSolution.getTaskPairIndex().put(tj, i);
        return neighborSolution;
    }
}
