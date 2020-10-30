package model;

import enums.OperationTypeEnum;
import enums.TaskTypeEnum;
import logist.simulation.Vehicle;

import java.util.List;

public class SwapTasksOperation extends Operation {

    int i; //first task index in vehicle's list to be swapped
    int j; //second task index in vehicle's list to be swapped
    Vehicle vehicle;

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
        List<TaskModel> tasks = neighborSolution.getVehicleTasksMap().get(vehicle);
        List<Double> loads = neighborSolution.getVehicleLoad().get(vehicle);

        TaskModel ti = tasks.get(i);
        TaskModel tj = tasks.get(j);

        System.out.println(ti);
        System.out.println(tj);

        System.out.println(neighborSolution.getPairIndex().get(ti));
        System.out.println(currentSolution.getPairIndex().get(ti));

        if (ti.getType() == TaskTypeEnum.PICKUP && neighborSolution.getPairIndex().get(ti) <= j
                ||
                tj.getType() == TaskTypeEnum.DELIVERY && neighborSolution.getPairIndex().get(tj) >= i)
            return null;


        loads.add(j, loads.get(j) + tasks.get(i).updateLoad());
        if (loads.get(j) > vehicle.capacity())
            return null;
        //FIXME k treba da ide do kraja svih taskova
        for (int k = i + 1; k < j; k++) {
            loads.add(k, loads.get(k) + tasks.get(k).updateLoad());
            if (loads.get(k) > vehicle.capacity())
                return null;
        }
        loads.add(j, loads.get(j) + tasks.get(i).updateLoad());
        if (loads.get(j) > vehicle.capacity())
            return null;
        neighborSolution.getVehicleLoad().put(vehicle, loads);

        tasks.add(i, tj);
        tasks.add(j, ti);
        neighborSolution.getVehicleTasksMap().put(vehicle, tasks);
        neighborSolution.getPairIndex().put(ti, j);
        neighborSolution.getPairIndex().put(tj, i);
        return neighborSolution;
    }
}
