package search;

import enums.TaskTypeEnum;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;
import models.SolutionModel;
import models.TaskModel;
import operations.ChangeVehicleOperation;
import operations.SwapTasksOperation;

import java.util.*;

public class StochasticLocalSearch {

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

    private SolutionModel bestSolution;
    private long remainingTime;

    public StochasticLocalSearch(List<Vehicle> vehicleList, TaskSet tasks,
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
        SolutionModel currentSolution = createInitialSolution();
        bestSolution = currentSolution;

        int count = 0;
        while (remainingTime > NEIGHBOUR_EXPLORATION_TIME) {
            long loopStartTime = System.currentTimeMillis();

            SolutionModel bestNeighbor = chooseNeighbors(currentSolution);
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
    private SolutionModel createInitialSolution() {
        switch (initialSolutionName) {
            case "fairBasedOnHomeCity":
                return fairBasedOnHomeCity();
            case "allTasksToBiggestVehicle":
                return allTasksToBiggestVehicle();
            case "giveToBiggestVehiclesFirst":
                return giveToBiggestVehiclesFirst();
        }
        return allTasksToBiggestVehicle();
    }

    private SolutionModel chooseNeighbors(SolutionModel currentSolution) {
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

    /**
     * For each pickup city, there is a vehicle that is close and big (given in closest big vehicle)
     * Try to fit as many tasks to these vehicles (first pick up all and then deliver all)
     * All other tasks assign randomly at the end of the plan (to random vehicle if it fits the task weight)
     *
     * @return
     */
    private SolutionModel fairBasedOnHomeCity() {
        Map<Vehicle, ArrayList<TaskModel>> map = new HashMap<>();
        Map<Vehicle, ArrayList<TaskModel>> deliveryAppendix = new HashMap<>();
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
                ArrayList<TaskModel> currentTasks = map.get(suboptimalVehicle);
                //add task pickup in the plan
                TaskModel taskModelPickup = new TaskModel(task, TaskTypeEnum.PICKUP);
                currentTasks.add(taskModelPickup);
                map.put(suboptimalVehicle, currentTasks);

                //add task delivery in the plan that's going to be appended later
                ArrayList<TaskModel> toBeAddedTasks = deliveryAppendix.get(suboptimalVehicle);
                TaskModel taskModelDelivery = new TaskModel(task, TaskTypeEnum.DELIVERY);
                toBeAddedTasks.add(taskModelDelivery);
                deliveryAppendix.put(suboptimalVehicle, toBeAddedTasks);

                vehicleLoad.put(suboptimalVehicle, toBeWeight);
            } else {
                //the task can not fit and it's going to be assigned randomly
                notAssignedTasks.add(task);
            }
        }

        //plan the delivery of all assigned tasks (appending after all pickups)
        for (Map.Entry<Vehicle, ArrayList<TaskModel>> entry : map.entrySet()) {
            ArrayList<TaskModel> currentTasks = entry.getValue();
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
            ArrayList<TaskModel> currentTasks = map.get(vehicle);
            TaskModel taskModelPickup = new TaskModel(task, TaskTypeEnum.PICKUP);
            currentTasks.add(taskModelPickup);
            TaskModel taskModelDelivery = new TaskModel(task, TaskTypeEnum.DELIVERY);
            currentTasks.add(taskModelDelivery);
            map.put(vehicle, currentTasks);
        }
        return new SolutionModel(map);
    }

    /**
     * Assign all tasks to the biggest vehicle. For each task the biggest vehicle does the pickup and then the delivery
     * and then it process the next task.
     *
     * @return
     */
    private SolutionModel allTasksToBiggestVehicle() {
        Map<Vehicle, ArrayList<TaskModel>> map = new HashMap<>();
        for (Vehicle vehicle : vehicleList) {
            map.put(vehicle, new ArrayList<>());
        }

        // sortedVehicles contain sorted vehicles (by capacity and cost)
        Vehicle biggestVehicle = sortedVehicles.get(0);
        ArrayList<TaskModel> currentTasks = new ArrayList<>();
        for (Task task : tasks) {
            TaskModel taskModelPickup = new TaskModel(task, TaskTypeEnum.PICKUP);
            currentTasks.add(taskModelPickup);

            TaskModel taskModelDelivery = new TaskModel(task, TaskTypeEnum.DELIVERY);
            currentTasks.add(taskModelDelivery);
        }
        map.put(biggestVehicle, currentTasks);
        return new SolutionModel(map);
    }

    private SolutionModel giveToBiggestVehiclesFirst() {
        Map<Vehicle, ArrayList<TaskModel>> map = new HashMap<>();
        Map<Vehicle, ArrayList<TaskModel>> deliveryAppendix = new HashMap<>();
        Map<Vehicle, Double> vehicleLoad = new HashMap<>();
        Set<Task> notAssignedTasks = new HashSet<>(tasks);
        Set<Task> assignedTasks = new HashSet<>();

        // maps init
        for (Vehicle vehicle : vehicleList) {
            vehicleLoad.put(vehicle, 0d);
            map.put(vehicle, new ArrayList<>());
            deliveryAppendix.put(vehicle, new ArrayList<>());
        }

        for (Vehicle vehicle : sortedVehicles) {
            for (Task task : notAssignedTasks) {
                double toBeWeight = vehicleLoad.get(vehicle) + task.weight;
                if (assignedTasks.contains(task) || toBeWeight > vehicle.capacity()) {
                    continue;
                }

                ArrayList<TaskModel> currentTasks = map.get(vehicle);

                TaskModel taskModelPickup = new TaskModel(task, TaskTypeEnum.PICKUP);
                currentTasks.add(taskModelPickup);
                map.put(vehicle, currentTasks);

                ArrayList<TaskModel> toBeAddedTasks = deliveryAppendix.get(vehicle);
                TaskModel taskModelDelivery = new TaskModel(task, TaskTypeEnum.DELIVERY);
                toBeAddedTasks.add(taskModelDelivery);

                deliveryAppendix.put(vehicle, toBeAddedTasks);
                vehicleLoad.put(vehicle, toBeWeight);
                assignedTasks.add(task);
            }
        }

        notAssignedTasks.removeAll(assignedTasks);

        for (Map.Entry<Vehicle, ArrayList<TaskModel>> entry : map.entrySet()) {
            ArrayList<TaskModel> currentTasks = entry.getValue();
            currentTasks.addAll(deliveryAppendix.get(entry.getKey()));
            map.put(entry.getKey(), currentTasks);
        }

        for (Task task : notAssignedTasks) {
            Vehicle vehicle;
            //find a random vehicle that can carry a task
            //there is a vehicle that can take the heaviest task
            do {
                vehicle = vehicleList.get(new Random().nextInt(vehicleList.size()));
            } while (task.weight > vehicle.capacity());
            ArrayList<TaskModel> currentTasks = map.get(vehicle);

            TaskModel taskModelPickup = new TaskModel(task, TaskTypeEnum.PICKUP);
            currentTasks.add(taskModelPickup);

            TaskModel taskModelDelivery = new TaskModel(task, TaskTypeEnum.DELIVERY);
            currentTasks.add(taskModelDelivery);

            map.put(vehicle, currentTasks);
        }
        return new SolutionModel(map);
    }

    public SolutionModel getBestSolution() {
        return bestSolution;
    }
}
