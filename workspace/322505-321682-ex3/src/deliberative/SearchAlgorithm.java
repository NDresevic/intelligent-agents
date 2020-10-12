package deliberative;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskSet;
import logist.topology.Topology;

public abstract class SearchAlgorithm {

    private TaskSet availableTaskSet;
    private TaskSet carriedTaskSet;
    private Topology topology;
    private Vehicle vehicle;

    public SearchAlgorithm(TaskSet availableTaskSet, TaskSet carriedTaskSet, Topology topology, Vehicle vehicle) {
        this.availableTaskSet = availableTaskSet;
        this.carriedTaskSet = carriedTaskSet;
        this.topology = topology;
        this.vehicle = vehicle;
    }

    public State getGraphRoot() {
        State rootState = new State(carriedTaskSet, availableTaskSet, vehicle, null);


        return rootState;
    }

    abstract Plan getPlan();
}
