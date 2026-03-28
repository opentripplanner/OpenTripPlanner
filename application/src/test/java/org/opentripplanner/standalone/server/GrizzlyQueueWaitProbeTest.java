package org.opentripplanner.standalone.server;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GrizzlyQueueWaitProbeTest {

  private GrizzlyQueueWaitProbe probe;

  @BeforeEach
  void setUp() {
    probe = new GrizzlyQueueWaitProbe();
  }

  @Test
  void recordsPositiveQueueWaitTime() throws InterruptedException {
    Runnable task = () -> {};

    probe.onTaskQueueEvent(null, task);
    Thread.sleep(5);
    probe.onTaskDequeueEvent(null, task);

    Long waitNanos = GrizzlyQueueWaitProbe.getAndClearQueueWaitNanos();
    assertTrue(waitNanos > 0, "Queue wait time should be positive");
  }

  @Test
  void getAndClearReturnsNullWhenNoValueRecorded() {
    assertNull(GrizzlyQueueWaitProbe.getAndClearQueueWaitNanos());
  }

  @Test
  void getAndClearConsumesValue() {
    Runnable task = () -> {};

    probe.onTaskQueueEvent(null, task);
    probe.onTaskDequeueEvent(null, task);

    // First call returns the value
    Long first = GrizzlyQueueWaitProbe.getAndClearQueueWaitNanos();
    assertTrue(first != null && first >= 0);

    // Second call returns null (consumed)
    assertNull(GrizzlyQueueWaitProbe.getAndClearQueueWaitNanos());
  }

  @Test
  void taskCompleteRemovesThreadLocal() {
    Runnable task = () -> {};

    probe.onTaskQueueEvent(null, task);
    probe.onTaskDequeueEvent(null, task);
    probe.onTaskCompleteEvent(null, task);

    assertNull(GrizzlyQueueWaitProbe.getAndClearQueueWaitNanos());
  }

  @Test
  void cancelRemovesMapEntryWithoutSettingThreadLocal() {
    Runnable task = () -> {};

    probe.onTaskQueueEvent(null, task);
    probe.onTaskCancelEvent(null, task);

    // Dequeue after cancel should not set a value (entry was removed)
    probe.onTaskDequeueEvent(null, task);
    assertNull(GrizzlyQueueWaitProbe.getAndClearQueueWaitNanos());
  }

  @Test
  void dequeueWithoutPriorEnqueueDoesNotSetValue() {
    Runnable task = () -> {};

    probe.onTaskDequeueEvent(null, task);

    assertNull(GrizzlyQueueWaitProbe.getAndClearQueueWaitNanos());
  }
}
