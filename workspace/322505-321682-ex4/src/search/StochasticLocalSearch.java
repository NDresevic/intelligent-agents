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

    // used for iteration count of operation swap tasks
    private static double ALPHA = 4;
    // used for iteration count of change task vehicle operation
    private static double BETA = 0.4;

    private List<Vehicle> vehicleList;
    private List<TaskModel> taskModelList;
    private TaskSet tasks;
    private SolutionModel bestSolution;
    private long remainingTime;
    private double p;
    private Map<City, Vehicle> closestBigVehicle;

    public StochasticLocalSearch(List<Vehicle> vehicleList, TaskSet tasks,
                                 long remainingTime, double p, Map<City, Vehicle> closestBigVehicle) {
        this.vehicleList = vehicleList;
        //makeTaskModelList(tasks);
        this.taskModelList = new ArrayList<>();
        this.tasks = tasks;
        this.remainingTime = remainingTime;
        this.p = p;
        this.closestBigVehicle = closestBigVehicle;
    }

//    private void makeTaskModelList(TaskSet tasks) {
//        taskModelList = new ArrayList<>();
//        for (Task task : tasks) {
//            taskModelList.add(new TaskModel(task, TaskTypeEnum.PICKUP));
//            taskModelList.add(new TaskModel(task, TaskTypeEnum.DELIVERY));
//        }
//    }


    public void SLS() {
        //SolutionModel currentSolution = createInitialSolution();
        SolutionModel currentSolution = createSmartInitialSolution();
        //double tmpCost = currentSolution.getCost();
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
                    System.out.println(String.format("Iteration: %d | Best cost: %.2f | Current cost: %.2f"
                            , count, bestSolution.getCost(), currentSolution.getCost()));
                }
//                if (count % 10000 == 0) {
//                    if(tmpCost != bestSolution.getCost()) {
//                        tmpCost = bestSolution.getCost();
//                    }
//                    else {
//                        ALPHA /= 2;
//                        BETA *= 2;
//                        System.out.println(ALPHA);
//                        System.out.println(BETA);
//                    }
//                }
                count++;
            }

            remainingTime -= System.currentTimeMillis() - loopStartTime;
        }
    }

    private SolutionModel exploreNeighbors(SolutionModel currentSolution) {
        SolutionModel bestNeighbor = null;
        Map<Vehicle, ArrayList<TaskModel>> map = currentSolution.getVehicleTasksMap();

        int iterCount = (int) (ALPHA * vehicleList.size());
        for (int k = 0; k < iterCount; k++) {
            Vehicle vehicle = vehicleList.get(new Random().nextInt(vehicleList.size()));
            if (map.get(vehicle).size() < 1) { // continue cause chosen vehicle doesn't have any task
                continue;
            }

            int i = new Random().nextInt(map.get(vehicle).size());
            int j = new Random().nextInt(map.get(vehicle).size());
            SolutionModel neighbor = new SwapTasksOperation(currentSolution, i, j, vehicle).getNewSolution();
            if (neighbor != null &&
                    (bestNeighbor == null || neighbor.getCost() < bestNeighbor.getCost())) {
                bestNeighbor = neighbor;
            }
        }

        iterCount = (int) (BETA * taskModelList.size());
        for (int k = 0; k < iterCount; k++) {
            Vehicle v1 = vehicleList.get(new Random().nextInt(vehicleList.size()));
            Vehicle v2 = vehicleList.get(new Random().nextInt(vehicleList.size()));
            if (v1.equals(v2) || map.get(v1).size() < 1) {
                continue;
            }

            int i = new Random().nextInt(map.get(v1).size());
            SolutionModel neighbor = new ChangeVehicleOperation(currentSolution, v1, v2, i).getNewSolution();
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

    private SolutionModel createSmartInitialSolution() {
        Map<Vehicle, ArrayList<TaskModel>> map = new HashMap<>();
        Map<Vehicle, ArrayList<TaskModel>> deliveryAppendix = new HashMap<>();
        Map<Vehicle, Double> vehicleLoad = new HashMap<>();
        Set<Task> notAssignedTasks = new HashSet<>();

        for (Vehicle vehicle : vehicleList) {
            vehicleLoad.put(vehicle, 0d);
            map.put(vehicle, new ArrayList<>());
            deliveryAppendix.put(vehicle, new ArrayList<>());
        }
        for (Task task : tasks) {
            Vehicle suboptimalVehicle = closestBigVehicle.get(task.pickupCity);
            double toBeWeight = vehicleLoad.get(suboptimalVehicle) + task.weight;
            if (toBeWeight <= suboptimalVehicle.capacity()) {
                ArrayList<TaskModel> currentTasks = map.get(suboptimalVehicle);
                TaskModel taskModelPickup = new TaskModel(task, TaskTypeEnum.PICKUP);
                currentTasks.add(taskModelPickup);
                taskModelList.add(taskModelPickup);
                map.put(suboptimalVehicle, currentTasks);

                ArrayList<TaskModel> toBeAddedTasks = deliveryAppendix.get(suboptimalVehicle);
                TaskModel taskModelDelivery = new TaskModel(task, TaskTypeEnum.DELIVERY);
                toBeAddedTasks.add(taskModelDelivery);
                taskModelList.add(taskModelDelivery);
                deliveryAppendix.put(suboptimalVehicle, toBeAddedTasks);

                vehicleLoad.put(suboptimalVehicle, toBeWeight);
            } else {
                notAssignedTasks.add(task);
            }
        }

        for (Map.Entry<Vehicle, ArrayList<TaskModel>> entry : map.entrySet()) {
            ArrayList<TaskModel> currentTasks = entry.getValue();
            currentTasks.addAll(deliveryAppendix.get(entry.getKey()));
            map.put(entry.getKey(), currentTasks);
        }

        for (Task task : notAssignedTasks) {
            Vehicle vehicle = vehicleList.get(new Random().nextInt(vehicleList.size()));
            ArrayList<TaskModel> currentTasks = map.get(vehicle);

            TaskModel taskModelPickup = new TaskModel(task, TaskTypeEnum.PICKUP);
            currentTasks.add(taskModelPickup);
            taskModelList.add(taskModelPickup);

            TaskModel taskModelDelivery = new TaskModel(task, TaskTypeEnum.DELIVERY);
            currentTasks.add(taskModelDelivery);
            taskModelList.add(taskModelDelivery);

            map.put(vehicle, currentTasks);
        }

        return new SolutionModel(map);
    }

    public SolutionModel getBestSolution() {
        return bestSolution;
    }
}
