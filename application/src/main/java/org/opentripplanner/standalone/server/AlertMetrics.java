package org.opentripplanner.standalone.server;

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
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlertMetrics implements MeterBinder {

  private static final Logger LOG = LoggerFactory.getLogger(AlertMetrics.class);
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final TimetableRepository timetableRepository;
  private MultiGauge statuses;

  public AlertMetrics(TimetableRepository timetableRepository) {
    this.timetableRepository = timetableRepository;
    scheduler.scheduleWithFixedDelay(this::buildMetrics, 0, 1, TimeUnit.MINUTES);
  }

  @Override
  public void bindTo(MeterRegistry meterRegistry) {
    this.statuses = MultiGauge.builder("alerts").register(meterRegistry);
    ApplicationShutdownSupport.addShutdownHook("alert-metrics-shutdown", scheduler::shutdownNow);
  }

  private void buildMetrics() {
    try {
      if (timetableRepository.getTransitAlertService() != null && statuses != null) {
        var rows = summarizeAlerts(timetableRepository.getTransitAlertService());
        statuses.register(rows, true);
      }
    } catch (Exception e) {
      LOG.error("Error building alerts metrics", e);
    }
  }

  private Iterable<MultiGauge.Row<Number>> summarizeAlerts(TransitAlertService alertService) {
    var alerts = alertService.getAllAlerts();

    ImmutableMultimap<AlertTags, TransitAlert> taggedAlerts = alerts
      .stream()
      .collect(
        ImmutableListMultimap.<TransitAlert, AlertTags, TransitAlert>flatteningToImmutableListMultimap(
          AlertTags::of,
          Stream::of
        )
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
