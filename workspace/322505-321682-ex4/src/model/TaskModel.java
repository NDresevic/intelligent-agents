package model;

import enums.TaskTypeEnum;
import logist.task.Task;

import java.util.Objects;

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

    public double updateLoad() {
        if (type == TaskTypeEnum.PICKUP)
            return task.weight;
        else
            return -task.weight;
    }

    public TaskTypeEnum getPairOperation() {
        if (type == TaskTypeEnum.PICKUP)
            return TaskTypeEnum.DELIVERY;
        else
            return TaskTypeEnum.PICKUP;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskModel taskModel = (TaskModel) o;
        return task.id == taskModel.task.id &&
                type == taskModel.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(task.id, type);
    }
}
