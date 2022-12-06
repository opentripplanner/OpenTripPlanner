package org.opentripplanner.openstreetmap.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class FloorNumberUnknownGuessedFromAltitude implements DataImportIssue {

  public static final String FMT =
    "Could not determine floor number for layer %s. Guessed %s (0-based) from altitude.";

  final String layer;
  final Integer floorNumber;

  public FloorNumberUnknownGuessedFromAltitude(String layer, Integer floorNumber) {
    this.layer = layer;
    this.floorNumber = floorNumber;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, layer, floorNumber);
  }
}
