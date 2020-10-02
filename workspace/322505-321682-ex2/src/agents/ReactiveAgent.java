package agents;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReactiveAgent implements ReactiveBehavior {

    private static final Double NO_REWARD = null;
    private static final Integer ACCEPT_TASK = -1;

    private double discountFactor;
    private double epsilon;

    private List<State> states;
    private List<Integer> actions;

    private Map<State, Map<Integer, Double>> R;
    private Map<State, Map<Integer, Map<State, Double>>> T;

    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        discountFactor = agent.readProperty("discount-factor", Double.class, 0.95);
        epsilon = agent.readProperty("epsilon", Double.class, 0.0001);

        makeStateSpace(topology);
        makeActionSpace(topology);

        calculateR(distribution, agent, topology);
        calculateT(distribution, topology);

        ReinforcementLearningAlgorithm rla = new ReinforcementLearningAlgorithm(
                states, actions, topology, discountFactor, epsilon, R, T);
        rla.reinforcementLearning();
    }

    /**
     * State space contains two types of states:
     * 1. The agent is in the city i and there is a packet for the city j
     * 2. The agent is in the city i and there are no packets for delivery
     *
     * @param topology
     */
    private void makeStateSpace(Topology topology) {
        //creating set of states
        states = new ArrayList<>();
        // Adding states that represent that:
        // The agent is in a sourceCity and there exists a packet for the destinationCity (size = num_of_cities^2)
        for (City sourceCity : topology.cities()) {
            for (City destinationCity : topology.cities()) {
                if (sourceCity.id != destinationCity.id)
                    states.add(new State(sourceCity, destinationCity));
            }
        }
        //Adding states that represent that:
        //The agent is in the sourceCity and there are no packets to pick up
        for (City sourceCity : topology.cities()) {
            states.add(new State(sourceCity));
        }
    }

    /**
     * Actions space contains two types of actions:
     * 1. go to the city i
     * 2. accept the packet
     *
     * @param topology
     * @return
     */
    private void makeActionSpace(Topology topology) {
        // Adding action: go to city city
        for (City city : topology.cities()) {
            actions.add(city.id);
        }
        // Adding action: accept a packet
        actions.add(ACCEPT_TASK);
    }

    private City getCityById(int id, Topology topology) {
        for (City city : topology.cities()) {
            if (city.id == id)
                return city;
        }
        return null;
    }

    /**
     * R(s,a) represents the instant reward that an agent gets if in the state s does the action a
     * first numberOfActions-1 columns represent "going to the neighbor i"
     * (this happens if an agents refuses a packet or there are no packets in a town)
     * the last column represents the action that an agent accepts the packet
     *
     * @param distribution
     * @param agent
     */
    private void calculateR(TaskDistribution distribution, Agent agent, Topology topology) {
        int transitPrice = agent.vehicles().get(0).costPerKm();
        for (State state : states) {
            Map<Integer, Double> RforFixedState = new HashMap<>();
            for (int action : actions) {
                if (action != ACCEPT_TASK) {
                    // no task or refuse a task
                    City nextCity = getCityById(action, topology);
                    if (state.getCurrentCity().hasNeighbor(nextCity)
                            //FIXME: delete this if I am not a neighbor to myself
                            && state.getCurrentCity().id != nextCity.id) {
                        RforFixedState.put(action, -transitPrice * state.getCurrentCity().distanceTo(nextCity));
                    } else
                        //the action in this state is impossible
                        RforFixedState.put(action, NO_REWARD);
                } else {
                    // accepting a task
                    RforFixedState.put(ACCEPT_TASK,
                            distribution.reward(state.getCurrentCity(), state.getTaskCity())
                                    - transitPrice * state.getCurrentCity().distanceTo(state.getTaskCity()));
                }
            }
            R.put(state, RforFixedState);
        }
    }

    /**
     * FIXME FIXME FIXME!!!
     * T.get(state).get(action).get(state') represents what is the probability that an agent end up in the state'
     * if in state does an action
     *
     * @param distribution
     */
    private void calculateT(TaskDistribution distribution, Topology topology) {
        //calculating the probability that in a city i no packets appear
        //that's the product over every city j
        //of probability that in the city i there is no packet for the city j
        int numberOfCities = topology.cities().size();
        Double[] probNoPackets = new Double[numberOfCities];
        for (int i = 0; i < numberOfCities; i++) {
            probNoPackets[i] = 1d;
            for (int j = 0; j < numberOfCities; j++)
                probNoPackets[i] *= 1 - distribution.probability(topology.cities().get(i), topology.cities().get(j));
        }

        //FIXME DUDA
        for (State initialState : states) {
            //action = accept a package
            if (initialState.getTaskCity() != null) {
                T.get(initialState).get(ACCEPT_TASK).put(new State(initialState.getTaskCity(), null)
                        , probNoPackets[topology.cities().indexOf(initialState.getTaskCity())]);
            } else {
                //TODO this is hard part because we need to calculate these probabilities on paper
                //  if I decide to go to the city i to deliver the packet I might end up in many states
                //  e.g. (i, have packet for 1), (i, have packet for 2) ..
                for (City packetForCity : topology.cities()) {
                    //FIXME
                    T.get(initialState).get(ACCEPT_TASK).put(new State(initialState.getTaskCity(), packetForCity)
                            , 0d);
                }
            }

            for (Integer action : actions) {
                //FIXME merge with ^^^^^^^^^^^^^
                if (initialState.isActionPossible(action, topology))
                    //if the agent decides to move to the neighbor i he will definitely end up in the neighbor city
                    //but again we have many possibilities for states e.g. (i, have packet for 1), (i, have packet for 2)...
                    for (City packetForCity : topology.cities()) {
                        //FIXME
                        T.get(initialState).get(action).put(new State(initialState.getTaskCity(), packetForCity)
                                , 0d);
                    }
            }
        }
    }

    @Override
    public Action act(Vehicle vehicle, Task availableTask) {
        return null;
    }
}
