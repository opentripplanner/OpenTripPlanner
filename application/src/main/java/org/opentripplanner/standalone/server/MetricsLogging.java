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
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueSummary;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.transit.service.TimetableRepository;

/**
 * This class is responsible for wiring up various metrics to micrometer, which we use for
 * performance logging, through the Actuator API.
 */
public class MetricsLogging {

  @Inject
  public MetricsLogging(
    TimetableRepository timetableRepository,
    RaptorConfig<TripSchedule> raptorConfig,
    DataImportIssueSummary issueSummary
  ) {
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
    if (OTPFeature.AlertMetrics.isOn()) {
      new AlertMetrics(timetableRepository::getTransitAlertService).bindTo(Metrics.globalRegistry);
    }

    if (timetableRepository.getRaptorTransitData() != null) {
      new GuavaCacheMetrics(
        timetableRepository.getRaptorTransitData().getTransferCache().getTransferCache(),
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

    if (timetableRepository.getUpdaterManager() != null) {
      new ExecutorServiceMetrics(
        timetableRepository.getUpdaterManager().getPollingUpdaterPool(),
        "pollingGraphUpdaters",
        List.of(Tag.of("pool", "pollingGraphUpdaters"))
      )
        .bindTo(Metrics.globalRegistry);

      new ExecutorServiceMetrics(
        timetableRepository.getUpdaterManager().getNonPollingUpdaterPool(),
        "nonPollingGraphUpdaters",
        List.of(Tag.of("pool", "nonPollingGraphUpdaters"))
      )
        .bindTo(Metrics.globalRegistry);

      new ExecutorServiceMetrics(
        timetableRepository.getUpdaterManager().getScheduler(),
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

    final Map<String, Long> issueCount = issueSummary.asMap();

    var totalIssues = issueCount.values().stream().mapToLong(i -> i).sum();
    Metrics.globalRegistry.gauge("graph_build_issues_total", totalIssues);

    issueCount.forEach((issueType, number) ->
      Metrics.globalRegistry.gauge("graph_build_issues", List.of(Tag.of("type", issueType)), number)
    );
  }
}
