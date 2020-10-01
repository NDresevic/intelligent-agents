package agents;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;
import model.State;

import java.util.List;
import java.util.Map;
import java.util.Vector;

public class ReactiveAgent implements ReactiveBehavior {

    private static final Double NO_REWARD = null;
    private double discountFactor;

    private List<State> states;
    private List<Integer> actions;
    private Topology topology;

    private Map<State, Map<Integer, Double>> R;
    private Map<State, Map<Integer, Map<State, Double>>> T;

    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        discountFactor = agent.readProperty("discount-factor", Double.class, 0.95);

        //creating set of states
        Vector<State> states = new Vector<>();
        //I am in the sourceCity and there exists a packet for the destinationCity (size = num_of_cities^2)
        for (City sourceCity : topology.cities()) {
            for (City destinationCity : topology.cities()) {
                states.add(new State(sourceCity, destinationCity));
            }
        }
        //The agent is in the sourceCity and there are no packets
        for (City sourceCity : topology.cities()) {
            states.add(new State(sourceCity));
        }

        int numberOfStates = states.size();
        int numberOfCities = topology.cities().size();
        int numberOfActions = topology.cities().size() + 1;

        //R(s,a) represents the instant reward that an agent gets if in the state s does the action a
        //first numberOfActions-1 columns represent "going to the neighbor i"
        //(this happens if an agents refuses a packet or there are no packets in a town)
        //the last column represents the action that an agent accepts the packet
        Double[][] R = new Double[numberOfStates][numberOfActions];
        for (int i = 0; i < numberOfStates; i++) {
            State state = states.get(i);
            int transitPrice = agent.vehicles().get(0).costPerKm();
            //accepting a task
            R[i][numberOfActions - 1] = distribution.reward(state.getCurrentCity(), state.getTaskCity())
                    - transitPrice * state.getCurrentCity().distanceTo(state.getTaskCity());
            //no task or refuse a task
            for (int j = 0; j < numberOfActions - 1; j++) {
                City nextCity = topology.cities().get(j);
                if (state.getCurrentCity().hasNeighbor(nextCity)
                        //delete this if I am not the neighbor to myself
                        && state.getCurrentCity().id != nextCity.id) {
                    R[i][j] = -transitPrice * state.getCurrentCity().distanceTo(nextCity);
                } else
                    R[i][j] = NO_REWARD;

            }
        }

        //calculating the probability that in a city i no packets appear
        //that's the product over every city j
        // of probabilities that in the city i there is no packet for the city j
        Double[] probNoPackets = new Double[numberOfCities];
        for (int i = 0; i < numberOfCities; i++) {
            probNoPackets[i] = 1d;
            for (int j = 0; j < numberOfCities; j++)
                probNoPackets[i] *= 1 - distribution.probability(topology.cities().get(i), topology.cities().get(j));
        }

        //T[s][a][s'] represents what is the probability that an agent end up in the state
        //s' if in state s does an action a
        Double[][][] T = new Double[numberOfStates][numberOfActions][numberOfStates];
        for (int i = 0; i < numberOfStates; i++) {
            State initialState = states.get(i);
            //action = accept a package
            if (initialState.getTaskCity() != null) {
                T[i][numberOfActions - 1][states.indexOf(new State(initialState.getTaskCity(), null))]
                        = probNoPackets[topology.cities().indexOf(initialState.getTaskCity())];
            } else {
                //TODO this is hard part because we need to calculate these probabilities on paper
                //  if I decide to go to the city i to deliver the packet I might end up in many states
                //  e.g. (i, have packet for 1), (i, have packet for 2) ..
                for (City packetForCity : topology.cities()) {
                    //FIXME
                    T[i][numberOfActions - 1][states.indexOf(new State(initialState.getTaskCity(), packetForCity))] = 0d;
                }
            }

            for (int j = 0; j < numberOfActions - 1; j++) {
                if (initialState.isActionPossible(j, topology))
                    //if the agent decides to move to the neighbor i he will definitely end up in the neighbor city
                    //but again we have many possibilities for states e.g. (i, have packet for 1), (i, have packet for 2)...
                    for (City packetForCity : topology.cities()) {
                        //FIXME
                        T[i][j][states.indexOf(new State(initialState.getTaskCity(), packetForCity))] = 0d;
                    }
            }
        }
    }

    @Override
    public Action act(Vehicle vehicle, Task availableTask) {
        return null;
    }
}
