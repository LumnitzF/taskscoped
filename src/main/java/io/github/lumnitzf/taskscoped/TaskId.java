package io.github.lumnitzf.taskscoped;

import javax.enterprise.inject.Vetoed;
import java.util.Objects;
import java.util.UUID;

@Vetoed
public class TaskId {

    private final UUID value;

    private TaskId() {
        value = UUID.randomUUID();
    }

    static TaskId create() {
        return new TaskId();
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TaskId taskId = (TaskId) o;
        return value.equals(taskId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "TaskId{" +
                "value=" + value +
                '}';
    }
}
