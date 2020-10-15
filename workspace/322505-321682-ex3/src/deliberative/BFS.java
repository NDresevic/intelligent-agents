package deliberative;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;

import java.util.*;

public class BFS extends SearchAlgorithm {

    List<State> optimalPath;
    private final Map<State, State> parentOptimal;

    public BFS(Set<Task> availableTaskSet, Set<Task> carriedTaskSet, Topology topology, Vehicle vehicle) {
        super(availableTaskSet, carriedTaskSet, topology, vehicle);
        this.parentOptimal = new HashMap<>();
        this.optimalPath = new ArrayList<>();
    }

    @Override
    public List<State> getOptimalPath() {
        State goalState = breadthFirstTraversal(rootState);

        return new ArrayList<>();
    }

    private State breadthFirstTraversal(State rootState) {
        Set<State> visited = new LinkedHashSet<>();
        Queue<State> queue = new LinkedList<>();
        State goalState = null;
        double costFromRoot = Double.MAX_VALUE;

        queue.add(rootState);
        visited.add(rootState);
        G.put(rootState, 0d);
        while (!queue.isEmpty()) {
            State currentState = queue.poll();
            if (currentState.isGoalState()) {
                if(G.get(currentState) < costFromRoot) {
                    costFromRoot = G.get(currentState);
                    goalState = currentState;
                }
            }
            for (State child : currentState.getChildren()) {
                G.putIfAbsent(child, Double.MAX_VALUE);
                double possiblyShorterPath = G.get(currentState) + currentState.getCurrentCity().distanceTo(child.getCurrentCity());
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

        if (goalState != null) {
            if (goalState != null) {
                State currentState = goalState;
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

        System.out.println("BROJ STANJA: " + visited.size());
        //return visited;

        return goalState;
    }
}
