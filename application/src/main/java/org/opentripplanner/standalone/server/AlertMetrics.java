package org.opentripplanner.standalone.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.opentripplanner.framework.application.ApplicationShutdownSupport;
import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.service.transitalert.TransitAlertRepository;
import org.opentripplanner.service.transitalert.TransitAlertRepositorySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A binder that creates metrics about the alerts present in the system. The metrics are read from
 * the alert repository twice a minute by a background thread.
 */
public class AlertMetrics implements MeterBinder {

  private static final Logger LOG = LoggerFactory.getLogger(AlertMetrics.class);
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final RepositoryRegistry repositoryRegistry;
  private final RepositoryHandle<
    TransitAlertRepositorySnapshot,
    TransitAlertRepository
  > transitAlertRepositoryHandle;
  private MultiGauge statuses;

  public AlertMetrics(
    RepositoryRegistry repositoryRegistry,
    RepositoryHandle<
      TransitAlertRepositorySnapshot,
      TransitAlertRepository
    > transitAlertRepositoryHandle
  ) {
    this.repositoryRegistry = repositoryRegistry;
    this.transitAlertRepositoryHandle = transitAlertRepositoryHandle;
    scheduler.scheduleWithFixedDelay(this::recordMetrics, 0, 30, TimeUnit.SECONDS);
  }

  @Override
  public void bindTo(MeterRegistry meterRegistry) {
    this.statuses = MultiGauge.builder("alerts")
      .description("Total number of alerts (sourced from GTFS-Alerts and SIRI-SX) in the system.")
      .register(meterRegistry);
    ApplicationShutdownSupport.addShutdownHook("alert-metrics-shutdown", scheduler::shutdownNow);
  }

  /**
   * Creates a {@link MultiGauge} for the alerts and publishes ("registers" in Micrometer language)
   * to the repository.
   */
  @VisibleForTesting
  void recordMetrics() {
    try {
      if (statuses != null) {
        // Resolve a fresh scope on every run so that each sample sees the latest committed alerts.
        var snapshot = transitAlertRepositoryHandle.repositorySnapshot(repositoryRegistry.scope());
        var rows = summarizeAlerts(snapshot);
        statuses.register(rows, true);
      }
    } catch (Exception e) {
      LOG.error("Error building alerts metrics", e);
    }
  }

  private Iterable<MultiGauge.Row<Number>> summarizeAlerts(
    TransitAlertRepositorySnapshot snapshot
  ) {
    var alerts = snapshot.getAllAlerts();

    ImmutableMultimap<AlertTags, TransitAlert> taggedAlerts = alerts
      .stream()
      .collect(
        ImmutableListMultimap.<
          TransitAlert,
          AlertTags,
          TransitAlert
        >flatteningToImmutableListMultimap(AlertTags::of, Stream::of)
      );
    return taggedAlerts
      .keySet()
      .stream()
      .map(alertTags -> {
        var tags = alertTags.toTags();
        var count = taggedAlerts.get(alertTags).size();
        return MultiGauge.Row.of(tags, count);
      })
      .toList();
  }

  /**
   * The tags that can be exported for an alert.
   */
  record AlertTags(
    String feedId,
    String siriCodespace,
    AlertSeverity severity,
    AlertEffect effect,
    AlertCause cause
  ) {
    static AlertTags of(TransitAlert a) {
      return new AlertTags(
        a.getId().getFeedId(),
        a.siriCodespace(),
        a.severity(),
        a.effect(),
        a.cause()
      );
    }

    Tags toTags() {
      var tags = new ArrayList<Tag>(5);
      tags.add(Tag.of("feedId", feedId));
      if (siriCodespace != null) {
        tags.add(Tag.of("siriCodespace", siriCodespace));
      }
      if (severity != null) {
        tags.add(Tag.of("severity", severity.name()));
      }
      if (effect != null) {
        tags.add(Tag.of("effect", effect.name()));
      }
      if (cause != null) {
        tags.add(Tag.of("cause", cause.name()));
      }
      return Tags.of(tags);
    }
  }
}
