package deliberative;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

import java.util.*;
import java.util.stream.Collectors;

public abstract class SearchAlgorithm {

    private Set<Task> availableTaskSet;
    private Set<Task> carriedTaskSet;
    protected Vehicle vehicle;

    protected State rootState;
    private Map<Integer, State> hashStateMap;
    private Map<City, List<Task>> nextCityTasksMap;
    private List<Task> newCarriedTaskSet;
    private List<Task> newAvailableTaskSet;
    protected final Map<State, Double> G;
    protected final Map<State, State> parentOptimal;
    protected int visitedStates;

    public SearchAlgorithm(Set<Task> availableTaskSet, Set<Task> carriedTaskSet, Vehicle vehicle) {
        this.availableTaskSet = availableTaskSet;
        this.carriedTaskSet = carriedTaskSet;
        this.vehicle = vehicle;

        this.hashStateMap = new HashMap<>();
        this.nextCityTasksMap = new HashMap<>();
        this.newCarriedTaskSet = new ArrayList<>();
        this.newAvailableTaskSet = new ArrayList<>();
        this.G = new HashMap<>();
        this.parentOptimal = new HashMap<>();
        this.visitedStates = 0;
        this.rootState = createGraphAndGetRoot();
    }

    private State createGraphAndGetRoot() {
        State rootState = new State(vehicle.getCurrentCity(), carriedTaskSet, availableTaskSet, vehicle);
        int ID = 0;
        boolean seenFinalState = false;
        hashStateMap.put(rootState.hashCode(), rootState);
        rootState.setId(++ID);
        int count = 1;

        List<State> unvisited = new ArrayList<>();
        unvisited.add(rootState);
        while (!unvisited.isEmpty()) {
            State currentState = unvisited.remove(0);
            newCarriedTaskSet = new ArrayList<>(currentState.getCarriedTasks());
            newAvailableTaskSet = new ArrayList<>(currentState.getAvailableTasks());
            vehicle = currentState.getVehicle();
            Set<State> children = new HashSet<>();

            // if the state is final just add it as a child and put in map
            if (newCarriedTaskSet.isEmpty() && newAvailableTaskSet.isEmpty()) {
                State finalState = new State(currentState.getCurrentCity(), new HashSet<>(), new HashSet<>(), vehicle);
                children.add(finalState);
                hashStateMap.putIfAbsent(finalState.hashCode(), finalState);
                if(!seenFinalState){
                    finalState.setId(++ID);
                    seenFinalState = true;
                }
            } else {

                // collect all the next cities it makes sense to go to and their tasks
                nextCityTasksMap.clear();
                for (Task t : newAvailableTaskSet) { // the ones in which you can pick up a task
                    nextCityTasksMap.putIfAbsent(t.pickupCity, new ArrayList<>());
                    List<Task> values = nextCityTasksMap.get(t.pickupCity);
                    values.add(t);
                    nextCityTasksMap.put(t.pickupCity, values);
                }
                for (Task t : newCarriedTaskSet) { // the ones in which you can deliver a task
                    nextCityTasksMap.putIfAbsent(t.deliveryCity, new ArrayList<>());
                    List<Task> values = nextCityTasksMap.get(t.deliveryCity);
                    values.add(t);
                    nextCityTasksMap.put(t.deliveryCity, values);
                }

                for (Map.Entry<City, List<Task>> entry : nextCityTasksMap.entrySet()) {
                    City nextCity = entry.getKey();
                    List<Task> allTasks = entry.getValue();

                    if (currentState.getCurrentCity().equals(nextCity)) {
                        continue;
                    }

                    newCarriedTaskSet = new ArrayList<>(currentState.getCarriedTasks());
                    newAvailableTaskSet = new ArrayList<>(currentState.getAvailableTasks());
                    int carriedTasksWeight = currentState.getCarriedTasksWeights();

                    boolean hasDeliveredTasks = false;
                    List<Task> deliveryTasks = new ArrayList<>();
                    for (Task task : allTasks) {
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

                    //creating all possible combinations of picking up in pickupCity
                    List<List<Task>> subsets = generateSubsets(allTasks,
                            vehicle.capacity() - carriedTasksWeight);
                    //create new state for all possible pickup combinations
                    for (List<Task> subset : subsets) {
                        List<Task> subsetAvailable = new ArrayList<>(newAvailableTaskSet);
                        subsetAvailable.removeAll(subset);
                        List<Task> subsetCarried = new ArrayList<>(newCarriedTaskSet);
                        subsetCarried.addAll(subset);
                        State nextState = new State(nextCity, new HashSet<>(subsetCarried),
                                new HashSet<>(subsetAvailable), vehicle);
                        children.add(nextState);
                    }
                }

                //currentState.setChildren(children);
                for (State child : children) {
                    if (!hashStateMap.containsKey(child.hashCode())) {
                        hashStateMap.put(child.hashCode(), child);
                        child.setId(++ID);
                        unvisited.add(child);
                        currentState.appendChild(child);
                    }
                    else
                        currentState.appendChild(hashStateMap.get(child.hashCode()));
                }
                count += children.size();
            }
        }

        System.out.println("BROJ SVE DECE " + count);
        System.out.println("GRAPF SIZE: " + hashStateMap.size());
        System.out.println("ID: " + ID);
        return rootState;
    }

    private List<List<Task>> generateSubsets(List<Task> tasks, int freeCapacity) {
        List<List<Task>> subsets = new ArrayList<>();
        //the weight of the current subset
        int currentWeight;
        // i = iterating over all possible bit combinations from 1 to pow(2,n)
        for (int i = 1; i < (1 << tasks.size()); i++) {
            List<Task> toBeAddedSubset = new ArrayList<>();
            boolean toAdd = true;
            currentWeight = 0;
            // j = iterate over all tasks
            for (int j = 0; j < tasks.size(); j++) {
                // the condition for the jth task to be in the subset
                if ((i & (1 << j)) > 0) {
                    toBeAddedSubset.add(tasks.get(j));
                    if (currentWeight + tasks.get(j).weight <= freeCapacity) //the task can fit into the vehicle
                        currentWeight += tasks.get(j).weight;
                    else { //the subset is too heavy and can not not fit to the vehicle
                        toAdd = false;
                        break;
                    }
                }
            }
            if (toAdd)
                subsets.add(toBeAddedSubset);
        }
        return subsets;
    }

    abstract State getGoalState();

    private List<State> createOptimalPath(State goalState) {
        List<State> optimalPath = new ArrayList<>();

        if (goalState == null) {
            return null;
        }
        State currentState = goalState;
        while (!currentState.equals(rootState)) {
            optimalPath.add(currentState);
            currentState = parentOptimal.get(currentState);
        }
        optimalPath.add(rootState);
        Collections.reverse(optimalPath);

        return optimalPath;
    }

    public Plan getPlan() {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);
        List<State> optimalPath = createOptimalPath(this.getGoalState());

        State previousState = optimalPath.get(0);
        for (int i = 1; i < optimalPath.size(); i++) {
            State currentState = optimalPath.get(i);

            List<City> intermediateCities = previousState.getCurrentCity().pathTo(currentState.getCurrentCity());
            for (City city : intermediateCities) {
                plan.appendMove(city);
            }

            // calculate which tasks are delivered
            Set<Task> previousCarriedTasks = previousState.getCarriedTasks();
            Set<Task> currentCarriedTasks = currentState.getCarriedTasks();
            List<Task> deliveredTasks = previousCarriedTasks.stream()
                    .filter(element -> !currentCarriedTasks.contains(element))
                    .collect(Collectors.toList());
            for (Task deliveredTask : deliveredTasks) {
                plan.appendDelivery(deliveredTask);
            }

            // calculate which tasks are picked up
            Set<Task> previousAvailableTasks = previousState.getAvailableTasks();
            Set<Task> currentAvailableTasks = currentState.getAvailableTasks();
            List<Task> pickedTasks = previousAvailableTasks.stream()
                    .filter(element -> !currentAvailableTasks.contains(element))
                    .collect(Collectors.toList());
            for (Task pickedTask : pickedTasks) {
                plan.appendPickup(pickedTask);
            }

            previousState = currentState;
        }

        return plan;
    }
}
