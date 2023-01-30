package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.timetable.Trip;

public record InterliningTeleport(Trip prevTrip, String blockId, int distance)
  implements DataImportIssue {
  private static String FMT = "Interlining trip '%s' on block '%s' implies teleporting %d meters.";

  @Override
  public String getMessage() {
    return String.format(FMT, prevTrip, blockId, distance);
  }

  @Override
  public int getPriority() {
    return -distance;
  }
}
