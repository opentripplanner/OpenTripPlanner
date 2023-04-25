package org.opentripplanner.framework.concurrent;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;

/**
 * Task Executor that provides support for interruption propagation.
 */
public class InterruptibleExecutor {

  private InterruptibleExecutor() {}

  /**
   * Executor service for interruptible tasks. Similarly to {@link ForkJoinPool#commonPool()}, its size
   * is the number of available processors minus 1.
   */
  private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors() - 1
  );

  /**
   * Execute a list of tasks in parallel. When the calling thread is interrupted, tasks
   * that are still running are cancelled and their interrupted flag is set.
   */
  public static void execute(List<Callable<Object>> tasks) throws ExecutionException {
    try {
      // when interrupted, ExecutorService#invokeAll() cancels the tasks that are still running
      // and sets the interrupted flag in the executing thread.
      List<Future<Object>> asyncResults = EXECUTOR_SERVICE.invokeAll(tasks);
      // retrieve the completed tasks to trigger ExecutionExceptions, if any.
      for (Future<Object> asyncResult : asyncResults) {
        asyncResult.get();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new OTPRequestTimeoutException();
    }
  }

  public static ExecutorService threadPool() {
    return EXECUTOR_SERVICE;
  }
}
