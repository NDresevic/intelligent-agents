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
    private final Map<State, Double> F;
    private final String heuristicName;

    public AStar(Set<Task> availableTaskSet, Set<Task> carriedTaskSet, Vehicle vehicle,
                 String heuristicName) {
        super(availableTaskSet, carriedTaskSet, vehicle);

        this.Q = new PriorityQueue<>(Comparator.comparingDouble(this::calculateF));
        this.C = new HashSet<>();
        this.H = new HashMap<>();
        this.F = new HashMap<>();
        this.heuristicName = heuristicName;
    }

    private Double calculateF(State state) {
        return G.get(state) + H.get(state);
    }

    @Override
    public State getGoalState() {
        calculateHeuristic();
        G.put(rootState, 0d);
        F.put(rootState, calculateF(rootState));
        Q.add(rootState);

        State currentState;
        // while there are unprocessed states
        while (!Q.isEmpty()) {
            visitedStates++;
            currentState = Q.poll();
            C.add(currentState);

            if (currentState.isFinalState()) {
                return currentState;
            }

            // process children
            for (State child : currentState.getChildren()) {
                double pathOverCurrentState = G.get(currentState) +
                        currentState.getCurrentCity().distanceTo(child.getCurrentCity());
                if (!Q.contains(child) && !C.contains(child)) {
                    G.put(child, pathOverCurrentState);
                    F.put(child, calculateF(child));
                    Q.add(child);
                    parentOptimal.put(child, currentState);
                } // another new path to child
                // path to child over current state is better than the previous optimal path to child
                else if (pathOverCurrentState < G.get(child)) {
                    // not removing because of the time complexity
//                    Q.remove(child);
                    G.put(child, pathOverCurrentState);
                    F.put(child, calculateF(child));
                    Q.add(child);
                    parentOptimal.put(child, currentState);
                }
            }
        }

        return null;
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
        double h1 = currentState.getCarriedTasks().isEmpty() ? 0d : Double.MAX_VALUE;
        for (Task task : currentState.getCarriedTasks()) {
            if (currentState.getCurrentCity().distanceTo(task.deliveryCity) < h1) {
                h1 = currentState.getCurrentCity().distanceTo(task.deliveryCity);
            }
        }
        double h2 = currentState.getAvailableTasks().isEmpty() ? 0d : Double.MAX_VALUE;
        for (Task task : currentState.getAvailableTasks()) {
            double possibleShorterPath = currentState.getCurrentCity().distanceTo(task.pickupCity) +
                    task.pickupCity.distanceTo(task.deliveryCity);
            if (possibleShorterPath < h2) {
                h2 = possibleShorterPath;
            }
        }
        double h = Math.max(h1, h2);
        H.put(currentState, h * vehicle.costPerKm());

        for (State child : currentState.getChildren()) {
            minCarriedPlusMinAvailable(child);
        }
    }

    private void zeroHeuristic(State currentState) {
        H.put(currentState, 0d);

        for (State child : currentState.getChildren()) {
            zeroHeuristic(child);
        }
    }
}
