package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class LevelAmbiguous implements DataImportIssue {

  public static final String FMT =
    "Could not infer floor number for layer called '%s' at %s. " +
    "Vertical movement will still be possible, but elevator cost might be incorrect. " +
    "Consider an OSM level map.";
  public static final String HTMLFMT =
    "Could not infer floor number for layer called <a href='http://www.openstreetmap.org/way/%d'>'%s' (%d)</a>" +
    "Vertical movement will still be possible, but elevator cost might be incorrect. " +
    "Consider an OSM level map.";

  final String layerName;

  final long osmWayId;

  public LevelAmbiguous(String layerName, long osmWayId) {
    this.layerName = layerName;
    this.osmWayId = osmWayId;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, layerName, osmWayId);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, osmWayId, layerName, osmWayId);
  }
}
