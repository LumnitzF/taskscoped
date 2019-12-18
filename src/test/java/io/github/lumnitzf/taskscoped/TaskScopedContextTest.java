package io.github.lumnitzf.taskscoped;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import javax.enterprise.inject.spi.BeanManager;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verifyZeroInteractions;

class TaskScopedContextTest {

    private BeanManager beanManagerMock;

    private TaskScopedContext testee;

    @BeforeEach
    void createMock() {
        beanManagerMock = mock(BeanManager.class);
    }

    @AfterEach
    void reset() {
        // Reset the ThreadLocal
        TaskIdManager.remove();
    }

    private void assertDestroyedEventFired(TaskId taskId) {
        Objects.requireNonNull(taskId);
        inOrder(beanManagerMock).verify(beanManagerMock).fireEvent(same(taskId),
                eq(new TaskScopedContext.DestroyedLiteral(TaskScoped.class)));
    }

    private void assertInitializedEventFired(TaskId taskId) {
        Objects.requireNonNull(taskId);
        inOrder(beanManagerMock).verify(beanManagerMock).fireEvent(same(taskId),
                eq(new TaskScopedContext.InitializedLiteral(TaskScoped.class)));
    }

    private void assertAfterTaskEnterEventFired(TaskId taskId) {
        Objects.requireNonNull(taskId);
        inOrder(beanManagerMock).verify(beanManagerMock).fireEvent(same(taskId),
                eq(AfterTaskEnter.Literal.INSTANCE));
    }

    private void assertBeforeTaskExitEventFired(TaskId taskId) {
        Objects.requireNonNull(taskId);
        inOrder(beanManagerMock).verify(beanManagerMock).fireEvent(same(taskId),
                eq(BeforeTaskExit.Literal.INSTANCE));
    }

    private void assertScopeNotActive() {
        assertThat(testee.isActive()).isFalse();
    }

    private void assertScopeActive() {
        assertThat(testee.isActive()).isTrue();
    }

    @Nested
    class WhenNew {

        @BeforeEach
        void createNewContext() {
            testee = new TaskScopedContext(beanManagerMock);
        }

        @Test
        void scopeNotActive() {
            assertScopeNotActive();
        }

        @Test
        void scopeIsTaskScoped() {
            assertThat(testee.getScope()).isEqualTo(TaskScoped.class);
        }

        @Nested
        class WithTaskId {
            private TaskId taskId;

            @BeforeEach
            void createTaskId() {
                taskId = TaskId.create();
            }

            @Nested
            class AfterEnter {

                @BeforeEach
                void enter() {
                    assumeThat(testee.isActive()).isFalse();
                    testee.enter(taskId);
                }

                @Test
                void scopeActive() {
                    assertScopeActive();
                }

                @Test
                void initializedEventFired() {
                    assertInitializedEventFired(taskId);
                }

                @Test
                void afterTaskEnterEventFired() {
                    assertAfterTaskEnterEventFired(taskId);
                }

                @Test
                void initializedEventFiredBeforeAfterTaskEnterEvent() {
                    InOrder order = inOrder(beanManagerMock);
                    order.verify(beanManagerMock).fireEvent(same(taskId),
                            eq(new TaskScopedContext.InitializedLiteral(TaskScoped.class)));
                    order.verify(beanManagerMock).fireEvent(same(taskId),
                            eq(AfterTaskEnter.Literal.INSTANCE));
                }
                @Nested
                class AfterExit {

                    @BeforeEach
                    void exit() {
                        assumeThat(testee.isActive()).isTrue();
                        testee.exit(null);
                    }

                    @Test
                    void scopeNotActive() {
                        assertScopeNotActive();
                    }

                    @Test
                    void destroyedEventFired() {
                        assertDestroyedEventFired(taskId);
                    }

                    @Test
                    void beforeTaskExitEventFired() {
                        assertBeforeTaskExitEventFired(taskId);
                    }

                    @Test
                    void beforeExitEventFiredBeforeDestroyedEvent() {
                        InOrder order = inOrder(beanManagerMock);
                        order.verify(beanManagerMock).fireEvent(same(taskId),
                                eq(BeforeTaskExit.Literal.INSTANCE));
                        order.verify(beanManagerMock).fireEvent(same(taskId),
                                eq(new TaskScopedContext.DestroyedLiteral(TaskScoped.class)));
                    }
                }
            }

            @Nested
            class WithRegistered {
                private Object registered;

                @BeforeEach
                void register() {
                    assumeThat(testee.isActive()).isFalse();
                    registered = new Object();
                    testee.register(taskId, registered);
                }

                @Test
                void notActive() {
                    assertScopeNotActive();
                }

                @Test
                void noEvents() {
                    verifyZeroInteractions(beanManagerMock);
                }

                @Nested
                class AfterEnter {

                    @BeforeEach
                    void enter() {
                        assumeThat(testee.isActive()).isFalse();
                        testee.enter(taskId);
                    }

                    @Test
                    void scopeActive() {
                        assertScopeActive();
                    }

                    @Test
                    void initializedEventFired() {
                        assertInitializedEventFired(taskId);
                    }

                    @Nested
                    class AfterExit {


                        @BeforeEach
                        void exit() {
                            assumeThat(testee.isActive()).isTrue();
                            testee.exit(null);
                        }

                        @Test
                        void scopeNotActive() {
                            assertScopeNotActive();
                        }

                        @Test
                        void destroyedEventNotFired() {
                            inOrder(beanManagerMock).verify(beanManagerMock, never()).fireEvent(same(taskId),
                                    eq(new TaskScopedContext.DestroyedLiteral(TaskScoped.class)));
                        }

                        @Nested
                        class AfterUnregister {

                            @BeforeEach
                            void unregister() {
                                testee.unregister(taskId, registered);
                            }

                            @Test
                            void scopeNotActive() {
                                assertScopeNotActive();
                            }

                            @Test
                            void destroyedEventFired() {
                                assertDestroyedEventFired(taskId);
                            }
                        }
                    }


                    @Nested
                    class AfterUnregister {

                        @BeforeEach
                        void unregister() {
                            testee.unregister(taskId, registered);
                        }

                        @Test
                        void scopeActive() {
                            assertScopeActive();
                        }

                        @Test
                        void destroyedEventNotFired() {
                            inOrder(beanManagerMock).verify(beanManagerMock, never()).fireEvent(same(taskId),
                                    eq(new TaskScopedContext.DestroyedLiteral(TaskScoped.class)));
                        }

                        @Nested
                        class AfterExit {
                            @BeforeEach
                            void exit() {
                                assumeThat(testee.isActive()).isTrue();
                                testee.exit(null);
                            }

                            @Test
                            void scopeNotActive() {
                                assertScopeNotActive();
                            }

                            @Test
                            void destroyedEventFired() {
                                assertDestroyedEventFired(taskId);
                            }
                        }
                    }
                }
            }
        }
    }
}
