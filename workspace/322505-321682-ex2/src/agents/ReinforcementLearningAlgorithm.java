package agents;

import logist.topology.Topology;

import java.util.*;

public class ReinforcementLearningAlgorithm {

    private Set<State> states;
    private Set<Integer> actions;
    private double discountFactor;
    private double epsilon;

    private Map<State, Map<Integer, Double>> R;
    private Map<State, Map<Integer, Map<State, Double>>> T;

    // best action from a state
    private Map<State, Integer> best;
    // best action and its accumulated value
    private Map<State, Double> V;
    private Map<State, Map<Integer, Double>> Q;

    public ReinforcementLearningAlgorithm(Set<State> states, Set<Integer> actions, Topology topology,
                                          double discountFactor, double epsilon, Map<State, Map<Integer, Double>> R,
                                          Map<State, Map<Integer, Map<State, Double>>> T) {
        this.states = states;
        this.actions = actions;
        this.discountFactor = discountFactor;
        this.epsilon = epsilon;
        this.R = R;
        this.T = T;

        best = new HashMap<>();
        V = new HashMap<>();
        Q = new HashMap<>();
        for (State state : this.states) {
            V.put(state, 0.0);
            Q.put(state, new HashMap<>());

            for (Integer action : this.actions) {
                if (ReactiveAgent.isActionPossible(state, action, topology)) {
                    Q.get(state).put(action, 0.0);
                }
            }
        }
    }

    public void reinforcementLearning() {
        int steps = 0;

        while (true) {
            double maxDifference = 0.0;

            for (State state : Q.keySet()) {
                for (Integer action : Q.get(state).keySet()) {
                    double value = R.get(state).get(action);
                    for (State nextState: T.get(state).get(action).keySet()) {
                        value += discountFactor * T.get(state).get(action).get(nextState) * V.get(nextState);
                    }

                    Q.get(state).put(action, value);
                }

                Integer bestAction = null;
                double bestValue = Double.MIN_VALUE;
                for (Integer action : Q.get(state).keySet()) {
                    double currentValue = Q.get(state).get(action);
                    if (currentValue > bestValue) {
                        bestAction = action;
                        bestValue = currentValue;
                    }
                }

                maxDifference = Math.max(Math.abs(V.get(state) - bestValue), maxDifference);
                best.put(state, bestAction);
                V.put(state, bestValue);
            }

            //  algorithm stops whenever there is no significant change in V
            if (maxDifference < epsilon) {
                System.out.println("maxDifference = " + maxDifference);
                break;
            }
            steps++;
        }

        System.out.println("Number of steps: " + steps);
        System.out.println("Best(S): " + best);
        System.out.println("V(S): " + V);
        System.out.println("2 * epsilon * y / (1 - y) = " + 2*epsilon*discountFactor/(1-discountFactor));
    }

    public Map<State, Integer> getBest() {
        return best;
    }
}
