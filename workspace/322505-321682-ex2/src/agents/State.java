package agents;

import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class State {

    // current city = where am I now
    private City currentCity;
    // if not null there exists a packet for the city destinationCity
    private City taskCity;

    public State(City currentCity, City taskCity) {
        this.currentCity = currentCity;
        this.taskCity = taskCity;
    }

    public State(City currentCity) {
        this(currentCity, null);
    }

    // todo: implement
    public static List<State> getAllNextStates(Integer action, Topology topology) {
        return new ArrayList<>();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return currentCity.equals(state.currentCity) &&
                Objects.equals(taskCity, state.taskCity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentCity, taskCity);
    }

    public City getCurrentCity() {
        return currentCity;
    }

    public City getTaskCity() {
        return taskCity;
    }
}
