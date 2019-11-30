package io.github.lumnitzf.taskscoped;

import java.util.Objects;
import java.util.UUID;

public class TaskId {

    private final UUID value;

    TaskId() {
        value = UUID.randomUUID();
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
