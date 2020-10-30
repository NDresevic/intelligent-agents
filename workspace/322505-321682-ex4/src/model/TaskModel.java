package model;

import enums.TaskTypeEnum;
import logist.task.Task;

public class TaskModel {

    private Task task;
    private TaskTypeEnum type;

    public TaskModel(Task task, TaskTypeEnum type) {
        this.task = task;
        this.type = type;
    }

    public Task getTask() {
        return task;
    }

    public TaskTypeEnum getType() {
        return type;
    }

    @Override
    public String toString() {
        return type + ": " + task.toString();
    }

    public double updateLoad(){
        if(type == TaskTypeEnum.PICKUP)
            return task.weight;
        else
            return -task.weight;
    }
}
