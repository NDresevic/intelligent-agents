package template;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

import logist.topology.Topology;

import java.util.ArrayList;
import java.util.List;

public class State {

    private City currentCity;
    private List<Task> myTasks;
    private List<Task> otherTasks;
    private int myTasksWeights;

    public State(City currentCity, List<Task> otherTasks) {
        this.currentCity = currentCity;
        this.myTasks = new ArrayList<>();
        this.otherTasks = otherTasks;
        this.myTasksWeights = 0;
    }

    public boolean isGoalState(){
        return !otherTasks.isEmpty();
    }

    private void updateMyTasksWeights(Task task){
        myTasksWeights += task.weight;
    }
}
