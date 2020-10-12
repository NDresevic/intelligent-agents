package template;

import logist.plan.Plan;
import logist.task.TaskSet;
import logist.topology.Topology;

public class BFS extends Algorithm{
    public BFS(TaskSet availableTaskSet, TaskSet carriedTaskSet, Topology topology, int capacity) {
        super(availableTaskSet, carriedTaskSet, topology, capacity);
    }

    public Plan getPlan(){
        return null;
    }
}
