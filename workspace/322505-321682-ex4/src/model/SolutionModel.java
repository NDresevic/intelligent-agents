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
    }

    public SolutionModel(SolutionModel solution){
        this.vehicleTasksMap = new HashMap<>(solution.getVehicleTasksMap());
        this.cost = solution.getCost();
    }

    private void createVehicleLoad(){
        vehicleLoad = new HashMap<>();
        for(Vehicle vehicle : vehicleTasksMap.keySet()){
            ArrayList<Double> loads = new ArrayList<>();
            List<TaskModel> tasks = vehicleTasksMap.get(vehicle);
            double currentLoad = 0;
            for(TaskModel task : tasks){
                currentLoad += task.updateLoad();
                loads.add(currentLoad);
            }
            vehicleLoad.put(vehicle, loads);
        }
    }

    private void createPairIndex(){
        //FIXME
    }

    public Map<Vehicle, List<TaskModel>> getVehicleTasksMap() {
        return vehicleTasksMap;
    }

    public Map<Vehicle, List<Double>> getVehicleLoad() { return vehicleLoad; }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) { this.cost = cost; }

    public Map<TaskModel, Integer> getPairIndex() { return pairIndex; }
}
