package io.github.lumnitzf.taskscoped;

import io.github.lumnitzf.taskscoped.beans.ExecutorServiceProducer;
import io.github.lumnitzf.taskscoped.beans.TaskIdStore;
import io.github.lumnitzf.taskscoped.beans.TaskScopeEnabledBean;
import org.junit.jupiter.api.Test;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.*;
import static org.mockito.Mockito.*;

class TaskPreservingExecutorServiceTest extends AbstractTaskScopedExtensionTest {

    @Inject
    @TaskPreserving
    private ExecutorService taskPreservingService;

    @Inject
    private TaskScopeEnabledBean taskScopeEnabledBean;


    @Test
    void sameTaskIdWithRunnable(TaskIdStore taskIdStore, TaskIdSettingRunnable runnable) throws Exception {
        taskScopeEnabledBean.doInTaskScope(bean -> {
            taskPreservingService.submit(runnable).get();
            assertThat(taskIdStore.getTaskId()).isEqualTo(bean.taskIdManager.getId());
        });
    }

    @Test
    void sameTaskIdWithCallable(TaskIdSettingCallable callable) throws Exception {
        taskScopeEnabledBean.doInTaskScope(bean -> {
            TaskId returnedTaskId = taskPreservingService.submit(callable).get();
            assertThat(returnedTaskId).isEqualTo(bean.taskIdManager.getId());
        });
    }

    @Test
    void taskPreservingServiceThrowsContextNotActiveExceptionOnNotActiveContext() {
        assertThatThrownBy(() -> taskPreservingService.submit(mock(Runnable.class))).isInstanceOf(
                ContextNotActiveException.class);
    }

    @Test
    void taskIsKeptAliveBetweenNonOverlappingCalls(TaskIdSettingCallable callable) throws Exception {
        taskScopeEnabledBean.doInTaskScope(bean -> {
            TaskId firstTaskId = taskPreservingService.submit(callable).get();
            TaskId secondTaskId = taskPreservingService.submit(callable).get();
            assertThat(firstTaskId).isEqualTo(secondTaskId);
        });
    }

    /*
     * Tests if the TaskScope is kept alive for a callable, when there is no other bean in the scope.
     * Using a single threaded ExecutorService, we can be sure, that one callable is executed after the other.
     * The important thing is, that the second callable is only called when the taskScopeEnabledBean has left its scope
     * and the first callable has also exited the scope.
     * This can be achieved using semaphores, where we can force the first callable to wait until
     * the taskScopeEnabledBean has left its scope:
     *
     *    testMethod     taskScopeEnabledBean      callable1     callable2
     *         |
     *         |----------------->|
     *         |                  |
     *         |<-----------------|                    |
     *         |                                       |
     *         |------------semaphore.release()------->|
     *         |                                       |
     *         |<------------futures[0].get()----------|
     *         |
     *         |      This is the important gap when the ExcutorService triggers the second callable
     *         |      and the TaskScope must be kept alive
     *         |
     *         |                                                     |
     *         |                   futures[1].get()                  |
     *         |<----------------------------------------------------|
     *         |
     *
     */
    @Test
    void taskIsKeptAliveBetweenNonOverlappingCallsWhenInitialExits(TaskIdSettingCallable callable) throws Exception {
        // Arrange
        final Semaphore sema = new Semaphore(0);
        final Future<TaskId>[] futures = new Future[2];
        final AtomicReference<TaskId> initial = new AtomicReference<>(null);

        // Act
        taskScopeEnabledBean.doInTaskScope(bean -> {
            initial.set(bean.taskIdManager.getId());
            // The first callable will be used as stopper, so that the second callable is executed alone
            futures[0] = taskPreservingService.submit(() -> {
                sema.acquireUninterruptibly();
                return callable.call();
            });
            futures[1] = taskPreservingService.submit(() -> {
                // For easier break point when debugging
                return callable.call();
            });
        }); // This already closes the scope for taskScopeEnabledBean
        // Check if it has really left the scope
        // Use direct field access, as getters will trigger a new scope to be entered
        assumeThatThrownBy(() -> taskScopeEnabledBean.taskIdManager.getId()).isInstanceOf(
                ContextNotActiveException.class);
        // The first one will halt until this point
        sema.release();
        final TaskId first = futures[0].get();
        final TaskId second = futures[1].get();

        // Assert
        // All three must have the same task id
        assertThat(second).isEqualTo(first).isEqualTo(initial.get());
    }

    @Override
    protected Collection<Class<?>> getBeanClasses() {
        return Arrays.asList(TaskScopeEnabledBean.class, ExecutorServiceProducer.class, TaskIdStore.class,
                TaskIdSettingRunnable.class, TaskIdSettingCallable.class);
    }

    @Dependent
    static class TaskIdSettingRunnable implements Runnable {
        @Inject
        private TaskIdStore store;

        @Inject
        private TaskIdManager manager;


        @Override
        public void run() {
            store.setTaskId(manager.getId());
        }
    }

    @Dependent
    static class TaskIdSettingCallable implements Callable<TaskId> {

        @Inject
        private TaskIdManager manager;

        @Override
        public TaskId call() {
            return manager.getId();
        }
    }
}
