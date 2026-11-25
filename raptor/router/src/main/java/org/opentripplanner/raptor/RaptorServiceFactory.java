package org.opentripplanner.raptor;

import java.util.concurrent.ExecutorService;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.raptor.api.request.RaptorEnvironment;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.extensions.extrasearch.ExtraMcRouterSearch;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * The entry point for creating {@link RaptorService}s. It is the only way to get a service from
 * outside the Raptor module, and it hides the internal Raptor wiring.
 * <p>
 * This class should have APPLICATION scope. It holds the tuning parameters and the environment,
 * and the environment has a thread-pool, which is shared by all the services created.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class RaptorServiceFactory<T extends RaptorTripSchedule> {

  private final RaptorConfig<T> config;

  public RaptorServiceFactory(
    RaptorTuningParameters tuningParameters,
    RaptorEnvironment environment
  ) {
    this.config = new RaptorConfig<>(tuningParameters, environment);
  }

  /** Create a new service. The service is cheap to create and should be used for one request. */
  public RaptorService<T> createService() {
    return new RaptorService<>(config);
  }

  /**
   * Create a new service, which runs the given extra multi-criteria search in parallel with the
   * main search, and merges the results.
   */
  public RaptorService<T> createService(@Nullable ExtraMcRouterSearch<T> extraMcSearch) {
    return new RaptorService<>(config, extraMcSearch);
  }

  /** {@code true} if the searches may run in parallel using the thread pool. */
  public boolean isMultiThreaded() {
    return config.isMultiThreaded();
  }

  /** The thread pool used for parallel searches, or {@code null} if single-threaded. */
  @Nullable
  public ExecutorService threadPool() {
    return config.threadPool();
  }

  /** Shut down the thread pool, if any. */
  public void shutdown() {
    config.shutdown();
  }
}
