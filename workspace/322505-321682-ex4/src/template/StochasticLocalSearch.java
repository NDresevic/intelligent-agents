package template;

import enums.TaskTypeEnum;
import logist.simulation.Vehicle;
import logist.topology.Topology;
import model.SolutionModel;
import model.TaskModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StochasticLocalSearch {

    private List<Vehicle> vehicleList;
    private List<TaskModel> taskModelList;
    private SolutionModel bestSolution;
    private long remainingTime;

    public StochasticLocalSearch(List<Vehicle> vehicleList, List<TaskModel> taskModelList, long remainingTime) {
        this.vehicleList = vehicleList;
        this.taskModelList = taskModelList;
        this.remainingTime = remainingTime;
    }

    public void SLS() {
        SolutionModel currentSolution = createInitialSolution();
        bestSolution = currentSolution;

        while (remainingTime > 0) {
            long loopStartTime = System.currentTimeMillis();
            List<SolutionModel> neighbors = new ArrayList<>();



            if (currentSolution.getCost() < bestSolution.getCost()) {
                bestSolution = currentSolution;
            }

            remainingTime -= System.currentTimeMillis() - loopStartTime;
        }
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

    private SolutionModel createInitialSolution() {

        return null;
    }

    private double calculateInitialSolutionCost(SolutionModel solutionModel) {
        double cost = 0.0;

        Map<Vehicle, List<TaskModel>> vehicleTasksMap = solutionModel.getVehicleTasksMap();
        for (Map.Entry<Vehicle, List<TaskModel>> entry: vehicleTasksMap.entrySet()) {
            Vehicle vehicle = entry.getKey();
            Topology.City currentCity = vehicle.getCurrentCity();
            List<TaskModel> tasks = entry.getValue();

            for (TaskModel task: tasks) {
                Topology.City nextCity = task.getType().equals(TaskTypeEnum.PICKUP) ?
                        task.getTask().pickupCity : task.getTask().deliveryCity;
                cost += currentCity.distanceTo(nextCity) * vehicle.costPerKm();
            }
        }

        return cost;
    }

    public SolutionModel getBestSolution() {
        return bestSolution;
    }
}
