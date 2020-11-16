package strategy;

import logist.agent.Agent;
import logist.task.Task;
import logist.topology.Topology.City;
import logist.topology.Topology;

import java.util.*;
import java.util.stream.Collectors;

public class StrategyPast {

    private Double epsilon;
    private Topology topology;
    private Map<Integer, double[][]> agentsCosts;
    private Map<City, List<City>> closeCities;
    private Map<City, List<City>> epsilonCloseCities;
    private Agent agent;

    public StrategyPast(Double epsilon, Topology topology, Agent agent) {
        this.epsilon = epsilon;
        this.topology = topology;
        this.agent = agent;
        this.agentsCosts = new HashMap<>();
        calculateCloseCities();
        calculateEpsilonCloseCities();
    }

    private void calculateCloseCities() {
        closeCities = new HashMap<>();
        for (City city : topology.cities()) {
            ArrayList<City> cities = new ArrayList<>(topology.cities());
            //sort cities based on distance from city
            Collections.sort(cities,
                    Comparator.comparingDouble(c -> c.distanceTo(city)));
            //remove itself from the list
            cities.remove(0);
            closeCities.put(city, cities);
        }
    }

    private void calculateEpsilonCloseCities() {
        //biggest distance between cities
        double biggestDistance = 0.0;
        for (City city : topology.cities()) {
            List<City> cities = closeCities.get(city);
            City furthestCity = cities.get(cities.size() - 1);
            if (city.distanceTo(furthestCity) > biggestDistance)
                biggestDistance = city.distanceTo(furthestCity);
        }

        //a city1 is epsilon close to a city2 iff distance(city1, city) < epsilon * biggest distance
        //calculate epsilon close cities
        double distanceThreshold = epsilon * biggestDistance;
        epsilonCloseCities = new HashMap<>();
        for (City city : topology.cities()) {
            List<City> currentEpsilonCloseCities = new ArrayList<>();
            List<City> cities = closeCities.get(city);
            for (int i = 0; i < cities.size() && city.distanceTo(cities.get(i)) < distanceThreshold; i++) {
                currentEpsilonCloseCities.add(cities.get(i));
            }
            epsilonCloseCities.put(city, currentEpsilonCloseCities);
        }
    }


    public Map<City, List<City>> getCloseCities() { return closeCities; }

    public Map<City, List<City>> getEpsilonCloseCities() { return epsilonCloseCities; }

    public Map<Integer, double[][]> getAgentsCosts() { return agentsCosts; }

    public void initializeAgentCosts(int numberOfAgents) {
        int n = topology.cities().size();
        for (int i = 0; i < numberOfAgents; i++) {
            double[][] cityMatrix = new double[n][n];
            agentsCosts.put(i, cityMatrix);
        }
    }
}
