package deliberative;
import logist.task.Task;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.List;

public class State {

    private City currentCity;
    private List<Task> carriedTasks;
    private List<Task> otherTasks;
    private int myTasksWeights;
    private int costFromTheRoot;

    public State(City currentCity, List<Task> otherTasks) {
        this.currentCity = currentCity;
        this.carriedTasks = new ArrayList<>();
        this.otherTasks = otherTasks;
        this.myTasksWeights = 0;
        this.costFromTheRoot = 0;
    }

    public boolean isGoalState(){
        return !carriedTasks.isEmpty() && !otherTasks.isEmpty();
    }

    private void updateMyTasksWeights(Task task){
        myTasksWeights += task.weight;
    }
}
