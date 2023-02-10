package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.model.StopTime;

public record BogusShapeDistanceTraveled(StopTime st) implements DataImportIssue {
  private static final String FMT =
    "The shape_dist_traveled field for stoptime %s is wrong -- " +
    "either it is the same as the value for the previous stoptime, or it is greater " +
    "than the max shape_dist_traveled for the shape in shapes.txt";

  @Override
  public String getMessage() {
    return String.format(FMT, st);
  }
}
