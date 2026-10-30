package org.opentripplanner.standalone.server;

import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.cache.CacheMeterBinder;
import java.util.concurrent.TimeUnit;
import org.opentripplanner.transit.transfer.regular.api.report.RegularTransferCacheMetrics;

/**
 * Exposes the counters of the cache of the Raptor transfers to micrometer, with the same meter
 * names as the standard cache metrics ({@code cache.size}, {@code cache.gets}, {@code cache.puts},
 * {@code cache.evictions} and {@code cache.load.duration}).
 * <p>
 * There are no per-key tags, so the number of meters is fixed whatever the requests are. Micrometer
 * only holds a weak reference to the counters. They are owned by the transfer repository for the
 * life of the application, so the meters are never detached, and they do not keep any cache or
 * snapshot alive. Reading a counter never blocks a request.
 */
class RegularTransferCacheMeterBinder extends CacheMeterBinder<RegularTransferCacheMetrics> {

  private final RegularTransferCacheMetrics metrics;

  RegularTransferCacheMeterBinder(
    RegularTransferCacheMetrics metrics,
    String cacheName,
    Iterable<Tag> tags
  ) {
    super(metrics, cacheName, tags);
    this.metrics = metrics;
  }

  @Override
  protected Long size() {
    return metrics.size();
  }

  @Override
  protected long hitCount() {
    return metrics.hitCount();
  }

  @Override
  protected Long missCount() {
    return metrics.missCount();
  }

  @Override
  protected Long evictionCount() {
    return metrics.evictionCount();
  }

  @Override
  protected long putCount() {
    return metrics.putCount();
  }

  @Override
  protected void bindImplementationSpecificMetrics(MeterRegistry registry) {
    FunctionTimer.builder(
      "cache.load.duration",
      metrics,
      m -> m.putCount() + m.loadFailureCount(),
      RegularTransferCacheMetrics::totalLoadTimeNanos,
      TimeUnit.NANOSECONDS
    )
      .tags(getTagsWithCacheName())
      .description("The time spent creating the Raptor transfers")
      .register(registry);
  }
}
