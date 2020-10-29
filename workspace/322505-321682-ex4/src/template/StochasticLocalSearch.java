package template;

import logist.simulation.Vehicle;
import model.SolutionModel;
import model.TaskModel;

import java.util.List;

public class StochasticLocalSearch {

    private List<Vehicle> vehicleList;
    private List<TaskModel> taskModelList;
    private SolutionModel bestSolution;

    public StochasticLocalSearch(List<Vehicle> vehicleList, List<TaskModel> taskModelList) {
        this.vehicleList = vehicleList;
        this.taskModelList = taskModelList;
    }

    public void SLS() {
        SolutionModel currentSolution;

    }

//    procedure SLS(X,D,C,f )
//    A ← SelectInitialSolution(X, D, C, f)
//    repeat
//        Aold ← A
//        N ← ChooseNeighbours(Aold, X, D, C, f)
//        A ← LocalChoice(N, f)
//    until termination condition met
//     return A
//    end procedure

    public SolutionModel getBestSolution() {
        return bestSolution;
    }
}
