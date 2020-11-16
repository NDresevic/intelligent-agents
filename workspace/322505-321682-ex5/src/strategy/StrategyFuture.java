package strategy;

import logist.agent.Agent;
import logist.task.TaskDistribution;
import logist.topology.Topology;

public class StrategyFuture {

    private TaskDistribution distribution;
    private Topology topology;
    private Agent agent;

    public StrategyFuture(TaskDistribution distribution, Topology topology, Agent agent) {
        this.distribution = distribution;
        this.topology = topology;
        this.agent = agent;
    }
}
