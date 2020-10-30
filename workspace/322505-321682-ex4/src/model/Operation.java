package model;

import enums.OperationTypeEnum;

public abstract class Operation {

    protected SolutionModel currentSolution;
    protected OperationTypeEnum operationType;

    public Operation(SolutionModel currentSolution, OperationTypeEnum operationType) {
        this.currentSolution = currentSolution;
        this.operationType = operationType;
    }

    public abstract SolutionModel getNewSolution();
}
