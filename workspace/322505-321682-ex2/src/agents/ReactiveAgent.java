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
import java.util.stream.Collectors;

public class ReactiveAgent implements ReactiveBehavior {

    private static final Double NO_REWARD = null;
    protected static final Integer ACCEPT_TASK = -1;

    private int numActions;
    private Agent myAgent;

    private double discountFactor;
    private double epsilon;

    private List<State> states;
    private List<Integer> actions;

    private Map<State, Map<Integer, Double>> R;
    private Map<State, Map<Integer, Map<State, Double>>> T;

    private Topology topology;
    private ReinforcementLearningAlgorithm rla;

    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        this.topology = topology;
        discountFactor = agent.readProperty("discount-factor", Double.class, 0.95);
        epsilon = agent.readProperty("epsilon", Double.class, 0.0001);
        this.numActions = 0;
        this.myAgent = agent;

        this.states = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.R = new HashMap<>();
        this.T = new HashMap<>();

        makeStateSpace(topology);
        makeActionSpace(topology);

        calculateR(distribution, agent, topology);
        calculateT(distribution, topology);

        rla = new ReinforcementLearningAlgorithm(
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

    public static City getCityById(int id, Topology topology) {
        for (City city : topology.cities()) {
            if (city.id == id)
                return city;
        }
        return null;
    }

    /**
     * @param state
     * @param action
     * @param topology
     * @return
     */
    public static boolean isActionPossible(State state, Integer action, Topology topology) {
        if (action == ACCEPT_TASK) {  // there are no packets
            //Accepting is possible if there is a packet to be delivered
            return state.getTaskCity() != null;
        }
        //Refusing or simply moving to another city is possible if it is neighboring city
        return state.getCurrentCity().hasNeighbor(getCityById(action, topology));
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
                        RforFixedState.putIfAbsent(action, -transitPrice * state.getCurrentCity().distanceTo(nextCity));
                    } else
                        //the action in this state is impossible
                        RforFixedState.putIfAbsent(action, NO_REWARD);
                } else if (state.getTaskCity() != null) {
                    // accepting a task
                    RforFixedState.putIfAbsent(ACCEPT_TASK,
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

        for (State initialState : states) {
            for (Integer action : actions) {
                for (State nextState : states) {
                    //if the agent decides to accept the task, the destination city of the current state and
                    //the current city of the next state must be the same
                    if (action.equals(ACCEPT_TASK) && initialState.getTaskCity() == nextState.getCurrentCity()
                            // if the agent decides to refuse the packet or there are no tasks he must end up in the desired city
                            // (the destination of the desired action and the town the agent end up in must be the same)
                            // and he must move to a neighboring city
                            || !action.equals(ACCEPT_TASK) && nextState.getCurrentCity().id == action
                            && initialState.getCurrentCity().hasNeighbor(nextState.getCurrentCity())) {

                        T.putIfAbsent(initialState, new HashMap<>());
                        T.get(initialState).putIfAbsent(action, new HashMap<>());
                        if (nextState.getTaskCity() != null) {
                            //there are no packets in the next state
                            T.get(initialState).get(action).put(nextState, distribution.probability(nextState.getCurrentCity(), null));
                        } else {
                            T.get(initialState).get(action).
                                    put(nextState, calculateProbability(distribution, nextState.getCurrentCity(), nextState.getTaskCity(), topology));
                        }

                    }
                }
            }
        }
    }


    /**
     * Calculate the probability that the agent in the city currentCity sees a packet
     * for delivery to city nextCity
     * According to the Logi
     *
     * @param distribution
     * @param currentCity
     * @param nextCity
     * @return
     */
    private Double calculateProbability(TaskDistribution distribution, City currentCity, City nextCity, Topology topology) {
        Double probability = 0d;
        List<City> filteredCities = topology.cities().stream().filter(city -> !city.equals(currentCity) && !city.equals(nextCity)).collect(Collectors.toList());
        String current = "0".repeat(filteredCities.size());
        String finished = "1".repeat(filteredCities.size());
        long numberOfCitiesInSubset;

        while (current.equals(finished)) {
            Double currentProbability = 1d;
            for (int i = 0; i < current.length(); i++) {
                if (current.charAt(i) == '1') {
                    currentProbability *= distribution.probability(currentCity, filteredCities.get(i));
                } else {
                    currentProbability *= 1 - distribution.probability(currentCity, filteredCities.get(i));
                }
            }
            numberOfCitiesInSubset = current.chars().filter(bit -> bit == '1').count();
            probability += 1 / (numberOfCitiesInSubset + 1) * distribution.probability(currentCity, nextCity) * currentProbability;
            current = Integer.toBinaryString(Integer.valueOf(current, 2) + 1);
        }
        return probability;
    }

    @Override
    public Action act(Vehicle vehicle, Task availableTask) {
        System.out.println("act");
        Action action;

        City currentCity = vehicle.getCurrentCity();
        City deliveryCity = availableTask != null ? availableTask.deliveryCity : null;
        State currentState = new State(currentCity, deliveryCity);

        Integer bestAction = rla.getBest().get(currentState);
        if (availableTask != null && bestAction.equals(ACCEPT_TASK)) {
            action = new Action.Pickup(availableTask);
        } else {
            action = new Action.Move(getCityById(bestAction, topology));
        }

        if (numActions >= 1) {
            System.out.println("The total profit after " + numActions + " actions is " + myAgent.getTotalProfit() +
                    " (average profit: " + (myAgent.getTotalProfit() / (double) numActions) + ")");
        }
        numActions++;

        return action;
    }
}
