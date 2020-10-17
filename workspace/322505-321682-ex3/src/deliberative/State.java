package deliberative;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class State {

    private City currentCity;
    private Set<Task> carriedTasks;
    private Set<Task> availableTasks;
    private int carriedTasksWeights = 0;
    private Integer id;
    private double h;

    private Vehicle vehicle;
    private Set<State> children;

    public State(City currentCity, Set<Task> carriedTasks, Set<Task> availableTasks, Vehicle vehicle) {
        this.currentCity = currentCity;
        this.carriedTasks = carriedTasks;
        this.availableTasks = availableTasks;
        for (Task task : carriedTasks) {
            this.carriedTasksWeights += task.weight;
        }
        this.vehicle = vehicle;
        this.children = new HashSet<>();
        this.id = null;
    }

    public boolean isFinalState() {
        return carriedTasks.isEmpty() && availableTasks.isEmpty();
    }

    public Set<State> getChildren() {
        return children;
    }

    public void setChildren(Set<State> children) {
        this.children = children;
    }

    public int getCarriedTasksWeights() {
        return carriedTasksWeights;
    }

    public City getCurrentCity() {
        return currentCity;
    }

    public Set<Task> getAvailableTasks() {
        return availableTasks;
    }

    public Set<Task> getCarriedTasks() {
        return carriedTasks;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Integer getId() { return id; }

    public void setId(int id) { this.id = id; }

    public void appendChild(State child){ children.add(child); }

    public Double getH() { return h; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return Objects.equals(currentCity, state.currentCity) &&
                Objects.equals(carriedTasks, state.carriedTasks) &&
                Objects.equals(availableTasks, state.availableTasks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentCity, carriedTasks, availableTasks);
    }

    @Override
    public String toString() {
        return "State{" +
                "currentCity=" + currentCity +
                ", carriedTasks=" + carriedTasks +
                ", availableTasks=" + availableTasks +
                ", carriedTasksWeights=" + carriedTasksWeights +
                //", vehicle=" + vehicle +
                //", children=" + children +
                '}';
    }

    public void calculateHeuristic() {
        double h1 = carriedTasks.isEmpty() ? 0d : Double.MIN_VALUE;
        for (Task task : carriedTasks) {
            if (currentCity.distanceTo(task.deliveryCity) > h1) {
                h1 = currentCity.distanceTo(task.deliveryCity);
            }
        }
        double h2 = availableTasks.isEmpty() ? 0d : Double.MIN_VALUE;
        for (Task task : availableTasks) {
            double possibleShorterPath = currentCity.distanceTo(task.pickupCity) +
                    task.pickupCity.distanceTo(task.deliveryCity);
            if (possibleShorterPath > h2) {
                h2 = possibleShorterPath;
            }
        }
        h = h1 > h2 ? h1 : h2;
    }
}
