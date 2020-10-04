package agents;

import logist.topology.Topology;
import agents.State;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReinforcementLearningAlgorithm {

    private List<State> states;
    private List<Integer> actions;
    private Topology topology;
    private double discountFactor;
    private double epsilon;

    private Map<State, Map<Integer, Double>> R;
    private Map<State, Map<Integer, Map<State, Double>>> T;

    // best action from a state
    private Map<State, Integer> best;
    // best action and its accumulated value
    private Map<State, Double> V;
    private Map<State, Map<Integer, Double>> Q;

    public ReinforcementLearningAlgorithm(List<State> states, List<Integer> actions, Topology topology,
                                          double discountFactor, double epsilon, Map<State,
            Map<Integer, Double>> R, Map<State, Map<Integer, Map<State, Double>>> T) {
        this.states = states;
        this.actions = actions;
        this.topology = topology;
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

        while (true) {
            for (State state : Q.keySet()) {
                for (Integer action : Q.get(state).keySet()) {
                    double sum = 0.0;

                    for (State nextState : State.getAllNextStates(action, topology)) {
                        sum += discountFactor * T.get(state).get(action).get(nextState) * V.get(nextState);
                    }

                    double value = R.get(state).get(action) + sum;
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

                //  algorithm stops whenever there is no more a change in V
                if (Math.abs(V.get(state) - bestValue) < epsilon) {
                    break;
                }

                best.put(state, bestAction);
                V.put(state, bestValue);
            }
        }
    }

    public Map<State, Integer> getBest() {
        return best;
    }

    public Map<State, Double> getV() {
        return V;
    }

    public Map<State, Map<Integer, Double>> getQ() {
        return Q;
    }
}
