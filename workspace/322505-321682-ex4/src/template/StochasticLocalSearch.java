package template;

import logist.simulation.Vehicle;
import model.SolutionModel;
import model.SwapTasksOperation;
import model.TaskModel;

import java.util.*;

public class StochasticLocalSearch {

    // used for iteration count of operation swap tasks
    private static double ALPHA = 1;
    private static double BETA = 0.4;

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

        int count = 0;
        while (remainingTime > 0) {
            long loopStartTime = System.currentTimeMillis();

            SolutionModel bestNeighbor = exploreNeighbors(currentSolution);
            if (bestNeighbor != null) {
                if (new Random().nextDouble() > p) {
                    currentSolution = bestNeighbor;
                }

                bestSolution = bestNeighbor.getCost() < bestSolution.getCost() ? bestNeighbor : bestSolution;
                if (count % 1000 == 0) {
                    System.out.println(String.format("Iteration: %d | Best cost: %.2f", count, bestSolution.getCost()));
                }
                count++;
            }

            remainingTime -= System.currentTimeMillis() - loopStartTime;
        }
    }

    private SolutionModel exploreNeighbors(SolutionModel currentSolution) {
        SolutionModel bestNeighbor = null;

        int iterCount = (int) ALPHA * vehicleList.size();
        for (int k = 0; k < iterCount; k++) {
            Vehicle vehicle = vehicleList.get(new Random().nextInt(vehicleList.size()));
            Map<Vehicle, ArrayList<TaskModel>> map = currentSolution.getVehicleTasksMap();

            int i = new Random().nextInt(map.get(vehicle).size());
            int j = new Random().nextInt(map.get(vehicle).size());
            SolutionModel neighbor = new SwapTasksOperation(currentSolution, i, j, vehicle).getNewSolution();
            if (neighbor != null &&
                    (bestNeighbor == null || neighbor.getCost() < bestNeighbor.getCost())) {
                bestNeighbor = neighbor;
            }
        }

        return bestNeighbor;
    }

    private SolutionModel createInitialSolution() {
        Map<Vehicle, ArrayList<TaskModel>> map = new HashMap<>();

        // todo: provera da li je moguca raspodela - ako ne - > dodeljivanje taska bilo kom vozilu !!!
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
