package org.opentripplanner.graph_builder.module;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.service.TransitModel;

/**
 * Adjust all scheduled times to match the transit model timezone.
 */
public class TimeZoneAdjusterModule implements GraphBuilderModule {

  @Override
  public void buildGraph(
    Graph graph,
    TransitModel transitModel,
    HashMap<Class<?>, Object> extra,
    DataImportIssueStore issueStore
  ) {
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

        final Timetable scheduledTimetable = pattern.getScheduledTimetable();

        scheduledTimetable.getTripTimes().forEach(tripTimes -> tripTimes.timeShift(timeShift));

        scheduledTimetable
          .getFrequencyEntries()
          .forEach(frequencyEntry -> frequencyEntry.tripTimes.timeShift(timeShift));
      });
  }

  @Override
  public void checkInputs() {}
}
