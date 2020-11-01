package operations;

import enums.OperationTypeEnum;
import models.SolutionModel;

public abstract class Operation {

    protected SolutionModel currentSolution;
    protected SolutionModel neighborSolution;
    protected OperationTypeEnum operationType;

    public Operation(SolutionModel currentSolution, OperationTypeEnum operationType) {
        this.currentSolution = currentSolution;
        this.operationType = operationType;
        this.neighborSolution = new SolutionModel(currentSolution);
    }

    public abstract SolutionModel getNewSolution();
}
