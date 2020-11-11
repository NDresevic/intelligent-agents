package c_operations;

import c_enums.COperationTypeEnum;
import c_models.CSolutionModel;

public abstract class COperation {

    protected CSolutionModel currentSolution;
    protected CSolutionModel neighborSolution;
    protected COperationTypeEnum operationType;

    public COperation(CSolutionModel currentSolution, COperationTypeEnum operationType) {
        this.currentSolution = currentSolution;
        this.operationType = operationType;
        this.neighborSolution = new CSolutionModel(currentSolution);
    }

    public abstract CSolutionModel getNewSolution();
}
