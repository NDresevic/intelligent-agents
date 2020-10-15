package deliberative;

import logist.simulation.Vehicle;
import logist.task.Task;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import java.util.Set;

public class AStar extends SearchAlgorithm {

    private final PriorityQueue<State> Q;
    private final Set<State> C;
    private final Map<State, Double> H;
    private final String heuristicName;

    public AStar(Set<Task> availableTaskSet, Set<Task> carriedTaskSet, Vehicle vehicle,
                 String heuristicName) {
        super(availableTaskSet, carriedTaskSet, vehicle);

        this.Q = new PriorityQueue<>(1, Comparator.comparingDouble(this::calculateF));
        this.C = new HashSet<>();
        this.H = new HashMap<>();
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
                    continue;
                }

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
            C.add(currentState);
        }

        return goalState;
    }

    private void calculateHeuristic() {
        try {
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
            if (currentState.getCurrentCity().distanceTo(task.deliveryCity) < curr) {
                curr = currentState.getCurrentCity().distanceTo(task.deliveryCity);
            }
        }

        h += curr;
        curr = currentState.getAvailableTasks().isEmpty() ? 0d : Double.MAX_VALUE;
        for (Task task : currentState.getAvailableTasks()) {
            double possibleShorterPath = currentState.getCurrentCity().distanceTo(task.pickupCity) +
                    task.pickupCity.distanceTo(task.deliveryCity);
            if (possibleShorterPath < curr) {
                curr = possibleShorterPath;
            }
        }
        h += curr;
        H.put(currentState, h);

        for (State child : currentState.getChildren()) {
            minCarriedPlusMinAvailable(child);
        }
    }
}
