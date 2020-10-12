package deliberative;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskSet;
import logist.topology.Topology;

public class BFS extends SearchAlgorithm {

    public BFS(TaskSet availableTaskSet, TaskSet carriedTaskSet, Topology topology, Vehicle vehicle) {
        super(availableTaskSet, carriedTaskSet, topology, vehicle);
    }

    public Plan getPlan() {
        return null;
    }
}
