package c_search;

import c_enums.CTaskTypeEnum;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;
import c_models.CSolutionModel;
import c_models.CTaskModel;
import c_operations.CChangeVehicleCOperation;
import c_operations.CSwapTasksCOperation;

import java.util.*;

public class CStochasticLocalSearch {

    // approximate upper bound for execution time of neighbor exploration in stochastic search
    private static long NEIGHBOUR_EXPLORATION_TIME = 1000;

    // used for iteration count of operation swap tasks
    private final double ALPHA;
    // used for iteration count of change task vehicle operation
    private final double BETA;
    private final double p;
    // any city maps to the closest vehicle (based on vehicle home town)
    private final Map<City, Vehicle> closestBigVehicle;
    // vehicles sorted by capacity
    private final List<Vehicle> sortedVehicles;
    // the name of the method that will be used for initial solution
    private final String initialSolutionName;
    private final List<Vehicle> vehicleList;
    private final TaskSet tasks;

    private CSolutionModel bestSolution;
    private long remainingTime;

    public CStochasticLocalSearch(List<Vehicle> vehicleList, TaskSet tasks,
                                  long remainingTime, double p, double alpha, double beta, String initialSolutionName,
                                  Map<City, Vehicle> closestBigVehicle, List<Vehicle> sortedVehicles) {
        this.vehicleList = vehicleList;
        this.tasks = tasks;
        this.remainingTime = remainingTime;
        this.p = p;
        this.ALPHA = alpha;
        this.BETA = beta;
        this.initialSolutionName = initialSolutionName;
        this.closestBigVehicle = closestBigVehicle;
        this.sortedVehicles = sortedVehicles;
    }

    public void SLS() {
        CSolutionModel currentSolution = createInitialSolution();
        bestSolution = currentSolution;

        int count = 0;
        while (remainingTime > NEIGHBOUR_EXPLORATION_TIME) {
            long loopStartTime = System.currentTimeMillis();

            CSolutionModel bestNeighbor = chooseNeighbors(currentSolution);
            if (bestNeighbor != null) {
                double randomDouble = new Random().nextDouble();
                if (randomDouble <= p) {
                    currentSolution = bestNeighbor;
                }

                bestSolution = bestNeighbor.getCost() < bestSolution.getCost() ? bestNeighbor : bestSolution;

                if (count % 1000 == 0) {
                    System.out.println(String.format("Iteration: %d | Best cost: %.2f | Current cost: %.2f"
                            , count, bestSolution.getCost(), currentSolution.getCost()));
                }
                count++;
            }

            remainingTime -= System.currentTimeMillis() - loopStartTime;
        }
    }

    /**
     * Invoking the proper method for initial solution.
     *
     * @return initial solution
     */
    private CSolutionModel createInitialSolution() {
        return initialSolutionName.equals("allTasksToBiggestVehicle") ?
                allTasksToBiggestVehicle() : fairBasedOnHomeCity();
    }

