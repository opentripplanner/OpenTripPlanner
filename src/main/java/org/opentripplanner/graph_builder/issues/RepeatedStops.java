package org.opentripplanner.graph_builder.issues;

import gnu.trove.list.TIntList;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.timetable.Trip;

public class RepeatedStops implements DataImportIssue {

  public static final String FMT =
    "Trip %s visits stops repeatedly. Removed duplicates at stop sequence numbers %s.";

  public final Trip trip;

  public final TIntList removedStopSequences;

  public RepeatedStops(Trip trip, TIntList removedStopSequences) {
    this.trip = trip;
    this.removedStopSequences = removedStopSequences;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, trip.getId(), removedStopSequences);
  }
}
