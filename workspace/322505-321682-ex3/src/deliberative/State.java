package deliberative;

import logist.simulation.Vehicle;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class State {

    private City currentCity;
    private TaskSet carriedTasks;
    private TaskSet availableTasks;
    private int carriedTasksWeights;
    private double costFromRoot;

    private Vehicle vehicle;
    private State parent;
    private Set<State> children;

    public State(TaskSet carriedTasks, TaskSet availableTasks, Vehicle vehicle, State parent) {
        this.currentCity = vehicle.getCurrentCity();
        this.carriedTasks = carriedTasks;
        this.availableTasks = availableTasks;
        this.carriedTasksWeights = carriedTasks.weightSum();
        this.vehicle = vehicle;
        this.costFromRoot = parent.costFromRoot + currentCity.distanceTo(parent.currentCity) * vehicle.costPerKm();
        this.parent = parent;
        this.children = new HashSet<>();
    }

    public boolean isGoalState() {
        return carriedTasks.isEmpty() && availableTasks.isEmpty();
    }

    public boolean isRoot() {
        return parent == null;
    }

    public Set<State> getChildren() {
        return children;
    }

    public void setChildren(Set<State> children) {
        this.children = children;
    }

    public State getParent() {
        return parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return currentCity.equals(state.currentCity) &&
                carriedTasks.equals(state.carriedTasks) &&
                availableTasks.equals(state.availableTasks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentCity, carriedTasks, availableTasks);
    }
}
