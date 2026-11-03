package org.opentripplanner.graph_builder.module;

import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.service.TransitRepository;

/**
 * Adjust all scheduled times to match the transit model timezone.
 */
public class TimeZoneAdjusterModule implements GraphBuilderModule {

  private final TransitRepository transitRepository;
  private final TimetableDataImport timetableDataImport;

  @Inject
  public TimeZoneAdjusterModule(
    TransitRepository transitRepository,
    TimetableDataImport timetableDataImport
  ) {
    this.transitRepository = transitRepository;
    this.timetableDataImport = timetableDataImport;
  }

  @Override
  public void buildGraph() {
    // TODO: We assume that all time zones follow the same DST rules. In reality we need to split up
    //  the services for each DST transition
    final Instant serviceStart = timetableDataImport
      .getTripCalendars()
      .transitServiceStarts(transitRepository.getTimeZone());
    var graphOffset = Duration.ofSeconds(
      transitRepository.getTimeZone().getRules().getOffset(serviceStart).getTotalSeconds()
    );

    Map<ZoneId, Duration> agencyShift = new HashMap<>();

    timetableDataImport.getAllTripPatterns().forEach(pattern -> {
      var timeShift = agencyShift.computeIfAbsent(
        pattern.getRoute().getAgency().getTimezone(),
        zoneId ->
          graphOffset.minusSeconds(zoneId.getRules().getOffset(serviceStart).getTotalSeconds())
      );

      if (timeShift.isZero()) {
        return;
      }

      TripPattern updatedPattern = pattern
        .copy()
        .withScheduledTimeTableBuilder(builder -> builder.withAdjustedTimes(timeShift))
        .build();
      // replace the original pattern with the updated pattern in the transit model
      timetableDataImport.addTripPattern(updatedPattern.getId(), updatedPattern);
    });
    transitRepository.index();
  }
}
