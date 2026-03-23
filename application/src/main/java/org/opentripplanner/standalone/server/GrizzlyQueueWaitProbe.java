package org.opentripplanner.standalone.server;

import java.util.concurrent.ConcurrentHashMap;
import org.glassfish.grizzly.threadpool.AbstractThreadPool;
import org.glassfish.grizzly.threadpool.ThreadPoolProbe;

/**
 * A Grizzly {@link ThreadPoolProbe} that measures how long tasks wait in the worker thread pool
 * queue before being picked up by a worker thread.
 * <p>
 * The queue wait time is stored in a {@link ThreadLocal} so that downstream code running in the
 * same worker thread (e.g., Jersey filters) can retrieve it.
 * <p>
 * The probe tracks task submission time in a {@link ConcurrentHashMap} keyed by the task's object
 * identity. This is safe because Grizzly creates a new {@link Runnable} for each dispatched task
 * and the entry is short-lived (removed on dequeue or cancel).
 */
public class GrizzlyQueueWaitProbe extends ThreadPoolProbe.Adapter {

  static final GrizzlyQueueWaitProbe INSTANCE = new GrizzlyQueueWaitProbe();

  private static final ThreadLocal<Long> QUEUE_WAIT_NANOS = new ThreadLocal<>();

  private final ConcurrentHashMap<Runnable, Long> taskQueueTimes = new ConcurrentHashMap<>();

  public GrizzlyQueueWaitProbe() {}

  @Override
  public void onTaskQueueEvent(AbstractThreadPool pool, Runnable task) {
    taskQueueTimes.put(task, System.nanoTime());
  }

  @Override
  public void onTaskDequeueEvent(AbstractThreadPool pool, Runnable task) {
    Long queuedAt = taskQueueTimes.remove(task);
    if (queuedAt != null) {
      QUEUE_WAIT_NANOS.set(System.nanoTime() - queuedAt);
    }
  }

  @Override
  public void onTaskCompleteEvent(AbstractThreadPool pool, Runnable task) {
    QUEUE_WAIT_NANOS.remove();
  }

  @Override
  public void onTaskCancelEvent(AbstractThreadPool pool, Runnable task) {
    taskQueueTimes.remove(task);
  }

  /**
   * Returns the queue wait time in nanoseconds for the current task running on this thread,
   * and clears the stored value. Returns {@code null} if no queue wait was recorded.
   */
  public static Long getAndClearQueueWaitNanos() {
    Long value = QUEUE_WAIT_NANOS.get();
    QUEUE_WAIT_NANOS.remove();
    return value;
  }
}
