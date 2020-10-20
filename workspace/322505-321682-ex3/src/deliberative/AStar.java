package deliberative;

import logist.simulation.Vehicle;
import logist.task.Task;

import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

public class AStar extends SearchAlgorithm {

    private final PriorityQueue<State> Q;
    private final Set<State> C;
    // copy of the queue Q for faster checking if the node is in the queue (contains)
    private final Set<State> Qin;

    public AStar(Set<Task> availableTaskSet, Set<Task> carriedTaskSet, Vehicle vehicle) {
        super(availableTaskSet, carriedTaskSet, vehicle);

        this.Q = new PriorityQueue<>(Comparator.comparingDouble(this::calculateF));
        this.C = new HashSet<>();
        this.Qin = new HashSet<>();
    }

    private Double calculateF(State state) {
        return G.get(state) + state.getH();
    }

    @Override
    public State getGoalState() {
        long startTime = System.currentTimeMillis();
        G.put(rootState, 0d);
        Q.add(rootState);
        Qin.add(rootState);
        visitedStates = 0;

        State currentState;
        // while there are unprocessed states
        while (!Q.isEmpty()) {
            visitedStates++;
            currentState = Q.poll();
            C.add(currentState);

            if (currentState.isFinalState()) {
                long endTime = System.currentTimeMillis();
                System.out.println("A-star traversal time: " + (endTime - startTime) + "ms");
                return currentState;
            }

            // process children
            for (State child : currentState.getChildren()) {
                double pathOverCurrentState = G.get(currentState) +
                        currentState.getCurrentCity().distanceTo(child.getCurrentCity());
                if (!C.contains(child) && !Qin.contains(child)) {
                    G.put(child, pathOverCurrentState);
                    Q.add(child);
                    Qin.add(child);
                    parentOptimal.put(child, currentState);
                } // another new path to child
                // path to child over current state is better than the previous optimal path to child
                else if (pathOverCurrentState < G.get(child)) {
                    Q.remove(child);
                    G.put(child, pathOverCurrentState);
                    parentOptimal.put(child, currentState);
                    Q.add(child);
                }
            }
        }

        return null;
    }
}
