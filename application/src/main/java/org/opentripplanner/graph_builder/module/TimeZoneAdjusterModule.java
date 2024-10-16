package org.opentripplanner.graph_builder.module;

import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.service.TimetableRepository;

/**
 * Adjust all scheduled times to match the transit model timezone.
 */
public class TimeZoneAdjusterModule implements GraphBuilderModule {

  private final TimetableRepository timetableRepository;

  @Inject
  public TimeZoneAdjusterModule(TimetableRepository timetableRepository) {
    this.timetableRepository = timetableRepository;
  }

  @Override
  public void buildGraph() {
    // TODO: We assume that all time zones follow the same DST rules. In reality we need to split up
    //  the services for each DST transition
    final Instant serviceStart = timetableRepository.getTransitServiceStarts().toInstant();
    var graphOffset = Duration.ofSeconds(
      timetableRepository.getTimeZone().getRules().getOffset(serviceStart).getTotalSeconds()
    );

    Map<ZoneId, Duration> agencyShift = new HashMap<>();

    timetableRepository
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

        TripPattern updatedPattern = pattern
          .copy()
          .withScheduledTimeTableBuilder(builder ->
            builder.updateAllTripTimes(tt -> tt.adjustTimesToGraphTimeZone(timeShift))
          )
          .build();
        // replace the original pattern with the updated pattern in the transit model
        timetableRepository.addTripPattern(updatedPattern.getId(), updatedPattern);
      });
    timetableRepository.index();
  }
}
