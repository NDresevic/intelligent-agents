package model;

import logist.simulation.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolutionModel {

    private Map<Vehicle, List<TaskModel>> vehicleTasksMap;
    private Map<Vehicle, List<Double>> vehicleLoad;
    private Map<TaskModel, Integer> pairIndex;
    private double cost;

    public SolutionModel(Map<Vehicle, List<TaskModel>> vehicleTasksMap, double cost) {
        this.vehicleTasksMap = vehicleTasksMap;
        this.cost = cost;
        createVehicleLoad();
        createPairIndex();

//        for (Map.Entry<TaskModel, Integer> entry : pairIndex.entrySet()){
//            System.out.println(entry.getKey() + " : " + entry.getValue());
//            System.out.println("!!!!!!!!!!!!!!");
//        }
//        System.out.println(pairIndex.size());
//
//        for(Vehicle vehicle : vehicleTasksMap.keySet()){
//            List<TaskModel> tasks = vehicleTasksMap.get(vehicle);
//
//            for(TaskModel task : tasks) {
//                System.out.println(pairIndex.containsKey(task));
//                System.out.println(task + " par: " + pairIndex.get(task));
//            }
//
//            System.out.println("**************");
//        }
    }

    public SolutionModel(SolutionModel solution) {
        this.vehicleTasksMap = new HashMap<>(solution.vehicleTasksMap);
        this.vehicleLoad = new HashMap<>(solution.vehicleLoad);
        this.pairIndex = new HashMap<>(solution.pairIndex);
        this.cost = solution.cost;
    }

    private void createVehicleLoad() {
        vehicleLoad = new HashMap<>();
        for (Map.Entry<Vehicle, List<TaskModel>> entry : vehicleTasksMap.entrySet()) {
            ArrayList<Double> loads = new ArrayList<>();
            List<TaskModel> tasks = entry.getValue();
            double currentLoad = 0;
            for (TaskModel task : tasks) {
                currentLoad += task.updateLoad();
                loads.add(currentLoad);
            }
            vehicleLoad.put(entry.getKey(), loads);
        }
    }

    private void createPairIndex() {
        pairIndex = new HashMap<>();
        for (Map.Entry<Vehicle, List<TaskModel>> entry : vehicleTasksMap.entrySet()) {
            List<TaskModel> tasks = entry.getValue();
            for (int i = 0; i < tasks.size(); i++) {
                pairIndex.put(new TaskModel(tasks.get(i).getTask(), tasks.get(i).getPairOperation()),
                        i);
            }
        }
    }

    public Map<Vehicle, List<TaskModel>> getVehicleTasksMap() {
        return vehicleTasksMap;
    }

    public Map<Vehicle, List<Double>> getVehicleLoad() {
        return vehicleLoad;
    }

    public double getCost() { return cost; }

    public void setCost(double cost) { this.cost = cost; }

    public Map<TaskModel, Integer> getPairIndex() { return pairIndex; }
}
