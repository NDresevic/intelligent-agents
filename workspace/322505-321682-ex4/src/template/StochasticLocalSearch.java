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

            SolutionModel bestNeighbor = exploreAllNeighborsForRandomVehicle(currentSolution);

            System.out.println("best neighbor cost: " + bestNeighbor.getCost());

            if (bestNeighbor != null && new Random().nextDouble() > p) {
                currentSolution = bestNeighbor;
            }
            if (bestNeighbor != null  && bestNeighbor.getCost() < bestSolution.getCost()) {
                bestSolution = bestNeighbor;
            }

            remainingTime -= System.currentTimeMillis() - loopStartTime;
        }
    }

    private SolutionModel exploreAllNeighborsForRandomVehicle(SolutionModel currentSolution) {
        Vehicle vehicle = vehicleList.get(new Random().nextInt(vehicleList.size()));
        Map<Vehicle, ArrayList<TaskModel>> map = currentSolution.getVehicleTasksMap();
        int i = new Random().nextInt(map.get(vehicle).size());
        int j = new Random().nextInt(map.get(vehicle).size());
        SolutionModel bestNeighbor = null;

        System.out.println("for v: " + vehicle.id() + " switching tasks " + i + " and " + j);
        SolutionModel neighbor = new SwapTasksOperation(currentSolution, i, j, vehicle).getNewSolution();
        if (neighbor != null) {
            System.out.println("Komsija cost: " + neighbor.getCost());
//            if (bestNeighbor == null || neighbor.getCost() < bestCost) {
                bestNeighbor = neighbor;
//            }
        } else {
            System.out.println("neighbor je null");
        }
        System.out.println("best nb cost: " + bestNeighbor.getCost());

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
