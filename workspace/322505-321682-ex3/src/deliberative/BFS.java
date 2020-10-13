package deliberative;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class BFS extends SearchAlgorithm {

    public BFS(Set<Task> availableTaskSet, Set<Task> carriedTaskSet, Topology topology, Vehicle vehicle) {
        super(availableTaskSet, carriedTaskSet, topology, vehicle);
    }

    public Plan getPlan() {
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
