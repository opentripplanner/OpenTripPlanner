package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class InvalidVehicleParkingCapacity implements DataImportIssue {

  public static final String FMT =
    "Capacity for osm node %d is not a number: '%s'; it's replaced with '-1' (unknown).";

  private final long osmId;
  private final String capacityValue;

  public InvalidVehicleParkingCapacity(long osmId, String capacityValue) {
    this.osmId = osmId;
    this.capacityValue = capacityValue;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, osmId, capacityValue);
  }

  @Override
  public String getHTMLMessage() {
    return getMessage();
  }
}
