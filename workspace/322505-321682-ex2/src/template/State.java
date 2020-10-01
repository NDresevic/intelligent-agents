package template;

import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.Objects;

public class State {

    //current city = where am I now
    City sourceCity;
    //if not null there exists a packet for the city destinationCity
    City destinationCity;

    public State(City sourceCity) {
        this.sourceCity = sourceCity;
        //explicitly
        this.destinationCity = null;
    }

    public State(City sourceCity, City destinationCity) {
        this.sourceCity = sourceCity;
        this.destinationCity = destinationCity;
    }

    public City getSourceCity() {
        return sourceCity;
    }

    public City getDestinationCity() {
        return destinationCity;
    }

    //if identificationOfDestinationCity is null, then desired action is accepting the packet
    //otherwise desired action is to move to the city with identificationOfNextCity
    public boolean isActionPossible(Integer identioficationOfNeighbor, Topology topology) {
        //desired action is to accept some packet
        if (destinationCity == null) {
            //there are no packets
            return this.destinationCity != null ? true : false;
        } else {
            //desired action is to move to the city with identification identificationOfDestinationCity
            return this.sourceCity.hasNeighbor(topology.cities().get(identioficationOfNeighbor));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return sourceCity.equals(state.sourceCity) &&
                Objects.equals(destinationCity, state.destinationCity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceCity, destinationCity);
    }
}
