package deliberative;

import logist.simulation.Vehicle;
import logist.task.Task;
import java.util.*;

public class BFS extends SearchAlgorithm {

    public BFS(Set<Task> availableTaskSet, Set<Task> carriedTaskSet, Vehicle vehicle) {
        super(availableTaskSet, carriedTaskSet, vehicle);
    }

    @Override
    public State getGoalState() {
        Set<State> visited = new LinkedHashSet<>();
        Queue<State> queue = new LinkedList<>();
        State goalState = null;
        double costFromRoot = Double.MAX_VALUE;

        queue.add(rootState);
        visited.add(rootState);
        G.put(rootState, 0d);
        while (!queue.isEmpty()) {
            State currentState = queue.poll();
            if (currentState.isGoalState() && G.get(currentState) < costFromRoot) {
                costFromRoot = G.get(currentState);
                goalState = currentState;
            }

            for (State child : currentState.getChildren()) {
                G.putIfAbsent(child, Double.MAX_VALUE);
                double possiblyShorterPath = G.get(currentState) +
                        currentState.getCurrentCity().distanceTo(child.getCurrentCity());
                if (possiblyShorterPath < G.get(child)) {
                    G.put(child, possiblyShorterPath);
                    parentOptimal.put(child, currentState);
                }

                if (!visited.contains(child)) {
                    visited.add(child);
                    queue.add(child);
                }
            }
        }

        return goalState;
    }
}
