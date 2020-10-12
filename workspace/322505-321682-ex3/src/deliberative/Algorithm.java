package template;

import logist.plan.Plan;
import logist.task.TaskSet;
import logist.topology.Topology;

public abstract class Algorithm {

    private TaskSet availableTaskSet;
    private TaskSet carriedTaskSet;
    private Topology topology;
    private int capacity;

    public Algorithm(TaskSet availableTaskSet, TaskSet carriedTaskSet, Topology topology, int capacity) {
        this.availableTaskSet = availableTaskSet;
        this.carriedTaskSet = carriedTaskSet;
        this.topology = topology;
        this.capacity = capacity;
    }

    abstract Plan getPlan();
}
