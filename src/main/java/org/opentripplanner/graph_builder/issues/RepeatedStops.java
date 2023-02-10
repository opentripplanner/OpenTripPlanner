package org.opentripplanner.graph_builder.issues;

import gnu.trove.list.TIntList;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.timetable.Trip;

public record RepeatedStops(Trip trip, TIntList removedStopSequences) implements DataImportIssue {
  private static final String FMT =
    "Trip %s visits stops repeatedly. Removed duplicates at stop sequence numbers %s.";

  @Override
  public String getMessage() {
    return String.format(FMT, trip.getId(), removedStopSequences);
  }

  @Override
  public int getPriority() {
    return removedStopSequences.size();
  }
}
