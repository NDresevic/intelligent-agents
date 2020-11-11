package c_models;

import c_enums.CTaskTypeEnum;
import logist.task.Task;

import java.util.Objects;

public class CTaskModel {

    private Task task;
    private CTaskTypeEnum type;

    public CTaskModel(Task task, CTaskTypeEnum type) {
        this.task = task;
        this.type = type;
    }

    public Task getTask() {
        return task;
    }

    public CTaskTypeEnum getType() {
        return type;
    }

    @Override
    public String toString() {
        return type + ": " + task.toString();
    }

    public int getUpdatedLoad() {
        return type == CTaskTypeEnum.PICKUP ? task.weight : -task.weight;
    }

    public CTaskTypeEnum getPairTaskType() {
        return type == CTaskTypeEnum.PICKUP ? CTaskTypeEnum.DELIVERY : CTaskTypeEnum.PICKUP;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CTaskModel CTaskModel = (CTaskModel) o;
        return task.id == CTaskModel.task.id &&
                type == CTaskModel.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(task.id, type);
    }
}
