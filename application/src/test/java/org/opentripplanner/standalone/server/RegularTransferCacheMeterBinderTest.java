package org.opentripplanner.standalone.server;

import static com.google.common.truth.Truth.assertThat;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.transfer.regular.api.report.RegularTransferCacheMetrics;

class RegularTransferCacheMeterBinderTest {

  private static final List<Tag> TAGS = List.of(Tag.of("cache", "raptorTransfers"));

  @Test
  void exposesTheCountersAsMetersWithAFixedSetOfTags() {
    var metrics = new TestMetrics();
    var registry = new SimpleMeterRegistry();
    new RegularTransferCacheMeterBinder(metrics, "raptorTransfersCache", TAGS).bindTo(registry);

    metrics.size.set(7);
    metrics.hits.set(30);
    metrics.misses.set(5);
    metrics.puts.set(4);
    metrics.evictions.set(2);
    metrics.loadFailures.set(1);
    metrics.loadNanos.set(TimeUnit.SECONDS.toNanos(10));

    assertThat(registry.get("cache.size").gauge().value()).isEqualTo(7.0);
    assertThat(registry.get("cache.gets").tag("result", "hit").functionCounter().count()).isEqualTo(
      30.0
    );
    assertThat(
      registry.get("cache.gets").tag("result", "miss").functionCounter().count()
    ).isEqualTo(5.0);
    assertThat(registry.get("cache.puts").functionCounter().count()).isEqualTo(4.0);
    assertThat(registry.get("cache.evictions").functionCounter().count()).isEqualTo(2.0);
    var load = registry.get("cache.load.duration").functionTimer();
    // The creations and the failures
    assertThat(load.count()).isEqualTo(5.0);
    assertThat(load.totalTime(TimeUnit.SECONDS)).isEqualTo(10.0);

    // Nothing is tagged by key, so the number of meters does not depend on the requests
    assertThat(registry.getMeters()).hasSize(6);
    registry
      .getMeters()
      .forEach(m ->
        assertThat(m.getId().getTags()).contains(Tag.of("cache", "raptorTransfersCache"))
      );
  }

  private static final class TestMetrics implements RegularTransferCacheMetrics {

    final AtomicLong size = new AtomicLong();
    final AtomicLong hits = new AtomicLong();
    final AtomicLong misses = new AtomicLong();
    final AtomicLong puts = new AtomicLong();
    final AtomicLong evictions = new AtomicLong();
    final AtomicLong loadFailures = new AtomicLong();
    final AtomicLong loadNanos = new AtomicLong();

    @Override
    public long size() {
      return size.get();
    }

    @Override
    public long hitCount() {
      return hits.get();
    }

    @Override
    public long missCount() {
      return misses.get();
    }

    @Override
    public long putCount() {
      return puts.get();
    }

    @Override
    public long evictionCount() {
      return evictions.get();
    }

    @Override
    public long loadFailureCount() {
      return loadFailures.get();
    }

    @Override
    public long totalLoadTimeNanos() {
      return loadNanos.get();
    }
  }
}
