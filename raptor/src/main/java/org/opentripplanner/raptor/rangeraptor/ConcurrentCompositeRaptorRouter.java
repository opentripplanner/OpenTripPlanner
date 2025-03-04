package org.opentripplanner.raptor.rangeraptor;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouter;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouterResult;

/**
 * Run two Raptor routers and join the result. The two searches are run concurrently if an
 * {@link ExecutorService} is provided.
 * @see CompositeResult for joining results.
 */
public class ConcurrentCompositeRaptorRouter<T extends RaptorTripSchedule>
  implements RaptorRouter<T> {

  private final RaptorRouter<T> mainWorker;
  private final RaptorRouter<T> alternativeWorker;
  private final BiFunction<
    Collection<RaptorPath<T>>,
    Collection<RaptorPath<T>>,
    Collection<RaptorPath<T>>
  > merger;

  @Nullable
  private final ExecutorService executorService;

  @Nullable
  private final Function<InterruptedException, RuntimeException> mapInterruptedException;

  public ConcurrentCompositeRaptorRouter(
    RaptorRouter<T> mainWorker,
    RaptorRouter<T> alternativeWorker,
    BiFunction<
      Collection<RaptorPath<T>>,
      Collection<RaptorPath<T>>,
      Collection<RaptorPath<T>>
    > merger,
    @Nullable ExecutorService executorService,
    @Nullable Function<InterruptedException, RuntimeException> mapInterruptedException
  ) {
    this.mainWorker = mainWorker;
    this.alternativeWorker = alternativeWorker;
    this.merger = merger;
    this.executorService = executorService;
    this.mapInterruptedException = mapInterruptedException;
  }

  @Override
  public RaptorRouterResult<T> route() {
    if (executorService == null) {
      var mainResult = mainWorker.route();
      var alternativeResult = alternativeWorker.route();
      return new CompositeResult<>(mainResult, alternativeResult, merger);
    }

    var mainResultFuture = executorService.submit(mainWorker::route);
    var alternativeResultFuture = executorService.submit(alternativeWorker::route);

    try {
      var mainResult = mainResultFuture.get();
      var alternativeResult = alternativeResultFuture.get();
      return new CompositeResult<>(mainResult, alternativeResult, merger);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      // propagate interruption to the running task.

      mainResultFuture.cancel(true);
      alternativeResultFuture.cancel(true);
      throw mapInterruptedException.apply(e);
    } catch (ExecutionException e) {
      throw (e.getCause() instanceof RuntimeException re) ? re : new RuntimeException(e);
    }
  }
}
