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
        long startTime = System.currentTimeMillis();
        Set<State> visited = new LinkedHashSet<>();
        Queue<State> queue = new LinkedList<>();
        State goalState = null;
        double minimumCost = Double.MAX_VALUE;

        queue.add(rootState);
        visited.add(rootState);
        G.put(rootState, 0d);
        while (!queue.isEmpty()) {
            visitedStates++;
            State currentState = queue.poll();
            visited.add(currentState);

            if (currentState.isFinalState() && G.get(currentState) < minimumCost) {
                minimumCost = G.get(currentState);
                goalState = currentState;
            }

            for (State child : currentState.getChildren()) {
                double pathOverCurrentState = G.get(currentState) +
                        currentState.getCurrentCity().distanceTo(child.getCurrentCity());
                if (!G.containsKey(child) || pathOverCurrentState < G.get(child)) {
                    G.put(child, pathOverCurrentState);
                    parentOptimal.put(child, currentState);
                }

                if (!visited.contains(child)) {
                    queue.add(child);
                }
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("BFS traversal time: " + (endTime - startTime) + "ms");
        return goalState;
    }
}
