package deliberative;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;

import java.util.Set;

public class AStar extends SearchAlgorithm {

    private String heuristicName;

    public AStar(Set<Task> availableTaskSet, Set<Task> carriedTaskSet, Topology topology, Vehicle vehicle,
                 String heuristicName) {
        super(availableTaskSet, carriedTaskSet, topology, vehicle);
        this.heuristicName = heuristicName;
    }

    @Override
    Plan getPlan() {
        return null;
    }
}
