package search;

import logist.simulation.Vehicle;
import models.SolutionModel;
import models.TaskModel;
import operations.ChangeVehicleOperation;
import operations.SwapTasksOperation;

import java.util.*;

public class CentralizedSLS {

    // approximate upper bound for execution time of neighbor exploration in stochastic search
    private static long NEIGHBOUR_EXPLORATION_TIME = 1000;

    // the initial constant for simulated annealing
    // TODO: SET THIS PARAMETER -> NOW IT IS ALWAYS 0
    private double simulatedAnnealingParam;

    // any city maps to the closest vehicle (based on vehicle home town)
    private final List<Vehicle> vehicleList;

    private SolutionModel bestSolution;
    private long remainingTime;

    public CentralizedSLS(List<Vehicle> vehicleList, long remainingTime, SolutionModel initialSolution) {
        this.vehicleList = vehicleList;
        this.remainingTime = remainingTime;
        this.bestSolution = initialSolution;
    }

    public void SLS() {
        SolutionModel currentSolution = bestSolution;

        int count = 0;
        while (remainingTime > NEIGHBOUR_EXPLORATION_TIME) {
            long loopStartTime = System.currentTimeMillis();

            SolutionModel bestNeighbor = chooseNeighbors(currentSolution);
            if (bestNeighbor != null) {

                double randomDouble = new Random().nextDouble();
                if (randomDouble < Math.exp(-simulatedAnnealingParam *
                        (bestNeighbor.getCost() - currentSolution.getCost()))) {
                    currentSolution = bestNeighbor;
                }

                bestSolution = bestNeighbor.getCost() < bestSolution.getCost() ? bestNeighbor : bestSolution;

                if (count % 1000 == 0) {
                    System.out.println(String.format("Iteration: %d | Best cost: %.2f | Current cost: %.2f",
                            count, bestSolution.getCost(), currentSolution.getCost()));
                }
                count++;
            }

            if (count % 50000 == 0) {
                simulatedAnnealingParam *= 1.1;
            }

            remainingTime -= System.currentTimeMillis() - loopStartTime;
        }
    }

    /**
     * Creates neighbour solution from the current solution and returns the best one. The transitions (operations) done
     * on current solution to get the neighbor solutions can be switching order of the task in one vehicle or removing
     * one task from a vehicle and adding it as the last task of some other vehicle. For the current solution we pick
     * one random vehicle (v1) and apply operation that switches tasks for each task, plus for each vehicle (v2) we try
     * to give one randomly chosen task to the previously picked random vehicle (v1).
     *
     * @param currentSolution - current best solution
     * @return - best solution out of neighbour solutions
     */
    private SolutionModel chooseNeighbors(SolutionModel currentSolution) {
        Map<Vehicle, ArrayList<TaskModel>> map = currentSolution.getVehicleTasksMap();

        // the neighbors of the current solution that have the same cost as the best neighbor of the current solution
        List<SolutionModel> currentBestNeighbors = new ArrayList<>();
        double bestNeighborCost = Double.MAX_VALUE;

        SolutionModel neighbor;
        // pick random vehicle and try to swap it's every task
        Vehicle v1 = vehicleList.get(new Random().nextInt(vehicleList.size()));
        List<TaskModel> v1TaskModels = map.get(v1);
        for (int i = 0; i < v1TaskModels.size(); i++) {
            int j = new Random().nextInt(v1TaskModels.size());
            if (i == j) {
                continue;
            }
            neighbor = new SwapTasksOperation(currentSolution, i, j, v1).getNewSolution();

            if (neighbor != null) {
                if (currentBestNeighbors.isEmpty() || neighbor.getCost() < bestNeighborCost) {
                    bestNeighborCost = neighbor.getCost();
                    currentBestNeighbors = new ArrayList<>();
                    currentBestNeighbors.add(neighbor);
                } else if (neighbor.getCost() == bestNeighborCost) {
                    currentBestNeighbors.add(neighbor);
                }
            }
        }

        // for every vehicle pick a random task and assign it to some other vehicle
        for (Vehicle v2 : vehicleList) {
            if (v1.equals(v2) || map.get(v2).size() < 1) {
                continue;
            }

            int i = new Random().nextInt(map.get(v2).size());
            neighbor = new ChangeVehicleOperation(currentSolution, v2, v1, i).getNewSolution();

            if (neighbor != null) {
                if (currentBestNeighbors.isEmpty() || neighbor.getCost() < bestNeighborCost) {
                    bestNeighborCost = neighbor.getCost();
                    currentBestNeighbors = new ArrayList<>();
                    currentBestNeighbors.add(neighbor);
                } else if (neighbor.getCost() == bestNeighborCost) {
                    currentBestNeighbors.add(neighbor);
                }
            }
        }

        // choose random best neighbor
        if (!currentBestNeighbors.isEmpty())
            return currentBestNeighbors.get(new Random().nextInt(currentBestNeighbors.size()));
        return null;
    }

    public SolutionModel getBestSolution() {
        return bestSolution;
    }
}
