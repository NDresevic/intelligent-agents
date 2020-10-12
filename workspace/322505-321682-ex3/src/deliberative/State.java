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
    private int carriedTasksWeights = 0;
    private double costFromRoot;    // g(n)

    private Vehicle vehicle;
    private State parent;
    private Set<State> children;

    public State(City currentCity, TaskSet carriedTasks, TaskSet availableTasks, Vehicle vehicle) {
        this.currentCity = currentCity;
        this.carriedTasks = carriedTasks;
        this.availableTasks = availableTasks;
        if (carriedTasks != null) {
            this.carriedTasksWeights = carriedTasks.weightSum();
        }
        this.vehicle = vehicle;
        this.costFromRoot = 0;
        this.parent = null;
        this.children = new HashSet<>();
    }

    public State(City currentCity, TaskSet carriedTasks, TaskSet availableTasks, Vehicle vehicle, State parent) {
        this(currentCity, carriedTasks, availableTasks, vehicle);
        this.costFromRoot = parent.costFromRoot + currentCity.distanceTo(parent.currentCity) * vehicle.costPerKm();
        this.parent = parent;
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

    public int getCarriedTasksWeights() {
        return carriedTasksWeights;
    }

    public City getCurrentCity() {
        return currentCity;
    }

    public TaskSet getCarriedTasks() {
        return carriedTasks;
    }

    public TaskSet getAvailableTasks() {
        return availableTasks;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return Objects.equals(currentCity, state.currentCity) &&
                Objects.equals(carriedTasks, state.carriedTasks) &&
                Objects.equals(availableTasks, state.availableTasks) &&
                Objects.equals(parent, state.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentCity, carriedTasks, availableTasks, parent);
    }

    @Override
    public String toString() {
        return "State{" +
                "currentCity=" + currentCity +
                ", carriedTasks=" + carriedTasks +
                ", availableTasks=" + availableTasks +
                ", carriedTasksWeights=" + carriedTasksWeights +
                ", costFromRoot=" + costFromRoot +
                ", vehicle=" + vehicle +
                ", parent=" + parent +
                ", children=" + children +
                '}';
    }
}
