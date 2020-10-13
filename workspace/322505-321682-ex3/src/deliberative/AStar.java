package deliberative;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;


import java.util.Set;

public class AStar extends SearchAlgorithm {

    private final String heuristicName;
    private final PriorityQueue<State> Q;
    //TODO this should be deleted if our graph is tree
    private final Set<State> C;
    //TODO this should be deleted if our graph is tree
    private final Map<State, State> parentOptimal;
    private final Map<State, Double> H;

    public AStar(Set<Task> availableTaskSet, Set<Task> carriedTaskSet, Topology topology, Vehicle vehicle,
                 String heuristicName) {
        super(availableTaskSet, carriedTaskSet, topology, vehicle);

        this.Q = new PriorityQueue<>(1, (state1, state2)
                -> Double.valueOf(calculateF(state1)).compareTo(Double.valueOf(calculateF(state2))));
        this.C = new HashSet<>();
        this.H = new HashMap<>();
        this.parentOptimal = new HashMap<>();
        this.heuristicName = heuristicName;
    }

    private double calculateF(State state) {
        return state.getCostFromRoot() + H.get(state);
    }

    @Override
    public State getGoalState() {
        calculateHeuristic();
        Q.add(rootState);

        State currentState;
        State goalState = null;

        //while there are unprocessed states
        while (!Q.isEmpty()) {
            currentState = Q.remove();
            if (currentState.isGoalState()) {
                goalState = currentState;
                break;
            }
            Set<State> children = currentState.getChildren();
            for (State child : children) {
                //TODO Q can not have child if we have a tree
                if(Q.contains(child))
                    System.err.println("Q: THIS SHOULD BE IMPOSSIBLE IN OUR STATE REPRESENTATION");
                if (!Q.contains(child) && !C.contains(child)) {
                    Q.add(child);
                    parentOptimal.put(child, currentState);
                } else {
                    double possibleBetterPath = currentState.getCostFromRoot() +
                            currentState.getCurrentCity().distanceTo(child.getCurrentCity());
                    //TODO this is impossible since we have a tree and not a graph
                    System.err.println("multiple paths: THIS SHOULD BE IMPOSSIBLE IN OUR STATE REPRESENTATION");
                    if (possibleBetterPath < child.getCostFromRoot()) {
                        parentOptimal.put(child, currentState);
                        if (C.contains(child)) {
                            C.remove(child);
                            Q.add(child);
                        }
                    }
                }
            }
            C.add(currentState);
        }

        return goalState;
    }

    private void calculateHeuristic() {
        try {
            System.out.println(heuristicName);
            Method method = this.getClass().getDeclaredMethod(heuristicName, rootState.getClass());
            method.invoke(this, rootState);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void minCarriedPlusMinAvailable(State currentState) {
        double h = 0d;
        double curr = currentState.getCarriedTasks().isEmpty() ? 0d : Double.MAX_VALUE;
        for (Task task : currentState.getCarriedTasks()) {
            if (currentState.getCurrentCity().distanceTo(task.deliveryCity) < curr)
                curr = currentState.getCurrentCity().distanceTo(task.deliveryCity);
        }
        h += curr;
        curr = currentState.getAvailableTasks().isEmpty() ? 0d : Double.MAX_VALUE;
        for (Task task : currentState.getAvailableTasks()) {
            double possibleShorterPath = currentState.getCurrentCity().distanceTo(task.pickupCity) +
                    task.pickupCity.distanceTo(task.deliveryCity);
            if (possibleShorterPath < curr)
                curr = possibleShorterPath;
        }
        h += curr;

        H.put(currentState, h);

        for (State child : currentState.getChildren()) {
            minCarriedPlusMinAvailable(child);
        }
    }
}
