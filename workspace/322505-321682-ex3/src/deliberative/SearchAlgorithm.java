package deliberative;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SearchAlgorithm {

    private TaskSet availableTaskSet;
    private TaskSet carriedTaskSet;
    private Topology topology;
    private Vehicle vehicle;

    private State rootState;

    public SearchAlgorithm(TaskSet availableTaskSet, TaskSet carriedTaskSet, Topology topology, Vehicle vehicle) {
        this.availableTaskSet = availableTaskSet;
        this.carriedTaskSet = carriedTaskSet;
        this.topology = topology;
        this.vehicle = vehicle;

        this.rootState = createGraphAndGetRoot();
    }

    private State createGraphAndGetRoot() {
        State rootState = new State(vehicle.getCurrentCity(), carriedTaskSet, availableTaskSet, vehicle);
        System.out.println(rootState);
        List<State> nodes = new ArrayList<>();

        nodes.add(rootState);
        while (!nodes.isEmpty()) {
            State currentState = nodes.remove(0);
            carriedTaskSet = currentState.getCarriedTasks();
            availableTaskSet = currentState.getAvailableTasks();
            vehicle = currentState.getVehicle();

            if (carriedTaskSet == null && availableTaskSet == null) {
                continue;
            }

            Set<State> children = new HashSet<>();
            for (City nextStateCity: topology.cities()) {

                if (currentState.getCurrentCity().equals(nextStateCity)) {  // if I am in nextStateCity continue
                    continue;
                }

                TaskSet newCarriedTaskSet = carriedTaskSet;
                if (carriedTaskSet != null) {
                    newCarriedTaskSet = carriedTaskSet.clone();
                    boolean hasDeliveredTasks = false;
                    for (Task carriedTask: carriedTaskSet) {    // deliver tasks that are for that city
                        if (carriedTask.deliveryCity.equals(nextStateCity)) {
                            newCarriedTaskSet.remove(carriedTask);
                            hasDeliveredTasks = true;
                        }
                    }
                    if (hasDeliveredTasks) {  // child when you just deliver the tasks you have for that city
                        State newState = new State(nextStateCity, newCarriedTaskSet, availableTaskSet, vehicle, currentState);
                        System.out.println("dodajem stanje posle isporuke");
                        children.add(newState);
                    }
                }

                for (Task task: availableTaskSet) { // possible states when you pick up new task

                    // if you are not going to the city where the task is located or you don't have enough capacity continue
                    if (!nextStateCity.equals(task.pickupCity) ||
                            currentState.getCarriedTasksWeights() + task.weight > vehicle.capacity()) {
                        continue;
                    }

                    TaskSet newAvailableTaskSet = availableTaskSet.clone();
                    newAvailableTaskSet.remove(task);
                    System.out.println("dodajem stanje");
                    children.add(new State(nextStateCity, newCarriedTaskSet, newAvailableTaskSet, vehicle,
                            currentState));
                }
            }
            currentState.setChildren(children);
            nodes.addAll(children);
            System.out.println(nodes.size());
        }

        return rootState;
    }

    public State getRootState() {
        return rootState;
    }

    abstract Plan getPlan();
}
