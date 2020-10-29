package model;

import enums.OperationTypeEnum;

public class OperationModel {

    private SolutionModel currentSolution;
    private OperationTypeEnum operationType;

    public OperationModel(SolutionModel currentSolution, OperationTypeEnum operationType) {
        this.currentSolution = currentSolution;
        this.operationType = operationType;
    }

    public SolutionModel getNewSolution() {
        SolutionModel newSolution =  null; //new SolutionModel();

        switch (operationType) {
            case CHANGE_TASK_ORDER:
                return this.changeTasksOrder();
            case GIVE_TASK:
                break;
        }

        return newSolution;
    }

    private SolutionModel changeTasksOrder() {
        // create new solution and return
        return null;
    }
}
