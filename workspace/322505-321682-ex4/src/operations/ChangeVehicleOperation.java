package operations;

import enums.OperationTypeEnum;
import logist.simulation.Vehicle;
import logist.task.Task;
import models.SolutionModel;

public class ChangeVehicleOperation extends Operation {

    Vehicle v1;
    Vehicle v2;
    Task task;

    public ChangeVehicleOperation(SolutionModel currentSolution, OperationTypeEnum operationType,
                                  Vehicle v1, Vehicle v2, Task task) {
        super(currentSolution, operationType);
        this.v1 = v1;
        this.v2 = v2;
        this.task = task;
    }

    @Override
    public SolutionModel getNewSolution() {
        SolutionModel neighborSolution = new SolutionModel(currentSolution);
        //FIXME

        return null;
    }
}
