package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.timetable.Trip;

public class InterliningTeleport implements DataImportIssue {

  public static final String FMT =
    "Interlining trip '%s' on block '%s' implies teleporting %d meters.";

  final Trip prevTrip;
  final String blockId;
  final int distance;

  public InterliningTeleport(Trip prevTrip, String blockId, int distance) {
    this.prevTrip = prevTrip;
    this.blockId = blockId;
    this.distance = distance;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, prevTrip, blockId, distance);
  }
}
