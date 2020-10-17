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

    public AStar(Set<Task> availableTaskSet, Set<Task> carriedTaskSet, Vehicle vehicle,
                 String heuristicName) {
        super(availableTaskSet, carriedTaskSet, vehicle);

        this.Q = new PriorityQueue<>(new Comparator<State>() {
            @Override
            public int compare(State s1, State s2) {
                if (!F.get(s1).equals(F.get(s2)))
                    return F.get(s1).compareTo(F.get(s2));
                return s1.getId().compareTo(s2.getId());
            }
        });
        this.C = new HashSet<>();
        this.H = new HashMap<>();
        this.F = new HashMap<>();
    }

    private Double calculateF(State state) {
        return G.get(state) + state.getH();
    }

    @Override
    public State getGoalState() {
        G.put(rootState, 0d);
        F.put(rootState, calculateF(rootState));
        Q.add(rootState);

        State currentState;
        State goalState = null;

        //while there are unprocessed states
        while (!Q.isEmpty()) {
            visitedStates++;
            currentState = Q.remove();

            if (currentState.isFinalState()) {
                goalState = currentState;
                break;
            }

            //process children
            Set<State> children = currentState.getChildren();
            for (State child : children) {
                double pathOverCurrentState = G.get(currentState) +
                        currentState.getCurrentCity().distanceTo(child.getCurrentCity());
                if (!Q.contains(child) && !C.contains(child)) {
                    G.put(child, pathOverCurrentState);
                    F.put(child, calculateF(child));
                    Q.add(child);
                    parentOptimal.put(child, currentState);
                } else {
                    //another new path to child
                    //path to child over current state is better than the previous optimal path to child
                    if (pathOverCurrentState < G.get(child)) {
                        if(Q.contains(child))
                            Q.remove(child);
                        G.put(child, pathOverCurrentState);
                        F.put(child, calculateF(child));
                        parentOptimal.put(child, currentState);
                        if (C.contains(child)) {
                            C.remove(child);
                        }
                        Q.add(child);
                    }
                }
            }
            C.add(currentState);
        }
        return goalState;
    }



}
