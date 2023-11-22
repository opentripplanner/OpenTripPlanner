package org.opentripplanner.graph_builder.module;

import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.transit.service.TransitModel;

/**
 * Adjust all scheduled times to match the transit model timezone.
 */
public class TimeZoneAdjusterModule implements GraphBuilderModule {

  private final TransitModel transitModel;

  @Inject
  public TimeZoneAdjusterModule(TransitModel transitModel) {
    this.transitModel = transitModel;
  }

  @Override
  public void buildGraph() {
    // TODO: We assume that all time zones follow the same DST rules. In reality we need to split up
    //  the services for each DST transition
    final Instant serviceStart = transitModel.getTransitServiceStarts().toInstant();
    var graphOffset = Duration.ofSeconds(
      transitModel.getTimeZone().getRules().getOffset(serviceStart).getTotalSeconds()
    );

    Map<ZoneId, Duration> agencyShift = new HashMap<>();

    transitModel
      .getAllTripPatterns()
      .forEach(pattern -> {
        var timeShift = agencyShift.computeIfAbsent(
          pattern.getRoute().getAgency().getTimezone(),
          zoneId ->
            (graphOffset.minusSeconds(zoneId.getRules().getOffset(serviceStart).getTotalSeconds()))
        );

        if (timeShift.isZero()) {
          return;
        }

        pattern
          .getScheduledTimetable()
          .updateAllTripTimes(it -> it.adjustTimesToGraphTimeZone(timeShift));
      });
  }
}
