package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmEntity;

public record InvalidVehicleParkingCapacity(OsmEntity entity, String capacityValue)
  implements DataImportIssue {
  private static final String FMT =
    "Capacity for osm node %d is not a number: '%s'; it's replaced with '-1' (unknown).";
  private static final String HTMLFMT =
    "Capacity for osm node <a href='%s'>'%s'</a> is not a number: '%s'; it's replaced with '-1' (unknown).";

  @Override
  public String getMessage() {
    return String.format(FMT, entity.getId(), capacityValue);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, entity.url(), entity.getId(), capacityValue);
  }
}
