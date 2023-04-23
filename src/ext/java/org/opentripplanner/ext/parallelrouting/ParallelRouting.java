package org.opentripplanner.ext.parallelrouting;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.routing.error.RoutingValidationException;

/**
 * Support class for the Parallel Routing feature.
 */
public class ParallelRouting {

  private ParallelRouting() {}

  /**
   * Executor service for parallel routing. Similarly to {@link ForkJoinPool#commonPool()}, its size
   * is the number of available processors minus 1.
   */
  private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors() - 1
  );

  /**
   * Execute a list of cancellable tasks in parallel. When the calling thread is interrupted, tasks
   * that are still running are cancelled and their interrupted flag is set.
   */
  public static void execute(List<Callable<Object>> tasks) {
    try {
      // when interrupted, ExecutorService#invokeAll() cancels the tasks that are still running.
      List<Future<Object>> asyncResults = EXECUTOR_SERVICE.invokeAll(tasks);
      // retrieve the completed tasks to trigger ExecutionExceptions, if any.
      for (Future<Object> asyncResult : asyncResults) {
        asyncResult.get();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new OTPRequestTimeoutException();
    } catch (ExecutionException e) {
      RoutingValidationException.unwrapAndRethrowExecutionException(e);
    }
  }

  public static ExecutorService threadPool() {
    return EXECUTOR_SERVICE;
  }
}
