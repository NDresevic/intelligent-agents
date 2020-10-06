package agents;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;
import java.util.stream.Collectors;

public class ReactiveAgent implements ReactiveBehavior {

    private static final Integer ACCEPT_TASK = -1;

    private int numActions;
    private Agent myAgent;

    private double discountFactor;
    private double epsilon;

    private Set<State> states;
    private Set<Integer> actions;

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

        this.states = new HashSet<>();
        this.actions = new HashSet<>();
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
        for (City sourceCity : topology.cities()) {
            // agent is in the sourceCity and there is a packet to deliver to destinationCity
            for (City destinationCity : topology.cities()) {
                if (sourceCity.id != destinationCity.id)
                    states.add(new State(sourceCity, destinationCity));
            }

            // agent is in the sourceCity and there are no packets to pick up
            states.add(new State(sourceCity));
        }
    }

    /**
     * Actions space contains two types of actions:
     * 1. go to the city i without the package
     * 2. accept the packet
     *
     * @param topology
     * @return
     */
    private void makeActionSpace(Topology topology) {
        // go to each city without a package
        for (City city : topology.cities()) {
            actions.add(city.id);
        }
        // accept task
        actions.add(ACCEPT_TASK);
    }

    private static City getCityById(int id, Topology topology) {
        for (City city : topology.cities()) {
            if (city.id == id)
                return city;
        }
        return null;
    }


    public static boolean isActionPossible(State state, Integer action, Topology topology) {
        if (action.equals(ACCEPT_TASK)) { // accept the task and the destination city exists
            return state.getTaskCity() != null;
        }
        // refusing or simply moving to another city is possible if it is neighboring city
        return state.getCurrentCity().hasNeighbor(getCityById(action, topology));
    }

    /**
     * R(s,a) represents the instant reward that an agent gets if in the state s does the action a. If the agent is
     * going to neighbouring city without a package, the reward is negative cost of transit to neighbouring city. The
     * other case is that agent is accepting the package and going to the destination city. There, the reward is the
     * reward for the task minus the cost of the transition to the destination city.
     *
     * @param distribution
     * @param agent
     */
    private void calculateR(TaskDistribution distribution, Agent agent, Topology topology) {
        // note: we assume that we only have one company and one vehicle (reactive agent)
        int transitPrice = agent.vehicles().get(0).costPerKm();
        for (State state : states) {
            Map<Integer, Double> RforFixedState = new HashMap<>();
            for (int action : actions) {
                City nextCity = getCityById(action, topology);

                if (action != ACCEPT_TASK && nextCity != null &&
                        state.getCurrentCity().hasNeighbor(nextCity)) {     // no task or refuse a task
                    RforFixedState.put(action, -transitPrice * state.getCurrentCity().distanceTo(nextCity));
                } else if (action == ACCEPT_TASK && state.getTaskCity() != null) {  // accepting a task
                    RforFixedState.put(ACCEPT_TASK,
                            distribution.reward(state.getCurrentCity(), state.getTaskCity())
                                    - transitPrice * state.getCurrentCity().distanceTo(state.getTaskCity()));
                }
            }
            R.put(state, RforFixedState);
        }
    }

    /**
     * T.get(s).get(a).get(s') represents the probability that an agent ends up in the state s' if it is in state s and
     * does an action a.
     *
     * @param distribution
     */
    private void calculateT(TaskDistribution distribution, Topology topology) {
        for (State initialState : states) {
            for (Integer action : actions) {

                for (State nextState : states) {
                    // if the agent decides to accept the task, the destination city of the current state and
                    // the current city of the next state must be the same
                    if ((action.equals(ACCEPT_TASK) && initialState.getTaskCity() == nextState.getCurrentCity())
                            // if the agent decides to refuse the task or there are no tasks its action represents the
                            // next city it wants to go to, plus that next city needs to be neighbour city of the city
                            // it is currently at
                            || (!action.equals(ACCEPT_TASK) && nextState.getCurrentCity().id == action
                            && initialState.getCurrentCity().hasNeighbor(nextState.getCurrentCity()))) {

                        T.putIfAbsent(initialState, new HashMap<>());
                        T.get(initialState).putIfAbsent(action, new HashMap<>());

                        //distribution.probability(city, null) gives the probability that there are no task in city
                        T.get(initialState).get(action).put(nextState,
                                distribution.probability(nextState.getCurrentCity(), nextState.getTaskCity()));

                        // in the case we actually need what is written in the documentation
//                        if (nextState.getTaskCity() != null) { // there are no packets in the next state
//                            T.get(initialState).get(action).put(nextState,
//                                    distribution.probability(nextState.getCurrentCity(), null));
//                        } else {
//                            T.get(initialState).get(action).
//                                    put(nextState, calculateProbability(distribution, nextState.getCurrentCity(),
//                                            nextState.getTaskCity(), topology));
//                        }
                    }
                }
            }
        }
    }

    /**
     * In the logist documentation regarding tasks xml it is said that the distribution tag represents relative
     * probability of a task. However, the meaning of relative is not defined and, additionally, there is
     * a paragraph that says that if multiple tasks are available in the city, the logist platform chooses
     * one randomly to show it to the agent. At first, we thought that this probability distribution is
     * independent for each pair (city1, city2), e.g. if distribution="uniform" all p(i,j) are drawn from
     * the same uniform distribution independently. Following method calculates the probability that in THAT
     * case the agent in city1 SEES for the delivery the packet for city2.
     * Summing up the distribution.probability(x,y) for fixed x and all possible y (including null) we got
     * the result 1 for all x. There we understood the meaning of "relative", but we are still confused
     * because of the paragraph about multiple tasks. We believe that this is just given as an explanation
     * that everything behind the scenes in the logist part is already calculated in proper manner
     * @param distribution
     * @param currentCity
     * @param nextCity
     * @return
     */
    private Double calculateProbability(TaskDistribution distribution, City currentCity, City nextCity, Topology topology) {
        //calculate the probability that over all possible choices of shown packets the packet from currentCity
        //  to nextCity is chosen
        //iterate over all subsets of cities that contain the city currentCity
        //if city x is presented in the subset that means that the task for city x is shown to the logist
        double probability = 0d;
        List<City> filteredCities = topology.cities().stream().filter(
                city -> !city.equals(currentCity) && !city.equals(nextCity)).collect(Collectors.toList());
        //current contains of 0s and 1s, if the i-th bit is 1, it means that i-th city in the filtered list
        //is in the subset
        String current = "0".repeat(filteredCities.size());
        String finished = "1".repeat(filteredCities.size());
        //number of packages shown to the logist
        long numberOfCitiesInSubset;

        while (!current.equals(finished)) {
            //currentProbability accumulates the probability of having particular subset of packets
            double currentProbability = 1d;
            for (int i = 0; i < current.length(); i++) {
                if (current.charAt(i) == '1') {
                    //multiplying with probability that the packet for i-th city is shown
                    currentProbability *= distribution.probability(currentCity, filteredCities.get(i));
                } else {
                    //multiplying with probability that the packet for i-th city is not shown
                    currentProbability *= 1 - distribution.probability(currentCity, filteredCities.get(i));
                }
            }

            // add 1 because the packet for nextCity is definitely in the subset
            numberOfCitiesInSubset = current.chars().filter(bit -> bit == '1').count() + 1;

            //add probability for this particular subset
            // divide by numberOfCitiesInSubset because logist chooses one packet from the subset randomly
            // multiplied by probability(currentCity, nextCity) because the packet for nextCity is definitely in the subset
            // multiplied by current probability to have chosen subset
            probability += 1.0 / (numberOfCitiesInSubset) * distribution.probability(currentCity, nextCity) * currentProbability;

            //prepare bits for next iteration
            String temp = Integer.toBinaryString(Integer.valueOf(current, 2) + 1);
            current = "0".repeat(filteredCities.size() - temp.length()) + temp;
        }
        return probability;
    }

    /**
     * Determines the action the agent takes when arriving to a city. If best action for the current state is to accept
     * the task and the task exists, the action is PICKUP. Otherwise, the agent moves to the best neighbouring city.
     * @param vehicle
     * @param availableTask
     * @return
     */
    @Override
    public Action act(Vehicle vehicle, Task availableTask) {
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
            System.out.println("Agent id " + myAgent.id() + ": The total profit after " + numActions + " actions is " + myAgent.getTotalProfit() +
                    " (average profit: " + (myAgent.getTotalProfit() / (double) numActions) + ")");
        }
        numActions++;

        return action;
    }
}
