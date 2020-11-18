package search;

import logist.simulation.Vehicle;
import logist.task.TaskSet;
import models.SolutionModel;
import models.TaskModel;
import operations.ChangeVehicleOperation;
import operations.SwapTasksOperation;

import java.util.*;

public class CentralizedSLS {

    // approximate upper bound for execution time of neighbor exploration in stochastic search
    private static long NEIGHBOUR_EXPLORATION_TIME = 1000;

    // used for iteration count of operation swap tasks
    private final double ALPHA;
    // used for iteration count of change task vehicle operation
    private final double BETA;
    // the initial constant for simulated annealing
    private double simulatedAnnealingParam;

    private final double p;
    // any city maps to the closest vehicle (based on vehicle home town)
    private final List<Vehicle> vehicleList;
    private final TaskSet tasks;

    private SolutionModel bestSolution;
    private long remainingTime;

    public CentralizedSLS(List<Vehicle> vehicleList, TaskSet tasks,
                          long remainingTime, double p, double alpha, double beta, SolutionModel initialSolution) {
        this.vehicleList = vehicleList;
        this.tasks = tasks;
        this.remainingTime = remainingTime;
        this.p = p;
        this.ALPHA = alpha;
        this.BETA = beta;
        this.bestSolution = initialSolution;
    }

    public void SLS() {
        SolutionModel currentSolution = bestSolution;

        int count = 0;
        double acceptanceProbability;
        while (remainingTime > NEIGHBOUR_EXPLORATION_TIME) {
            long loopStartTime = System.currentTimeMillis();

            SolutionModel bestNeighbor = chooseNeighbors(currentSolution);
            if (bestNeighbor != null) {

                double randomDouble = new Random().nextDouble();
                if (randomDouble <= p) {
                    currentSolution = bestNeighbor;
                }
//comment this out for simmulated annealing
//                double randomDouble = new Random().nextDouble();
//                if (randomDouble < Math.exp(-simulatedAnnealingParam * (bestNeighbor.getCost() - currentSolution.getCost()))) {
//                    currentSolution = bestNeighbor;
//                }

                bestSolution = bestNeighbor.getCost() < bestSolution.getCost() ? bestNeighbor : bestSolution;

                if (count % 1000 == 0) {
                    System.out.println(String.format("Iteration: %d | Best cost: %.2f | Current cost: %.2f"
                            , count, bestSolution.getCost(), currentSolution.getCost()));
                }
                count++;
            }

            if (count % 50000 == 0){
                simulatedAnnealingParam *= 1.1;
            }

            remainingTime -= System.currentTimeMillis() - loopStartTime;
        }
    }

    /**
     * version 2
     *
     * @param currentSolution
     * @return
     */
    // todo: refactor this and delete first version if we decide this way is better - compare performance of both v!!!
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

    /**
     * version 1
     * Creating neighbours by iterating using the ALPHA and BETA parameters read from config.
     *
     * @param currentSolution
     * @return
     */
    private SolutionModel chooseNeighborsWithAlphaBeta(SolutionModel currentSolution) {
        Map<Vehicle, ArrayList<TaskModel>> map = currentSolution.getVehicleTasksMap();

        // the neighbors of the current solution that have the same cost as the best neighbor of the current solution
        List<SolutionModel> currentBestNeighbors = new ArrayList<>();
        double bestNeighborCost = Double.MAX_VALUE;
        int alphaIterCount = (int) (ALPHA * vehicleList.size());
        int betaIterCount = (int) (BETA * 2 * tasks.size());

        SolutionModel neighbor;
        for (int k = 0; k < alphaIterCount + betaIterCount; k++) {
            Vehicle v1 = vehicleList.get(new Random().nextInt(vehicleList.size()));

            // swap two tasks for a vehicle
            if (k < alphaIterCount) {
                if (map.get(v1).size() < 1) { // continue cause chosen vehicle doesn't have any task
                    continue;
                }

                int i = new Random().nextInt(map.get(v1).size());
                int j = new Random().nextInt(map.get(v1).size());
                neighbor = new SwapTasksOperation(currentSolution, i, j, v1).getNewSolution();
            }
            // give a random task of a random vehicle to other random vehicle (append to the end of its plan)
            else {
                Vehicle v2 = vehicleList.get(new Random().nextInt(vehicleList.size()));
                if (v1.equals(v2) || map.get(v1).size() < 1) {
                    continue;
                }

                int i = new Random().nextInt(map.get(v1).size());
                neighbor = new ChangeVehicleOperation(currentSolution, v1, v2, i).getNewSolution();
            }

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
