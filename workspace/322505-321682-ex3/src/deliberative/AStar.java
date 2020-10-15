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
    private final Set<State> C;
    private final Map<State, State> parentOptimal;
    private final Map<State, Double> H;
    List<State> optimalPath;

    public AStar(Set<Task> availableTaskSet, Set<Task> carriedTaskSet, Topology topology, Vehicle vehicle,
                 String heuristicName) {
        super(availableTaskSet, carriedTaskSet, topology, vehicle);

        this.Q = new PriorityQueue<>(1, (state1, state2)
                -> Double.valueOf(calculateF(state1)).compareTo(Double.valueOf(calculateF(state2))));
        this.C = new HashSet<>();
        this.H = new HashMap<>();
        this.parentOptimal = new HashMap<>();
        this.optimalPath = new ArrayList<>();
        this.heuristicName = heuristicName;

    }

    private double calculateF(State state) {
        return G.get(state) + H.get(state);
    }

    @Override
    public State getGoalState() {
        calculateHeuristic();
        G.put(rootState, 0d);
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
                if (!Q.contains(child) && !C.contains(child)) {
                    G.put(child, G.get(currentState) + currentState.getCurrentCity().distanceTo(child.getCurrentCity()));
                    Q.add(child);
                    parentOptimal.put(child, currentState);
                } else {
                    double possibleBetterPath = G.get(currentState) +
                            currentState.getCurrentCity().distanceTo(child.getCurrentCity());
                    if (possibleBetterPath < G.get(child)) {
                        parentOptimal.put(child, currentState);
                        G.put(child, possibleBetterPath);
                        if (C.contains(child)) {
                            C.remove(child);
                            Q.add(child);
                        }
                    }
                }
            }
            C.add(currentState);
        }

        if (goalState != null) {
            if (goalState != null) {
                currentState = goalState;
                    while (!currentState.equals(rootState)) {
                        optimalPath.add(currentState);
                        currentState = parentOptimal.get(currentState);
                    }
            }
            optimalPath.add(rootState);
        }
        Collections.reverse(optimalPath);

        for(State s : optimalPath)
            System.out.println(s + "\n");

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
