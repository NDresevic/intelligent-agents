package strategy;

import logist.task.Task;
import logist.topology.Topology.City;
import logist.topology.Topology;

import java.util.*;
import java.util.stream.Collectors;

public class AgentsBidStrategy {

    //TODO: EXPLAIN what is epsilon
    private Double epsilon;
    private Topology topology;
    //the id of the agent
    private Integer agentId;
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
    //approximate what other bidders would bid for the last task
    // approximation for cost per kilometer for other agents
    private double approximatedVehicleCost;
    // biggest distance between 2 cities
    private long biggestCityDistance;

    public AgentsBidStrategy(Double epsilon, Topology topology, double approximatedVehicleCost, Integer agentId) {
        this.epsilon = epsilon;
        this.topology = topology;
        this.agentId = agentId;
        this.approximatedVehicleCost = approximatedVehicleCost;
        this.agentEstimatedCostsMap = new HashMap<>();
        this.idCityMap = new HashMap<>();
        this.closeCitiesMap = new HashMap<>();
        this.epsilonCloseCityIdsMap = new HashMap<>();
        this.biggestCityDistance = Long.MIN_VALUE;
        this.initializeCityAndDistanceMaps();
    }

    /**
     * initializes maps: idCityMap, closeCitiesMap and epsilonCloseCityIdsMap
     */
    private void initializeCityAndDistanceMaps() {
        for (City city : topology.cities()) {
            idCityMap.put(city.id, city);

            ArrayList<City> cities = new ArrayList<>(topology.cities());
            // sort cities based on distance from city
            cities.sort(Comparator.comparingDouble(c -> c.distanceTo(city)));
            // remove itself from the list
            cities.remove(0);
            closeCitiesMap.put(city, cities);

            City furthestCity = cities.get(cities.size() - 1);
            biggestCityDistance = (long) Math.max(city.distanceTo(furthestCity), biggestCityDistance);
        }

        // city1 is epsilon close to a city2 iff distance(city1, city) < epsilon * biggest distance
        // calculate epsilon close cities
        double distanceThreshold = epsilon * biggestCityDistance;
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

    /**
     * initialize the matrix of bidding approximations for other agents
     * @param numberOfAgents number of agents that participate in bidding
     */
    public void initializeAgentCosts(int numberOfAgents) {
        int n = topology.cities().size();
        for (int i = 0; i < numberOfAgents; i++) {
            if (i != agentId)
                agentEstimatedCostsMap.put(i, new Double[n][n]);
        }
    }

    /**
     * Update bidding approximations (tables) for other bidders based on the last task
     * (tables are stored in agentEstimatedCostsMap map for [agent id -> table])
     * table[i,j] has the approximation of the bid for task t where
     * t.pickup = i and t.delivery = j
     * Update is done on [t.pickup, t.delivery] fields and also on epsilon close fields for all agents
     * the field [i,j] is epsilon close to [k,m]
     * if (the city with id i is epsilon close to the city with id k) and (j = m)
     * or if (i = k) and (the city with id j is epsilon close to the city with id m)
     * since the approximation for the agent cost per km is needed (for other agents), it is approximated as
     * the average cost per km for myself
     * if the bidder did not win in the auction:
     * - the approximation for [t.pickup, t.delivery] is his bid
     * - the update for epsilon close fields to [t.pickup, t.delivery] is done only if it lowers
     * already existing approximation; where new approximation is calculated as
     * bid + cost per km * epsilon distance from the city of t
     * (for epsilon close task the approximation is that the bid would be:
     * bid for t + how far epsilon city is from the city for t * cost per km)
     * if the bidder won the auction:
     * - the approximation for [t.pickup, t.delivery] is 0
     * - similarly as in the previous case (counting that the bid for this task in future would be 0):
     * the update for epsilon close fields to [t.pickup, t.delivery] is done only if it lowers
     * already existing approximation; where new approximation is calculated as
     * 0 + cost per km * epsilon distance from the city of t
     * (for epsilon close task the approximation is that the bid would be:
     * bid for t + how far epsilon city is from the city for t * cost per km)
     * For example: the bid for pair [c1, c2] is b, c3 is epsilon close to c2 and the agent did not win in the auction
     * Then the approximation for [c1, c3] is b + dist(c2, c3) * cost per km
     * Symmetric reasoning is in case when c3 is epsilon close to c1
     * For example: the bid for pair [c1, c2] is b, c3 is epsilon close to c2 and the agent won the auction
     * Then the approximation for [c1, c3] is dist(c2,c3) * cost per km
     * Symmetric reasoning is in the case when c3 is epsilon close to c1
     *
     * @param lastTask
     * @param lastWinner
     * @param lastOffers
     */
    public void updateTables(Task lastTask, int lastWinner, Long[] lastOffers, long maxMarginalCost) {
        // one would bid this if it approximated that other agents would bid nulls
        double upper_bidding_limit = maxMarginalCost * 10;

        for (int i = 0; i < lastOffers.length; i++) {
            if (i == agentId) {
                //tables are only for other agents
                continue;
            }
            Double[][] costs = agentEstimatedCostsMap.get(i);
            double futureBid;
            //TODO: maybe this should not be zero since he has limited capacity, maybe change how we update
            if (lastWinner == i) {
                futureBid = 0.0;
            } else if (lastOffers[i] == null) {
                futureBid = upper_bidding_limit;
            } else {
                futureBid = lastOffers[i];
            }

            // fill the matrix with the other bidder bid
            costs[lastTask.pickupCity.id][lastTask.deliveryCity.id] = futureBid;

            // fill the matrix for tasks similar to the task that was on the auction
            //      (tasks with the same pickup city and close delivery city or vice versa)

            // first it is filled for epsilon close delivery city
            List<Integer> epsilonCloseCitiesDelivery = epsilonCloseCityIdsMap.get(lastTask.deliveryCity);
            for (Integer id : epsilonCloseCitiesDelivery) {
                City epsilonCloseCity = idCityMap.get(id);
                //future bid is UPPER_BIDDING_LIMIT if the bidder bidded null in the auction
                //      in that case we approximate that it would also bid null for epsilon close cities
                double approxCost = futureBid == upper_bidding_limit ? upper_bidding_limit
                        //otherwise we approximate the bid for the future
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
                double approxCost = futureBid == upper_bidding_limit ? upper_bidding_limit
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

    /**
     * @param task
     * @param marginalCost
     * @return
     */
    public Long calculateMyBid(Task task, Long marginalCost) {
        Long extractedBidOfOthers = null;
        double beliefForExtractedBid = 0;

        //extract minimum of approximations for [task.pickup id, task.delivery id] for all opponents
        //     count belief for the approximation =  number of non-null values / number of opponents
        if (!agentEstimatedCostsMap.isEmpty()) {
            Double minBid = Double.MAX_VALUE;
            int numberOfOtherAgents = agentEstimatedCostsMap.size();
            //for how many opponents we have information from the past
            int filledEntries = numberOfOtherAgents;
            for (Map.Entry<Integer, Double[][]> entry : agentEstimatedCostsMap.entrySet()) {
                Double bid = entry.getValue()[task.pickupCity.id][task.deliveryCity.id];
                if (bid == null) {
                    filledEntries--;
                } else if (bid < minBid) {
                    minBid = bid;
                }
            }
            // if we extracted any information, assign it to extracted bid
            extractedBidOfOthers = minBid != Double.MAX_VALUE ? (long) Math.ceil(minBid) : null;
            beliefForExtractedBid = filledEntries * 1.0 / numberOfOtherAgents;

            System.out.println(String.format("Extracted bid: %d", extractedBidOfOthers));
            System.out.println("Belief: " + beliefForExtractedBid);
        }

        //TODO comment this
        Long approximateBidOfOthers = extractedBidOfOthers != null ?
                (long) (Math.ceil(beliefForExtractedBid * extractedBidOfOthers + (1 - beliefForExtractedBid) * marginalCost)) - 1 :
                (long) (Math.ceil(marginalCost))- 1;
        System.out.println("Approximated bid: " + approximateBidOfOthers);

        return approximateBidOfOthers > marginalCost ? approximateBidOfOthers : (long) Math.ceil(marginalCost);
        //return Math.max(approximateBidOfOthers, Math.ceil(marginalCost));
    }

    public Map<Integer, Double[][]> getAgentEstimatedCostsMap() {
        return agentEstimatedCostsMap;
    }

    public double getBiggestCityDistance() {
        return biggestCityDistance;
    }
}
