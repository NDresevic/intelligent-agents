package template;

import enums.OperationTypeEnum;
import logist.simulation.Vehicle;
import model.SolutionModel;
import model.SwapTasksOperation;
import model.TaskModel;

import java.util.*;

public class StochasticLocalSearch {

    private List<Vehicle> vehicleList;
    private List<TaskModel> taskModelList;
    private SolutionModel bestSolution;
    private long remainingTime;
    private double p;

    public StochasticLocalSearch(List<Vehicle> vehicleList, List<TaskModel> taskModelList,
                                 long remainingTime, double p) {
        this.vehicleList = vehicleList;
        this.taskModelList = taskModelList;
        this.remainingTime = remainingTime;
        this.p = p;
    }

    public void SLS() {
        SolutionModel currentSolution = createInitialSolution();
        bestSolution = currentSolution;

        while (remainingTime > 0) {
            long loopStartTime = System.currentTimeMillis();

            SolutionModel bestNeighbor = exploreAllNeighborsForRandomVehicle(currentSolution, bestSolution.getCost());

            System.out.println("best neighbor cost: " + bestNeighbor.getCost());

            if (new Random().nextDouble() > p)
                currentSolution = bestNeighbor;

            remainingTime -= System.currentTimeMillis() - loopStartTime;
        }
    }

    private SolutionModel exploreAllNeighborsForRandomVehicle(SolutionModel currentSolution, Double bestCost) {
        Vehicle vehicle = vehicleList.get(new Random().nextInt(vehicleList.size()));

        TaskModel[] tasks = currentSolution.getVehicleTasksMap().get(vehicle);

        SolutionModel bestNeighbor = null;

        //swapping every two tasks
        for (int i = 0; i < tasks.length; i++) {
            for (int j = i + 1; j < tasks.length; j++) {
                SolutionModel neighbor = new SwapTasksOperation(currentSolution,
                        OperationTypeEnum.CHANGE_TASK_ORDER, i, j, vehicle).getNewSolution();
                if (neighbor == null)
                    continue;

                System.out.println("Komsija cost: " + neighbor.getCost());
                if (bestNeighbor == null || neighbor.getCost() < bestNeighbor.getCost())
                    bestNeighbor = neighbor;
            }
        }

        if(bestNeighbor.getCost() < bestSolution.getCost())
            bestSolution = bestNeighbor;

        return bestNeighbor;
    }

    private SolutionModel createInitialSolution() {
        int noTaskModel = taskModelList.size();
        Map<Vehicle, TaskModel[]> map = new HashMap<>();

        for (int i = 0; i < noTaskModel; i += 2) {
            Vehicle vehicle = vehicleList.get((i / 2) % vehicleList.size());
            if (!map.containsKey(vehicle)) {
                map.put(vehicle, new TaskModel[taskModelList.size()]);
            }

            TaskModel[] currentTasks = map.get(vehicle);
            currentTasks[i] = taskModelList.get(i);
            currentTasks[i + 1] = taskModelList.get(i + 1);
            map.put(vehicle, currentTasks);
        }

        return new SolutionModel(map);
    }

    public SolutionModel getBestSolution() {
        return bestSolution;
    }

}
