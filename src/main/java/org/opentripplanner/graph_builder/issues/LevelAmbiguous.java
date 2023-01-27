package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public record LevelAmbiguous(String layerName, long osmWayId) implements DataImportIssue {
  private static String FMT =
    "Could not infer floor number for layer called '%s' at %s. " +
    "Vertical movement will still be possible, but elevator cost might be incorrect. " +
    "Consider an OSM level map.";
  private static String HTMLFMT =
    "Could not infer floor number for layer called <a href='http://www.openstreetmap.org/way/%d'>'%s' (%d)</a>" +
    "Vertical movement will still be possible, but elevator cost might be incorrect. " +
    "Consider an OSM level map.";

  @Override
  public String getMessage() {
    return String.format(FMT, layerName, osmWayId);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, osmWayId, layerName, osmWayId);
  }
}
