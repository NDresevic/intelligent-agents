package deliberative;

import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;
import java.util.stream.Collectors;

public abstract class SearchAlgorithm {

    private Set<Task> availableTaskSet;
    private Set<Task> carriedTaskSet;
    private Topology topology;
    private Vehicle vehicle;

    protected State rootState;

    public SearchAlgorithm(Set<Task> availableTaskSet, Set<Task> carriedTaskSet, Topology topology, Vehicle vehicle) {
        this.availableTaskSet = availableTaskSet;
        this.carriedTaskSet = carriedTaskSet;
        this.topology = topology;
        this.vehicle = vehicle;

        this.rootState = createGraphAndGetRoot();
    }

    private State createGraphAndGetRoot() {
        State rootState = new State(vehicle.getCurrentCity(), carriedTaskSet, availableTaskSet, vehicle);
        List<State> nodes = new ArrayList<>();

        nodes.add(rootState);
        while (!nodes.isEmpty()) {
            System.out.println("----------------------------------");
            State currentState = nodes.remove(0);
            carriedTaskSet = currentState.getCarriedTasks();
            availableTaskSet = currentState.getAvailableTasks();
            vehicle = currentState.getVehicle();
            int carriedTasksWeight = currentState.getCarriedTasksWeights();

            System.out.println("CURRENT AVAILABLE: " + availableTaskSet);
            System.out.println("CURRENT CARRIED: " + carriedTaskSet + "\n" + currentState.getCurrentCity());


            Set<State> children = new HashSet<>();
            if (carriedTaskSet.isEmpty() && availableTaskSet.isEmpty()) {
                State finalState = new State(currentState.getCurrentCity(), new HashSet<>(), new HashSet<>(), vehicle,
                        currentState);
                if (!currentState.getChildren().contains(finalState)) {
                    children.add(finalState);
                }
                continue;
            }

            for (City nextStateCity: topology.cities()) {
                Set<Task> newCarriedTaskSet = new HashSet<>(carriedTaskSet);
                Set<Task> newAvailableTaskSet = new HashSet<>(availableTaskSet);

                if (currentState.getCurrentCity().equals(nextStateCity)) {  // if I am in nextStateCity continue
                    continue;
                }

                boolean hasDeliveredTasks = false;
                for (Task carriedTask: carriedTaskSet) {    // deliver tasks that are for that city
                    if (carriedTask.deliveryCity.equals(nextStateCity)) {
                        newCarriedTaskSet.remove(carriedTask);
                        carriedTasksWeight -= carriedTask.weight;
                        hasDeliveredTasks = true;
                    }
                }
                if (hasDeliveredTasks) {  // child when you just deliver the tasks you have for that city
                    System.out.println("pravim dete 1");
                    System.out.println("CURRENT AVAILABLE: " + newAvailableTaskSet);
                    System.out.println("CURRENT CARRIED: " + newCarriedTaskSet + "\n" + nextStateCity);

                    State newState = new State(nextStateCity, newCarriedTaskSet, newAvailableTaskSet, vehicle,
                            currentState);
                    children.add(newState);
                }

                for (Task availableTask: availableTaskSet) { // possible states when you pick up new task

                    // todo: izracunati novi
                    // if you are not going to the city where the task is located or you don't have enough capacity continue
                    if (nextStateCity.equals(availableTask.pickupCity) &&
                            carriedTasksWeight + availableTask.weight <= vehicle.capacity()) {
                        newAvailableTaskSet.remove(availableTask);

                        Set<Task> noviZaNosenje = new HashSet<>(newCarriedTaskSet);
                        noviZaNosenje.add(availableTask);
                        State newState = new State(nextStateCity, noviZaNosenje, newAvailableTaskSet, vehicle,
                                currentState);

                        System.out.println("pravim dete 2");
                        System.out.println("CURRENT AVAILABLE: " + newAvailableTaskSet);
                        System.out.println("CURRENT CARRIED: " + noviZaNosenje + "\n" + nextStateCity);

                        children.add(newState);
                    }
                }
            }

            currentState.setChildren(children);
            nodes.addAll(children);
        }

        return rootState;
    }

    abstract State getGoalState();

    public Plan getPlan() {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);
        List<Action> actions = new ArrayList<>();

        State currentState = this.getGoalState();
        while (currentState.getParent() != null) {
            State parent = currentState.getParent();

            System.out.println("NOVA ITER");

            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("AVAILABLE: " + currentState.getAvailableTasks());
            System.out.println("CARRIED: " + currentState.getCarriedTasks() + "\n" + currentState.getCurrentCity());

            // get which tasks are delivered

            Set<Task> parentCarriedTasks = parent.getCarriedTasks();
            Set<Task> currentCarriedTasks = currentState.getCarriedTasks();
            List<Task> deliveredTasks = parentCarriedTasks.stream()
                    .filter(element -> !currentCarriedTasks.contains(element))
                    .collect(Collectors.toList());
            for (Task deliveredTask: deliveredTasks) {
                actions.add(new Action.Delivery(deliveredTask));
            }
//            System.out.println("taskovi za delivery \n" + deliveredTasks);
            System.out.println("PARENT CARRIED: " + parentCarriedTasks + "\n" + parent.getCurrentCity());
            System.out.println("CURRENT CARRIED: " + currentCarriedTasks + "\n" + currentState.getCurrentCity());


            // get which tasks are picked up
            Set<Task> parentAvailableTasks = parent.getAvailableTasks();
            Set<Task> currentAvailableTasks = currentState.getAvailableTasks();
            List<Task> pickedTasks = parentAvailableTasks.stream()
                    .filter(element -> !currentAvailableTasks.contains(element))
                    .collect(Collectors.toList());
            for (Task pickedTask: pickedTasks) { // TODO: videti da li uzimamo jedan ili vise
                actions.add(new Action.Pickup(pickedTask));
            }
//            System.out.println("taskovi za pickup \n" + deliveredTasks);
            System.out.println("PARENT AVAILABLE: " + parentAvailableTasks + "\n" + parent.getCurrentCity());
            System.out.println("CURRENT AVAILABLE: " + currentAvailableTasks + "\n" + currentState.getCurrentCity());

            List<City> intermediateCities = currentState.getCurrentCity().pathTo(parent.getCurrentCity());
            for (City city: intermediateCities) {
                actions.add(new Action.Move(city));
            }
            currentState = parent;
        }

        Collections.reverse(actions);
        System.out.println(actions);
        for (Action action: actions) {
            plan.append(action);
        }
        return plan;
//        return null;
    }
}
