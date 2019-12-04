package io.github.lumnitzf.taskscoped;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.enterprise.inject.spi.BeanManager;
import java.util.Objects;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.*;
import static org.mockito.Mockito.*;

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

            @Nested
            class WithRegistered {
                private Object registered;

                @BeforeEach
                void register() {
                    assumeThat(testee.isActive()).isFalse();
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