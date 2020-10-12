package deliberative;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskSet;
import logist.topology.Topology;

public class BFS extends SearchAlgorithm {

    private State rootState;

    public BFS(TaskSet availableTaskSet, TaskSet carriedTaskSet, Topology topology, Vehicle vehicle,
               String heuristicName) {
        super(availableTaskSet, carriedTaskSet, topology, vehicle);
        this.rootState = super.getGraphRoot();
    }

    public Plan getPlan() {
        return null;
    }
}
