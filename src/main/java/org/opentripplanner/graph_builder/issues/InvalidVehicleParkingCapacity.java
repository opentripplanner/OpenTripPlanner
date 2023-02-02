package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public record InvalidVehicleParkingCapacity(long osmId, String capacityValue)
  implements DataImportIssue {
  private static String FMT =
    "Capacity for osm node %d is not a number: '%s'; it's replaced with '-1' (unknown).";

  @Override
  public String getMessage() {
    return String.format(FMT, osmId, capacityValue);
  }
}
