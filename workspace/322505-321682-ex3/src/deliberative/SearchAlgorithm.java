package deliberative;

import logist.plan.Action;
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

    private Map<Integer, State> hashStateMap;
    private Map<City, List<Task>> nextCityTasksMap;
    private List<Task> newCarriedTaskSet;
    private List<Task> newAvailableTaskSet;
    protected final Map<State, Double> G;

    public SearchAlgorithm(Set<Task> availableTaskSet, Set<Task> carriedTaskSet, Topology topology, Vehicle vehicle) {
        this.availableTaskSet = availableTaskSet;
        this.carriedTaskSet = carriedTaskSet;
        this.topology = topology;
        this.vehicle = vehicle;

        this.hashStateMap = new HashMap<>();
        this.nextCityTasksMap = new HashMap<>();
        this.newCarriedTaskSet = new ArrayList<>();
        this.newAvailableTaskSet = new ArrayList<>();
        this.G = new HashMap<>();
        this.rootState = createGraphAndGetRoot();
    }

    private State createGraphAndGetRoot() {
        State rootState = new State(vehicle.getCurrentCity(), carriedTaskSet, availableTaskSet, vehicle);
        hashStateMap.put(rootState.hashCode(), rootState);

        List<State> unvisited = new ArrayList<>();
        unvisited.add(rootState);
        while (!unvisited.isEmpty()) {
            State currentState = unvisited.remove(0);
            newCarriedTaskSet = new ArrayList<>(currentState.getCarriedTasks());
            newAvailableTaskSet = new ArrayList<>(currentState.getAvailableTasks());
            vehicle = currentState.getVehicle();
            int carriedTasksWeight = currentState.getCarriedTasksWeights();
            Set<State> children = new HashSet<>();

            // if the state is final just add it as a child and put in map
            if (newCarriedTaskSet.isEmpty() && newAvailableTaskSet.isEmpty()) {
                State finalState = new State(currentState.getCurrentCity(), new HashSet<>(), new HashSet<>(), vehicle);
                children.add(finalState);
                hashStateMap.putIfAbsent(finalState.hashCode(), finalState);
            } else {

                // collect all the next cities it makes sense to go to and their tasks
                nextCityTasksMap.clear();
                for (Task t: newAvailableTaskSet) { // the ones in which you can pick up a task
                    nextCityTasksMap.putIfAbsent(t.pickupCity, new ArrayList<>());
                    List<Task> values = nextCityTasksMap.get(t.pickupCity);
                    values.add(t);
                    nextCityTasksMap.put(t.pickupCity, values);
                }
                for (Task t: newCarriedTaskSet) { // the ones in which you can deliver a task
                    nextCityTasksMap.putIfAbsent(t.deliveryCity, new ArrayList<>());
                    List<Task> values = nextCityTasksMap.get(t.deliveryCity);
                    values.add(t);
                    nextCityTasksMap.put(t.deliveryCity, values);
                }

                for (Map.Entry<City, List<Task>> entry: nextCityTasksMap.entrySet()) {
                    City nextCity = entry.getKey();
                    List<Task> allTasks = entry.getValue();

                    if (currentState.getCurrentCity().equals(nextCity)) {
                        continue;
                    }

                    newCarriedTaskSet = new ArrayList<>(currentState.getCarriedTasks());
                    newAvailableTaskSet = new ArrayList<>(currentState.getAvailableTasks());
                    carriedTasksWeight = currentState.getCarriedTasksWeights();

                    boolean hasDeliveredTasks = false;
                    List<Task> deliveryTasks = new ArrayList<>();
                    for (Task task: allTasks) {
                        if (task.deliveryCity.equals(nextCity)) {
                            deliveryTasks.add(task);
                            carriedTasksWeight -= task.weight;
                            hasDeliveredTasks = true;
                        }
                    }
                    if (hasDeliveredTasks) {  // child when you just deliver the tasks you have for that city
                        allTasks.removeAll(deliveryTasks); // remove all delivery tasks for the city
                        newCarriedTaskSet.removeAll(deliveryTasks);

                        State nextState = new State(nextCity, new HashSet<>(newCarriedTaskSet),
                                new HashSet<>(newAvailableTaskSet), vehicle);
                        children.add(nextState);
                    }

                    for (Task task: allTasks) { // only pick up tasks are left
                        if (carriedTasksWeight + task.weight > vehicle.capacity()) {
                            continue;
                        }

                        newAvailableTaskSet = new ArrayList<>(currentState.getAvailableTasks());
                        newAvailableTaskSet.remove(task);
                        newCarriedTaskSet = new ArrayList<>(newCarriedTaskSet);
                        newCarriedTaskSet.add(task);
                        State nextState = new State(nextCity, new HashSet<>(newCarriedTaskSet),
                                new HashSet<>(newAvailableTaskSet), vehicle);
                        children.add(nextState);
                    }
                }

                currentState.setChildren(children);
                for (State state: children) {
                    if (!hashStateMap.containsKey(state.hashCode())) {
                        hashStateMap.put(state.hashCode(), state);
                        unvisited.add(state);
                    }
                }
            }
        }

        return rootState;
    }

    abstract State getGoalState();

    public Plan getPlan() {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);
        List<Action> actions = new ArrayList<>();

        State currentState = this.getGoalState();
        System.out.println("plan");

        return null;
    }
}
