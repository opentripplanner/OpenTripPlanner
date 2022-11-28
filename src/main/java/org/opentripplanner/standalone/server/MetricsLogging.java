package org.opentripplanner.standalone.server;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.cache.GuavaCacheMetrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmCompilationMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import javax.inject.Inject;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.transit.service.TransitModel;

/**
 * This class is responsible for wiring up various metrics to micrometer, which we use for
 * performance logging, through the Actuator API.
 */
public class MetricsLogging {

  @Inject
  public MetricsLogging(TransitModel transitModel, RaptorConfig<TripSchedule> raptorConfig) {
    new ClassLoaderMetrics().bindTo(Metrics.globalRegistry);
    new FileDescriptorMetrics().bindTo(Metrics.globalRegistry);
    new JvmCompilationMetrics().bindTo(Metrics.globalRegistry);
    new JvmGcMetrics().bindTo(Metrics.globalRegistry);
    new JvmHeapPressureMetrics().bindTo(Metrics.globalRegistry);
    new JvmInfoMetrics().bindTo(Metrics.globalRegistry);
    new JvmMemoryMetrics().bindTo(Metrics.globalRegistry);
    new JvmThreadMetrics().bindTo(Metrics.globalRegistry);
    new LogbackMetrics().bindTo(Metrics.globalRegistry);
    new ProcessorMetrics().bindTo(Metrics.globalRegistry);
    new UptimeMetrics().bindTo(Metrics.globalRegistry);

    if (transitModel.getTransitLayer() != null) {
      new GuavaCacheMetrics(
        transitModel.getTransitLayer().getTransferCache().getTransferCache(),
        "raptorTransfersCache",
        List.of(Tag.of("cache", "raptorTransfers"))
      )
        .bindTo(Metrics.globalRegistry);
    }
    new ExecutorServiceMetrics(
      ForkJoinPool.commonPool(),
      "commonPool",
      List.of(Tag.of("pool", "commonPool"))
    )
      .bindTo(Metrics.globalRegistry);

    if (transitModel.getUpdaterManager() != null) {
      new ExecutorServiceMetrics(
        transitModel.getUpdaterManager().getUpdaterPool(),
        "graphUpdaters",
        List.of(Tag.of("pool", "graphUpdaters"))
      )
        .bindTo(Metrics.globalRegistry);

      new ExecutorServiceMetrics(
        transitModel.getUpdaterManager().getScheduler(),
        "graphUpdateScheduler",
        List.of(Tag.of("pool", "graphUpdateScheduler"))
      )
        .bindTo(Metrics.globalRegistry);
    }

    if (raptorConfig.isMultiThreaded()) {
      new ExecutorServiceMetrics(
        raptorConfig.threadPool(),
        "raptorHeuristics",
        List.of(Tag.of("pool", "raptorHeuristics"))
      )
        .bindTo(Metrics.globalRegistry);
    }
  }
}
