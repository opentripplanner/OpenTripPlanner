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
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.service.TimetableRepository;

public class AlertMetrics implements MeterBinder {


  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final TimetableRepository timetableRepository;
  private MultiGauge statuses;

  public AlertMetrics(TimetableRepository timetableRepository) {
    this.timetableRepository = timetableRepository;
  }

  @Override
  public void bindTo(MeterRegistry meterRegistry) {
    this.statuses = MultiGauge.builder("alerts.count").register(meterRegistry);
    scheduler.scheduleAtFixedRate(this::buildMetrics, 1, 1, TimeUnit.MINUTES);
    buildMetrics();
  }

  private void buildMetrics() {
    var rows= summarizeAlerts();
    statuses.register(rows, true);
  }

  private Iterable<MultiGauge.Row<Number>> summarizeAlerts() {
    var alerts = timetableRepository.getTransitAlertService().getAllAlerts();

    ImmutableMultimap<AlertTags, TransitAlert> taggedAlerts = alerts.stream().collect(
      ImmutableListMultimap.<TransitAlert, AlertTags, TransitAlert>flatteningToImmutableListMultimap(
        AlertTags::of,
        Stream::of
      )
    );
    return taggedAlerts.keySet().stream().map(alertTags -> {
      var tags = alertTags.toTags();
      var count = taggedAlerts.get(alertTags).size();
      return MultiGauge.Row.of(tags, count);
    }).toList();
  }

  record AlertTags(String siriCodespace, AlertSeverity severity, String feedId){
    public static AlertTags of(TransitAlert a) {
      return new AlertTags(a.siriCodespace(), a.severity(), a.getId().getFeedId());
    }

    Tags toTags() {
      var tags = new ArrayList<Tag>();
      tags.add(Tag.of("severity", feedId));
      if(siriCodespace != null) {
        tags.add(Tag.of("siriCodespace", siriCodespace));
      }
      if(severity != null) {
        tags.add(Tag.of("severity", severity.name()));
      }

      return Tags.of(tags);
    }
  }
}
