package strategy;

import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;
import logist.topology.Topology;

import java.util.*;
import java.util.stream.Collectors;

public class AgentsBidStrategy {

    private Double epsilon;
    private Topology topology;
    // [agent id -> approximate bid for t.pickup = i and t.delivery = j]
    // dimension of matrix is cities X cities (index is city id)
    private Map<Integer, Double[][]> agentEstimatedCostsMap;
    // [city id -> city]
    private Map<Integer, City> idCityMap;
    // [city -> list of cities sorted ascending by distance to city]
    private Map<City, List<City>> closeCitiesMap;
    // [city -> list of ids of cities that are epsilon-close to city ordered by distance]
    // city1 is epsilon close to a city2 iff distance(city1, city) < epsilon * biggest distance between 2 cities
    private Map<City, List<Integer>> epsilonCloseCityIdsMap;
    // approximation for cost per kilometer for other agents
    private double approximatedVehicleCost;

    public AgentsBidStrategy(Double epsilon, Topology topology, Agent agent) {
        this.epsilon = epsilon;
        this.topology = topology;
        this.agentEstimatedCostsMap = new HashMap<>();
        this.idCityMap = new HashMap<>();
        this.closeCitiesMap = new HashMap<>();
        this.epsilonCloseCityIdsMap = new HashMap<>();
        // approximate that all vehicles from other agents have average capacity of our agent vehicles
        this.approximatedVehicleCost = agent.vehicles().stream().map(Vehicle::costPerKm).reduce(0, Integer::sum)
                * 1.0 / agent.vehicles().size();

        this.initializeCityAndCostMaps();
    }

    // todo: comment
    private void initializeCityAndCostMaps() {
        // biggest distance between cities
        double biggestDistance = Double.MIN_VALUE;

        for (City city : topology.cities()) {
            idCityMap.put(city.id, city);

            ArrayList<City> cities = new ArrayList<>(topology.cities());
            // sort cities based on distance from city
            cities.sort(Comparator.comparingDouble(c -> c.distanceTo(city)));
            // remove itself from the list
            cities.remove(0);
            closeCitiesMap.put(city, cities);

            City furthestCity = cities.get(cities.size() - 1);
            biggestDistance = Math.max(city.distanceTo(furthestCity), biggestDistance);
        }

        // city1 is epsilon close to a city2 iff distance(city1, city) < epsilon * biggest distance
        // calculate epsilon close cities
        double distanceThreshold = epsilon * biggestDistance;
        for (City city : topology.cities()) {
            List<City> currentEpsilonCloseCities = new ArrayList<>();
            List<City> cities = closeCitiesMap.get(city);
            for (int i = 0; i < cities.size() && city.distanceTo(cities.get(i)) < distanceThreshold; i++) {
                currentEpsilonCloseCities.add(cities.get(i));
            }

            epsilonCloseCityIdsMap.put(city, currentEpsilonCloseCities.stream().map(c -> c.id)
                    .collect(Collectors.toList()));
        }
    }

    public void initializeAgentCosts(int numberOfAgents) {
        int n = topology.cities().size();
        for (int i = 0; i < numberOfAgents; i++) {
            Double[][] cityMatrix = new Double[n][n];
            agentEstimatedCostsMap.put(i, cityMatrix);
        }
    }

    // todo: comment
    public void updateTables(Task lastTask, int lastWinner, Long[] lastOffers) {
        for (int i = 0; i < lastOffers.length; i++) {
            Double[][] costs = agentEstimatedCostsMap.get(i);
            double futureBid;
            //TODO: maybe this should not be zero since he has limited capacity, maybe change how we update
            if (lastWinner == i) {
                futureBid = 0.0;
            } else if (lastOffers[i] == null) {
                futureBid = Double.MAX_VALUE;
            } else {
                futureBid = lastOffers[i];
            }

            // fill the matrix with the other bidder bid
            costs[lastTask.pickupCity.id][lastTask.deliveryCity.id] = futureBid;

            // fill the matrix for similar tasks (tasks with the same pickup city and close delivery city or vice versa)
            // first it is filled for epsilon close delivery city
            List<Integer> epsilonCloseCitiesDelivery = epsilonCloseCityIdsMap.get(lastTask.deliveryCity);
            for (Integer id : epsilonCloseCitiesDelivery) {
                City epsilonCloseCity = idCityMap.get(id);
                double approxCost = futureBid == Double.MAX_VALUE ? Double.MAX_VALUE
                        : futureBid + approximatedVehicleCost * epsilonCloseCity.distanceTo(lastTask.deliveryCity);
                costs[lastTask.pickupCity.id][epsilonCloseCity.id] =
                        costs[lastTask.pickupCity.id][epsilonCloseCity.id] == null ?
                                // use bidder information as the truth since we don't have any information from the past
                                approxCost :
                                // bid minimum of previous and approximated info
                                Math.min(costs[lastTask.pickupCity.id][epsilonCloseCity.id], approxCost);
            }
            // next, the same reasoning for epsilon close pickup city
            List<Integer> epsilonCloseCitiesPickup = epsilonCloseCityIdsMap.get(lastTask.pickupCity);
            for (Integer id : epsilonCloseCitiesPickup) {
                City epsilonCloseCity = idCityMap.get(id);
                double approxCost = futureBid == Double.MAX_VALUE ? Double.MAX_VALUE
                        : lastOffers[i] + approximatedVehicleCost * epsilonCloseCity.distanceTo(lastTask.pickupCity);
                costs[epsilonCloseCity.id][lastTask.deliveryCity.id] =
                        costs[epsilonCloseCity.id][lastTask.deliveryCity.id] == null ?
                                // use bidder information as the truth since we don't have any information from the past
                                approxCost :
                                // bid minimum of previous and approximated info
                                Math.min(costs[epsilonCloseCity.id][lastTask.deliveryCity.id], approxCost);
            }

            agentEstimatedCostsMap.put(i, costs);
        }
    }

    public Long extractBidPriceForOthers(Task task) {
        Double minBid = Double.MAX_VALUE;
        for (Map.Entry<Integer, Double[][]> entry : agentEstimatedCostsMap.entrySet()) {
            Double bid = entry.getValue()[task.pickupCity.id][task.deliveryCity.id];
            if (bid != null && bid < minBid) {
                minBid = bid;
            }
        }

        return minBid != Double.MAX_VALUE ? (long) Math.ceil(minBid) : null;
    }

    public Map<Integer, Double[][]> getAgentEstimatedCostsMap() {
        return agentEstimatedCostsMap;
    }
}
