package template;

import enums.OperationTypeEnum;
import enums.TaskTypeEnum;
import logist.simulation.Vehicle;
import logist.topology.Topology;
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

    //        procedure SLS(X,D,C,f )
//    A ← SelectInitialSolution(X, D, C, f)
//    repeat
//        Aold ← A
//        N ← ChooseNeighbours(Aold, X, D, C, f)
//        A ← LocalChoice(N, f)
//    until termination condition met
//     return A
//    end procedure
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

        List<TaskModel> tasks = currentSolution.getVehicleTasksMap().get(vehicle);

        SolutionModel bestNeighbor = null;

        //swapping every two tasks
        for (int i = 0; i < tasks.size(); i++) {
            for (int j = i + 1; j < tasks.size(); j++) {
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
        Map<Vehicle, List<TaskModel>> map = new HashMap<>();

        for (int i = 0; i < noTaskModel; i += 2) {
            Vehicle vehicle = vehicleList.get((i / 2) % vehicleList.size());
            if (!map.containsKey(vehicle)) {
                map.put(vehicle, new ArrayList<>());
            }

            List<TaskModel> currentTasks = map.get(vehicle);
            currentTasks.add(taskModelList.get(i));
            currentTasks.add(taskModelList.get(i + 1));
            map.put(vehicle, currentTasks);
        }

        double cost = this.calculateInitialSolutionCost(map);
        return new SolutionModel(map, cost);
    }

    private double calculateInitialSolutionCost(Map<Vehicle, List<TaskModel>> vehicleTasksMap) {
        double cost = 0.0;

        for (Map.Entry<Vehicle, List<TaskModel>> entry : vehicleTasksMap.entrySet()) {
            Vehicle vehicle = entry.getKey();
            Topology.City currentCity = vehicle.getCurrentCity();
            List<TaskModel> tasks = entry.getValue();

            for (TaskModel task : tasks) {
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
