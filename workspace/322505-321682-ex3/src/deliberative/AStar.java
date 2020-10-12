package template;

import logist.plan.Plan;
import logist.task.TaskSet;
import logist.topology.Topology;

public class AStar extends Algorithm{
    private String heuristicName;

    public AStar(TaskSet availableTaskSet, TaskSet carriedTaskSet, Topology topology, int capacity, String heuristicName) {
        super(availableTaskSet, carriedTaskSet, topology, capacity);
        this.heuristicName = heuristicName;
    }

    @Override
    Plan getPlan() {
        return null;
    }
}
