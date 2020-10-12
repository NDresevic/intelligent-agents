package deliberative;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskSet;
import logist.topology.Topology;

public class AStar extends SearchAlgorithm {

    private String heuristicName;

    public AStar(TaskSet availableTaskSet, TaskSet carriedTaskSet, Topology topology, Vehicle vehicle,
                 String heuristicName) {
        super(availableTaskSet, carriedTaskSet, topology, vehicle);
        this.heuristicName = heuristicName;
    }

    @Override
    Plan getPlan() {
        return null;
    }
}
