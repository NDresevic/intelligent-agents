package strategy;

import models.TaskModel;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;
import logist.topology.Topology;

import java.awt.geom.Arc2D;
import java.util.*;
import java.util.stream.Collectors;

public class StrategyPast {

    private Double epsilon;
    private Topology topology;
    //agent id -> information about the agent
    //the matrix has dimensions cities x cities (index is city id)
    //the matrix approximate the information: for t.pickup = i and t.delivery = j what the agent would bif
    private Map<Integer, Double[][]> agentsCosts;
    //maps city id to city
    private Map<Integer, City> idToCity;
    //city -> list of cities ordered by distance to city
    private Map<City, List<City>> closeCities;
    //city -> list of ids of cities that are epsilon-close to city ordered by distance
    //a city1 is epsilon close to a city2 iff distance(city1, city) < epsilon * biggest distance between 2 cities
    private Map<City, List<Integer>> epsilonCloseCitiesId;
    private Agent agent;
    //approximation for cost per kilometer for other agents
    private double approximatedVehicleCost;

    public StrategyPast(Double epsilon, Topology topology, Agent agent) {
        this.epsilon = epsilon;
        this.topology = topology;
        this.agent = agent;
        this.agentsCosts = new HashMap<>();
        //approximate that all vehicles from other agents have average capacity of the agent vehicles
        this.approximatedVehicleCost = agent.vehicles().stream().map(Vehicle::costPerKm).reduce(0, Integer::sum) * 1.0
                / agent.vehicles().size();
        initializeIdToCity();
        calculateCloseCities();
        calculateEpsilonCloseCities();
    }

    private void initializeIdToCity() {
        idToCity = new HashMap<>();
        for (City city : topology.cities()) {
            idToCity.put(city.id, city);
        }
    }

    private void calculateCloseCities() {
        closeCities = new HashMap<>();
        for (City city : topology.cities()) {
            ArrayList<City> cities = new ArrayList<>(topology.cities());
            //sort cities based on distance from city
            Collections.sort(cities,
                    Comparator.comparingDouble(c -> c.distanceTo(city)));
            //remove itself from the list
            //todo: maybe we don't want to remove it
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
        epsilonCloseCitiesId = new HashMap<>();
        for (City city : topology.cities()) {
            List<City> currentEpsilonCloseCities = new ArrayList<>();
            List<City> cities = closeCities.get(city);
            for (int i = 0; i < cities.size() && city.distanceTo(cities.get(i)) < distanceThreshold; i++) {
                currentEpsilonCloseCities.add(cities.get(i));
            }
            epsilonCloseCitiesId.put(city, currentEpsilonCloseCities.stream().map(c -> c.id).
                    collect(Collectors.toList()));
        }
    }


    public Map<City, List<City>> getCloseCities() {
        return closeCities;
    }

    public Map<City, List<Integer>> getEpsilonCloseCitiesId() {
        return epsilonCloseCitiesId;
    }

    public Map<Integer, Double[][]> getAgentsCosts() {
        return agentsCosts;
    }

    public void initializeAgentCosts(int numberOfAgents) {
        int n = topology.cities().size();
        for (int i = 0; i < numberOfAgents; i++) {
            Double[][] cityMatrix = new Double[n][n];
            agentsCosts.put(i, cityMatrix);
        }
    }

    public void updateTables(Task lastTask, int lastWinner, Long[] lastOffers) {
        for (int i = 0; i < lastOffers.length; i++) {
            Double[][] costs = agentsCosts.get(i);
            double futureBid;
            if(lastWinner == i)
                //TODO: maybe this should not be zero since he has limited capacity
                futureBid = 0.0;
            else if (lastOffers[i] == null)
                futureBid = Double.MAX_VALUE;
            else
                futureBid = lastOffers[i];

            //fill the matrix with the other bidder bid
            costs[lastTask.pickupCity.id][lastTask.deliveryCity.id] = futureBid;

            //fill the matrix for similar tasks i.e. tasks with the same pickup city and close delivery city or vice versa
            double taskDistance = lastTask.pickupCity.distanceTo(lastTask.deliveryCity);
            //first it is filled for epsilon close delivery city
            List<Integer> epsilonCloseCitiesDelivery = epsilonCloseCitiesId.get(lastTask.deliveryCity);
            for (Integer id : epsilonCloseCitiesDelivery) {
                City epsilonCloseCity = idToCity.get(id);
                double approxCost = futureBid == Double.MAX_VALUE ?
                        Double.MAX_VALUE : futureBid + approximatedVehicleCost * epsilonCloseCity.distanceTo(lastTask.deliveryCity);
                costs[lastTask.pickupCity.id][epsilonCloseCity.id] =
                        costs[lastTask.pickupCity.id][epsilonCloseCity.id] == null ?
                                //take the information from the bidder as the truth since we don't have any information from the past
                                approxCost :
                                //we have some information from the past, the other bidder would bid minimum of previous and approximated info
                                Math.min(costs[lastTask.pickupCity.id][epsilonCloseCity.id], approxCost);
            }
            //next, the same reasoning for epsilon close pickup city
            List<Integer> epsilonCloseCitiesPickup = epsilonCloseCitiesId.get(lastTask.pickupCity);
            for (Integer id : epsilonCloseCitiesPickup) {
                City epsilonCloseCity = idToCity.get(id);
                double approxCost = futureBid == Double.MAX_VALUE ?
                        Double.MAX_VALUE : lastOffers[i] + approximatedVehicleCost * epsilonCloseCity.distanceTo(lastTask.pickupCity);
                costs[epsilonCloseCity.id][lastTask.deliveryCity.id] =
                        costs[epsilonCloseCity.id][lastTask.deliveryCity.id] == null ?
                                //take the information from the bidder as the truth since we don't have any information from the past
                                approxCost :
                                //we have some information from the past, the other bidder would bid minimum of previous and approximated info
                                Math.min(costs[epsilonCloseCity.id][lastTask.deliveryCity.id], approxCost);
            }

            //todo: do I need this
            agentsCosts.put(i, costs);
        }
    }

    public Long extractBidPriceForOthers(Task task){
        Double minBid = Double.MAX_VALUE;
        for (Map.Entry<Integer, Double[][]> entry : agentsCosts.entrySet()) {
           Double bid = entry.getValue()[task.pickupCity.id][task.deliveryCity.id];
           if (bid != null && bid < minBid)
               minBid = bid;
        }
        return minBid != Double.MAX_VALUE ? (long) Math.ceil(minBid) : null;
    }
}
