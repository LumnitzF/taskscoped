package io.github.lumnitzf.taskscoped;

import io.github.lumnitzf.taskscoped.beans.TaskScopeEnabledBean;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

class TaskIdProxyTest extends AbstractTaskScopedExtensionTest {

    @Inject
    private TaskScopeEnabledBean taskScopeEnabledBean;

    @Override
    protected Collection<Class<?>> getBeanClasses() {
        return Collections.singleton(TaskScopeEnabledBean.class);
    }

    @Test
    void injectedTaskIdIsProxy() {
        taskScopeEnabledBean.doInTaskScope(bean -> {
            assertThat(bean.taskId).isNotExactlyInstanceOf(TaskId.class);
        });
    }

    @Test
    void acquiredTaskIdIsNoProxy() {
        taskScopeEnabledBean.doInTaskScope(bean -> {
            assertThat(bean.taskIdManager.getId()).isExactlyInstanceOf(TaskId.class);
        });
    }

    @Test
    void equalTaskId() {
        taskScopeEnabledBean.doInTaskScope(bean -> {
            assertThat(bean.taskIdManager.getId().getValue()).isEqualTo(bean.taskId.getValue());
        });
    }

}
