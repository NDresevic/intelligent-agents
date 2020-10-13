package deliberative;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;

public abstract class SearchAlgorithm {

    private Set<Task> availableTaskSet;
    private Set<Task> carriedTaskSet;
    private Topology topology;
    private Vehicle vehicle;

    protected State rootState;

    public SearchAlgorithm(Set<Task> availableTaskSet, Set<Task> carriedTaskSet, Topology topology, Vehicle vehicle) {
        this.availableTaskSet = availableTaskSet;
        this.carriedTaskSet = carriedTaskSet;
        this.topology = topology;
        this.vehicle = vehicle;

        this.rootState = createGraphAndGetRoot();
    }

    private State createGraphAndGetRoot() {
        State rootState = new State(vehicle.getCurrentCity(), carriedTaskSet, availableTaskSet, vehicle);
        List<State> nodes = new ArrayList<>();

        nodes.add(rootState);
        while (!nodes.isEmpty()) {
            State currentState = nodes.remove(0);
            carriedTaskSet = currentState.getCarriedTasks();
            availableTaskSet = currentState.getAvailableTasks();
            vehicle = currentState.getVehicle();

            Set<State> children = new HashSet<>();
            if (carriedTaskSet.isEmpty() && availableTaskSet.isEmpty()) {
                State finalState = new State(currentState.getCurrentCity(), new HashSet<>(), new HashSet<>(), vehicle,
                        currentState);
                if (!currentState.getChildren().contains(finalState)) {
                    children.add(finalState);
                }
                continue;
            }

            Set<Task> newCarriedTaskSet = new HashSet<>(carriedTaskSet);
            Set<Task> newAvailableTaskSet = new HashSet<>(availableTaskSet);
            for (City nextStateCity: topology.cities()) {

                if (currentState.getCurrentCity().equals(nextStateCity)) {  // if I am in nextStateCity continue
                    continue;
                }

                boolean hasDeliveredTasks = false;
                for (Task carriedTask: carriedTaskSet) {    // deliver tasks that are for that city
                    if (carriedTask.deliveryCity.equals(nextStateCity)) {
                        newCarriedTaskSet.remove(carriedTask);
                        hasDeliveredTasks = true;
                    }
                }
                if (hasDeliveredTasks) {  // child when you just deliver the tasks you have for that city
                    State newState = new State(nextStateCity, newCarriedTaskSet, new HashSet<>(availableTaskSet), vehicle,
                            currentState);
                    children.add(newState);
                }

                for (Task availableTask: availableTaskSet) { // possible states when you pick up new task

                    // if you are not going to the city where the task is located or you don't have enough capacity continue
                    if (nextStateCity.equals(availableTask.pickupCity) &&
                            currentState.getCarriedTasksWeights() + availableTask.weight <= vehicle.capacity()) {
                        newAvailableTaskSet.remove(availableTask);

                        Set<Task> novi = new HashSet<>(newCarriedTaskSet);
                        novi.add(availableTask);
                        State newState = new State(nextStateCity, novi, newAvailableTaskSet, vehicle,
                                currentState);

                        children.add(newState);
                    }
                }
            }

            currentState.setChildren(children);
            nodes.addAll(children);
        }

        return rootState;
    }

    abstract Plan getPlan();
}
