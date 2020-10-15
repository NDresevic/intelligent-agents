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
//        System.out.println("root: " + rootState);

//        System.out.println("graph: " + breadthFirstTraversal(rootState));
        State goalState = breadthFirstTraversal(rootState);
//        System.out.println(goalState.getCurrentCity());

        return goalState;
    }

    private State breadthFirstTraversal(State rootState) {
        Set<State> visited = new LinkedHashSet<>();
        Queue<State> queue = new LinkedList<>();
        double minCostFromRoot = Double.MAX_VALUE;
        State optimalGoalState = null;

        queue.add(rootState);
        visited.add(rootState);
        while (!queue.isEmpty()) {
            State currentState = queue.poll();

//            if (currentState.isGoalState() && currentState.getCostFromRoot() < minCostFromRoot) {
//                minCostFromRoot = currentState.getCostFromRoot();
//                optimalGoalState = currentState;
//            }

            for (State child : currentState.getChildren()) {
                if (!visited.contains(child)) {
                    visited.add(child);
                    queue.add(child);
                }
            }
        }

        System.out.println("BROJ STANJA: " + visited.size());
        //return visited;

//        System.out.println(visited);
        return optimalGoalState;
    }
}
