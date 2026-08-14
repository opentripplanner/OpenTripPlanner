package org.opentripplanner.framework.transaction.internal;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards the "graphUpdateScheduler" metric wired up in
 * {@link org.opentripplanner.standalone.server.MetricsLogging}: micrometer can only read queue and
 * pool-size gauges from an unwrapped {@link java.util.concurrent.ThreadPoolExecutor}. If the writer
 * executor is created through {@code Executors.newSingleThreadExecutor(..)}, micrometer needs
 * reflective access to JDK internals to unwrap it, which fails silently at runtime and drops the
 * queue gauges.
 */
class UpdateManagerMetricsTest {

  @Test
  void writerThreadExecutorExposesQueueMetrics() {
    var threadFactory = new ThreadFactoryBuilder().setNameFormat("metrics-test").build();
    var updateManager = TransactionFactory.createUpdateManagerWithAtomicCommits(
      "metrics-test",
      TransactionFactory.createRepositoryRegistry(),
      threadFactory
    );
    try {
      var registry = new SimpleMeterRegistry();
      new ExecutorServiceMetrics(
        updateManager.writerThreadExecutor(),
        "graphUpdateScheduler",
        List.of(Tag.of("pool", "graphUpdateScheduler"))
      ).bindTo(registry);

      var queuedTasks = registry.find("executor.queued").tag("name", "graphUpdateScheduler");
      assertThat(queuedTasks.gauge()).isNotNull();
    } finally {
      updateManager.shutdown();
    }
  }
}
