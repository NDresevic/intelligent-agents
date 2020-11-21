package strategy;

import enums.TaskTypeEnum;
import logist.simulation.Vehicle;
import logist.topology.Topology.City;
import models.SolutionModel;
import models.TaskModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EstimateSolutionStrategy {

    /**
     * Adds the first task to the biggest vehicle and properly updates the solution.
     *
     * @param solution     - solution to which we add the task
     * @param pickupTask   - first task model for pick up
     * @param deliveryTask - first task model for delivery
     * @return - solution with added task
     */
    public static SolutionModel addFirstTaskToSolution(SolutionModel solution,
                                                       TaskModel pickupTask, TaskModel deliveryTask) {
        // find vehicle with biggest capacity
        int biggestCapacity = 0;
        Vehicle biggestVehicle = null;
        for (Map.Entry<Vehicle, ArrayList<TaskModel>> entry : solution.getVehicleTasksMap().entrySet()) {
            if (entry.getKey().capacity() > biggestCapacity) {
                biggestVehicle = entry.getKey();
                biggestCapacity = biggestVehicle.capacity();
            }
            solution.getVehicleCostMap().put(entry.getKey(), 0.0);
        }

        // add tasks and update task pair map
        ArrayList<TaskModel> taskModels = new ArrayList<>();
        taskModels.add(pickupTask);
        taskModels.add(deliveryTask);
        solution.getVehicleTasksMap().put(biggestVehicle, taskModels);
        solution.getTaskPairIndexMap().put(pickupTask, 1);
        solution.getTaskPairIndexMap().put(deliveryTask, 0);

        // calculate and update costs
        double distance = biggestVehicle.getCurrentCity().distanceTo(pickupTask.getTask().pickupCity) +
                pickupTask.getTask().pickupCity.distanceTo(deliveryTask.getTask().deliveryCity);
        double cost = distance * biggestVehicle.costPerKm();
        solution.getVehicleCostMap().put(biggestVehicle, cost);
        solution.setCost(cost);

        return solution;
    }

    /**
     * Method that tries all possible combinations of adding new task in the current solution and returns the optimal
     * one based on overall cost.
     *
     * @param solution     - solution to which we add the task
     * @param pickupTask   - new task model for pick up
     * @param deliveryTask - new task model for delivery
     * @return - best solution when inserting new task or null if no such solution is valid
     */
    public static SolutionModel optimalSolutionWithTask(SolutionModel solution,
                                                        TaskModel pickupTask, TaskModel deliveryTask) {
        double bestCost = Double.MAX_VALUE;
        SolutionModel bestSolution = null;

        for (Map.Entry<Vehicle, ArrayList<TaskModel>> entry : solution.getVehicleTasksMap().entrySet()) {
            Vehicle vehicle = entry.getKey();
            List<TaskModel> tasks = entry.getValue();

            for (int i = 0; i < tasks.size() + 1; i++) {   // i - position of a pick up task
                for (int j = i + 1; j < tasks.size() + 2; j++) {   // j - position of a delivery task
                    ArrayList<TaskModel> newTaskModels = new ArrayList<>(tasks);
                    newTaskModels.add(i, pickupTask);
                    newTaskModels.add(j, deliveryTask);

                    SolutionModel newSolution = new SolutionModel(solution);
                    newSolution.getVehicleTasksMap().put(vehicle, newTaskModels);
                    double cost = updateSolutionAndGetCost(newSolution, vehicle);

                    // update best solution if the solution is valid and new best
                    if (Double.compare(cost, -1) != 0 && cost < bestCost) {
                        bestCost = cost;
                        bestSolution = newSolution;
                    }
                }
            }
        }

        return bestSolution;
    }

    /**
     * Checks if the new solution with a new task added to a vehicle is possible. If so, the costs and task pair
     * map are updated.
     *
     * @param solution - solution to update
     * @param vehicle  - vehicle in which new task is added
     * @return - solution cost or -1 if the plan is not valid
     */
    private static double updateSolutionAndGetCost(SolutionModel solution, Vehicle vehicle) {
        Map<Vehicle, ArrayList<TaskModel>> vehicleTasksMap = solution.getVehicleTasksMap();

        double cost = 0;
        for (Map.Entry<Vehicle, ArrayList<TaskModel>> entry : vehicleTasksMap.entrySet()) {
            if (vehicle.id() != entry.getKey().id()) {  // vehicle which tasks are unchanged
                cost += solution.getVehicleCostMap().get(entry.getKey());
                continue;
            }
            City currentCity = vehicle.getCurrentCity();
            ArrayList<TaskModel> taskModels = entry.getValue();

            double vehicleCost = 0;
            double vehicleLoad = 0;
            for (int i = 0; i < taskModels.size(); i++) {
                TaskModel task = taskModels.get(i);
                City nextCity = task.getType().equals(TaskTypeEnum.PICKUP) ?
                        task.getTask().pickupCity : task.getTask().deliveryCity;
                vehicleCost += currentCity.distanceTo(nextCity) * vehicle.costPerKm();

                vehicleLoad += task.getUpdatedLoad();
                // return -1 if the plan is not valid because load is bigger than capacity
                if (vehicleLoad > vehicle.capacity()) {
                    return -1;
                }

                solution.getTaskPairIndexMap().put(new TaskModel(task.getTask(), task.getPairTaskType()), i);
                currentCity = nextCity;
            }
            // update vehicle cost
            solution.getVehicleCostMap().put(vehicle, vehicleCost);
            cost += vehicleCost;
        }

        solution.setCost(cost);
        return cost;
    }

    /**
     * Adds the task to the end of the route for a vehicle it can fit in.
     *
     * @param solution     - solution to which we add the task
     * @param pickupTask   - task model for pick up
     * @param deliveryTask - task model for delivery
     * @return - solution with added task
     */
    public static SolutionModel addTaskToEnd(SolutionModel solution,
                                             TaskModel pickupTask, TaskModel deliveryTask) {
        for (Map.Entry<Vehicle, ArrayList<TaskModel>> entry : solution.getVehicleTasksMap().entrySet()) {
            Vehicle vehicle = entry.getKey();
            double vehicleCost = solution.getVehicleCostMap().get(vehicle);

            if (vehicle.capacity() > pickupTask.getTask().weight) {
                ArrayList<TaskModel> taskModels = new ArrayList<>(entry.getValue());
                taskModels.add(pickupTask);
                taskModels.add(deliveryTask);
                solution.getTaskPairIndexMap().put(pickupTask, taskModels.indexOf(deliveryTask));
                solution.getTaskPairIndexMap().put(deliveryTask, taskModels.indexOf(pickupTask));
                solution.getVehicleTasksMap().put(vehicle, taskModels);

                // distance to pick up new task and deliver it
                double additionalDistance = pickupTask.getTask().pickupCity.distanceTo(deliveryTask.getTask().deliveryCity);
                if (taskModels.size() > 2) {
                    additionalDistance += taskModels.get(taskModels.size() - 3).getTask().deliveryCity.
                            distanceTo(pickupTask.getTask().pickupCity);
                } else {
                    additionalDistance += vehicle.getCurrentCity().distanceTo(pickupTask.getTask().pickupCity);
                }
                double newVehicleCost = vehicleCost + additionalDistance * vehicle.costPerKm();
                solution.getVehicleCostMap().put(vehicle, newVehicleCost);
                solution.setCost(solution.getCost() - vehicleCost + newVehicleCost);

                break;
            }
        }
        return solution;
    }
}