    private CSolutionModel chooseNeighbors(CSolutionModel currentSolution) {
        Map<Vehicle, ArrayList<CTaskModel>> map = currentSolution.getVehicleTasksMap();

        // the neighbors of the current solution that have the same cost as the best neighbor of the current solution
        List<CSolutionModel> currentBestNeighbors = new ArrayList<>();
        double bestNeighborCost = Double.MAX_VALUE;
        int alphaIterCount = (int) (ALPHA * vehicleList.size());
        int betaIterCount = (int) (BETA * 2 * tasks.size());

        CSolutionModel neighbor;
        for (int k = 0; k < alphaIterCount + betaIterCount; k++) {
            Vehicle v1 = vehicleList.get(new Random().nextInt(vehicleList.size()));

            // swap two tasks for a vehicle
            if (k < alphaIterCount) {
                if (map.get(v1).size() < 1) { // continue cause chosen vehicle doesn't have any task
                    continue;
                }

                int i = new Random().nextInt(map.get(v1).size());
                int j = new Random().nextInt(map.get(v1).size());
                neighbor = new CSwapTasksCOperation(currentSolution, i, j, v1).getNewSolution();
            }
            // give a random task of a random vehicle to other random vehicle (append to the end of its plan)
            else {
                Vehicle v2 = vehicleList.get(new Random().nextInt(vehicleList.size()));
                if (v1.equals(v2) || map.get(v1).size() < 1) {
                    continue;
                }

                int i = new Random().nextInt(map.get(v1).size());
                neighbor = new CChangeVehicleCOperation(currentSolution, v1, v2, i).getNewSolution();
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

    /**
     * For each pickup city, there is a vehicle that is close and big (given in closest big vehicle)
     * Try to fit as many tasks to these vehicles (first pick up all and then deliver all)
     * All other tasks assign randomly at the end of the plan (to random vehicle if it fits the task weight)
     *
     * @return
     */
    private CSolutionModel fairBasedOnHomeCity() {
        Map<Vehicle, ArrayList<CTaskModel>> map = new HashMap<>();
        Map<Vehicle, ArrayList<CTaskModel>> deliveryAppendix = new HashMap<>();
        Map<Vehicle, Double> vehicleLoad = new HashMap<>();
        Set<Task> notAssignedTasks = new HashSet<>();

        //maps init
        for (Vehicle vehicle : vehicleList) {
            vehicleLoad.put(vehicle, 0d);
            map.put(vehicle, new ArrayList<>());
            deliveryAppendix.put(vehicle, new ArrayList<>());
        }

        //try to assign a task to a vehicle dedicated to task pickup city
        for (Task task : tasks) {
            Vehicle suboptimalVehicle = closestBigVehicle.get(task.pickupCity);
            double toBeWeight = vehicleLoad.get(suboptimalVehicle) + task.weight;
            if (toBeWeight <= suboptimalVehicle.capacity()) { //if dedicated vehicle can pick up a task
                ArrayList<CTaskModel> currentTasks = map.get(suboptimalVehicle);
                //add task pickup in the plan
                CTaskModel CTaskModelPickup = new CTaskModel(task, CTaskTypeEnum.PICKUP);
                currentTasks.add(CTaskModelPickup);
                map.put(suboptimalVehicle, currentTasks);

                //add task delivery in the plan that's going to be appended later
                ArrayList<CTaskModel> toBeAddedTasks = deliveryAppendix.get(suboptimalVehicle);
                CTaskModel CTaskModelDelivery = new CTaskModel(task, CTaskTypeEnum.DELIVERY);
                toBeAddedTasks.add(CTaskModelDelivery);
                deliveryAppendix.put(suboptimalVehicle, toBeAddedTasks);

                vehicleLoad.put(suboptimalVehicle, toBeWeight);
            } else {
                //the task can not fit and it's going to be assigned randomly
                notAssignedTasks.add(task);
            }
        }

        //plan the delivery of all assigned tasks (appending after all pickups)
        for (Map.Entry<Vehicle, ArrayList<CTaskModel>> entry : map.entrySet()) {
            ArrayList<CTaskModel> currentTasks = entry.getValue();
            currentTasks.addAll(deliveryAppendix.get(entry.getKey()));
            map.put(entry.getKey(), currentTasks);
        }

        //randomly (to random vehicle) assign all not assigned tasks
        //there is always a solution since all vehicles should deliver all tasks before
        //taking new task from notAssignedTasks
        for (Task task : notAssignedTasks) {
            Vehicle vehicle;
            //find a random vehicle that can carry a task
            //there is a vehicle that can take the heaviest task
            do {
                vehicle = vehicleList.get(new Random().nextInt(vehicleList.size()));
            } while (task.weight > vehicle.capacity());

            //append the task to the plan
            ArrayList<CTaskModel> currentTasks = map.get(vehicle);
            CTaskModel CTaskModelPickup = new CTaskModel(task, CTaskTypeEnum.PICKUP);
            currentTasks.add(CTaskModelPickup);
            CTaskModel CTaskModelDelivery = new CTaskModel(task, CTaskTypeEnum.DELIVERY);
            currentTasks.add(CTaskModelDelivery);
            map.put(vehicle, currentTasks);
        }
        return new CSolutionModel(map);
    }

    /**
     * Assign all tasks to the biggest vehicle. For each task the biggest vehicle does the pickup and then the delivery
     * and then it process the next task.
     *
     * @return
     */
    private CSolutionModel allTasksToBiggestVehicle() {
        Map<Vehicle, ArrayList<CTaskModel>> map = new HashMap<>();
        for (Vehicle vehicle : vehicleList) {
            map.put(vehicle, new ArrayList<>());
        }

        // sortedVehicles contain sorted vehicles (by capacity and cost)
        Vehicle biggestVehicle = sortedVehicles.get(0);
        ArrayList<CTaskModel> currentTasks = new ArrayList<>();
        for (Task task : tasks) {
            CTaskModel CTaskModelPickup = new CTaskModel(task, CTaskTypeEnum.PICKUP);
            currentTasks.add(CTaskModelPickup);

            CTaskModel CTaskModelDelivery = new CTaskModel(task, CTaskTypeEnum.DELIVERY);
            currentTasks.add(CTaskModelDelivery);
        }
        map.put(biggestVehicle, currentTasks);
        return new CSolutionModel(map);
    }

    public CSolutionModel getBestSolution() {
        return bestSolution;
    }
}
