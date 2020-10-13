package deliberative;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;

import java.util.*;

public class BFS extends SearchAlgorithm {

    public BFS(Set<Task> availableTaskSet, Set<Task> carriedTaskSet, Topology topology, Vehicle vehicle) {
        super(availableTaskSet, carriedTaskSet, topology, vehicle);
    }

    @Override
    public State getGoalState() {
        System.out.println("root: " + rootState);

        System.out.println("graph: " + breadthFirstTraversal(rootState));

        return null;
    }

    private Set<State> breadthFirstTraversal(State rootState) {
        Set<State> visited = new LinkedHashSet<>();
        Queue<State> queue = new LinkedList<>();

        queue.add(rootState);
        visited.add(rootState);
        while (!queue.isEmpty()) {
            State currentState = queue.poll();
            for (State child : currentState.getChildren()) {
                if (!visited.contains(child)) {
                    visited.add(child);
                    queue.add(child);
                }
            }
        }
        return visited;
    }
}
