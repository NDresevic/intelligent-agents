package template;

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
        List<TaskModel> tasks = currentSolution.getVehicleTasksMap().get(vehicle);

        SolutionModel bestNeighbor = null;
        //swapping every two tasks
//        for (int i = 0; i < tasks.size(); i++) {
//            for (int j = i + 1; j < tasks.size(); j++) {
                SolutionModel neighbor = new SwapTasksOperation(currentSolution, 1, 2, vehicle).getNewSolution();
                if (neighbor == null)
                    //continue;
                    System.out.println("neighbor je null");

                System.out.println("Komsija cost: " + neighbor.getCost());
                if (bestNeighbor == null || neighbor.getCost() < bestNeighbor.getCost())
                    bestNeighbor = neighbor;
//            }
//        }

        if (bestNeighbor != null && bestNeighbor.getCost() < bestSolution.getCost())
            bestSolution = bestNeighbor;

        return bestNeighbor;
    }

    private SolutionModel createInitialSolution() {
        Map<Vehicle, ArrayList<TaskModel>> map = new HashMap<>();

        for (int i = 0; i < taskModelList.size(); i += 2) {
            Vehicle vehicle = vehicleList.get((i / 2) % vehicleList.size());
            if (!map.containsKey(vehicle)) {
                map.put(vehicle, new ArrayList<>());
            }

            ArrayList<TaskModel> currentTasks = map.get(vehicle);
            currentTasks.add(taskModelList.get(i));
            currentTasks.add(taskModelList.get(i + 1));
            map.put(vehicle, currentTasks);
        }

        return new SolutionModel(map);
    }

    public SolutionModel getBestSolution() {
        return bestSolution;
    }
}
